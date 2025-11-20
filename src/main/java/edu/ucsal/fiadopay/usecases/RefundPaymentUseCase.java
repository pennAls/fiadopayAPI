package edu.ucsal.fiadopay.usecases;

import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.repo.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;


@Component
public class RefundPaymentUseCase {
    private final PaymentRepository payments;

    public RefundPaymentUseCase(PaymentRepository payments) {
        this.payments = payments;
    }

    public Payment execute(Long mid,String paymentId){
        var p = payments.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!mid.equals(p.getMerchantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        p.setStatus(Payment.Status.REFUNDED);
        p.setUpdatedAt(Instant.now());
        payments.save(p);
        return p;
    }
}
