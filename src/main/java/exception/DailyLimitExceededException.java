package exception;

/** Thrown when a withdrawal would take the day's total above the account's daily cap. */
public class DailyLimitExceededException extends BankException {
    private static final long serialVersionUID = 1L;

    private final long dailyLimit;
    private final long remaining;

    public DailyLimitExceededException(String accountNumber, long dailyLimit, long alreadyWithdrawn) {
        super("Daily withdrawal limit of " + dailyLimit + " reached for " + accountNumber
                + ": only " + (dailyLimit - alreadyWithdrawn) + " more can be withdrawn today");
        this.dailyLimit = dailyLimit;
        this.remaining = dailyLimit - alreadyWithdrawn;
    }

    public long getDailyLimit() {
        return dailyLimit;
    }

    /** What can still be withdrawn today. */
    public long getRemaining() {
        return remaining;
    }
}
