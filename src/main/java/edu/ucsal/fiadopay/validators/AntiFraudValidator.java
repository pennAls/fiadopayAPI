package edu.ucsal.fiadopay.validators;

import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.strategies.PaymentStategy;

import java.math.BigDecimal;

public class AntiFraudValidator implements PaymentStategy {
    private final PaymentStategy paymentStategy;
    private final double threshold;

    public AntiFraudValidator(PaymentStategy paymentStategy, double threshold) {
        this.paymentStategy = paymentStategy;
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
