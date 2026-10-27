package demo;

import model.Account;
import model.SavingsAccount;
import service.AccountWorker;

/**
 * Practical 9 demo: 10 threads each deposit 1 rupee 1000 times into the same
 * account. The expected final balance is 10000.
 */
public class Lab09Demo {
    private static final int THREADS = 10;
    private static final int DEPOSITS_PER_THREAD = 1000;

    public static void main(String[] args) throws InterruptedException {
        for (int run = 1; run <= 3; run++) {
            Account account = new SavingsAccount("Race Test", 0, 0);
            Thread[] threads = new Thread[THREADS];
            for (int i = 0; i < THREADS; i++) {
                threads[i] = new Thread(new AccountWorker(account, DEPOSITS_PER_THREAD, 1), "worker-" + (i + 1));
            }
            System.out.println("Run " + run + ": thread state before start = " + threads[0].getState());
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
            System.out.println("Run " + run + ": thread state after join  = " + threads[0].getState());
            System.out.println("Run " + run + ": expected " + (THREADS * DEPOSITS_PER_THREAD)
                    + ", actual balance " + account.getBalance());
            System.out.println();
        }
    }
}
