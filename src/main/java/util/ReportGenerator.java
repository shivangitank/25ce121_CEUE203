package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Builds the end-of-day report: walks the logs folder with NIO, reads every
 * .log file line by line and totals the deposits, withdrawals and transfers.
 */
public final class ReportGenerator {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** The totals found in the logs. */
    public record Summary(int filesRead, int filesSkipped, int malformedLines,
                          int depositCount, long totalDeposits,
                          int withdrawalCount, long totalWithdrawals,
                          int transferCount, long totalTransfers) {

        /** Money that came into the bank minus money that left it (transfers move money inside the bank). */
        public long netChange() {
            return totalDeposits - totalWithdrawals;
        }
    }

    private ReportGenerator() {
        // only static methods
    }

    /** Reads every log in logsDir, writes the report to reportFile and returns the totals. */
    public static Summary generate(Path logsDir, Path reportFile) throws IOException {
        int filesRead = 0;
        int filesSkipped = 0;
        int malformed = 0;
        int depositCount = 0;
        int withdrawalCount = 0;
        int transferCount = 0;
        long deposits = 0;
        long withdrawals = 0;
        long transfers = 0;
        StringBuilder fileDetails = new StringBuilder();

        for (Path file : listFiles(logsDir)) {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            String name = file.getFileName().toString();
            if (!attributes.isRegularFile() || !name.endsWith(".log") || attributes.size() == 0) {
                filesSkipped++;
                fileDetails.append(String.format("  %-32s skipped (%s)%n", name,
                        attributes.size() == 0 ? "empty" : "not a .log file"));
                continue;
            }
            filesRead++;
            LocalDateTime modified = LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneId.systemDefault());
            fileDetails.append(String.format(Locale.ROOT, "  %-32s %,8d bytes   modified %s%n",
                    name, attributes.size(), modified.format(STAMP)));

            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length < 3 || !Validator.isValidAmount(parts[2])) {
                        malformed++;
                        continue;
                    }
                    long amount = Long.parseLong(parts[2]);
                    switch (parts[0]) {
                        case "DEPOSIT" -> {
                            depositCount++;
                            deposits += amount;
                        }
                        case "WITHDRAW" -> {
                            withdrawalCount++;
                            withdrawals += amount;
                        }
                        case "TRANSFER" -> {
                            transferCount++;
                            transfers += amount;
                        }
                        default -> malformed++;
                    }
                }
            }
        }

        Summary summary = new Summary(filesRead, filesSkipped, malformed, depositCount, deposits,
                withdrawalCount, withdrawals, transferCount, transfers);
        Path parent = reportFile.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Files.writeString(reportFile, format(summary, logsDir, fileDetails.toString()), StandardCharsets.UTF_8);
        return summary;
    }

    /** The files in the folder, sorted by name so the report is always in the same order. */
    private static List<Path> listFiles(Path logsDir) throws IOException {
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(logsDir)) {
            return files;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDir)) {
            for (Path file : stream) {
                files.add(file);
            }
        }
        Collections.sort(files);
        return files;
    }

    private static String format(Summary s, Path logsDir, String fileDetails) {
        String line = "-".repeat(56);
        StringBuilder sb = new StringBuilder();
        sb.append("MiniBank - End-of-day report\n");
        sb.append("Generated   : ").append(LocalDateTime.now().format(STAMP)).append('\n');
        sb.append("Logs folder : ").append(logsDir).append('\n');
        sb.append(line).append('\n');
        sb.append("Files (").append(s.filesRead()).append(" read, ").append(s.filesSkipped()).append(" skipped):\n");
        sb.append(fileDetails.isEmpty() ? "  (no log files)\n" : fileDetails);
        sb.append(line).append('\n');
        sb.append(String.format(Locale.ROOT, "Deposits    : %4d   %s%n", s.depositCount(), StatementFormatter.rupees(s.totalDeposits())));
        sb.append(String.format(Locale.ROOT, "Withdrawals : %4d   %s%n", s.withdrawalCount(), StatementFormatter.rupees(s.totalWithdrawals())));
        sb.append(String.format(Locale.ROOT, "Transfers   : %4d   %s (moved between accounts)%n", s.transferCount(), StatementFormatter.rupees(s.totalTransfers())));
        sb.append(String.format(Locale.ROOT, "Net change  :        %s%n", StatementFormatter.rupees(s.netChange())));
        if (s.malformedLines() > 0) {
            sb.append("Malformed lines skipped: ").append(s.malformedLines()).append('\n');
        }
        return sb.toString();
    }
}
