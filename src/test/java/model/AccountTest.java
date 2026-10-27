package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import exception.BankException;
import exception.DailyLimitExceededException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;

class AccountTest {

    @Test
    void depositIncreasesBalance() throws BankException {
        Account account = new SavingsAccount("Riya", 5000);
        account.deposit(2000);
        assertEquals(7000, account.getBalance());
    }

    @Test
    void withdrawalOverTheLimitThrowsAndKeepsBalance() {
        Account account = new SavingsAccount("Riya", 1000, 0);
        InsufficientFundsException e = assertThrows(InsufficientFundsException.class, () -> account.withdraw(5000));
        assertEquals(4000, e.getShortfall());
        assertEquals(1000, account.getBalance());
    }

    @Test
    void practicalTwoSequence() throws BankException {
        Account account = new SavingsAccount("Riya", 5000);
        account.deposit(2000);
        assertEquals(7000, account.getBalance());
        assertTrue(account.withdraw(3000));
        assertEquals(4000, account.getBalance());
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(10000));
        assertEquals(4000, account.getBalance());
    }

    @Test
    void zeroOrNegativeAmountsAreRejected() {
        Account account = new SavingsAccount("Riya", 5000);
        assertThrows(InvalidAmountException.class, () -> account.deposit(0));
        assertThrows(InvalidAmountException.class, () -> account.deposit(-100));
        assertThrows(InvalidAmountException.class, () -> account.withdraw(-1));
        assertEquals(5000, account.getBalance());
    }

    @Test
    void savingsKeepsMinimumBalance() throws BankException {
        SavingsAccount account = new SavingsAccount("Riya", 5000, 1000);
        assertTrue(account.withdraw(4000));
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(1));
        assertEquals(1000, account.getBalance());
    }

    @Test
    void currentAccountMayUseOverdraft() throws BankException {
        CurrentAccount account = new CurrentAccount("Aman", 2000, 10000);
        account.withdraw(12000);
        assertEquals(-10000, account.getBalance());
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(1));
    }

    @Test
    void fixedDepositIsLockedUntilMaturity() throws BankException {
        Account locked = new FixedDepositAccount("Neha", 50000, LocalDate.now().plusDays(1));
        assertThrows(InsufficientFundsException.class, () -> locked.withdraw(100));

        Account matured = new FixedDepositAccount("Neha", 50000, LocalDate.now());
        assertTrue(matured.withdraw(50000));
        assertEquals(0, matured.getBalance());
    }

    @Test
    void dailyLimitIsEnforced() throws BankException {
        Account account = new SavingsAccount("Kavya", 300000);
        account.withdraw(60000);
        DailyLimitExceededException e = assertThrows(DailyLimitExceededException.class, () -> account.withdraw(60000));
        assertEquals(40000, e.getRemaining());
        assertEquals(240000, account.getBalance());
    }

    @Test
    void premiumAccountsHaveHigherDailyLimit() {
        assertEquals(Account.PREMIUM_DAILY_WITHDRAWAL_LIMIT, new CurrentAccount("Aman", 0).dailyLimit());
        assertEquals(Account.DAILY_WITHDRAWAL_LIMIT, new SavingsAccount("Riya", 5000).dailyLimit());
    }

    @Test
    void failedTransferChangesNothing() throws BankException {
        Account from = new SavingsAccount("A", 3000);
        Account to = new SavingsAccount("B", 3000);
        assertThrows(InsufficientFundsException.class, () -> from.transfer(to, 5000));
        assertEquals(3000, from.getBalance());
        assertEquals(3000, to.getBalance());

        from.transfer(to, 1500);
        assertEquals(1500, from.getBalance());
        assertEquals(4500, to.getBalance());
        assertThrows(BankException.class, () -> from.transfer(from, 10));
    }

    @Test
    void equalityAndOrderUseAccountNumber() {
        Account a1 = new SavingsAccount("AC7001", "Riya", 4000, 1000);
        Account a2 = new CurrentAccount("AC7001", "Riya", 9999, 0);
        Account a3 = new SavingsAccount("AC7002", "Aman", 4000, 1000);
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1, a3);
        assertTrue(a1.compareTo(a3) < 0);
        assertEquals("AC7001 | Riya | 4000 | ACTIVE", a1.toString());
    }

    @Test
    void interestFollowsAccountType() {
        assertEquals(4.0, new SavingsAccount("Riya", 12000).interestRate());
        assertEquals(0.0, new CurrentAccount("Aman", 12000).interestRate());
        assertEquals(7.0, new FixedDepositAccount("Neha", 12000).interestRate());
        assertEquals(480.0, new SavingsAccount("Riya", 12000).yearlyInterest(), 0.001);
        assertEquals(40.0, new SavingsAccount("Riya", 12000).monthlyInterest(), 0.001);
    }

    @Test
    void historyRecordsEveryOperation() throws BankException {
        Account account = new SavingsAccount("Riya", 5000);
        account.deposit(100);
        account.withdraw(50);
        assertEquals(3, account.getHistory().size());
        assertTrue(account.getHistory().get(2).contains("WITHDRAW"));
        assertFalse(account.getHistory().isEmpty());
    }
}
