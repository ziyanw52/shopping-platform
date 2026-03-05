package com.ziyan.payment.repository;

import com.ziyan.payment.model.Payment;
import com.ziyan.payment.model.PaymentKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends CassandraRepository<Payment, PaymentKey> {
    Optional<Payment> findByKeyPaymentId(String paymentId);
}
