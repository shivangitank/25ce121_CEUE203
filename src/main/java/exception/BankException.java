package exception;

/**
 * Base class of every MiniBank business error. It is a checked exception
 * (extends Exception), so the compiler forces callers to handle or declare it.
 */
public class BankException extends Exception {
    private static final long serialVersionUID = 1L;

    public BankException(String message) {
        super(message);
    }

    public BankException(String message, Throwable cause) {
        super(message, cause);
    }
}
