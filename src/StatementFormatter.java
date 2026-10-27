import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Builds a multi-line account statement. StringBuilder appends into one
 * growing buffer instead of creating a new String for every '+'.
 */
public final class StatementFormatter {
    private static final String LINE = "=".repeat(46);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private StatementFormatter() {
        // only static methods
    }

    public static String buildStatement(Account account) {
        StringBuilder sb = new StringBuilder();
        sb.append(LINE).append('\n');
        sb.append("              MINIBANK STATEMENT\n");
        sb.append(LINE).append('\n');
        sb.append(String.format("Account No : %s%n", account.getAccountNumber()));
        sb.append(String.format("Owner      : %s%n", account.getOwnerName()));
        sb.append(String.format("Type       : %s%n", account.getAccountType()));
        sb.append(String.format("Status     : %s%n", account.isActive() ? "ACTIVE" : "INACTIVE"));
        sb.append(String.format("Balance    : %s%n", rupees(account.getBalance())));
        sb.append(String.format(Locale.ROOT, "Interest   : %.1f%% a year%n", account.interestRate()));
        sb.append(String.format("Generated  : %s%n", LocalDateTime.now().format(STAMP)));
        sb.append(LINE);
        return sb.toString();
    }

    /** Formats an amount as "Rs. 12,500". */
    public static String rupees(long amount) {
        return String.format(Locale.ROOT, "Rs. %,d", amount);
    }
}
