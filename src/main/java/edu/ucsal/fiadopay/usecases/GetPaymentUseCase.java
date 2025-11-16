package edu.ucsal.fiadopay.usecases;

import edu.ucsal.fiadopay.controller.PaymentResponse;
import edu.ucsal.fiadopay.mappers.PaymentMapper;
import edu.ucsal.fiadopay.repo.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GetPaymentUseCase {
    private final PaymentMapper mapper;
    private final PaymentRepository payments;


    public GetPaymentUseCase(PaymentMapper mapper, PaymentRepository payments) {
        this.mapper = mapper;
        this.payments = payments;
    }

    public PaymentResponse execute(String paymentId) {
            return mapper.toResponse(payments.findById(paymentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        }
    }

