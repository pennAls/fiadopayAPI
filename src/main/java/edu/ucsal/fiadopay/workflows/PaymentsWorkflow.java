package edu.ucsal.fiadopay.workflows;

import edu.ucsal.fiadopay.controller.PaymentCreationDTO;
import edu.ucsal.fiadopay.controller.PaymentRequest;
import edu.ucsal.fiadopay.controller.PaymentResponse;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.usecases.AuthorizePaymentUseCase;
import edu.ucsal.fiadopay.usecases.CreatePendingPaymentUseCase;
import edu.ucsal.fiadopay.usecases.DispatchWebhookUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentsWorkflow {
    private final CreatePendingPaymentUseCase createPendingPaymentUseCase;
    private final AuthorizePaymentUseCase authorizePaymentUseCase;
    private final DispatchWebhookUseCase dispatchWebhookUseCase;

    public PaymentResponse execute (String auth, String idemKey, PaymentRequest req) throws Exception {
        PaymentCreationDTO result = createPendingPaymentUseCase.createPayment(auth, idemKey, req);
        Payment payment = result.payment();

        if(result.isNew()) {
            return toResponse(payment);
        } else{
            throw new Exception("Erro de pagamento");
        }
    }
    private PaymentResponse toResponse(Payment p){
        return new PaymentResponse(
                p.getId(), p.getStatus().name(), p.getMethod(),
                p.getAmount(), p.getInstallments(), p.getMonthlyInterest(),
                p.getTotalWithInterest()
        );
    }
}
