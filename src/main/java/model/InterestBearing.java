package model;

/**
 * Something that earns interest on its balance. Implementing classes only
 * supply interestRate() and getBalance(); the default methods do the rest.
 */
public interface InterestBearing {

    /** Yearly rate in percent. */
    double interestRate();

    long getBalance();

    /** Interest for one year at the current balance. */
    default double yearlyInterest() {
        return interestForMonths(12);
    }

    /** Interest for one month at the current balance. */
    default double monthlyInterest() {
        return interestForMonths(1);
    }

    /** Simple interest P x R x T / 100. */
    static double simpleInterest(long principal, double ratePercent, double years) {
        return principal * ratePercent * years / 100;
    }

    /** Shared helper for the default methods; not visible to implementing classes. */
    private double interestForMonths(int months) {
        return simpleInterest(getBalance(), interestRate(), months / 12.0);
    }
}
