package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import model.Account;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.SavingsAccount;

/**
 * Builds a multi-line account statement. StringBuilder appends into one
 * growing buffer instead of creating a new String for every '+'.
 */
public final class StatementFormatter {
    public static final int RECENT_TRANSACTIONS = 10;

    private static final String LINE = "=".repeat(60);
    private static final String THIN_LINE = "-".repeat(60);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private StatementFormatter() {
        // only static methods
    }

    public static String buildStatement(Account account) {
        StringBuilder sb = new StringBuilder();
        sb.append(LINE).append('\n');
        sb.append("                     MINIBANK STATEMENT\n");
        sb.append(LINE).append('\n');
        sb.append(String.format("Account No : %s%n", account.getAccountNumber()));
        sb.append(String.format("Owner      : %s%n", account.getOwnerName()));
        sb.append(String.format("Type       : %s%n", account.getAccountType()));
        sb.append(String.format("Status     : %s%n", account.isActive() ? "ACTIVE" : "INACTIVE"));
        sb.append(String.format("Balance    : %s%n", rupees(account.getBalance())));
        sb.append(String.format("Available  : %s%n", rupees(account.getAvailableBalance())));
        sb.append(String.format(Locale.ROOT, "Interest   : %.1f%% a year (about %s this year)%n",
                account.interestRate(), rupees(Math.round(account.yearlyInterest()))));
        if (account instanceof SavingsAccount savings) {
            sb.append(String.format("Rule       : keep a minimum balance of %s%n", rupees(savings.getMinBalance())));
        } else if (account instanceof CurrentAccount current) {
            sb.append(String.format("Rule       : overdraft allowed up to %s%n", rupees(current.getOverdraftLimit())));
        } else if (account instanceof FixedDepositAccount fd) {
            sb.append(String.format("Rule       : locked until %s%s%n", fd.getMaturityDate(), fd.isMatured() ? " (matured)" : ""));
        }

        List<String> history = account.getHistory();
        int from = Math.max(0, history.size() - RECENT_TRANSACTIONS);
        sb.append(THIN_LINE).append('\n');
        sb.append("Transactions (").append(history.size() - from).append(" most recent of ")
                .append(history.size()).append("):\n");
        for (String entry : history.subList(from, history.size())) {
            sb.append("  ").append(entry).append('\n');
        }
        sb.append(THIN_LINE).append('\n');
        sb.append(String.format("Generated  : %s%n", LocalDateTime.now().format(STAMP)));
        sb.append(LINE);
        return sb.toString();
    }

    /** Formats an amount as "Rs. 12,500". */
    public static String rupees(long amount) {
        return String.format(Locale.ROOT, "Rs. %,d", amount);
    }
}
