package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import exception.BankException;
import model.Account;
import model.Command;
import model.SavingsAccount;
import model.TransactionType;

class ConcurrencyTest {

    @Test
    void manyConcurrentDepositsKeepTheBalanceCorrect() throws InterruptedException {
        Account account = new SavingsAccount("Race", 0, 0);
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new AccountWorker(account, 1000, 1), "worker-" + i);
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(10000, account.getBalance());
    }

    @Test
    void threadPoolProcessesEveryCommand() throws InterruptedException {
        Account account = new SavingsAccount("Pool", 5000, 0);
        TransactionProcessor processor = new TransactionProcessor();
        processor.register(account);
        for (int i = 0; i < 500; i++) {
            processor.submit(new Command(TransactionType.DEPOSIT, account.getAccountNumber(), 2));
            processor.submit(new Command(TransactionType.WITHDRAW, account.getAccountNumber(), 1));
        }
        assertTrue(processor.stop());
        assertEquals(1000, processor.getSucceededCount());
        assertEquals(5500, account.getBalance());
        assertTrue(processor.getTasksPerThread().size() <= TransactionProcessor.POOL_SIZE);
    }

    @Test
    void oppositeTransfersDoNotDeadlock() {
        Account a = new SavingsAccount("A", 100000, 0);
        Account b = new SavingsAccount("B", 100000, 0);
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            Thread t1 = new Thread(() -> transferMany(a, b));
            Thread t2 = new Thread(() -> transferMany(b, a));
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        });
        assertEquals(200000, a.getBalance() + b.getBalance());
        assertEquals(100000, a.getBalance());
    }

    private static void transferMany(Account from, Account to) {
        for (int i = 0; i < 500; i++) {
            try {
                from.transfer(to, 10);
            } catch (BankException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
