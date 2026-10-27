package service;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import exception.AccountNotFoundException;
import exception.BankException;
import model.Account;
import model.Command;

/**
 * Runs banking tasks on a fixed pool of 4 reusable threads instead of
 * creating a new thread for every task. The accounts it works on are kept in a
 * ConcurrentHashMap, which many pool threads can read and update safely at once.
 */
public class TransactionProcessor {
    public static final int POOL_SIZE = 4;
    private static final long SLOW_TASK_MILLIS = 500;

    /** A bank operation that may fail with a BankException. */
    @FunctionalInterface
    public interface BankAction {
        void run() throws BankException;
    }

    private final ExecutorService pool;
    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> tasksPerThread = new ConcurrentHashMap<>();
    private final AtomicInteger succeeded = new AtomicInteger();
    private final Queue<String> failures = new ConcurrentLinkedQueue<>();
    private final Queue<String> warnings = new ConcurrentLinkedQueue<>();
    private final Queue<Command> completedCommands = new ConcurrentLinkedQueue<>();

    public TransactionProcessor() {
        AtomicInteger threadNumber = new AtomicInteger();
        pool = Executors.newFixedThreadPool(POOL_SIZE,
                task -> new Thread(task, "txn-worker-" + threadNumber.incrementAndGet()));
    }

    /** A processor that already knows the given accounts. */
    public TransactionProcessor(Collection<? extends Account> initialAccounts) {
        this();
        initialAccounts.forEach(this::register);
    }

    public void register(Account account) {
        accounts.put(account.getAccountNumber(), account);
    }

    public Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    /** Queues a parsed command (DEPOSIT or WITHDRAW) against the shared account map. */
    public void submit(Command command) {
        String description = command.type() + " " + command.accountNumber() + " " + command.amount();
        submit(description, () -> {
            Account account = accounts.get(command.accountNumber());
            if (account == null) {
                throw new AccountNotFoundException(command.accountNumber());
            }
            switch (command.type()) {
                case DEPOSIT -> account.deposit(command.amount());
                case WITHDRAW -> account.withdraw(command.amount());
                case TRANSFER -> throw new BankException("TRANSFER needs a target account; use BankService.transfer()");
            }
            completedCommands.add(command);
        });
    }

    /** Queues a task; one of the pool threads will run it. */
    public void submit(Runnable task) {
        pool.execute(() -> {
            String thread = Thread.currentThread().getName();
            tasksPerThread.computeIfAbsent(thread, name -> new AtomicInteger()).incrementAndGet();
            long start = System.nanoTime();
            task.run();
            long millis = (System.nanoTime() - start) / 1_000_000;
            if (millis > SLOW_TASK_MILLIS) {
                String warning = "WARNING: a task on " + thread + " took " + millis + " ms (limit " + SLOW_TASK_MILLIS + " ms)";
                warnings.add(warning);
                System.out.println("  " + warning);
            }
        });
    }

    /**
     * Queues a bank operation. A failure is recorded with its description
     * instead of stopping the rest of the batch.
     */
    public void submit(String description, BankAction action) {
        submit(() -> {
            try {
                action.run();
                succeeded.incrementAndGet();
            } catch (BankException e) {
                failures.add(description + " -> " + e.getMessage());
            }
        });
    }

    /** Stops accepting tasks and waits for the queued ones to finish. */
    public boolean stop() throws InterruptedException {
        pool.shutdown();
        boolean finished = pool.awaitTermination(60, TimeUnit.SECONDS);
        if (!finished) {
            pool.shutdownNow();
        }
        return finished;
    }

    public int getSucceededCount() {
        return succeeded.get();
    }

    public Queue<String> getFailures() {
        return failures;
    }

    public Queue<String> getWarnings() {
        return warnings;
    }

    /** Commands submitted with submit(Command) that succeeded, in completion order. */
    public Queue<Command> getCompletedCommands() {
        return completedCommands;
    }

    /** How many tasks each pool thread ran, sorted by thread name. */
    public Map<String, Integer> getTasksPerThread() {
        Map<String, Integer> counts = new TreeMap<>();
        tasksPerThread.forEach((thread, count) -> counts.put(thread, count.get()));
        return counts;
    }
}
