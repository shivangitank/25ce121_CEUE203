package model;

/**
 * Savings account: earns 4% a year and must always keep a minimum balance.
 */
public class SavingsAccount extends Account {
    private static final long serialVersionUID = 1L;

    public static final long DEFAULT_MIN_BALANCE = 1000;

    private final long minBalance;

    public SavingsAccount(String ownerName, long openingBalance, long minBalance) {
        super(ownerName, openingBalance);
        this.minBalance = minBalance;
    }

    public SavingsAccount(String ownerName, long openingBalance) {
        this(ownerName, openingBalance, DEFAULT_MIN_BALANCE);
    }

    public SavingsAccount(String accountNumber, String ownerName, long balance, long minBalance) {
        super(accountNumber, ownerName, balance);
        this.minBalance = minBalance;
    }

    @Override
    public double interestRate() {
        return 4.0;
    }

    /** Allowed only if the balance left behind is at least minBalance. */
    @Override
    public boolean canWithdraw(long amount) {
        return getBalance() - amount >= minBalance;
    }

    /** Everything above the minimum balance. */
    @Override
    public long getAvailableBalance() {
        return Math.max(getBalance() - minBalance, 0);
    }

    public long getMinBalance() {
        return minBalance;
    }
}
