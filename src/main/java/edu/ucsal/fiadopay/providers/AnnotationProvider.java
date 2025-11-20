package edu.ucsal.fiadopay.providers;

import edu.ucsal.fiadopay.strategies.PaymentStategy;

public interface AnnotationProvider {
    PaymentStategy apply (PaymentStategy current ,Class<?> CLass);
}
