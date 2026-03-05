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

        // Mock: Specific amount triggers decline (for testing failure scenarios)
        if (payment.getAmount().compareTo(new BigDecimal("4242.42")) == 0) {
            throw new PaymentException("Mock: Card declined - test failure amount");
        }

        // Mock: Amount over $10,000 triggers error
        if (payment.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            throw new PaymentException("Mock: Amount exceeds limit");
        }

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
