package com.ecommers.pagos.service;

import com.ecommers.pagos.dto.PaymentDto.PaymentRequest;
import com.ecommers.pagos.dto.PaymentDto.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse getPaymentById(Long id);

    PaymentResponse getPaymentByOrder(Long orderId);

    List<PaymentResponse> getPaymentsByUser(Long userId);

    PaymentResponse refundPayment(Long id);
}
