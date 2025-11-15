package edu.ucsal.fiadopay.strategies;

import edu.ucsal.fiadopay.annotations.AntiFraud;
import edu.ucsal.fiadopay.annotations.PaymentMethod;
import edu.ucsal.fiadopay.domain.Payment;
import org.springframework.stereotype.Component;

@Component
@PaymentMethod(type = "CARD")
@AntiFraud(threshold = 5000)
public class CardStrategy implements PaymentStategy{
    @Override
    public Payment.Status process(Payment payment) {
        return Payment.Status.APPROVED;
    }
}
