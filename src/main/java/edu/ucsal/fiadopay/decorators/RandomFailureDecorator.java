package edu.ucsal.fiadopay.decorators;

import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.strategies.PaymentStategy;

public class RandomFailureDecorator implements PaymentStategy {
    private final double failRate;

    public RandomFailureDecorator(double failRate) {
        this.failRate = failRate;
    }

    @Override
    public Payment.Status process(Payment payment) {
        var approved = Math.random() > failRate;
        return approved ? Payment.Status.APPROVED : Payment.Status.DECLINED;
    }
}
