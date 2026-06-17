package com.ecommers.pagos.controller;

import com.ecommers.pagos.dto.PaymentDto.PaymentRequest;
import com.ecommers.pagos.dto.PaymentDto.PaymentResponse;
import com.ecommers.pagos.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    @PostMapping
    public ResponseEntity<EntityModel<PaymentResponse>> processPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toModel(service.processPayment(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(toModel(service.getPaymentById(id)));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<EntityModel<PaymentResponse>> getPaymentByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(toModel(service.getPaymentByOrder(orderId)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<CollectionModel<EntityModel<PaymentResponse>>> getPaymentsByUser(@PathVariable Long userId) {
        List<EntityModel<PaymentResponse>> payments = service.getPaymentsByUser(userId).stream()
                .map(this::toModel)
                .toList();
        return ResponseEntity.ok(CollectionModel.of(payments,
                linkTo(methodOn(PaymentController.class).getPaymentsByUser(userId)).withSelfRel()));
    }

    @PatchMapping("/{id}/refund")
    public ResponseEntity<EntityModel<PaymentResponse>> refundPayment(@PathVariable Long id) {
        return ResponseEntity.ok(toModel(service.refundPayment(id)));
    }

    private EntityModel<PaymentResponse> toModel(PaymentResponse payment) {
        return EntityModel.of(payment,
                linkTo(methodOn(PaymentController.class).getPaymentById(payment.id())).withSelfRel(),
                linkTo(methodOn(PaymentController.class).getPaymentByOrder(payment.orderId())).withRel("order-payment"),
                linkTo(methodOn(PaymentController.class).getPaymentsByUser(payment.userId())).withRel("user-payments"));
    }
}
