package demo;

import model.Account;
import model.CurrentAccount;
import model.Customer;
import model.FixedDepositAccount;
import model.SavingsAccount;
import service.Repository;

/**
 * Practical 13 demo: one generic Repository class used for Accounts and for
 * Customers, keys found through @Id by reflection, a @SafeVarargs method and
 * a bounded generic method.
 */
public class Lab13Demo {

    public static void main(String[] args) {
        Repository<Account> accounts = new Repository<>();
        Account riya = new SavingsAccount("Riya", 5000);
        Account aman = new CurrentAccount("Aman", 2000);
        Account neha = new FixedDepositAccount("Neha", 9000);
        accounts.save(riya);
        accounts.saveAll(aman, neha);                     // varargs
        // accounts.save(new Customer(...));              // does not compile: a Customer is not an Account

        System.out.println("Repository<Account> holds " + accounts.count() + " accounts");
        System.out.println("findById(\"" + aman.getAccountNumber() + "\") -> " + accounts.findById(aman.getAccountNumber()));
        System.out.println("findAll():");
        for (Account account : accounts.findAll()) {        // no cast needed: findAll() returns ArrayList<Account>
            System.out.println("  " + account);
        }
        System.out.println("findAll(balance > 4000) -> " + accounts.findAll(a -> a.getBalance() > 4000).size() + " accounts");
        System.out.println("delete(\"" + neha.getAccountNumber() + "\") -> " + accounts.delete(neha.getAccountNumber())
                + ", count now " + accounts.count());
        System.out.println("delete(\"AC9999\") -> " + accounts.delete("AC9999"));

        System.out.println();
        Repository<Customer> customers = new Repository<>();         // the same class, a different type
        customers.saveAll(new Customer("Riya Patel", "riya@example.com", "9876543210"),
                new Customer("Aman Shah", "aman@example.com", "9123456780"));
        System.out.println("Repository<Customer> holds " + customers.count() + " customers (key = @Id customerId)");
        for (Customer customer : customers.findAll()) {
            System.out.println("  " + customer);
        }
        System.out.println("findById(\"CUST101\") -> " + customers.findById("CUST101").getName());

        System.out.println();
        Integer[] counts = {3, 5, 7};
        Double[] rates = {4.0, 0.0, 7.0};
        Long[] balances = {5000L, 2000L, 9000L};
        System.out.println("average(Integer[] {3, 5, 7})        = " + Repository.average(counts));
        System.out.println("average(Double[] {4.0, 0.0, 7.0})   = " + Repository.average(rates));
        System.out.printf("average(Long[] {5000, 2000, 9000}) = %.2f%n", Repository.average(balances));
        // Repository.average(new String[] {"a"});   // does not compile: String is not a Number

        try {
            new Repository<String>().save("no id here");
        } catch (IllegalArgumentException e) {
            System.out.println("Saving a String -> " + e.getMessage());
        }
    }
}
