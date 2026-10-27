package service;

import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import exception.BankException;

/**
 * Runs banking tasks on a fixed pool of 4 reusable threads instead of
 * creating a new thread for every task.
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
    private final ConcurrentHashMap<String, AtomicInteger> tasksPerThread = new ConcurrentHashMap<>();
    private final AtomicInteger succeeded = new AtomicInteger();
    private final Queue<String> failures = new ConcurrentLinkedQueue<>();
    private final Queue<String> warnings = new ConcurrentLinkedQueue<>();

    public TransactionProcessor() {
        AtomicInteger threadNumber = new AtomicInteger();
        pool = Executors.newFixedThreadPool(POOL_SIZE,
                task -> new Thread(task, "txn-worker-" + threadNumber.incrementAndGet()));
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

    /** How many tasks each pool thread ran, sorted by thread name. */
    public Map<String, Integer> getTasksPerThread() {
        Map<String, Integer> counts = new TreeMap<>();
        tasksPerThread.forEach((thread, count) -> counts.put(thread, count.get()));
        return counts;
    }
}
