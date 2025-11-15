package edu.ucsal.fiadopay.decorators;

import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.strategies.PaymentStategy;

import java.math.BigDecimal;

public class AntiFraudDecorator implements PaymentStategy {
    private final double threshold;

    public AntiFraudDecorator( double threshold) {
        this.threshold = threshold;
    }

    @Override
    public Payment.Status process(Payment payment) {
        if (payment.getAmount().compareTo(BigDecimal.valueOf(this.threshold)) < 0) {
            return Payment.Status.APPROVED;
        } else {
            return Payment.Status.DECLINED;
        }
    }
}
