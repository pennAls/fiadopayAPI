package edu.ucsal.fiadopay.strategies;

import edu.ucsal.fiadopay.annotations.PaymentMethod;
import edu.ucsal.fiadopay.annotations.RandomicFailureRate;
import edu.ucsal.fiadopay.domain.Payment;
import org.springframework.stereotype.Component;

@Component
@PaymentMethod(type = "DEBITO")
@RandomicFailureRate(failureRate = 0.3)
public class DebitoStrategy implements PaymentStategy {
    @Override
    public Payment.Status process(Payment payment) {
        return Payment.Status.APPROVED;
    }
}
