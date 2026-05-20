package com.ecommers.pagos.repository;

import com.ecommers.pagos.model.Payment;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends CrudRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(String status);
}
