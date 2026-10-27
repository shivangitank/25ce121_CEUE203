package demo;

import static util.Validator.*;

import exception.BankException;
import model.Account;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.InterestBearing;
import model.Premium;
import model.SavingsAccount;
import model.Transactable;
import model.WithdrawRule;
import service.WithdrawPolicy;

/**
 * Practical 6 demo: interfaces (Transactable, InterestBearing with default
 * methods, the WithdrawRule functional interface and the Premium marker),
 * packages and a static import.
 */
public class Lab06Demo {

    public static void main(String[] args) throws BankException {
        SavingsAccount savings = new SavingsAccount("Riya", 60000);
        CurrentAccount current = new CurrentAccount("Aman Traders", 80000);
        FixedDepositAccount fd = new FixedDepositAccount("Neha", 100000);

        // 1. The same rule written as an anonymous class and as a lambda.
        WithdrawRule anonymousRule = new WithdrawRule() {
            @Override
            public boolean allow(Account account, long amount) {
                return account.isActive() && amount <= 20000;
            }
        };
        WithdrawRule lambdaRule = (account, amount) -> account.isActive() && amount <= 20000;

        System.out.println("WithdrawRule: active and amount <= 20000");
        long[] amounts = {5000, 20000, 25000};
        for (long amount : amounts) {
            System.out.printf("  amount %-6d anonymous class -> %-5b lambda -> %b%n",
                    amount, anonymousRule.allow(savings, amount), lambdaRule.allow(savings, amount));
        }

        // 2. Default methods of InterestBearing, called through the interface type.
        System.out.println();
        System.out.println("Interest (InterestBearing default methods):");
        InterestBearing[] earners = {savings, current, fd};
        for (InterestBearing earner : earners) {
            System.out.printf("  rate %.1f%%  balance %-7d yearly %9.2f  monthly %8.2f%n",
                    earner.interestRate(), earner.getBalance(), earner.yearlyInterest(), earner.monthlyInterest());
        }
        System.out.printf("  static InterestBearing.simpleInterest(10000, 4.0, 2) = %.2f%n",
                InterestBearing.simpleInterest(10000, 4.0, 2));

        // 3. Transactable: code that only needs deposit/withdraw sees just that.
        Transactable wallet = savings;
        wallet.deposit(500);
        System.out.println();
        System.out.println("Deposited 500 through a Transactable reference -> balance " + savings.getBalance());

        // 4. The Premium marker interface changes the per-transaction limit.
        System.out.println();
        Account[] accounts = {savings, current, fd};
        for (Account account : accounts) {
            System.out.printf("  %-13s Premium? %-5b limit per withdrawal %d%n", account.getAccountType(),
                    account instanceof Premium, WithdrawPolicy.transactionLimitFor(account));
        }
        System.out.println("  STANDARD policy, savings withdraw 55000 -> " + WithdrawPolicy.STANDARD.allow(savings, 55000));
        System.out.println("  STANDARD policy, current withdraw 55000 -> " + WithdrawPolicy.STANDARD.allow(current, 55000));
        savings.setActive(false);
        System.out.println("  STANDARD policy, frozen savings withdraw 100 -> " + WithdrawPolicy.STANDARD.allow(savings, 100));

        // 5. Static import: Validator methods used without the class name.
        System.out.println();
        System.out.println("Static import of util.Validator:");
        System.out.println("  isValidMobile(\"9876543210\") -> " + isValidMobile("9876543210"));
        System.out.println("  isValidEmail(\"riya@\")        -> " + isValidEmail("riya@"));
    }
}
