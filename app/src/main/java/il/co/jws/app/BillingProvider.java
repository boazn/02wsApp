package il.co.jws.app;

/**
 * An interface that provides an access to BillingLibrary methods
 */
public interface BillingProvider {
    BillingManager getBillingManager();
    boolean isPremiumPurchased();
    boolean isMonthlySubscribed();
    boolean isYearlySubscribed();
    boolean isAlertsOnly();
}
