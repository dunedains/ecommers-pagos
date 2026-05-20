package com.ecommers.pagos.client;

import com.ecommers.pagos.dto.PaymentDto.OrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${feign.client.order-url}")
public interface OrderClient {

    @GetMapping("/api/orders/{id}")
    OrderDto getOrderById(@PathVariable Long id);

    @PatchMapping("/api/orders/{id}/status")
    OrderDto updateOrderStatus(@PathVariable Long id, @RequestParam String status);
}
