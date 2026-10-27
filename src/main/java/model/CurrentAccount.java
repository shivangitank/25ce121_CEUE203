package model;

/**
 * Current (business) account: no interest, but the balance may go below zero
 * down to minus the overdraft limit. Business accounts are Premium (higher limits).
 */
public class CurrentAccount extends Account implements Premium {
    public static final long DEFAULT_OVERDRAFT = 10000;

    private final long overdraftLimit;

    public CurrentAccount(String ownerName, long openingBalance, long overdraftLimit) {
        super(ownerName, openingBalance);
        this.overdraftLimit = overdraftLimit;
    }

    public CurrentAccount(String ownerName, long openingBalance) {
        this(ownerName, openingBalance, DEFAULT_OVERDRAFT);
    }

    public CurrentAccount(String ownerName) {
        super(ownerName);
        this.overdraftLimit = DEFAULT_OVERDRAFT;
    }

    public CurrentAccount(String accountNumber, String ownerName, long balance, long overdraftLimit) {
        super(accountNumber, ownerName, balance);
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    public double interestRate() {
        return 0;
    }

    /** Allowed while the balance stays at or above -overdraftLimit. */
    @Override
    public boolean canWithdraw(long amount) {
        return getBalance() - amount >= -overdraftLimit;
    }

    /** The balance plus the unused overdraft. */
    @Override
    public long getAvailableBalance() {
        return getBalance() + overdraftLimit;
    }

    public long getOverdraftLimit() {
        return overdraftLimit;
    }
}
