package model;

import exception.DailyLimitExceededException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;

/** Anything money can be put into and taken out of. */
public interface Transactable {

    void deposit(long amount) throws InvalidAmountException;

    /** @return true when the money was taken out (failures are reported by exceptions) */
    boolean withdraw(long amount)
            throws InsufficientFundsException, InvalidAmountException, DailyLimitExceededException;
}
