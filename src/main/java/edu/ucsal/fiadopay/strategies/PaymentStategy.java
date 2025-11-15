package edu.ucsal.fiadopay.strategies;

import edu.ucsal.fiadopay.domain.Payment;

public interface PaymentStategy {
    Payment.Status process(Payment payment);
}
