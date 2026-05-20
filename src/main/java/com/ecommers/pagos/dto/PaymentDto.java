package com.ecommers.pagos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class PaymentDto {

    public record PaymentRequest(
            @NotNull(message = "orderId es obligatorio")
            Long orderId,

            @NotNull(message = "userId es obligatorio")
            Long userId,

            @NotNull(message = "El monto es obligatorio")
            @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
            Double amount,

            @NotBlank(message = "El método de pago es obligatorio")
            @Pattern(
                    regexp = "CREDIT_CARD|DEBIT_CARD|TRANSFER",
                    message = "Método inválido. Use: CREDIT_CARD, DEBIT_CARD o TRANSFER"
            )
            String method
    ) {}

    public record PaymentResponse(
            Long id,
            Long orderId,
            Long userId,
            Double amount,
            String method,
            String status
    ) {}

    public record OrderDto(
            Long id,
            Long userId,
            Long productId,
            Integer quantity,
            Double totalAmount,
            String status
    ) {}
}
