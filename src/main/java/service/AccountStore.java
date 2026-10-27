package service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import exception.AccountNotFoundException;
import exception.BankException;
import model.Account;

/**
 * Keeps accounts in a HashMap keyed by account number, so finding one is a
 * single lookup instead of a search through an array.
 */
public class AccountStore {
    /** Highest balance first; ties broken by account number (natural order). */
    public static final Comparator<Account> BY_BALANCE_DESC =
            Comparator.comparingLong(Account::getBalance).reversed().thenComparing(Comparator.naturalOrder());

    private final HashMap<String, Account> accounts = new HashMap<>();
    private final ArrayList<String> history = new ArrayList<>();

    public void add(Account account) {
        accounts.put(account.getAccountNumber(), account);
        history.add("ADD " + account.getAccountNumber() + " " + account.getOwnerName());
    }

    /** @return the account, or null if there is no account with that number */
    public Account get(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public Collection<Account> getAll() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    public int size() {
        return accounts.size();
    }

    public void deposit(String accountNumber, long amount) throws BankException {
        require(accountNumber).deposit(amount);
        history.add("DEPOSIT " + accountNumber + " " + amount);
    }

    public void withdraw(String accountNumber, long amount) throws BankException {
        require(accountNumber).withdraw(amount);
        history.add("WITHDRAW " + accountNumber + " " + amount);
    }

    /** Operations done through this store, oldest first. */
    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /** A new list sorted with a Comparator: highest balance first. */
    public ArrayList<Account> sortedByBalance() {
        ArrayList<Account> list = new ArrayList<>(accounts.values());
        list.sort(BY_BALANCE_DESC);
        return list;
    }

    /** A new list in natural order (Comparable: by account number). */
    public ArrayList<Account> sortedByNumber() {
        ArrayList<Account> list = new ArrayList<>(accounts.values());
        Collections.sort(list);
        return list;
    }

    /** Accounts whose balance is greater than the given amount. */
    public List<Account> balanceAbove(long amount) {
        List<Account> result = new ArrayList<>();
        for (Account account : accounts.values()) {
            if (account.getBalance() > amount) {
                result.add(account);
            }
        }
        return result;
    }

    /** A TreeMap keeps its keys sorted, unlike the HashMap used for storage. */
    public TreeMap<String, Account> asSortedMap() {
        return new TreeMap<>(accounts);
    }

    private Account require(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }
}
