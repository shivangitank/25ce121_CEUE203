package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.DailyLimitExceededException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.annotation.Id;
import model.annotation.MaxLength;
import model.annotation.Positive;

/**
 * Base class for every MiniBank account. The balance is private and changes
 * only through deposit() and withdraw() - there is deliberately no public
 * setBalance(). Each subclass decides its own interest rate and withdrawal
 * rule; withdraw() calls canWithdraw(), so the right rule is picked at run time.
 * Every failure is reported with an exception that explains what went wrong.
 * Methods that read or change the balance are synchronized, so only one
 * thread at a time can run them on the same account (no lost updates).
 * Accounts sort by account number (Comparable) and keep their own history.
 */
public abstract class Account implements Transactable, InterestBearing, Serializable, Comparable<Account> {
    private static final long serialVersionUID = 1L;

    public static final long DAILY_WITHDRAWAL_LIMIT = 1_00_000;
    public static final long PREMIUM_DAILY_WITHDRAWAL_LIMIT = 5_00_000;
    private static final DateTimeFormatter HISTORY_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static int accountCounter = 0;

    @Id
    private final String accountNumber;
    @MaxLength(30)
    private String ownerName;
    @Positive
    private long balance;          // whole rupees
    private boolean active;
    private long withdrawnToday;
    private LocalDate withdrawalDay;
    private ArrayList<String> history = new ArrayList<>();
    private transient String lastActivity;   // transient: not written to the file, null after loading

    protected Account(String ownerName, long openingBalance) {
        this.accountNumber = generateAccountNumber();
        this.ownerName = ownerName;
        this.balance = openingBalance;
        this.active = true;
        record("OPENED", openingBalance);
    }

    protected Account(String ownerName) {
        this(ownerName, 0);
    }

    /** Re-creates an account whose number is already known (for example, one read back from storage). */
    protected Account(String accountNumber, String ownerName, long balance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = balance;
        this.active = true;
        record("OPENED", balance);
        reserveNumber(accountNumber);
    }

    /** Yearly interest rate in percent. */
    @Override
    public abstract double interestRate();

    /** True if this account's rules allow taking out the given amount now. */
    public abstract boolean canWithdraw(long amount);

    /** How much could be withdrawn right now under this account's rules (ignoring the daily limit). */
    public long getAvailableBalance() {
        return balance;
    }

    /**
     * Why a withdrawal is blocked when the reason is not simply a low balance
     * (e.g. a locked fixed deposit). Null means "not enough money".
     */
    protected String withdrawalBlockedReason() {
        return null;
    }

    /** Produces numbers such as AC0001, AC0002, ... from one counter shared by every account. */
    private static synchronized String generateAccountNumber() {
        accountCounter++;
        return String.format("AC%04d", accountCounter);
    }

    /**
     * Called by Java while an account is being read back from a file. Constructors
     * do not run during deserialization, so the counter is moved past this number here.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (history == null) {
            history = new ArrayList<>();
        }
        reserveNumber(accountNumber);
    }

    /** Moves the counter past an existing number so a generated number never repeats it. */
    private static synchronized void reserveNumber(String accountNumber) {
        try {
            int used = Integer.parseInt(accountNumber.substring(2));
            if (used > accountCounter) {
                accountCounter = used;
            }
        } catch (RuntimeException notInAcFormat) {
            // numbers not in the ACnnnn format do not affect the counter
        }
    }

    /** Adds money to the account. */
    @Override
    public synchronized void deposit(long amount) throws InvalidAmountException {
        credit(amount, "DEPOSIT");
    }

    /**
     * Takes money out of the account if the subclass rule (canWithdraw) and
     * the daily limit allow it. The balance is unchanged when an exception is thrown.
     * @return true on success
     */
    @Override
    public synchronized boolean withdraw(long amount)
            throws InsufficientFundsException, InvalidAmountException, DailyLimitExceededException {
        debit(amount, "WITHDRAW");
        return true;
    }

    /** Deposit logic shared by deposit() and transfer(); the caller holds this account's lock. */
    private void credit(long amount, String description) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        balance += amount;
        record(description, amount);
    }

    /** Withdrawal logic shared by withdraw() and transfer(); the caller holds this account's lock. */
    private void debit(long amount, String description)
            throws InsufficientFundsException, InvalidAmountException, DailyLimitExceededException {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        if (!canWithdraw(amount)) {
            long shortfall = Math.max(amount - Math.max(getAvailableBalance(), 0), 0);
            String reason = withdrawalBlockedReason();
            if (reason != null) {
                throw new InsufficientFundsException(accountNumber, amount, shortfall, reason);
            }
            throw new InsufficientFundsException(accountNumber, amount, shortfall);
        }
        LocalDate today = LocalDate.now();
        if (!today.equals(withdrawalDay)) {
            withdrawalDay = today;          // a new day starts a new limit
            withdrawnToday = 0;
        }
        if (withdrawnToday + amount > dailyLimit()) {
            throw new DailyLimitExceededException(accountNumber, dailyLimit(), withdrawnToday);
        }
        balance -= amount;
        withdrawnToday += amount;
        record(description, -amount);
    }

    /** Adds a line such as "27-10-2026 10:15  DEPOSIT   +500  bal 5500" to the history. */
    private void record(String description, long signedAmount) {
        history.add(String.format("%s  %-20s %+9d  bal %d",
                LocalDateTime.now().format(HISTORY_TIME), description, signedAmount, balance));
        lastActivity = description + " " + Math.abs(signedAmount);
    }

    /**
     * Moves money to another account. It is all-or-nothing: if the deposit
     * side fails after the withdrawal succeeded, the money is put back and the
     * original exception is re-thrown to the caller.
     * <p>
     * Both accounts are locked for the whole transfer, always in the same order
     * (smaller account number first). Two opposite transfers, A to B and B to A,
     * therefore ask for the locks in the same order and can never deadlock.
     */
    public void transfer(Account to, long amount) throws BankException {
        if (to == null) {
            throw new AccountNotFoundException("(missing target account)");
        }
        if (to.equals(this)) {
            throw new BankException("Cannot transfer from " + accountNumber + " to itself");
        }
        Account first = this.compareTo(to) < 0 ? this : to;
        Account second = first == this ? to : this;
        synchronized (first) {
            synchronized (second) {
                boolean withdrawn = false;
                boolean completed = false;
                try {
                    debit(amount, "TRANSFER TO " + to.accountNumber);
                    withdrawn = true;
                    to.credit(amount, "TRANSFER FROM " + accountNumber);
                    completed = true;
                } catch (BankException e) {
                    if (withdrawn) {
                        refund(amount);
                    }
                    throw e;
                } finally {
                    lastActivity = (completed ? "Transferred " : "Failed transfer of ") + amount + " to " + to.accountNumber;
                }
            }
        }
    }

    /** Puts back money taken by a withdrawal that is being rolled back. */
    private synchronized void refund(long amount) {
        balance += amount;
        withdrawnToday -= amount;
        record("REVERSAL", amount);
    }

    /** A copy of this account's transaction history, oldest first. */
    public synchronized List<String> getHistory() {
        return new ArrayList<>(history);
    }

    /** Higher cap for accounts marked with the Premium interface. */
    public long dailyLimit() {
        return this instanceof Premium ? PREMIUM_DAILY_WITHDRAWAL_LIMIT : DAILY_WITHDRAWAL_LIMIT;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public synchronized long getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }

    /** Freezes (false) or re-opens (true) the account. */
    public void setActive(boolean active) {
        this.active = active;
    }

    /** A short note about the last operation, e.g. "Deposited 500". */
    public synchronized String getLastActivity() {
        return lastActivity;
    }

    /** "SavingsAccount" -> "Savings", "FixedDepositAccount" -> "FixedDeposit". */
    public String getAccountType() {
        return getClass().getSimpleName().replace("Account", "");
    }

    /** One readable line, e.g. "AC0001 | Riya | 4000 | ACTIVE". */
    @Override
    public String toString() {
        return accountNumber + " | " + ownerName + " | " + balance + " | " + (active ? "ACTIVE" : "INACTIVE");
    }

    /** Two accounts are the same account when their account numbers are equal. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Account)) {
            return false;
        }
        Account other = (Account) o;
        return accountNumber.equals(other.accountNumber);
    }

    /** Uses the same field as equals(), so equal accounts always have equal hash codes. */
    @Override
    public int hashCode() {
        return accountNumber.hashCode();
    }

    /** Natural order: by account number (AC0001 before AC0002). Consistent with equals(). */
    @Override
    public int compareTo(Account other) {
        return accountNumber.compareTo(other.accountNumber);
    }
}
