package com.ziyan.payment.gateway;

import com.ziyan.payment.exception.PaymentException;
import com.ziyan.payment.model.Payment;

public interface PaymentGateway {
    void processPayment(Payment payment) throws PaymentException;
}
