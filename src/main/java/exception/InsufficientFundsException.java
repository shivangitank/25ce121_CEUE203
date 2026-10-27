package exception;

/** Thrown when a withdrawal would break the account's balance rule. Carries the shortfall. */
public class InsufficientFundsException extends BankException {
    private static final long serialVersionUID = 1L;

    private final String accountNumber;
    private final long requested;
    private final long shortfall;

    public InsufficientFundsException(String accountNumber, long requested, long shortfall) {
        super("Insufficient funds in " + accountNumber + ": requested " + requested + ", short by " + shortfall);
        this.accountNumber = accountNumber;
        this.requested = requested;
        this.shortfall = shortfall;
    }

    /** For accounts that block withdrawals for a reason other than the balance (e.g. a locked deposit). */
    public InsufficientFundsException(String accountNumber, long requested, long shortfall, String reason) {
        super("Cannot withdraw " + requested + " from " + accountNumber + ": " + reason);
        this.accountNumber = accountNumber;
        this.requested = requested;
        this.shortfall = shortfall;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public long getRequested() {
        return requested;
    }

    /** How much more money would have been needed. */
    public long getShortfall() {
        return shortfall;
    }
}
