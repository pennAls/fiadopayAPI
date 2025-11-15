package edu.ucsal.fiadopay.controller;

import edu.ucsal.fiadopay.domain.Payment;

public record PaymentCreationDTO(Payment payment,boolean isNew) {

}
