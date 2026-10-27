package demo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import exception.BankException;
import model.Account;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.SavingsAccount;
import service.AccountStore;
import service.TransactionProcessor;
import util.CommandParser;
import util.StatementFormatter;

/**
 * Practical 12 demo (Milestone 2): AccountStore on a HashMap, sorting with a
 * Comparator and with Comparable, a TreeMap view, the history list, and the
 * thread-safe ConcurrentHashMap inside TransactionProcessor.
 */
public class Lab12Demo {

    public static void main(String[] args) throws BankException, InterruptedException {
        AccountStore store = new AccountStore();
        store.add(new SavingsAccount("Riya", 5000));          // AC0001
        store.add(new CurrentAccount("Aman", 2000));          // AC0002
        store.add(new FixedDepositAccount("Neha", 9000));     // AC0003

        System.out.println("get(\"AC0002\")   -> " + store.get("AC0002"));
        System.out.println("get(\"AC9999\")   -> " + store.get("AC9999"));

        System.out.println();
        System.out.println("sortedByBalance() (Comparator, highest first):");
        store.sortedByBalance().forEach(account -> System.out.println("  " + account));

        System.out.println("sortedByNumber() (Comparable, natural order):");
        store.sortedByNumber().forEach(account -> System.out.println("  " + account));

        System.out.println("balanceAbove(3000):");
        store.balanceAbove(3000).forEach(account -> System.out.println("  " + account));

        System.out.println("TreeMap keys (always sorted): " + store.asSortedMap().keySet());

        // List vs Set: a list keeps duplicates and order, a set keeps each value once.
        List<String> owners = new ArrayList<>(List.of("Riya", "Aman", "Riya", "Neha"));
        Set<String> uniqueOwners = new HashSet<>(owners);
        System.out.println("List of owners " + owners + " has " + owners.size()
                + " items; as a Set it has " + uniqueOwners.size());

        System.out.println();
        store.deposit("AC0001", 1500);
        store.withdraw("AC0002", 500);
        try {
            store.deposit("AC0042", 100);
        } catch (BankException e) {
            System.out.println("store.deposit(\"AC0042\", 100) -> " + e.getMessage());
        }
        System.out.println("AccountStore history (ArrayList<String>): " + store.getHistory());

        System.out.println();
        System.out.println("Thread-safe processing: 40 commands on " + TransactionProcessor.POOL_SIZE
                + " threads over a ConcurrentHashMap");
        TransactionProcessor processor = new TransactionProcessor(store.getAll());
        for (int i = 0; i < 20; i++) {
            processor.submit(CommandParser.parse("DEPOSIT AC0001 100"));
            processor.submit(CommandParser.parse("WITHDRAW AC0002 50"));
        }
        processor.stop();
        System.out.println("  succeeded " + processor.getSucceededCount() + ", failed " + processor.getFailures().size());
        for (Map.Entry<String, Integer> entry : processor.getTasksPerThread().entrySet()) {
            System.out.println("  " + entry.getKey() + " ran " + entry.getValue() + " tasks");
        }
        System.out.println("  AC0001: 6500 + 20 x 100 = " + store.get("AC0001").getBalance());
        System.out.println("  AC0002: 1500 - 20 x 50  = " + store.get("AC0002").getBalance());

        System.out.println();
        Account riya = store.get("AC0001");
        System.out.println(StatementFormatter.buildStatement(riya));
    }
}
