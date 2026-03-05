package com.ziyan.payment.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.datastax.oss.driver.api.core.CqlSession;

/**
 * Initialize Cassandra schema (keyspace and tables)
 * Runs on application startup
 */
@Component
public class CassandraSchemaInit {

    private final CqlSession cqlSession;

    public CassandraSchemaInit(CqlSession cqlSession) {
        this.cqlSession = cqlSession;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSchema() {
        try {
            // Create keyspace if not exists
            String createKeyspaceQuery = "CREATE KEYSPACE IF NOT EXISTS payment " +
                    "WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};";
            cqlSession.execute(createKeyspaceQuery);
            System.out.println("✓ Cassandra keyspace 'payment' created");

            // Switch to payment keyspace
            cqlSession.execute("USE payment;");

            // Create payments table with composite primary key for idempotency
            String createTableQuery = "CREATE TABLE IF NOT EXISTS payment.payments (" +
                    "payment_id text," +
                    "request_id text," +
                    "order_id bigint," +
                    "amount decimal," +
                    "currency text," +
                    "status text," +
                    "created_at timestamp," +
                    "updated_at timestamp," +
                    "PRIMARY KEY ((payment_id), request_id)" +
                    ");";
            cqlSession.execute(createTableQuery);
            System.out.println("✓ Cassandra table 'payments' created");
        } catch (Exception e) {
            System.err.println("Warning: Could not initialize Cassandra schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
