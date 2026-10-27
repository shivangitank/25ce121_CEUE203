package demo;

import exception.BankException;
import exception.DailyLimitExceededException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.Account;
import model.FixedDepositAccount;
import model.SavingsAccount;
import service.TellerSession;

/**
 * Practical 8 demo (Milestone 1): the custom exception hierarchy,
 * try-catch-finally, re-throwing from transfer() and try-with-resources.
 */
public class Lab08Demo {

    public static void main(String[] args) {
        // 1. Insufficient funds: the shortfall travels inside the exception.
        Account account = new SavingsAccount("Riya", 1000, 0);
        try {
            account.withdraw(5000);
            System.out.println("Withdrawal succeeded");
        } catch (InsufficientFundsException e) {
            System.out.println("Withdrawal failed: short by " + e.getShortfall());
        } catch (BankException e) {
            System.out.println("Withdrawal failed: " + e.getMessage());
        } finally {
            System.out.println("finally -> balance is still " + account.getBalance());
        }

        // 2. Invalid amounts are rejected for both deposit and withdraw.
        System.out.println();
        try {
            account.deposit(-50);
        } catch (InvalidAmountException e) {
            System.out.println("Deposit rejected: " + e.getMessage());
        }

        // 3. A locked fixed deposit.
        Account fd = new FixedDepositAccount("Neha", 100000);
        try {
            fd.withdraw(5000);
        } catch (BankException e) {
            System.out.println("FD withdrawal rejected: " + e.getMessage());
        }

        // 4. Daily limit (supplementary problem): 1,00,000 per day for a normal account.
        Account rich = new SavingsAccount("Kavya", 300000);
        try {
            rich.withdraw(60000);
            System.out.println("First 60000 withdrawn, balance " + rich.getBalance());
            rich.withdraw(60000);
        } catch (DailyLimitExceededException e) {
            System.out.println("Second 60000 rejected: " + e.getMessage());
        } catch (BankException e) {
            System.out.println("Unexpected: " + e.getMessage());
        }

        // 5. transfer() re-throws the exception; both balances stay unchanged on failure.
        System.out.println();
        Account from = new SavingsAccount("Aman", 8000);
        Account to = new SavingsAccount("Neel", 2000);
        transfer(from, to, 3000);
        transfer(from, to, 9000);
        transfer(from, from, 100);

        // 6. try-with-resources: close() runs even though the second withdrawal throws.
        System.out.println();
        System.out.println("try-with-resources:");
        try (TellerSession session = new TellerSession("Desk-1")) {
            session.deposit(to, 500);
            session.withdraw(to, 100000);
            System.out.println("  not reached");
        } catch (BankException e) {
            System.out.println("  caught after the session closed: " + e.getMessage());
        }
    }

    private static void transfer(Account from, Account to, long amount) {
        try {
            from.transfer(to, amount);
            System.out.println("Transfer " + amount + " " + from.getAccountNumber() + " -> " + to.getAccountNumber() + " done");
        } catch (BankException e) {
            System.out.println("Transfer " + amount + " failed: " + e.getMessage());
        } finally {
            System.out.println("  balances: " + from.getAccountNumber() + "=" + from.getBalance()
                    + ", " + to.getAccountNumber() + "=" + to.getBalance());
        }
    }
}
