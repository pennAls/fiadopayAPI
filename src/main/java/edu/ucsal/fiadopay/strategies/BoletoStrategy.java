package edu.ucsal.fiadopay.strategies;

import edu.ucsal.fiadopay.annotations.PaymentMethod;
import edu.ucsal.fiadopay.annotations.RandomicFailureRate;
import edu.ucsal.fiadopay.domain.Payment;
import org.springframework.stereotype.Component;

@Component
@PaymentMethod(type = "BOLETO")
@RandomicFailureRate(failureRate = 0.5)
public class BoletoStrategy implements PaymentStategy{
    @Override
    public Payment.Status process(Payment payment) {
        return Payment.Status.APPROVED;
    }
}
