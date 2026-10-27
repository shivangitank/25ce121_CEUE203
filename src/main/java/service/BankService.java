package service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InvalidAmountException;
import exception.ValidationException;
import model.Account;
import model.AccountType;
import model.BankInfo;
import model.Command;
import model.CurrentAccount;
import model.Customer;
import model.FixedDepositAccount;
import model.SavingsAccount;
import model.TransactionType;
import model.WithdrawRule;
import model.annotation.Audit;
import util.AnnotationValidator;
import util.ReportGenerator;
import util.StatePersister;
import util.StatementFormatter;
import util.TransactionLog;
import util.Validator;

/**
 * The service layer: wires the repositories, validation, exceptions,
 * persistence, logging and the thread pool together. Every successful change
 * is logged and saved straight away, so nothing is lost if the program is
 * closed unexpectedly.
 */
public class BankService implements BankOperations {
    private static final BankInfo BANK = new BankInfo("MiniBank", "CHARUSAT Campus, Changa");

    private final Path accountsFile;
    private final Path customersFile;
    private final Path logsDir;
    private final Path reportsDir;
    private final Repository<Account> accounts = new Repository<>();
    private final Repository<Customer> customers = new Repository<>();
    private final WithdrawRule withdrawRule;

    public BankService(Path dataDir) {
        this(dataDir, WithdrawPolicy.STANDARD);
    }

    public BankService(Path dataDir, WithdrawRule withdrawRule) {
        this.accountsFile = dataDir.resolve("accounts.dat");
        this.customersFile = dataDir.resolve("customers.dat");
        this.logsDir = dataDir.resolve("logs");
        this.reportsDir = dataDir.resolve("reports");
        this.withdrawRule = withdrawRule;
    }

    @Override
    public BankInfo getBankInfo() {
        return BANK;
    }

    @Override
    public synchronized int loadState() throws IOException {
        accounts.saveAll(StatePersister.load(accountsFile));
        customers.saveAll(StatePersister.loadCustomers(customersFile));
        return accounts.count();
    }

    @Override
    public synchronized void saveState() throws IOException {
        Account[] allAccounts = accounts.findAll().toArray(new Account[0]);
        Arrays.sort(allAccounts);
        StatePersister.save(allAccounts, accountsFile);
        StatePersister.saveCustomers(customers.findAll().toArray(new Customer[0]), customersFile);
    }

    @Audit
    @Override
    public synchronized Account openAccount(AccountType type, String ownerName, String mobile, String email,
                                            long openingDeposit) throws BankException {
        String name = ownerName == null ? "" : ownerName.trim();
        if (!Validator.isValidName(name)) {
            throw new ValidationException("Invalid name \"" + name + "\": use letters and spaces, at most 30 characters");
        }
        if (!Validator.isValidMobile(mobile)) {
            throw new ValidationException("Invalid mobile number \"" + mobile + "\": 10 digits starting with 6, 7, 8 or 9");
        }
        if (!Validator.isValidEmail(email)) {
            throw new ValidationException("Invalid email address \"" + email + "\"");
        }
        if (openingDeposit <= 0) {
            throw new InvalidAmountException(openingDeposit);
        }
        if (type == AccountType.SAVINGS && openingDeposit < SavingsAccount.DEFAULT_MIN_BALANCE) {
            throw new ValidationException("A savings account needs an opening deposit of at least "
                    + StatementFormatter.rupees(SavingsAccount.DEFAULT_MIN_BALANCE));
        }

        Account account = switch (type) {
            case SAVINGS -> new SavingsAccount(name, openingDeposit);
            case CURRENT -> new CurrentAccount(name, openingDeposit);
            case FIXED_DEPOSIT -> new FixedDepositAccount(name, openingDeposit);
        };
        String[] errors = AnnotationValidator.validate(account);       // @Positive / @MaxLength safety net
        if (errors.length > 0) {
            throw new ValidationException(String.join("; ", errors));
        }

        Customer customer = new Customer(name, email, mobile);
        account.setCustomerId(customer.getCustomerId());
        customers.save(customer);
        accounts.save(account);
        log(TransactionLog.entry(TransactionType.DEPOSIT, account.getAccountNumber(), openingDeposit));
        autoSave();
        return account;
    }

    @Audit
    @Override
    public synchronized long deposit(String accountNumber, long amount) throws BankException {
        Account account = findAccount(accountNumber);
        account.deposit(amount);
        log(TransactionLog.entry(TransactionType.DEPOSIT, account.getAccountNumber(), amount));
        autoSave();
        return account.getBalance();
    }

    @Audit
    @Override
    public synchronized long withdraw(String accountNumber, long amount) throws BankException {
        Account account = findAccount(accountNumber);
        checkWithdrawRule(account, amount);
        account.withdraw(amount);
        log(TransactionLog.entry(TransactionType.WITHDRAW, account.getAccountNumber(), amount));
        autoSave();
        return account.getBalance();
    }

    @Audit
    @Override
    public synchronized void transfer(String fromAccount, String toAccount, long amount) throws BankException {
        Account from = findAccount(fromAccount);
        Account to = findAccount(toAccount);
        checkWithdrawRule(from, amount);
        from.transfer(to, amount);
        log(TransactionLog.transferEntry(from.getAccountNumber(), to.getAccountNumber(), amount));
        autoSave();
    }

    @Override
    public synchronized String statement(String accountNumber) throws BankException {
        Account account = findAccount(accountNumber);
        Customer customer = account.getCustomerId() == null ? null : customers.findById(account.getCustomerId());
        return StatementFormatter.buildStatement(account, customer);
    }

    /** All accounts in account-number order. */
    @Override
    public synchronized List<Account> listAccounts() {
        List<Account> all = accounts.findAll();
        Collections.sort(all);
        return all;
    }

    /** Accounts whose owner name contains the text, ignoring case. */
    @Override
    public synchronized List<Account> searchByOwner(String namePart) {
        String wanted = namePart == null ? "" : namePart.trim().toLowerCase(Locale.ROOT);
        List<Account> found = accounts.findAll(a -> a.getOwnerName().toLowerCase(Locale.ROOT).contains(wanted));
        Collections.sort(found);
        return found;
    }

    /**
     * Runs the commands on the TransactionProcessor's thread pool. The pool
     * works on a ConcurrentHashMap of the same Account objects; each account's
     * synchronized methods keep its balance correct.
     */
    @Audit
    @Override
    public synchronized BatchResult processBatch(List<Command> commands) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        TransactionProcessor processor = new TransactionProcessor(accounts.findAll());
        processor.setWithdrawRule(withdrawRule);
        for (Command command : commands) {
            processor.submit(command);
        }
        processor.stop();
        Path logFile = TransactionLog.dailyFile(logsDir, LocalDate.now());
        for (Command done : processor.getCompletedCommands()) {
            TransactionLog.append(logFile, TransactionLog.entry(done.type(), done.accountNumber(), done.amount()));
        }
        saveState();
        return new BatchResult(commands.size(), processor.getSucceededCount(),
                new ArrayList<>(processor.getFailures()), processor.getTasksPerThread(),
                System.currentTimeMillis() - start);
    }

    @Audit
    @Override
    public Path generateReport() throws IOException {
        Path report = reportsDir.resolve("eod-report-" + LocalDate.now() + ".txt");
        ReportGenerator.generate(logsDir, report);
        return report;
    }

    /** @throws AccountNotFoundException if there is no such account */
    public synchronized Account findAccount(String accountNumber) throws AccountNotFoundException {
        String key = accountNumber == null ? "" : accountNumber.trim().toUpperCase(Locale.ROOT);
        Account account = accounts.findById(key);
        if (account == null) {
            throw new AccountNotFoundException(key);
        }
        return account;
    }

    public synchronized Customer findCustomer(String customerId) {
        return customers.findById(customerId);
    }

    public Path getLogsDir() {
        return logsDir;
    }

    private void checkWithdrawRule(Account account, long amount) throws BankException {
        if (!withdrawRule.allow(account, amount)) {
            String reason = account.isActive()
                    ? "the limit per withdrawal is " + StatementFormatter.rupees(WithdrawPolicy.transactionLimitFor(account))
                    : "the account is frozen";
            throw new BankException("Withdrawal of " + amount + " from " + account.getAccountNumber()
                    + " is not allowed: " + reason);
        }
    }

    private void log(String line) throws BankStorageException {
        try {
            TransactionLog.append(TransactionLog.dailyFile(logsDir, LocalDate.now()), line);
        } catch (IOException e) {
            throw new BankStorageException("Could not write the transaction log", e);
        }
    }

    private void autoSave() throws BankStorageException {
        try {
            saveState();
        } catch (IOException e) {
            throw new BankStorageException("Could not save accounts", e);
        }
    }

    /** A file problem met while completing a bank operation. */
    public static class BankStorageException extends BankException {
        private static final long serialVersionUID = 1L;

        public BankStorageException(String message, IOException cause) {
            super(message + ": " + cause.getMessage(), cause);
        }
    }
}
