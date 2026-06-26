package com.ecommers.pagos.controller;

import com.ecommers.pagos.dto.PaymentDto.PaymentResponse;
import com.ecommers.pagos.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService service;

    private PaymentResponse sample(String status) {
        return new PaymentResponse(1L, 100L, 2L, 59.97, "CREDIT_CARD", status);
    }

    @Test
    @DisplayName("POST /api/payments válido -> 201")
    void process_devuelve201() throws Exception {
        when(service.processPayment(any())).thenReturn(sample("COMPLETED"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":100,\"userId\":2,\"amount\":59.97,\"method\":\"CREDIT_CARD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/payments con método inválido -> 400")
    void process_metodoInvalido_devuelve400() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":100,\"userId\":2,\"amount\":59.97,\"method\":\"CASH\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/payments/{id} -> 200")
    void getById_devuelve200() throws Exception {
        when(service.getPaymentById(1L)).thenReturn(sample("COMPLETED"));

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/payments/order/{orderId} -> 200")
    void getByOrder_devuelve200() throws Exception {
        when(service.getPaymentByOrder(100L)).thenReturn(sample("COMPLETED"));

        mockMvc.perform(get("/api/payments/order/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100));
    }

    @Test
    @DisplayName("PATCH /api/payments/{id}/refund -> 200")
    void refund_devuelve200() throws Exception {
        when(service.refundPayment(1L)).thenReturn(sample("REFUNDED"));

        mockMvc.perform(patch("/api/payments/1/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }
}
