/**
 * Practical 2 demo: Customer and Account objects, an Account[] array and the
 * encapsulated deposit / withdraw logic.
 */
public class Lab02Demo {

    public static void main(String[] args) {
        Account[] accounts = {
                new SavingsAccount("Riya", 5000),
                new CurrentAccount("Aman", 12000),
                new CurrentAccount("Neha")      // chained constructor: opening balance 0
        };

        Account riya = accounts[0];
        System.out.println("Opened " + riya.getAccountNumber() + " for " + riya.getOwnerName()
                + " with balance " + riya.getBalance());

        riya.deposit(2000);
        System.out.println("After deposit(2000)   : " + riya.getBalance());

        boolean first = riya.withdraw(3000);
        System.out.println("withdraw(3000)  -> " + first + " : " + riya.getBalance());

        boolean second = riya.withdraw(10000);
        System.out.println("withdraw(10000) -> " + second + " : " + riya.getBalance());

        // riya.balance = 1_000_000;   // does not compile: balance has private access in Account

        accounts[2].deposit(-500);      // rejected, balance stays 0
        accounts[2].deposit(1500);
        accounts[1].withdraw(2500);
        boolean moved = accounts[1].transferTo(accounts[2], 1000);
        System.out.println("Transfer 1000 from " + accounts[1].getAccountNumber()
                + " to " + accounts[2].getAccountNumber() + " -> " + moved);

        System.out.println();
        System.out.println("All balances:");
        for (Account account : accounts) {
            System.out.println("  " + account.getAccountNumber() + "  " + account.getOwnerName()
                    + "  " + account.getBalance());
        }

        Customer customer = new Customer("Riya Patel", "riya.patel@example.com", "9876543210");
        System.out.println();
        System.out.println("Customer " + customer.getCustomerId() + ": " + customer.getName()
                + ", " + customer.getEmail() + ", " + customer.getMobile());
    }
}
