package com.ziyan.payment.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.net.InetSocketAddress;

@Configuration
@EnableCassandraRepositories(basePackages = "com.ziyan.payment.repository")
public class CassandraConfig {

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.port}")
    private int port;

    @Value("${spring.cassandra.keyspace-name}")
    private String keyspaceName;

    @Value("${spring.cassandra.local-datacenter}")
    private String datacenter;

    @Bean
    public CqlSession cassandraSession() {
        // First connect WITHOUT keyspace to create it
        try (CqlSession tempSession = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(contactPoints, port))
                .withLocalDatacenter(datacenter)
                .build()) {

            tempSession.execute(
                "CREATE KEYSPACE IF NOT EXISTS " + keyspaceName +
                " WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}");

            tempSession.execute(
                "CREATE TABLE IF NOT EXISTS " + keyspaceName + ".payments (" +
                "payment_id text," +
                "request_id text," +
                "order_id bigint," +
                "amount decimal," +
                "currency text," +
                "status text," +
                "created_at timestamp," +
                "updated_at timestamp," +
                "PRIMARY KEY ((payment_id), request_id)" +
                ")");

            System.out.println("✓ Cassandra keyspace '" + keyspaceName + "' and table 'payments' ready");
        }

        // Now return a session connected to the keyspace
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress(contactPoints, port))
                .withLocalDatacenter(datacenter)
                .withKeyspace(keyspaceName)
                .build();
    }
}
