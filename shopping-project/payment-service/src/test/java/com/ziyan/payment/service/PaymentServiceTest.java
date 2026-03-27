package com.ziyan.payment.service;

import com.ziyan.payment.dto.PaymentRequest;
import com.ziyan.payment.dto.PaymentResponse;
import com.ziyan.payment.model.Payment;
import com.ziyan.payment.enums.PaymentStatus;
import com.ziyan.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Minimal unit tests for PaymentService
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void testContextLoads() {
        assertNotNull(paymentService);
    }
}
