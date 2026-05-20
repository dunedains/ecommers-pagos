package com.ecommers.pagos.exception;

public record ErrorResponse(int status, String message, String timestamp) {
}
