package service;

import model.Account;
import model.Premium;
import model.WithdrawRule;

/**
 * The bank's standard withdrawal rules, written as lambdas of the
 * WithdrawRule functional interface.
 */
public final class WithdrawPolicy {
    public static final long TRANSACTION_LIMIT = 50_000;
    public static final long PREMIUM_TRANSACTION_LIMIT = 2_00_000;

    /** Frozen accounts cannot withdraw. */
    public static final WithdrawRule ACTIVE_ONLY = (account, amount) -> account.isActive();

    /** At most 50,000 per withdrawal, or 2,00,000 for Premium accounts. */
    public static final WithdrawRule WITHIN_TRANSACTION_LIMIT =
            (account, amount) -> amount <= transactionLimitFor(account);

    /** Both rules together - what MiniBank applies to every withdrawal. */
    public static final WithdrawRule STANDARD = ACTIVE_ONLY.and(WITHIN_TRANSACTION_LIMIT);

    private WithdrawPolicy() {
        // only constants and static methods
    }

    public static long transactionLimitFor(Account account) {
        return account instanceof Premium ? PREMIUM_TRANSACTION_LIMIT : TRANSACTION_LIMIT;
    }
}
