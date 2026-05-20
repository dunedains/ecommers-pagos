package com.ecommers.pagos.service.impl;

import com.ecommers.pagos.client.NotificationClient;
import com.ecommers.pagos.client.OrderClient;
import com.ecommers.pagos.dto.PaymentDto.*;

import java.util.Map;
import com.ecommers.pagos.exception.PaymentNotFoundException;
import com.ecommers.pagos.model.Payment;
import com.ecommers.pagos.repository.PaymentRepository;
import com.ecommers.pagos.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String STATUS_PENDING   = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED    = "FAILED";
    private static final String STATUS_REFUNDED  = "REFUNDED";

    private static final String ORDER_CONFIRMED  = "CONFIRMED";
    private static final String ORDER_CANCELLED  = "CANCELLED";

    private final PaymentRepository repository;
    private final OrderClient orderClient;
    private final NotificationClient notificationClient;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Procesando pago orderId={}, userId={}, amount={}, method={}", request.orderId(), request.userId(), request.amount(), request.method());
        // Valida que la orden exista y no esté cancelada
        OrderDto order = orderClient.getOrderById(request.orderId());
        if (ORDER_CANCELLED.equals(order.status())) {
            throw new IllegalArgumentException("No se puede procesar el pago de una orden cancelada");
        }

        // Verifica que la orden no tenga ya un pago completado
        repository.findByOrderId(request.orderId()).ifPresent(existing -> {
            if (STATUS_COMPLETED.equals(existing.getStatus())) {
                throw new IllegalArgumentException("La orden ya tiene un pago completado");
            }
        });

        Payment payment = new Payment();
        payment.setOrderId(request.orderId());
        payment.setUserId(request.userId());
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setStatus(STATUS_PENDING);

        Payment saved = repository.save(payment);

        // Simula la aprobación del pago y actualiza la orden
        try {
            saved.setStatus(STATUS_COMPLETED);
            repository.save(saved);
            log.info("Pago id={} completado para orden={}", saved.getId(), request.orderId());
            orderClient.updateOrderStatus(request.orderId(), ORDER_CONFIRMED);
            notify(request.userId(), "PAYMENT_COMPLETED",
                    "Tu pago de $" + request.amount() + " para la orden #" + request.orderId() + " fue procesado exitosamente");
        } catch (Exception e) {
            log.error("Error al procesar pago para orden {}: {} - {}", request.orderId(), e.getClass().getSimpleName(), e.getMessage());
            saved.setStatus(STATUS_FAILED);
            repository.save(saved);
            notify(request.userId(), "PAYMENT_FAILED",
                    "No se pudo procesar el pago para la orden #" + request.orderId());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        log.info("Buscando pago id={}", id);
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrder(Long orderId) {
        log.info("Buscando pago por orderId={}", orderId);
        Payment payment = repository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("No existe pago para la orden: " + orderId));
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUser(Long userId) {
        log.info("Listando pagos del usuario userId={}", userId);
        return repository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(Long id) {
        log.info("Procesando reembolso pago id={}", id);
        Payment payment = findOrThrow(id);

        if (!STATUS_COMPLETED.equals(payment.getStatus())) {
            throw new IllegalArgumentException("Solo se pueden reembolsar pagos con estado COMPLETED");
        }

        payment.setStatus(STATUS_REFUNDED);
        log.info("Reembolso completado pago id={}, orderId={}", id, payment.getOrderId());
        orderClient.updateOrderStatus(payment.getOrderId(), ORDER_CANCELLED);
        notify(payment.getUserId(), "PAYMENT_REFUNDED",
                "Se realizó el reembolso de $" + payment.getAmount() + " de la orden #" + payment.getOrderId());

        return toResponse(repository.save(payment));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void notify(Long userId, String type, String message) {
        try {
            notificationClient.send(Map.of("userId", userId, "type", type, "message", message));
        } catch (Exception e) {
            log.warn("No se pudo enviar notificación [{}] al usuario {}: {}", type, userId, e.getMessage());
        }
    }

    private Payment findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Pago no encontrado con id: " + id));
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getOrderId(),
                p.getUserId(),
                p.getAmount(),
                p.getMethod(),
                p.getStatus()
        );
    }
}
