package com.ziyan.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

@Configuration
@EnableCassandraRepositories(basePackages = "com.ziyan.payment.repository")
public class CassandraConfig {
}
