package demo;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import exception.BankException;
import model.Account;
import model.Command;
import model.SavingsAccount;
import model.TransactionType;
import service.TransactionBuffer;
import service.TransactionProcessor;

/**
 * Practical 10 demo: a thread-pool transaction processor, producer-consumer
 * with wait()/notify(), and a deadlock followed by its lock-ordering fix.
 */
public class Lab10Demo {

    public static void main(String[] args) throws InterruptedException {
        threadPoolDemo();
        producerConsumerDemo();
        deadlockDemo();
    }

    private static void threadPoolDemo() throws InterruptedException {
        System.out.println("1) TransactionProcessor - 22 tasks on a pool of " + TransactionProcessor.POOL_SIZE + " threads");
        Account riya = new SavingsAccount("Riya", 10000);
        Account aman = new SavingsAccount("Aman", 5000);
        TransactionProcessor processor = new TransactionProcessor();
        for (int i = 1; i <= 10; i++) {
            processor.submit("DEPOSIT " + riya.getAccountNumber() + " 100", () -> riya.deposit(100));
            processor.submit("WITHDRAW " + aman.getAccountNumber() + " 300", () -> aman.withdraw(300));
        }
        processor.submit("WITHDRAW " + aman.getAccountNumber() + " 99999", () -> aman.withdraw(99999));
        processor.submit("slow TRANSFER", () -> {          // a long-running transfer triggers a warning
            pause(600);
            riya.transfer(aman, 1000);
        });
        processor.stop();
        System.out.println("   succeeded: " + processor.getSucceededCount() + ", failed: " + processor.getFailures().size());
        processor.getFailures().forEach(failure -> System.out.println("   failed: " + failure));
        System.out.println("   tasks per pool thread: " + processor.getTasksPerThread());
        System.out.println("   final balances: " + riya + "  /  " + aman);
    }

    private static void producerConsumerDemo() throws InterruptedException {
        System.out.println();
        System.out.println("2) Producer-consumer with wait()/notifyAll() (buffer of 3)");
        Account account = new SavingsAccount("Buffer Test", 1000, 0);
        TransactionBuffer buffer = new TransactionBuffer(3);
        int total = 8;

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= total; i++) {
                    Command command = new Command(TransactionType.DEPOSIT, account.getAccountNumber(), i * 10L);
                    buffer.put(command);
                    System.out.println("   produced #" + i + " " + command.type() + " " + command.amount());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= total; i++) {
                    Command command = buffer.take();
                    account.deposit(command.amount());
                    System.out.println("   consumed #" + i + " " + command.type() + " " + command.amount()
                            + " -> balance " + account.getBalance());
                    pause(20);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (BankException e) {
                System.out.println("   failed: " + e.getMessage());
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("   expected balance 1000 + 10+20+...+80 = 1360, actual " + account.getBalance());
    }

    private static void deadlockDemo() throws InterruptedException {
        System.out.println();
        System.out.println("3a) Opposite lock order: A->B and B->A");
        Account a = new SavingsAccount("A", 5000, 0);
        Account b = new SavingsAccount("B", 5000, 0);
        Thread t1 = new Thread(() -> naiveTransfer(a, b, 100), "transfer-A-to-B");
        Thread t2 = new Thread(() -> naiveTransfer(b, a, 100), "transfer-B-to-A");
        t1.setDaemon(true);        // daemon threads do not keep the JVM alive once main ends
        t2.setDaemon(true);
        t1.start();
        t2.start();
        t1.join(1500);
        t2.join(1500);

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        long[] deadlocked = threads.findDeadlockedThreads();
        if (deadlocked == null) {
            System.out.println("   no deadlock this time (both threads finished)");
        } else {
            System.out.println("   DEADLOCK detected - the program would hang forever:");
            for (ThreadInfo info : threads.getThreadInfo(deadlocked)) {
                System.out.println("     " + info.getThreadName() + " is " + info.getThreadState()
                        + ", waiting for a lock held by " + info.getLockOwnerName());
            }
        }

        System.out.println();
        System.out.println("3b) Fixed: Account.transfer() always locks the smaller account number first");
        Account c = new SavingsAccount("C", 5000, 0);
        Account d = new SavingsAccount("D", 5000, 0);
        Thread t3 = new Thread(() -> repeatTransfer(c, d), "transfer-C-to-D");
        Thread t4 = new Thread(() -> repeatTransfer(d, c), "transfer-D-to-C");
        long start = System.currentTimeMillis();
        t3.start();
        t4.start();
        t3.join();
        t4.join();
        System.out.println("   both threads finished 50 transfers each in " + (System.currentTimeMillis() - start) + " ms");
        System.out.println("   balances: " + c.getAccountNumber() + "=" + c.getBalance() + ", "
                + d.getAccountNumber() + "=" + d.getBalance() + " (total still " + (c.getBalance() + d.getBalance()) + ")");
    }

    /** Locks the source first, then the target - opposite calls lock in opposite order. */
    private static void naiveTransfer(Account from, Account to, long amount) {
        synchronized (from) {
            pause(100);                               // let the other thread lock its first account
            synchronized (to) {
                try {
                    from.withdraw(amount);
                    to.deposit(amount);
                } catch (BankException e) {
                    System.out.println("   failed: " + e.getMessage());
                }
            }
        }
    }

    private static void repeatTransfer(Account from, Account to) {
        for (int i = 0; i < 50; i++) {
            try {
                from.transfer(to, 100);
            } catch (BankException e) {
                System.out.println("   failed: " + e.getMessage());
            }
            pause(1);
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
