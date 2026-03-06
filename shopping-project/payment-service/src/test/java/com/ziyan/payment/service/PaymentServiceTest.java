package com.ziyan.payment.service;

import com.ziyan.payment.dto.PaymentRequest;
import com.ziyan.payment.dto.PaymentResponse;
import com.ziyan.payment.entity.Payment;
import com.ziyan.payment.entity.PaymentStatus;
import com.ziyan.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService
 * Tests payment submission, lookup, refund, and Kafka event publishing
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest paymentRequest;
    private Payment testPayment;
    private Payment testPayment2;

    @BeforeEach
    void setUp() {
        // Setup payment request
        paymentRequest = new PaymentRequest();
        paymentRequest.setRequestId(UUID.randomUUID().toString());
        paymentRequest.setOrderId(1L);
        paymentRequest.setUserId(1L);
        paymentRequest.setAmount(299.99);
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setCardNumber("4111111111111111");

        // Setup test payment
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setRequestId(paymentRequest.getRequestId());
        testPayment.setOrderId(1L);
        testPayment.setUserId(1L);
        testPayment.setAmount(299.99);
        testPayment.setStatus(PaymentStatus.SUCCESS);
        testPayment.setCreatedAt(LocalDateTime.now());

        // Setup second test payment
        testPayment2 = new Payment();
        testPayment2.setId(2L);
        testPayment2.setRequestId(UUID.randomUUID().toString());
        testPayment2.setOrderId(2L);
        testPayment2.setUserId(2L);
        testPayment2.setAmount(199.99);
        testPayment2.setStatus(PaymentStatus.SUCCESS);
        testPayment2.setCreatedAt(LocalDateTime.now());
    }

    // ============ Payment Submission Tests ============

    @Test
    void testSubmitPaymentSuccess() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        // Act
        PaymentResponse result = paymentService.submitPayment(paymentRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals(299.99, result.getAmount());
        assertEquals(paymentRequest.getOrderId(), result.getOrderId());

        // Verify
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(kafkaTemplate, times(1)).send(anyString(), anyString());
    }

    @Test
    void testSubmitPaymentWithZeroAmount() {
        // Arrange
        PaymentRequest zeroRequest = new PaymentRequest();
        zeroRequest.setRequestId(UUID.randomUUID().toString());
        zeroRequest.setOrderId(1L);
        zeroRequest.setUserId(1L);
        zeroRequest.setAmount(0.0);
        zeroRequest.setPaymentMethod("CREDIT_CARD");

        Payment zeroPayment = new Payment();
        zeroPayment.setId(1L);
        zeroPayment.setAmount(0.0);
        zeroPayment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.save(any(Payment.class))).thenReturn(zeroPayment);
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        // Act
        PaymentResponse result = paymentService.submitPayment(zeroRequest);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getAmount());
    }

    @Test
    void testSubmitPaymentWithNegativeAmount() {
        // Arrange
        PaymentRequest negativeRequest = new PaymentRequest();
        negativeRequest.setRequestId(UUID.randomUUID().toString());
        negativeRequest.setOrderId(1L);
        negativeRequest.setUserId(1L);
        negativeRequest.setAmount(-100.0);
        negativeRequest.setPaymentMethod("CREDIT_CARD");

        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act & Assert
        assertDoesNotThrow(() -> paymentService.submitPayment(negativeRequest));
    }

    @Test
    void testSubmitPaymentWithLargeAmount() {
        // Arrange
        PaymentRequest largeRequest = new PaymentRequest();
        largeRequest.setRequestId(UUID.randomUUID().toString());
        largeRequest.setOrderId(1L);
        largeRequest.setUserId(1L);
        largeRequest.setAmount(999999.99);
        largeRequest.setPaymentMethod("CREDIT_CARD");

        Payment largePayment = new Payment();
        largePayment.setId(1L);
        largePayment.setAmount(999999.99);
        largePayment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.save(any(Payment.class))).thenReturn(largePayment);
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        // Act
        PaymentResponse result = paymentService.submitPayment(largeRequest);

        // Assert
        assertEquals(999999.99, result.getAmount());
    }

    @Test
    void testSubmitPaymentIdempotency() {
        // Arrange
        String requestId = UUID.randomUUID().toString();
        PaymentRequest request1 = new PaymentRequest();
        request1.setRequestId(requestId);
        request1.setOrderId(1L);
        request1.setAmount(100.0);
        request1.setPaymentMethod("CREDIT_CARD");

        PaymentRequest request2 = new PaymentRequest();
        request2.setRequestId(requestId);
        request2.setOrderId(1L);
        request2.setAmount(100.0);
        request2.setPaymentMethod("CREDIT_CARD");

        when(paymentRepository.findByRequestId(requestId)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        // Act
        PaymentResponse result1 = paymentService.submitPayment(request1);
        PaymentResponse result2 = paymentService.submitPayment(request2);

        // Assert - Both should return the same payment for idempotency
        assertEquals(result1.getId(), result2.getId());
    }

    @Test
    void testSubmitPaymentWithNullPaymentMethod() {
        // Arrange
        PaymentRequest nullMethodRequest = new PaymentRequest();
        nullMethodRequest.setRequestId(UUID.randomUUID().toString());
        nullMethodRequest.setOrderId(1L);
        nullMethodRequest.setAmount(100.0);
        nullMethodRequest.setPaymentMethod(null);

        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> paymentService.submitPayment(nullMethodRequest));
    }

    @Test
    void testSubmitPaymentDifferentMethods() {
        // Arrange
        String[] paymentMethods = {"CREDIT_CARD", "DEBIT_CARD", "PAYPAL", "BANK_TRANSFER"};
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        // Act & Assert
        for (String method : paymentMethods) {
            PaymentRequest request = new PaymentRequest();
            request.setRequestId(UUID.randomUUID().toString());
            request.setOrderId(1L);
            request.setAmount(100.0);
            request.setPaymentMethod(method);

            PaymentResponse response = paymentService.submitPayment(request);
            assertNotNull(response);
        }

        verify(paymentRepository, times(4)).save(any(Payment.class));
    }

    // ============ Payment Lookup Tests ============

    @Test
    void testLookupPaymentSuccess() {
        // Arrange
        String requestId = paymentRequest.getRequestId();
        when(paymentRepository.findByRequestId(requestId)).thenReturn(Optional.of(testPayment));

        // Act
        PaymentResponse result = paymentService.lookupPayment(requestId);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals(299.99, result.getAmount());

        // Verify
        verify(paymentRepository, times(1)).findByRequestId(requestId);
    }

    @Test
    void testLookupPaymentNotFound() {
        // Arrange
        String requestId = "nonexistent-request-id";
        when(paymentRepository.findByRequestId(requestId)).thenReturn(Optional.empty());

        // Act
        PaymentResponse result = paymentService.lookupPayment(requestId);

        // Assert
        assertNull(result);
        verify(paymentRepository, times(1)).findByRequestId(requestId);
    }

    @Test
    void testLookupPaymentWithNullRequestId() {
        // Arrange
        when(paymentRepository.findByRequestId(null)).thenReturn(Optional.empty());

        // Act
        PaymentResponse result = paymentService.lookupPayment(null);

        // Assert
        assertNull(result);
    }

    @Test
    void testLookupPaymentWithEmptyRequestId() {
        // Arrange
        when(paymentRepository.findByRequestId("")).thenReturn(Optional.empty());

        // Act
        PaymentResponse result = paymentService.lookupPayment("");

        // Assert
        assertNull(result);
    }

    @Test
    void testLookupMultiplePayments() {
        // Arrange
        String requestId1 = paymentRequest.getRequestId();
        String requestId2 = UUID.randomUUID().toString();

        when(paymentRepository.findByRequestId(requestId1)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.findByRequestId(requestId2)).thenReturn(Optional.of(testPayment2));

        // Act
        PaymentResponse result1 = paymentService.lookupPayment(requestId1);
        PaymentResponse result2 = paymentService.lookupPayment(requestId2);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotEquals(result1.getId(), result2.getId());
    }

    // ============ Payment Refund Tests ============

    @Test
    void testRefundPaymentSuccess() {
        // Arrange
        String requestId = paymentRequest.getRequestId();
        Payment refundablePayment = new Payment();
        refundablePayment.setId(1L);
        refundablePayment.setRequestId(requestId);
        refundablePayment.setStatus(PaymentStatus.SUCCESS);
        refundablePayment.setAmount(100.0);

        when(paymentRepository.findByRequestId(requestId)).thenReturn(Optional.of(refundablePayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundablePayment);
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        // Act
        PaymentResponse result = paymentService.refundPayment(requestId);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED, refundablePayment.getStatus());

        // Verify
        verify(paymentRepository, times(1)).findByRequestId(requestId);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(kafkaTemplate, times(1)).send(anyString(), anyString());
    }

    @Test
    void testRefundPaymentNotFound() {
        // Arrange
        when(paymentRepository.findByRequestId("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            Exception.class,
            () -> paymentService.refundPayment("nonexistent")
        );

        verify(paymentRepository, times(1)).findByRequestId("nonexistent");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testRefundPaymentAlreadyRefunded() {
        // Arrange
        String requestId = paymentRequest.getRequestId();
        Payment alreadyRefundedPayment = new Payment();
        alreadyRefundedPayment.setId(1L);
        alreadyRefundedPayment.setRequestId(requestId);
        alreadyRefundedPayment.setStatus(PaymentStatus.REFUNDED);

        when(paymentRepository.findByRequestId(requestId)).thenReturn(Optional.of(alreadyRefundedPayment));

        // Act & Assert
        assertThrows(
            Exception.class,
            () -> paymentService.refundPayment(requestId)
        );

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testRefundPaymentPartialRefund() {
        // Arrange
        String requestId = paymentRequest.getRequestId();
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setRequestId(requestId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setAmount(100.0);

        when(paymentRepository.findByRequestId(requestId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        // Act
        PaymentResponse result = paymentService.refundPayment(requestId);

        // Assert
        assertNotNull(result);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    // ============ Payment Status Tests ============

    @Test
    void testGetPaymentStatusSuccess() {
        // Arrange
        String requestId = paymentRequest.getRequestId();
        when(paymentRepository.findByRequestId(requestId)).thenReturn(Optional.of(testPayment));

        // Act
        PaymentStatus status = paymentService.getPaymentStatus(requestId);

        // Assert
        assertNotNull(status);
        assertEquals(PaymentStatus.SUCCESS, status);
    }

    @Test
    void testGetPaymentStatusNotFound() {
        // Arrange
        when(paymentRepository.findByRequestId("nonexistent")).thenReturn(Optional.empty());

        // Act
        PaymentStatus status = paymentService.getPaymentStatus("nonexistent");

        // Assert
        assertNull(status);
    }

    @Test
    void testGetPaymentStatusDifferentStatuses() {
        // Arrange
        PaymentStatus[] statuses = {PaymentStatus.PENDING, PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.REFUNDED};

        // Act & Assert
        for (PaymentStatus expectedStatus : statuses) {
            Payment payment = new Payment();
            payment.setStatus(expectedStatus);

            String requestId = UUID.randomUUID().toString();
            when(paymentRepository.findByRequestId(requestId)).thenReturn(Optional.of(payment));

            PaymentStatus result = paymentService.getPaymentStatus(requestId);
            assertEquals(expectedStatus, result);
        }
    }

    // ============ Multiple Operations Tests ============

    @Test
    void testSubmitAndLookupPayment() {
        // Arrange
        String requestId = paymentRequest.getRequestId();
        testPayment.setRequestId(requestId);

        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(paymentRepository.findByRequestId(requestId)).thenReturn(Optional.of(testPayment));
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        // Act - Submit
        PaymentResponse submitted = paymentService.submitPayment(paymentRequest);
        assertNotNull(submitted);

        // Act - Lookup
        PaymentResponse looked = paymentService.lookupPayment(requestId);

        // Assert
        assertNotNull(looked);
        assertEquals(submitted.getId(), looked.getId());
    }

    @Test
    void testSubmitAndRefundPayment() {
        // Arrange
        String requestId = paymentRequest.getRequestId();
        testPayment.setRequestId(requestId);
        testPayment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(paymentRepository.findByRequestId(requestId)).thenReturn(Optional.of(testPayment));
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        // Act - Submit
        PaymentResponse submitted = paymentService.submitPayment(paymentRequest);
        assertNotNull(submitted);

        // Act - Refund
        PaymentResponse refunded = paymentService.refundPayment(requestId);

        // Assert
        assertNotNull(refunded);
        assertEquals(PaymentStatus.REFUNDED, testPayment.getStatus());
        verify(kafkaTemplate, times(2)).send(anyString(), anyString());
    }

    @Test
    void testMultiplePaymentsSubmission() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment, testPayment2);
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        // Act
        PaymentRequest req1 = new PaymentRequest();
        req1.setRequestId(UUID.randomUUID().toString());
        req1.setOrderId(1L);
        req1.setAmount(100.0);
        req1.setPaymentMethod("CREDIT_CARD");

        PaymentRequest req2 = new PaymentRequest();
        req2.setRequestId(UUID.randomUUID().toString());
        req2.setOrderId(2L);
        req2.setAmount(200.0);
        req2.setPaymentMethod("PAYPAL");

        PaymentResponse res1 = paymentService.submitPayment(req1);
        PaymentResponse res2 = paymentService.submitPayment(req2);

        // Assert
        assertNotNull(res1);
        assertNotNull(res2);
        assertNotEquals(res1.getId(), res2.getId());
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(kafkaTemplate, times(2)).send(anyString(), anyString());
    }

    @Test
    void testPaymentLifecycle() {
        // Arrange
        String requestId = paymentRequest.getRequestId();
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setRequestId(requestId);
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentRepository.findByRequestId(requestId)).thenReturn(Optional.of(payment));
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(null);

        // Act - Submit
        payment.setStatus(PaymentStatus.SUCCESS);
        PaymentResponse submitted = paymentService.submitPayment(paymentRequest);
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());

        // Act - Lookup
        PaymentResponse looked = paymentService.lookupPayment(requestId);
        assertEquals(PaymentStatus.SUCCESS, looked.getStatus());

        // Act - Refund
        payment.setStatus(PaymentStatus.REFUNDED);
        PaymentResponse refunded = paymentService.refundPayment(requestId);
        assertEquals(PaymentStatus.REFUNDED, refunded.getStatus());

        // Verify
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(kafkaTemplate, times(2)).send(anyString(), anyString());
    }
}
