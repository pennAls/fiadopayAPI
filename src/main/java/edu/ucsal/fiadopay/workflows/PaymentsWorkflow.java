package edu.ucsal.fiadopay.workflows;

import edu.ucsal.fiadopay.controller.PaymentCreationDTO;
import edu.ucsal.fiadopay.controller.PaymentRequest;
import edu.ucsal.fiadopay.controller.PaymentResponse;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.mappers.PaymentMapper;
import edu.ucsal.fiadopay.usecases.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class PaymentsWorkflow {
    private final CreatePendingPaymentUseCase createPendingPaymentUseCase;
    private final AuthorizePaymentUseCase authorizePaymentUseCase;
    private final DispatchWebhookUseCase dispatchWebhookUseCase;
    private final ValidateMerchantAuthUseCase validateMerchantAuthUseCase;
    private final CreateWebhookUseCase createWebhookUseCase;
    private final PaymentMapper mapper;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public PaymentResponse execute(String auth, String idemKey, PaymentRequest req) {
        var merchant = validateMerchantAuthUseCase.execute(auth);
        var mid = merchant.getId();
        PaymentCreationDTO result = createPendingPaymentUseCase.createPayment(mid, idemKey, req);
        Payment payment = result.payment();

        if (result.isNew()) {
            executorService.submit(() -> {
                try {
                    authorizePaymentUseCase.execute(payment.getId());
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    var delivery = createWebhookUseCase.execute(payment.getId());
                    if (delivery != null) {
                        executorService.submit(() -> dispatchWebhookUseCase.execute(delivery.getId()));
                    }
                }
            });
        }
        return mapper.toResponse(payment);
    }
}
