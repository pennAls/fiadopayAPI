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

}
