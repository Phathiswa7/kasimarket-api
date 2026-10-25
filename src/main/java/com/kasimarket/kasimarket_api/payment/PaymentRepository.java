package com.kasimarket.kasimarket_api.payment;

import org.springframework.data.jpa.repository.JpaRepository;

// Lets our code save payments without writing SQL.
public interface PaymentRepository extends JpaRepository<Payment, Long> {
}