package edu.ucsal.fiadopay.strategies;

import edu.ucsal.fiadopay.domain.Payment;

public class CardStrategy implements PaymentStategy{
    @Override
    public Payment.Status process(Payment payment) {
        return null;
    }
}
