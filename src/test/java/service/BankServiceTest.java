package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.ValidationException;
import model.Account;
import model.AccountType;
import model.Command;
import model.TransactionType;
import util.ReportGenerator;

class BankServiceTest {

    @TempDir
    Path dataDir;

    private BankService bank;

    @BeforeEach
    void setUp() {
        bank = new BankService(dataDir);
    }

    private Account open(AccountType type, String name, long deposit) throws BankException {
        return bank.openAccount(type, name, "9876543210", "test@example.com", deposit);
    }

    @Test
    void depositIncreasesBalance() throws BankException {
        Account riya = open(AccountType.SAVINGS, "Riya", 5000);
        assertEquals(7000, bank.deposit(riya.getAccountNumber(), 2000));
    }

    @Test
    void withdrawalOverTheBalanceThrows() throws BankException {
        Account riya = open(AccountType.SAVINGS, "Riya", 5000);
        assertThrows(InsufficientFundsException.class, () -> bank.withdraw(riya.getAccountNumber(), 10000));
        assertEquals(5000, bank.findAccount(riya.getAccountNumber()).getBalance());
    }

    @Test
    void withdrawalOverThePerTransactionLimitThrows() throws BankException {
        Account rich = open(AccountType.SAVINGS, "Kavya", 200000);
        BankException e = assertThrows(BankException.class, () -> bank.withdraw(rich.getAccountNumber(), 55000));
        assertTrue(e.getMessage().contains("limit per withdrawal"));
        assertEquals(200000, rich.getBalance());
    }

    @Test
    void transferMovesMoneyAndShowsInStatement() throws BankException {
        Account a = open(AccountType.SAVINGS, "Riya", 5000);
        Account b = open(AccountType.CURRENT, "Aman", 1000);
        bank.transfer(a.getAccountNumber(), b.getAccountNumber(), 1500);
        assertEquals(3500, a.getBalance());
        assertEquals(2500, b.getBalance());
        String statement = bank.statement(a.getAccountNumber());
        assertTrue(statement.contains("TRANSFER TO " + b.getAccountNumber()));
        assertTrue(statement.contains(a.getCustomerId()));
    }

    @Test
    void unknownAccountThrows() {
        assertThrows(AccountNotFoundException.class, () -> bank.deposit("AC9999", 100));
        assertThrows(AccountNotFoundException.class, () -> bank.statement("AC9999"));
    }

    @Test
    void invalidDetailsAreRejected() {
        assertThrows(ValidationException.class,
                () -> bank.openAccount(AccountType.SAVINGS, "Riya", "12345", "riya@example.com", 5000));
        assertThrows(ValidationException.class,
                () -> bank.openAccount(AccountType.SAVINGS, "Riya", "9876543210", "riya@", 5000));
        assertThrows(ValidationException.class,
                () -> bank.openAccount(AccountType.SAVINGS, "R1ya", "9876543210", "riya@example.com", 5000));
        assertThrows(ValidationException.class,
                () -> bank.openAccount(AccountType.SAVINGS, "Riya", "9876543210", "riya@example.com", 500));
        assertTrue(bank.listAccounts().isEmpty());
    }

    @Test
    void searchByOwnerIgnoresCase() throws BankException {
        open(AccountType.SAVINGS, "Riya Patel", 5000);
        open(AccountType.CURRENT, "Aman Shah", 5000);
        open(AccountType.FIXED_DEPOSIT, "Priya Shah", 5000);
        assertEquals(2, bank.searchByOwner("shah").size());
        assertEquals(2, bank.searchByOwner("RIYA").size());
        assertEquals(0, bank.searchByOwner("Neha").size());
    }

    @Test
    void stateSurvivesRestartUnchanged() throws Exception {
        Account riya = open(AccountType.SAVINGS, "Riya", 5000);
        bank.deposit(riya.getAccountNumber(), 2500);
        bank.saveState();

        BankService restarted = new BankService(dataDir);
        assertEquals(1, restarted.loadState());
        Account reloaded = restarted.findAccount(riya.getAccountNumber());
        assertNotSame(riya, reloaded);
        assertEquals(7500, reloaded.getBalance());
        assertEquals("Riya", reloaded.getOwnerName());
        assertEquals(riya.getHistory(), reloaded.getHistory());
        assertNotNull(restarted.findCustomer(riya.getCustomerId()));

        Account next = restarted.openAccount(AccountType.CURRENT, "Aman", "9123456780", "aman@example.com", 1000);
        assertTrue(next.compareTo(reloaded) > 0, "new account numbers continue after the loaded ones");
    }

    @Test
    void batchOnThreadPoolKeepsBalancesCorrect() throws Exception {
        Account riya = open(AccountType.SAVINGS, "Riya", 10000);
        List<Command> commands = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            commands.add(new Command(TransactionType.DEPOSIT, riya.getAccountNumber(), 100));
            commands.add(new Command(TransactionType.WITHDRAW, riya.getAccountNumber(), 50));
        }
        commands.add(new Command(TransactionType.DEPOSIT, "AC9999", 100));

        BatchResult result = bank.processBatch(commands);
        assertEquals(201, result.submitted());
        assertEquals(200, result.succeeded());
        assertEquals(1, result.failures().size());
        assertEquals(15000, riya.getBalance());
    }

    @Test
    void reportTotalsMatchTheLoggedTransactions() throws Exception {
        Account a = open(AccountType.SAVINGS, "Riya", 5000);
        Account b = open(AccountType.CURRENT, "Aman", 3000);
        bank.deposit(a.getAccountNumber(), 1000);
        bank.withdraw(a.getAccountNumber(), 500);
        bank.transfer(a.getAccountNumber(), b.getAccountNumber(), 700);

        ReportGenerator.Summary summary = ReportGenerator.generate(bank.getLogsDir(), dataDir.resolve("check.txt"));
        assertEquals(3, summary.depositCount());
        assertEquals(9000, summary.totalDeposits());
        assertEquals(500, summary.totalWithdrawals());
        assertEquals(700, summary.totalTransfers());
        assertEquals(8500, summary.netChange());

        Path report = bank.generateReport();
        assertTrue(Files.readString(report).contains("Net change"));
    }

    @Test
    void concurrentDepositsThroughTheServiceAreAllCounted() throws Exception {
        Account riya = open(AccountType.SAVINGS, "Riya", 1000);
        Thread[] threads = new Thread[10];
        for (int t = 0; t < threads.length; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 50; i++) {
                    try {
                        bank.deposit(riya.getAccountNumber(), 10);
                    } catch (BankException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(1000 + 10 * 50 * 10, riya.getBalance());
    }
}
