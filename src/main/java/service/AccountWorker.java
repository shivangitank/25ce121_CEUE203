package service;

import exception.InvalidAmountException;
import model.Account;

/** A task that deposits the same amount into one account many times. */
public class AccountWorker implements Runnable {
    private final Account account;
    private final int times;
    private final long amount;

    public AccountWorker(Account account, int times, long amount) {
        this.account = account;
        this.times = times;
        this.amount = amount;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        System.out.println("  " + name + " started (" + Thread.currentThread().getState() + ")");
        for (int i = 0; i < times; i++) {
            try {
                account.deposit(amount);
            } catch (InvalidAmountException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        System.out.println("  " + name + " finished " + times + " deposits");
    }
}
