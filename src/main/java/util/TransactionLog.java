package util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import model.TransactionType;

/**
 * Append-only text log of transactions, one line each, for example
 * "DEPOSIT AC0001 500 2026-10-27T10:15:30". Lines are only ever added, never changed.
 */
public final class TransactionLog {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private TransactionLog() {
        // only static methods
    }

    /**
     * Adds one line to the end of the file, creating the file (and its folder)
     * if needed. Synchronized so lines from different threads never mix.
     */
    public static synchronized void append(Path file, String line) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Files.write(file, List.of(line), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** One log file per day, e.g. logs/transactions-2026-10-27.log. */
    public static Path dailyFile(Path logsDir, LocalDate day) {
        return logsDir.resolve("transactions-" + day + ".log");
    }

    /** "DEPOSIT AC0001 500 2026-10-27T10:15:30" */
    public static String entry(TransactionType type, String accountNumber, long amount) {
        return type + " " + accountNumber + " " + amount + " " + now();
    }

    /** "TRANSFER AC0001 500 AC0002 2026-10-27T10:15:30" (from, amount, to) */
    public static String transferEntry(String fromAccount, String toAccount, long amount) {
        return TransactionType.TRANSFER + " " + fromAccount + " " + amount + " " + toAccount + " " + now();
    }

    /** The current time as "2026-10-27T10:15:30" (seconds always shown). */
    public static String now() {
        return LocalDateTime.now().format(TIMESTAMP);
    }
}
