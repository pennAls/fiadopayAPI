package edu.ucsal.fiadopay.workflows;

import edu.ucsal.fiadopay.controller.PaymentCreationDTO;
import edu.ucsal.fiadopay.controller.PaymentRequest;
import edu.ucsal.fiadopay.controller.PaymentResponse;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.usecases.AuthorizePaymentUseCase;
import edu.ucsal.fiadopay.usecases.CreatePendingPaymentUseCase;
import edu.ucsal.fiadopay.usecases.CreateWebhookUseCase;
import edu.ucsal.fiadopay.usecases.DispatchWebhookUseCase;
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
    private final CreateWebhookUseCase createWebhookUseCase;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public PaymentResponse execute(String auth, String idemKey, PaymentRequest req) {
        PaymentCreationDTO result = createPendingPaymentUseCase.createPayment(auth, idemKey, req);
        Payment payment = result.payment();

        if (result.isNew()) {
            executorService.submit(() -> {
                try {
                    authorizePaymentUseCase.execute(payment.getId());
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    var delivery = createWebhookUseCase.execute(payment);
                    if (delivery != null) {
                        executorService.submit(() -> dispatchWebhookUseCase.tryDeliver(delivery.getId()));
                    }
                }
            });
        }
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(), p.getStatus().name(), p.getMethod(),
                p.getAmount(), p.getInstallments(), p.getMonthlyInterest(),
                p.getTotalWithInterest()
        );
    }
}
