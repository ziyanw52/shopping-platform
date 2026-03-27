package com.ziyan.payment.service;

/**
 * Retry Service for failed payments
 * Handles automatic retry logic with exponential backoff
 */
public interface RetryService {
    /**
     * Schedule a payment retry
     * Retries after initial delay, then exponential backoff
     */
    void scheduleRetry(String paymentId, String failureReason);

    /**
     * Retry a failed payment
     */
    void retryPayment(String paymentId);

    /**
     * Get retry count for a payment
     */
    Integer getRetryCount(String paymentId);

    /**
     * Cleanup old retry entries
     */
    void cleanupOldRetries();
}
