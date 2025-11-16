package edu.ucsal.fiadopay.workflows;

import edu.ucsal.fiadopay.mappers.PaymentMapper;
import edu.ucsal.fiadopay.usecases.CreateWebhookUseCase;
import edu.ucsal.fiadopay.usecases.DispatchWebhookUseCase;
import edu.ucsal.fiadopay.usecases.RefundPaymentUseCase;
import edu.ucsal.fiadopay.usecases.ValidateMerchantAuthUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class RefundsWorkflow {
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final DispatchWebhookUseCase dispatchWebhookUseCase;
    private final ValidateMerchantAuthUseCase validateMerchantAuthUseCase;
    private final CreateWebhookUseCase createWebhookUseCase;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public Map<String, Object> execute(String auth, String paymentId) {
        var merchant = validateMerchantAuthUseCase.execute(auth);
        var mid = merchant.getId();
        var refundedPayment = refundPaymentUseCase.execute(mid, paymentId);

        executorService.submit(() -> {
            try {
                var delivery = createWebhookUseCase.execute(refundedPayment.getId());
                if (delivery != null) {
                    dispatchWebhookUseCase.execute(delivery.getId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return Map.of("id", "ref_" + UUID.randomUUID(), "status", "PENDING");
    }
}