package model;

/** Anything money can be put into and taken out of. */
public interface Transactable {

    void deposit(long amount);

    /** @return true if the money was taken out */
    boolean withdraw(long amount);
}
