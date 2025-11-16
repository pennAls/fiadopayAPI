package edu.ucsal.fiadopay.usecases;

import edu.ucsal.fiadopay.controller.PaymentCreationDTO;
import edu.ucsal.fiadopay.controller.PaymentRequest;
import edu.ucsal.fiadopay.controller.PaymentResponse;
import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.repo.MerchantRepository;
import edu.ucsal.fiadopay.repo.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class CreatePendingPaymentUseCase {
    private final MerchantRepository merchants;
    private final PaymentRepository payments;

    public CreatePendingPaymentUseCase(MerchantRepository merchants, PaymentRepository payments) {
        this.merchants = merchants;
        this.payments = payments;
    }
    @Transactional
    public PaymentCreationDTO createPayment(Long mid, String idemKey, PaymentRequest req){
        if (idemKey != null) {
            var existing = payments.findByIdempotencyKeyAndMerchantId(idemKey, mid);
            if(existing.isPresent())
                return new PaymentCreationDTO(existing.get(),false);
        }

        Double interest = null;
        BigDecimal total = req.amount();
        if ("CARD".equalsIgnoreCase(req.method()) && req.installments()!=null && req.installments()>1){
            interest = 1.0; // 1%/mês
            var base = new BigDecimal("1.01");
            var factor = base.pow(req.installments());
            total = req.amount().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        }

        var payment = Payment.builder()
                .id("pay_"+ UUID.randomUUID().toString().substring(0,8))
                .merchantId(mid)
                .method(req.method().toUpperCase())
                .amount(req.amount())
                .currency(req.currency())
                .installments(req.installments()==null?1:req.installments())
                .monthlyInterest(interest)
                .totalWithInterest(total)
                .status(Payment.Status.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .idempotencyKey(idemKey)
                .metadataOrderId(req.metadataOrderId())
                .build();

        payments.save(payment);
        return new PaymentCreationDTO(payment,true);
    }
}
