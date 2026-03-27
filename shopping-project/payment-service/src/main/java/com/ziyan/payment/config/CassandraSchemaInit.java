package com.ziyan.payment.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.datastax.oss.driver.api.core.CqlSession;

/**
 * Verifies Cassandra schema on application startup.
 * Keyspace and table creation is handled by CassandraConfig.
 */
@Component
public class CassandraSchemaInit {

    private final CqlSession cqlSession;

    public CassandraSchemaInit(CqlSession cqlSession) {
        this.cqlSession = cqlSession;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verifySchema() {
        try {
            cqlSession.execute("SELECT * FROM payments LIMIT 1");
            System.out.println("✓ Cassandra schema verified successfully");
        } catch (Exception e) {
            System.err.println("Warning: Cassandra schema verification failed: " + e.getMessage());
        }
    }
}
