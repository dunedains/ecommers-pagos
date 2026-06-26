package com.ecommers.pagos.service.impl;

import com.ecommers.pagos.client.NotificationClient;
import com.ecommers.pagos.client.OrderClient;
import com.ecommers.pagos.dto.PaymentDto.OrderDto;
import com.ecommers.pagos.dto.PaymentDto.PaymentRequest;
import com.ecommers.pagos.dto.PaymentDto.PaymentResponse;
import com.ecommers.pagos.model.Payment;
import com.ecommers.pagos.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de la lógica de negocio de pagos.
 * Se mockean el repositorio y los clientes Feign (order/notification) para
 * validar los flujos críticos: aprobación, rechazos y reembolso, sin infraestructura.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository repository;
    @Mock
    private OrderClient orderClient;
    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private PaymentServiceImpl service;

    private PaymentRequest request() {
        return new PaymentRequest(100L, 2L, 59.97, "CREDIT_CARD");
    }

    @Test
    @DisplayName("processPayment: orden válida -> pago COMPLETED y la orden pasa a CONFIRMED")
    void processPayment_ordenValida_completaYConfirma() {
        // Given: la orden existe y está pendiente, sin pago previo
        when(orderClient.getOrderById(100L))
                .thenReturn(new OrderDto(100L, 2L, 10L, 3, 59.97, "PENDING"));
        when(repository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(repository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });

        // When
        PaymentResponse response = service.processPayment(request());

        // Then: pago completado, se confirma la orden y se notifica
        assertThat(response.status()).isEqualTo("COMPLETED");
        verify(orderClient).updateOrderStatus(100L, "CONFIRMED");
        verify(notificationClient).send(anyMap()); // PAYMENT_COMPLETED
    }

    @Test
    @DisplayName("processPayment: no se puede pagar una orden cancelada")
    void processPayment_ordenCancelada_lanzaExcepcion() {
        // Given: la orden está CANCELLED
        when(orderClient.getOrderById(100L))
                .thenReturn(new OrderDto(100L, 2L, 10L, 3, 59.97, "CANCELLED"));

        // When / Then: se rechaza antes de crear ningún pago
        assertThatThrownBy(() -> service.processPayment(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelada");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("processPayment: una orden con pago ya completado no se cobra dos veces")
    void processPayment_pagoYaCompletado_lanzaExcepcion() {
        // Given: ya existe un pago COMPLETED para esa orden
        when(orderClient.getOrderById(100L))
                .thenReturn(new OrderDto(100L, 2L, 10L, 3, 59.97, "PENDING"));
        Payment existente = new Payment();
        existente.setStatus("COMPLETED");
        when(repository.findByOrderId(100L)).thenReturn(Optional.of(existente));

        // When / Then
        assertThatThrownBy(() -> service.processPayment(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya tiene un pago completado");
    }

    @Test
    @DisplayName("processPayment: si falla la confirmación remota, el pago queda en FAILED")
    void processPayment_falloRemoto_marcaFailed() {
        // Given: orden válida, pero el servicio de órdenes falla al confirmar
        when(orderClient.getOrderById(100L))
                .thenReturn(new OrderDto(100L, 2L, 10L, 3, 59.97, "PENDING"));
        when(repository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(repository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });
        when(orderClient.updateOrderStatus(eq(100L), anyString()))
                .thenThrow(new RuntimeException("order-service caído"));

        // When
        PaymentResponse response = service.processPayment(request());

        // Then: el pago se marca FAILED y se notifica el fallo (no propaga la excepción)
        assertThat(response.status()).isEqualTo("FAILED");
        verify(notificationClient).send(anyMap()); // PAYMENT_FAILED
    }

    @Test
    @DisplayName("refundPayment: solo se reembolsan pagos COMPLETED")
    void refundPayment_noCompletado_lanzaExcepcion() {
        // Given: el pago 1 está en PENDING
        Payment pendiente = new Payment();
        pendiente.setId(1L);
        pendiente.setStatus("PENDING");
        when(repository.findById(1L)).thenReturn(Optional.of(pendiente));

        // When / Then
        assertThatThrownBy(() -> service.refundPayment(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COMPLETED");
        verify(orderClient, never()).updateOrderStatus(any(), anyString());
    }

    @Test
    @DisplayName("refundPayment: un pago COMPLETED pasa a REFUNDED y cancela la orden")
    void refundPayment_completado_reembolsaYCancelaOrden() {
        // Given: el pago 1 está COMPLETED para la orden 100
        Payment completado = new Payment();
        completado.setId(1L);
        completado.setOrderId(100L);
        completado.setUserId(2L);
        completado.setAmount(59.97);
        completado.setStatus("COMPLETED");
        when(repository.findById(1L)).thenReturn(Optional.of(completado));
        when(repository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        PaymentResponse response = service.refundPayment(1L);

        // Then: estado REFUNDED, se cancela la orden y se notifica
        assertThat(response.status()).isEqualTo("REFUNDED");
        verify(orderClient).updateOrderStatus(100L, "CANCELLED");
        verify(notificationClient).send(anyMap()); // PAYMENT_REFUNDED
    }

    @Test
    @DisplayName("getPaymentById: devuelve el pago existente")
    void getPaymentById_existente_devuelve() {
        Payment p = new Payment();
        p.setId(1L);
        p.setOrderId(100L);
        p.setAmount(59.97);
        p.setStatus("COMPLETED");
        when(repository.findById(1L)).thenReturn(Optional.of(p));

        assertThat(service.getPaymentById(1L).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getPaymentById: si no existe, lanza PaymentNotFoundException")
    void getPaymentById_inexistente_lanzaExcepcion() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPaymentById(99L))
                .isInstanceOf(com.ecommers.pagos.exception.PaymentNotFoundException.class);
    }

    @Test
    @DisplayName("getPaymentByOrder / getPaymentsByUser: devuelven datos del repositorio")
    void getters_devuelvenDatos() {
        Payment p = new Payment();
        p.setId(1L);
        p.setOrderId(100L);
        p.setUserId(2L);
        p.setAmount(59.97);
        p.setStatus("COMPLETED");
        when(repository.findByOrderId(100L)).thenReturn(Optional.of(p));
        when(repository.findByUserId(2L)).thenReturn(java.util.List.of(p));

        assertThat(service.getPaymentByOrder(100L).orderId()).isEqualTo(100L);
        assertThat(service.getPaymentsByUser(2L)).hasSize(1);
    }
}
