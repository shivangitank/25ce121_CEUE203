package model;

/**
 * A bank policy that decides whether a withdrawal may go ahead. It has one
 * abstract method, so it can be written as a lambda:
 * {@code WithdrawRule rule = (account, amount) -> account.isActive();}
 */
@FunctionalInterface
public interface WithdrawRule {

    boolean allow(Account account, long amount);

    /** Combines two rules: the withdrawal must pass both. */
    default WithdrawRule and(WithdrawRule other) {
        return (account, amount) -> allow(account, amount) && other.allow(account, amount);
    }
}
