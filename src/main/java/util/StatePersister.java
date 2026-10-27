package util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import model.Account;
import model.Customer;

/**
 * Saves and loads MiniBank's objects with Java serialization (byte streams).
 * Each save writes a temporary file first and then replaces the old file, so
 * a crash in the middle of saving never leaves a half-written accounts file.
 */
public final class StatePersister {

    private StatePersister() {
        // only static methods
    }

    public static void save(Account[] accounts, Path file) throws IOException {
        writeObject(accounts, file);
    }

    /** @return the saved accounts, or an empty array if the file does not exist yet */
    public static Account[] load(Path file) throws IOException {
        return Files.exists(file) ? readObject(file, Account[].class) : new Account[0];
    }

    public static void saveCustomers(Customer[] customers, Path file) throws IOException {
        writeObject(customers, file);
    }

    public static Customer[] loadCustomers(Path file) throws IOException {
        return Files.exists(file) ? readObject(file, Customer[].class) : new Customer[0];
    }

    private static void writeObject(Object data, Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temp = parent.resolve(file.getFileName() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))) {
            out.writeObject(data);
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    private static <T> T readObject(Path file, Class<T> type) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            return type.cast(in.readObject());
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException(file + " does not contain " + type.getSimpleName() + " data", e);
        }
    }
}
