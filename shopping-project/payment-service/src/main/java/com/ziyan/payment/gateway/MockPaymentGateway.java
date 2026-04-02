package com.ziyan.payment.gateway;

import com.ziyan.payment.exception.PaymentException;
import com.ziyan.payment.model.Payment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "payment.gateway.type", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public void processPayment(Payment payment) throws PaymentException {
        // Validation
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new PaymentException("Invalid payment amount");
        }

        // ===== MOCK FAILURE SCENARIOS FOR DEMO =====
        
        // Check if requestId contains mock failure trigger
        String requestId = payment.getKey().getRequestId();
        
        if (requestId != null) {
            if (requestId.contains("mock-card-declined") || requestId.contains("test-fail-card")) {
                throw new PaymentException("MOCK FAILURE: Card declined - insufficient funds or card blocked");
            }
            if (requestId.contains("mock-insufficient-funds") || requestId.contains("test-fail-funds")) {
                throw new PaymentException("MOCK FAILURE: Insufficient funds in account");
            }
            if (requestId.contains("mock-exceeds-limit") || requestId.contains("test-fail-limit")) {
                throw new PaymentException("MOCK FAILURE: Transaction amount exceeds daily limit ($10,000)");
            }
        }
        
        // Legacy: Also support exact amount matching for backward compatibility
        // Mock: Card Declined - amount 666.66
        if (payment.getAmount().compareTo(new BigDecimal("666.66")) == 0) {
            throw new PaymentException("MOCK FAILURE: Card declined - insufficient funds or card blocked");
        }

        // Mock: Insufficient Funds - amount 999.99
        if (payment.getAmount().compareTo(new BigDecimal("999.99")) == 0) {
            throw new PaymentException("MOCK FAILURE: Insufficient funds in account");
        }

        // Mock: Amount Exceeds Limit - amount >= 10000
        if (payment.getAmount().compareTo(new BigDecimal("10000")) >= 0) {
            throw new PaymentException("MOCK FAILURE: Transaction amount exceeds daily limit ($10,000)");
        }

        // Legacy test amount
        if (payment.getAmount().compareTo(new BigDecimal("4242.42")) == 0) {
            throw new PaymentException("MOCK FAILURE: Card declined - test failure amount");
        }

        // ===== END MOCK FAILURES =====

        // Simulate API call delay
        try {
            Thread.sleep(200);  // Simulate network latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Payment processing interrupted");
        }

        // Success - log for debugging
        System.out.println("✓ MOCK PAYMENT PROCESSED: " 
            + payment.getAmount() + " " 
            + payment.getCurrency() 
            + " for Order #" + payment.getOrderId());
    }
}
