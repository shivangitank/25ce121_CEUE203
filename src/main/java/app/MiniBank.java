package app;

import static util.Validator.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;

import exception.BankException;
import model.Account;
import model.AccountType;
import model.BankInfo;
import model.Command;
import model.TransactionType;
import service.AuditProxy;
import service.BankOperations;
import service.BankService;
import service.BatchResult;
import util.CommandParser;
import util.StatementFormatter;

/**
 * MiniBank - console banking application (CEUE203 OOP mini project).
 * The user interface layer: it reads and validates input, calls the bank
 * through the BankOperations interface and prints the result. Saved accounts
 * are loaded at start-up and saved again on exit.
 */
public class MiniBank {
    private static final String WORKING_HOURS_TEXT =
            "Working hours: Mon-Fri 10:00 AM - 4:00 PM, Sat 10:00 AM - 1:00 PM (closed on Sundays and bank holidays)";

    private final BankOperations bank;
    private final Scanner scanner;

    public MiniBank(BankOperations bank, Scanner scanner) {
        this.bank = bank;
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        Path dataDir = Path.of(args.length > 0 ? args[0] : "data");
        BankOperations bank = AuditProxy.wrap(new BankService(dataDir), dataDir.resolve("audit").resolve("audit-log.txt"));
        new MiniBank(bank, new Scanner(System.in)).run();
    }

    public void run() {
        printHeader(bank.getBankInfo());
        try {
            int loaded = bank.loadState();
            System.out.println(loaded == 0 ? "No saved accounts yet - starting fresh."
                    : "Loaded " + loaded + " saved account(s).");
        } catch (IOException e) {
            System.out.println("Could not load saved data (" + e.getMessage() + ") - starting with no accounts.");
        }

        MenuOption choice;
        do {
            printMenu();
            choice = readChoice();
            try {
                String result = switch (choice) {
                    case OPEN_ACCOUNT -> openAccount();
                    case DEPOSIT -> deposit();
                    case WITHDRAW -> withdraw();
                    case TRANSFER -> transfer();
                    case STATEMENT -> statement();
                    case LIST_ACCOUNTS -> listAccounts();
                    case SEARCH_BY_OWNER -> searchByOwner();
                    case BATCH -> processBatch();
                    case REPORT -> report();
                    case WORKING_HOURS -> WORKING_HOURS_TEXT;
                    case EXIT -> exit();
                };
                System.out.println(result);
            } catch (BankException e) {
                System.out.println("Sorry - " + e.getMessage());
            } catch (CancelledException e) {
                System.out.println("Cancelled - back to the menu.");
            } catch (IOException e) {
                System.out.println("File error: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted.");
            }
        } while (choice != MenuOption.EXIT);
    }

    // ----- menu actions: each returns the message to print -----

    private String openAccount() throws BankException {
        System.out.println("Account types:");
        AccountType[] types = AccountType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.println("  " + (i + 1) + ". " + types[i].getLabel() + " - " + types[i].getDescription());
        }
        String typeChoice = askValid("Account type (1-" + types.length + "): ",
                text -> text.matches("[1-" + types.length + "]"), "Please enter a number from the list.");
        AccountType type = types[Integer.parseInt(typeChoice) - 1];
        String name = askValid("Owner name: ", text -> isValidName(text),
                "Use letters and spaces only (at most 30 characters).");
        String mobile = askValid("Mobile number: ", text -> isValidMobile(text),
                "Enter 10 digits starting with 6, 7, 8 or 9.");
        String email = askValid("Email: ", text -> isValidEmail(text), "That does not look like an email address.");
        long deposit = askAmount("Opening deposit (Rs.): ");

        Account account = bank.openAccount(type, name, mobile, email, deposit);
        return "Opened " + type.getLabel() + " account " + account.getAccountNumber() + " for " + account.getOwnerName()
                + " with " + StatementFormatter.rupees(account.getBalance()) + " (customer id " + account.getCustomerId() + ").";
    }

    private String deposit() throws BankException {
        Command command = askCommand(TransactionType.DEPOSIT, "Deposit to account: ", "Amount to deposit (Rs.): ");
        long balance = bank.deposit(command.accountNumber(), command.amount());
        return "Deposited " + StatementFormatter.rupees(command.amount()) + " into " + command.accountNumber()
                + ". New balance: " + StatementFormatter.rupees(balance);
    }

    private String withdraw() throws BankException {
        Command command = askCommand(TransactionType.WITHDRAW, "Withdraw from account: ", "Amount to withdraw (Rs.): ");
        long balance = bank.withdraw(command.accountNumber(), command.amount());
        return "Withdrew " + StatementFormatter.rupees(command.amount()) + " from " + command.accountNumber()
                + ". New balance: " + StatementFormatter.rupees(balance);
    }

    private String transfer() throws BankException {
        String from = askAccountNumber("From account: ");
        String to = askAccountNumber("To account: ");
        long amount = askAmount("Amount to transfer (Rs.): ");
        bank.transfer(from, to, amount);
        return "Transferred " + StatementFormatter.rupees(amount) + " from " + from + " to " + to + ".";
    }

    private String statement() throws BankException {
        return bank.statement(askAccountNumber("Account number: "));
    }

    private String listAccounts() {
        return formatTable(bank.listAccounts(), "No accounts yet - choose option 1 to open one.");
    }

    private String searchByOwner() {
        String text = askValid("Name (or part of it): ", part -> !part.isBlank(), "Please type a name.");
        return formatTable(bank.searchByOwner(text), "No account owner matches \"" + text + "\".");
    }

    private String processBatch() throws IOException, InterruptedException {
        System.out.println("Type one command per line, e.g.  DEPOSIT AC0001 500  or  WITHDRAW AC0002 200");
        System.out.println("Type SAMPLE for a ready-made batch; an empty line runs what you typed.");
        List<Command> commands = new ArrayList<>();
        while (true) {
            System.out.print("  command> ");
            String line = readLineOrNull();
            if (line == null || line.isBlank()) {
                break;
            }
            if (line.trim().equalsIgnoreCase("SAMPLE")) {
                commands.addAll(sampleBatch());
                System.out.println("  added " + commands.size() + " sample commands");
                break;
            }
            try {
                commands.add(CommandParser.parse(line));
            } catch (IllegalArgumentException e) {
                System.out.println("  skipped: " + e.getMessage());
            }
        }
        if (commands.isEmpty()) {
            return "No commands to run.";
        }

        BatchResult result = bank.processBatch(commands);
        StringBuilder sb = new StringBuilder();
        sb.append("Batch finished in ").append(result.millis()).append(" ms: ")
                .append(result.succeeded()).append(" of ").append(result.submitted()).append(" succeeded.\n");
        for (String failure : result.failures()) {
            sb.append("  failed: ").append(failure).append('\n');
        }
        sb.append("Tasks per pool thread:");
        for (Map.Entry<String, Integer> entry : result.tasksPerThread().entrySet()) {
            sb.append("  ").append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    /** 20 deposits and 20 withdrawals spread over the existing accounts. */
    private List<Command> sampleBatch() {
        List<Command> commands = new ArrayList<>();
        List<Account> accounts = bank.listAccounts();
        if (accounts.isEmpty()) {
            System.out.println("  (open an account first - the sample batch needs existing accounts)");
            return commands;
        }
        for (int i = 0; i < 20; i++) {
            String number = accounts.get(i % accounts.size()).getAccountNumber();
            commands.add(new Command(TransactionType.DEPOSIT, number, 100));
            commands.add(new Command(TransactionType.WITHDRAW, number, 50));
        }
        return commands;
    }

    private String report() throws IOException {
        Path report = bank.generateReport();
        return Files.readString(report) + "(saved to " + report + ")";
    }

    private String exit() {
        try {
            bank.saveState();
            return "All accounts saved. Thank you for banking with " + bank.getBankInfo().name() + ". Goodbye!";
        } catch (IOException e) {
            return "WARNING: could not save accounts (" + e.getMessage() + "). Goodbye.";
        }
    }

    // ----- input helpers -----

    private MenuOption readChoice() {
        int last = MenuOption.values().length;
        while (true) {
            System.out.print("Enter your choice (1-" + last + "): ");
            String input = readLineOrNull();
            if (input == null) {
                return MenuOption.EXIT;           // input closed: save and leave
            }
            try {
                MenuOption option = MenuOption.fromNumber(Integer.parseInt(input.trim()));
                if (option != null) {
                    return option;
                }
            } catch (NumberFormatException notANumber) {
                // fall through to the error message below
            }
            System.out.println("Invalid choice '" + input.trim() + "'. Please enter a number from 1 to " + last + ".");
        }
    }

    /** Reads account number and amount, validated, and builds the Command through CommandParser. */
    private Command askCommand(TransactionType type, String accountPrompt, String amountPrompt) {
        String accountNumber = askAccountNumber(accountPrompt);
        long amount = askAmount(amountPrompt);
        return CommandParser.parse(type + " " + accountNumber + " " + amount);
    }

    private String askAccountNumber(String prompt) {
        return askValid(prompt, text -> isValidAccountNumber(text.toUpperCase(Locale.ROOT)),
                "Account numbers look like AC0001.").toUpperCase(Locale.ROOT);
    }

    private long askAmount(String prompt) {
        return Long.parseLong(askValid(prompt, text -> isValidAmount(text),
                "Enter a positive whole number of rupees, e.g. 500."));
    }

    /** Asks until the answer passes the check. An empty answer cancels the action. */
    private String askValid(String prompt, Predicate<String> check, String error) {
        while (true) {
            System.out.print(prompt);
            String input = readLineOrNull();
            if (input == null || input.isBlank()) {
                throw new CancelledException();
            }
            input = input.trim();
            if (check.test(input)) {
                return input;
            }
            System.out.println("  " + error + " (Press Enter on an empty line to cancel.)");
        }
    }

    private String readLineOrNull() {
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    // ----- output helpers -----

    private static void printHeader(BankInfo bank) {
        System.out.println("============================================================");
        System.out.println("  " + bank.name() + "  |  " + bank.branch());
        System.out.println("============================================================");
    }

    private static void printMenu() {
        System.out.println();
        for (MenuOption option : MenuOption.values()) {
            System.out.printf("%2d. %s%n", option.getNumber(), option.getLabel());
        }
    }

    private static String formatTable(List<Account> accounts, String emptyMessage) {
        if (accounts.isEmpty()) {
            return emptyMessage;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s %-24s %-13s %14s  %s%n", "Account", "Owner", "Type", "Balance", "Status"));
        long total = 0;
        for (Account account : accounts) {
            sb.append(String.format("%-8s %-24s %-13s %14s  %s%n", account.getAccountNumber(), account.getOwnerName(),
                    account.getAccountType(), StatementFormatter.rupees(account.getBalance()),
                    account.isActive() ? "ACTIVE" : "FROZEN"));
            total += account.getBalance();
        }
        sb.append(accounts.size()).append(" account(s), total balance ").append(StatementFormatter.rupees(total));
        return sb.toString();
    }

    /** Thrown when the user presses Enter on an empty line to leave an action. */
    private static class CancelledException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
