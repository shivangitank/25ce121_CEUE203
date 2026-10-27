package demo;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import exception.BankException;
import model.Account;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.SavingsAccount;
import model.TransactionType;
import util.ReportGenerator;
import util.StatePersister;
import util.TransactionLog;

/**
 * Practical 11 demo: save accounts with ObjectOutputStream, load them back,
 * append transactions to a log and produce the end-of-day report with NIO.
 * Everything is written under data/lab11-demo/ (not committed to git).
 */
public class Lab11Demo {

    public static void main(String[] args) throws IOException, BankException {
        Path base = Path.of("data", "lab11-demo");
        Path logs = base.resolve("logs");
        Path accountsFile = base.resolve("accounts.dat");
        Path reportFile = base.resolve("reports").resolve("eod-report.txt");
        clearFolder(logs);                    // fresh logs every run so the totals are predictable

        Account[] accounts = {
                new SavingsAccount("Riya", 5000),
                new CurrentAccount("Aman", 2000),
                new FixedDepositAccount("Neha", 9000)
        };
        Path todayLog = TransactionLog.dailyFile(logs, LocalDate.now());
        deposit(accounts[0], 500, todayLog);
        withdraw(accounts[1], 200, todayLog);
        deposit(accounts[2], 1500, todayLog);
        withdraw(accounts[0], 1000, todayLog);
        accounts[0].transfer(accounts[1], 700);
        TransactionLog.append(todayLog, TransactionLog.transferEntry(accounts[0].getAccountNumber(),
                accounts[1].getAccountNumber(), 700));

        Path olderLog = logs.resolve("transactions-branch2.log");     // a second log file
        TransactionLog.append(olderLog, "DEPOSIT AC0002 300 2026-10-26T16:00:00");
        TransactionLog.append(olderLog, "this line is malformed");
        Files.createFile(logs.resolve("empty.log"));                   // skipped: empty
        Files.writeString(logs.resolve("notes.txt"), "not a log\n");  // skipped: not .log

        System.out.println("Before saving:");
        for (Account account : accounts) {
            System.out.println("  " + account + "   lastActivity = " + account.getLastActivity());
        }

        StatePersister.save(accounts, accountsFile);
        System.out.println();
        System.out.println("Saved " + accounts.length + " accounts to " + accountsFile + " (" + Files.size(accountsFile) + " bytes)");

        Account[] reloaded = StatePersister.load(accountsFile);
        System.out.println("Reloaded " + reloaded.length + " accounts into a new array:");
        for (int i = 0; i < reloaded.length; i++) {
            Account copy = reloaded[i];
            System.out.println("  " + copy + "   lastActivity = " + copy.getLastActivity()
                    + "   same data? " + (copy.equals(accounts[i]) && copy.getBalance() == accounts[i].getBalance())
                    + "   same object? " + (copy == accounts[i]));
        }
        System.out.println("  (lastActivity is transient, so it was not saved and is null after loading)");

        ReportGenerator.Summary summary = ReportGenerator.generate(logs, reportFile);
        System.out.println();
        System.out.println("Report written to " + reportFile + ":");
        System.out.println(Files.readString(reportFile));
        System.out.println("Check: deposits 500+1500+300 = " + summary.totalDeposits()
                + ", withdrawals 200+1000 = " + summary.totalWithdrawals());
    }

    private static void deposit(Account account, long amount, Path log) throws BankException, IOException {
        account.deposit(amount);
        TransactionLog.append(log, TransactionLog.entry(TransactionType.DEPOSIT, account.getAccountNumber(), amount));
    }

    private static void withdraw(Account account, long amount, Path log) throws BankException, IOException {
        account.withdraw(amount);
        TransactionLog.append(log, TransactionLog.entry(TransactionType.WITHDRAW, account.getAccountNumber(), amount));
    }

    private static void clearFolder(Path folder) throws IOException {
        if (Files.isDirectory(folder)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(folder)) {
                for (Path file : files) {
                    Files.delete(file);
                }
            }
        }
        Files.createDirectories(folder);
    }
}
