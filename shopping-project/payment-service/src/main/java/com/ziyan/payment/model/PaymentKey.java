package com.ziyan.payment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;

/**
 * Composite primary key for idempotency
 * payment_id: partition key (identifies payment)
 * request_id: clustering key (ensures idempotency)
 */
@PrimaryKeyClass
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentKey implements Serializable {

    @PrimaryKeyColumn(name = "payment_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String paymentId;

    @PrimaryKeyColumn(name = "request_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private String requestId;
}
