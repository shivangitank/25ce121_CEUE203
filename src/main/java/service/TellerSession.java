package service;

import exception.BankException;
import model.Account;

/**
 * A teller's working session. It implements AutoCloseable, so a
 * try-with-resources block closes it automatically - even when an operation
 * inside the block throws an exception.
 */
public class TellerSession implements AutoCloseable {
    private final String tellerName;
    private int operations;
    private boolean open;

    public TellerSession(String tellerName) {
        this.tellerName = tellerName;
        this.open = true;
        System.out.println("  [session] opened for teller " + tellerName);
    }

    public void deposit(Account account, long amount) throws BankException {
        ensureOpen();
        account.deposit(amount);
        operations++;
    }

    public void withdraw(Account account, long amount) throws BankException {
        ensureOpen();
        account.withdraw(amount);
        operations++;
    }

    public boolean isOpen() {
        return open;
    }

    private void ensureOpen() {
        if (!open) {
            throw new IllegalStateException("Session for " + tellerName + " is closed");
        }
    }

    @Override
    public void close() {
        open = false;
        System.out.println("  [session] closed for teller " + tellerName + " after " + operations + " successful operation(s)");
    }
}
