package com.ziyan.payment.gateway;

import com.ziyan.payment.exception.PaymentException;
import com.ziyan.payment.model.Payment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "payment.gateway.type", havingValue = "stripe")
public class StripePaymentGateway implements PaymentGateway {

    @Override
    public void processPayment(Payment payment) throws PaymentException {
        // In production, this would call Stripe API:
        // com.stripe.Stripe.setApiKey("sk_live_...");
        // com.stripe.model.Charge.create(...)
        
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Invalid payment amount");
        }

        if (payment.getAmount().compareTo(new BigDecimal("999999.99")) > 0) {
            throw new PaymentException("Amount exceeds maximum limit");
        }

        // Simulate real API call
        try {
            Thread.sleep(1000);  // Real API has higher latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Stripe API timeout");
        }

        System.out.println("✓ STRIPE PAYMENT PROCESSED: " 
            + payment.getAmount() + " " 
            + payment.getCurrency() 
            + " for Order #" + payment.getOrderId());

        // TODO: Implement real Stripe API call
        // if (response.getStatus() != "succeeded") {
        //     throw new PaymentException("Stripe error: " + response.getError());
        // }
    }
}
