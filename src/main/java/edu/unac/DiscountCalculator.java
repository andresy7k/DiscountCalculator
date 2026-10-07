package edu.unac;

import edu.unac.domain.InvalidPurchaseException;

public class DiscountCalculator {
    public double calculateFinalPrice(double amount, boolean premiumCustomer) {

        validateAmount(amount);

        double discount = calculateDiscount(amount, premiumCustomer);

        return amount - (amount * discount);
    }

    private void validateAmount(double amount) {

        if (amount <= 0) {
            throw new InvalidPurchaseException(
                    "Amount must be greater than zero"
            );
        }
    }

    private double calculateDiscount(
            double amount,
            boolean premiumCustomer
    ) {

        if (premiumCustomer && amount >= 500) {
            return 0.20;
        }

        if (premiumCustomer) {
            return 0.10;
        }

        if (amount >= 500) {
            return 0.05;
        }

        return 0.0;
    }

}
