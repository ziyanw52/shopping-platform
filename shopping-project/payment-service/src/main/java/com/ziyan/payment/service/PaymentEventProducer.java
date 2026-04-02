package com.ziyan.payment.service;

import com.ziyan.payment.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka producer for payment events
 * Publishes to topics: payment.success, payment.failed, payment.refunded
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    private static final String TOPIC_SUCCESS = "payment.success";
    private static final String TOPIC_FAILED = "payment.failed";
    private static final String TOPIC_REFUNDED = "payment.refunded";

    public void publishPaymentSuccess(PaymentEvent event) {
        event.setEventType("PAYMENT_SUCCESS");
        log.info("Publishing payment success event: paymentId={}, requestId={}", 
                event.getPaymentId(), event.getRequestId());
        kafkaTemplate.send(TOPIC_SUCCESS, event.getPaymentId(), event);
    }

    public void publishPaymentFailed(PaymentEvent event) {
        event.setEventType("PAYMENT_FAILED");
        log.info("Publishing payment failed event: paymentId={}, requestId={}", 
                event.getPaymentId(), event.getRequestId());
        kafkaTemplate.send(TOPIC_FAILED, event.getPaymentId(), event);
    }

    public void publishPaymentRefunded(PaymentEvent event) {
        event.setEventType("PAYMENT_REFUNDED");
        log.info("Publishing payment refunded event: paymentId={}, requestId={}", 
                event.getPaymentId(), event.getRequestId());
        kafkaTemplate.send(TOPIC_REFUNDED, event.getPaymentId(), event);
    }
}
