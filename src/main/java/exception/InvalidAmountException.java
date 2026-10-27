package exception;

/** Thrown for a deposit or withdrawal amount that is zero or negative. */
public class InvalidAmountException extends BankException {
    private static final long serialVersionUID = 1L;

    private final long amount;

    public InvalidAmountException(long amount) {
        super("Invalid amount " + amount + ": must be greater than 0");
        this.amount = amount;
    }

    public long getAmount() {
        return amount;
    }
}
