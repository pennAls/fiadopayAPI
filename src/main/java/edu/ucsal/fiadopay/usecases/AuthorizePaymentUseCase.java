package edu.ucsal.fiadopay.usecases;

import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.factories.PaymentStrategyFactory;
import edu.ucsal.fiadopay.repo.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class AuthorizePaymentUseCase {
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final PaymentRepository paymentRepository;

    public AuthorizePaymentUseCase(PaymentStrategyFactory paymentStrategyFactory, PaymentRepository paymentRepository) {
        this.paymentStrategyFactory = paymentStrategyFactory;
        this.paymentRepository = paymentRepository;
    }
    @Transactional
    public void execute(String paymentId) {
        var payment = paymentRepository.getOne(paymentId);
        var strategy = paymentStrategyFactory.getStrategy(payment.getMethod());
        var newStatus = strategy.process(payment);
        payment.setStatus(newStatus);
        paymentRepository.save(payment);
    }
}
