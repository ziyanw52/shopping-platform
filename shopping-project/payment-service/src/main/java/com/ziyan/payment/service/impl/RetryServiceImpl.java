package com.ziyan.payment.service.impl;

import com.ziyan.payment.service.RetryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Retry Service Implementation
 * Handles automatic retry of failed payments
 * Uses in-memory cache for retry tracking (can be replaced with database)
 */
@Service
@Slf4j
public class RetryServiceImpl implements RetryService {

    // In-memory cache: paymentId -> retryCount
    // TODO: Replace with database table for production
    private final ConcurrentHashMap<String, Integer> retryCountMap = new ConcurrentHashMap<>();

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_SECONDS = 300; // 5 minutes

    @Override
    @Async
    public void scheduleRetry(String paymentId, String failureReason) {
        Integer retryCount = retryCountMap.getOrDefault(paymentId, 0);

        if (retryCount >= MAX_RETRIES) {
            log.warn("✗ Payment {} exceeded max retries ({}). Giving up.", paymentId, MAX_RETRIES);
            return;
        }

        long delayMillis = calculateBackoffDelay(retryCount);
        log.info("⏱️ Scheduling retry for payment {} in {} seconds (Attempt {}/{})",
            paymentId, delayMillis / 1000, retryCount + 1, MAX_RETRIES);

        try {
            Thread.sleep(delayMillis);
            retryPayment(paymentId);
        } catch (InterruptedException e) {
            log.error("✗ Retry scheduling interrupted for payment {}", paymentId);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void retryPayment(String paymentId) {
        Integer retryCount = retryCountMap.getOrDefault(paymentId, 0);
        retryCountMap.put(paymentId, retryCount + 1);

        log.info("🔄 Retrying payment {} (Attempt {}/{})", paymentId, retryCount + 1, MAX_RETRIES);

        // TODO: Fetch payment from database and retry with payment gateway
        // paymentGateway.charge(payment);
    }

    @Override
    public Integer getRetryCount(String paymentId) {
        return retryCountMap.getOrDefault(paymentId, 0);
    }

    /**
     * Calculate exponential backoff delay
     * Attempt 0: 5 minutes
     * Attempt 1: 10 minutes
     * Attempt 2: 20 minutes
     */
    private long calculateBackoffDelay(Integer retryCount) {
        long delaySeconds = INITIAL_DELAY_SECONDS * (long) Math.pow(2, retryCount);
        return TimeUnit.SECONDS.toMillis(delaySeconds);
    }

    /**
     * Cleanup old retry entries (runs daily)
     * TODO: Remove entries older than 24 hours
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    public void cleanupOldRetries() {
        log.info("🧹 Cleaning up old retry entries...");
        // TODO: Implement cleanup logic
    }
}
