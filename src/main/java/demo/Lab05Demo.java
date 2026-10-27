package demo;

import java.time.LocalDate;

import exception.BankException;
import model.Account;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.SavingsAccount;

/**
 * Practical 5 demo: an Account[] holding three different subclasses. The same
 * call - interestRate() or canWithdraw() - runs a different method for each
 * element (run-time polymorphism / dynamic method dispatch).
 */
public class Lab05Demo {

    public static void main(String[] args) throws BankException {
        Account[] accounts = {
                new SavingsAccount("Riya", 20000),
                new CurrentAccount("Aman Traders", 5000, 10000),
                new FixedDepositAccount("Neha", 100000)
        };

        System.out.println("Interest rate of each account:");
        for (Account account : accounts) {
            System.out.printf("  %-6s %-13s %-12s rate = %.1f%%   monthly interest = %.2f%n",
                    account.getAccountNumber(), account.getAccountType(), account.getOwnerName(),
                    account.interestRate(), account.monthlyInterest());
        }

        System.out.println();
        System.out.println("Can each account withdraw 15000?");
        for (Account account : accounts) {
            String note = "";
            if (account instanceof SavingsAccount savings) {
                note = "(must keep minimum " + savings.getMinBalance() + ")";
            } else if (account instanceof CurrentAccount current) {
                note = "(overdraft up to " + current.getOverdraftLimit() + ")";
            } else if (account instanceof FixedDepositAccount fd) {
                note = "(locked until " + fd.getMaturityDate() + ")";
            }
            System.out.printf("  %-13s -> %-5b %s%n", account.getAccountType(), account.canWithdraw(15000), note);
        }

        System.out.println();
        Account current = accounts[1];
        System.out.println("Current account withdraw(12000) -> " + current.withdraw(12000)
                + ", balance now " + current.getBalance());

        FixedDepositAccount matured = new FixedDepositAccount("Old Deposit", 50000, LocalDate.now().minusDays(1));
        System.out.println("Matured FD withdraw(50000)      -> " + matured.withdraw(50000)
                + ", balance now " + matured.getBalance());
    }
}
