package demo;

import java.util.concurrent.atomic.AtomicLong;

import exception.BankException;
import model.Account;
import model.SavingsAccount;
import service.AccountWorker;

/**
 * Practical 9 demo: 10 threads each deposit 1 rupee 1000 times into the same
 * balance. The expected final balance is 10000.
 *   1. An unsynchronized balance (what Account.deposit() was before the fix) loses updates.
 *   2. The real Account, whose deposit() is synchronized, is exact every run.
 *   3. An AtomicLong also gives the exact result without locks.
 *   4. Depositing and withdrawing threads together leave the balance correct.
 */
public class Lab09Demo {
    private static final int THREADS = 10;
    private static final int DEPOSITS_PER_THREAD = 1000;
    private static final long EXPECTED = THREADS * DEPOSITS_PER_THREAD;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("1) BEFORE the fix - unsynchronized balance += amount");
        for (int run = 1; run <= 3; run++) {
            UnsafeBalance unsafe = new UnsafeBalance();
            runAll(() -> {
                for (int i = 0; i < DEPOSITS_PER_THREAD; i++) {
                    unsafe.deposit(1);
                }
            });
            System.out.println("   run " + run + ": expected " + EXPECTED + ", actual " + unsafe.balance);
        }

        System.out.println();
        System.out.println("2) AFTER the fix - Account.deposit() is synchronized");
        Account account = new SavingsAccount("Race Test", 0, 0);
        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(new AccountWorker(account, DEPOSITS_PER_THREAD, 1), "worker-" + (i + 1));
        }
        System.out.println("   state of worker-1 before start(): " + threads[0].getState());
        for (Thread thread : threads) {
            thread.start();
        }
        System.out.println("   state of worker-1 just after start(): " + threads[0].getState());
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("   state of worker-1 after join(): " + threads[0].getState());
        System.out.println("   expected " + EXPECTED + ", actual " + account.getBalance());
        for (int run = 2; run <= 3; run++) {
            Account again = new SavingsAccount("Race Test", 0, 0);
            runAll(() -> {
                for (int i = 0; i < DEPOSITS_PER_THREAD; i++) {
                    depositQuietly(again, 1);
                }
            });
            System.out.println("   run " + run + ": expected " + EXPECTED + ", actual " + again.getBalance());
        }

        System.out.println();
        System.out.println("3) AtomicLong instead of synchronized");
        AtomicLong atomic = new AtomicLong();
        runAll(() -> {
            for (int i = 0; i < DEPOSITS_PER_THREAD; i++) {
                atomic.addAndGet(1);
            }
        });
        System.out.println("   expected " + EXPECTED + ", actual " + atomic.get());

        System.out.println();
        System.out.println("4) 5 depositing + 5 withdrawing threads on a balance of 10000");
        Account shared = new SavingsAccount("Mixed", 10000, 0);
        Thread[] mixed = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            boolean depositor = i % 2 == 0;
            mixed[i] = new Thread(() -> {
                for (int n = 0; n < DEPOSITS_PER_THREAD; n++) {
                    try {
                        if (depositor) {
                            shared.deposit(1);
                        } else {
                            shared.withdraw(1);
                        }
                    } catch (BankException e) {
                        System.out.println("   unexpected: " + e.getMessage());
                    }
                }
            });
        }
        startAndJoin(mixed);
        System.out.println("   expected 10000, actual " + shared.getBalance());
    }

    /** Starts THREADS threads running the same task and waits for all of them. */
    private static void runAll(Runnable task) throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(task);
        }
        startAndJoin(threads);
    }

    private static void startAndJoin(Thread[] threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static void depositQuietly(Account account, long amount) {
        try {
            account.deposit(amount);
        } catch (BankException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** The pre-fix deposit logic: a read-modify-write with no lock. */
    private static class UnsafeBalance {
        private long balance;

        void deposit(long amount) {
            long current = balance;         // 1. read
            Thread.yield();                 //    (another thread may run here)
            balance = current + amount;     // 2. write - may overwrite another thread's update
        }
    }
}
