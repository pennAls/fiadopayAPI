package edu.ucsal.fiadopay.strategies;

import edu.ucsal.fiadopay.annotations.PaymentMethod;
import edu.ucsal.fiadopay.domain.Payment;
import org.springframework.stereotype.Component;

@Component
@PaymentMethod(type = "PIX")
public class PixStrategy implements PaymentStategy {
    @Override
    public Payment.Status process(Payment payment) {
        return Payment.Status.APPROVED;
    }
}
