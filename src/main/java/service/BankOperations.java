package service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import exception.BankException;
import model.Account;
import model.AccountType;
import model.BankInfo;
import model.Command;

/**
 * Everything the user interface may ask the bank to do. The menu depends only
 * on this interface, not on how BankService implements it.
 */
public interface BankOperations {

    BankInfo getBankInfo();

    /** Loads saved accounts and customers; returns how many accounts were loaded. */
    int loadState() throws IOException;

    void saveState() throws IOException;

    Account openAccount(AccountType type, String ownerName, String mobile, String email, long openingDeposit)
            throws BankException;

    /** @return the new balance */
    long deposit(String accountNumber, long amount) throws BankException;

    /** @return the new balance */
    long withdraw(String accountNumber, long amount) throws BankException;

    void transfer(String fromAccount, String toAccount, long amount) throws BankException;

    String statement(String accountNumber) throws BankException;

    List<Account> listAccounts();

    List<Account> searchByOwner(String namePart);

    BatchResult processBatch(List<Command> commands) throws IOException, InterruptedException;

    /** Writes the end-of-day report and returns its path. */
    Path generateReport() throws IOException;
}
