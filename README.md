# Pagos Service

Microservicio de procesamiento de pagos. Gestiona el pago de órdenes y actualiza el estado de la orden al confirmar o reembolsar.

## Información general

| Campo | Valor |
|-------|-------|
| Puerto | `8088` |
| Base de datos | `db_pagos` (PostgreSQL) |
| Contexto | `/api/payments` |

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/payments` | Procesar pago de una orden |
| `GET` | `/api/payments/{id}` | Obtener pago por ID |
| `GET` | `/api/payments/order/{orderId}` | Obtener pago por orden |
| `GET` | `/api/payments/user/{userId}` | Listar pagos de un usuario |
| `PATCH` | `/api/payments/{id}/refund` | Reembolsar pago |

## Métodos de pago aceptados

- `CREDIT_CARD`
- `DEBIT_CARD`
- `TRANSFER`

## Estados de un pago

```
PENDING → COMPLETED  (pago exitoso, orden pasa a CONFIRMED)
PENDING → FAILED     (error al procesar)
COMPLETED → REFUNDED (reembolso, orden pasa a CANCELLED)
```

## Ejemplo de uso

**Procesar pago:**
```bash
curl -X POST http://localhost:8088/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "userId": 1,
    "amount": 1499.99,
    "method": "CREDIT_CARD"
  }'
```

**Respuesta:**
```json
{
  "id": 1,
  "orderId": 1,
  "userId": 1,
  "amount": 1499.99,
  "method": "CREDIT_CARD",
  "status": "COMPLETED"
}
```

**Reembolsar:**
```bash
curl -X PATCH http://localhost:8088/api/payments/1/refund
```

## Modelo de datos

```sql
CREATE TABLE payments (
    id       BIGINT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT           NOT NULL UNIQUE,
    user_id  BIGINT           NOT NULL,
    amount   DOUBLE PRECISION NOT NULL,
    method   VARCHAR(30)      NOT NULL,
    status   VARCHAR(20)      NOT NULL DEFAULT 'PENDING'
);
```

## Dependencias externas

| Servicio | Uso | Puerto |
|---------|-----|--------|
| **orders** | Valida estado de la orden y actualiza a CONFIRMED/CANCELLED | `8087` |
| **notifications** | Envía notificaciones al completar, fallar o reembolsar pagos | `8089` |

> **Nota técnica:** usa Apache HttpClient 5 (`feign-hc5`) para que Feign pueda enviar peticiones PATCH, ya que `HttpURLConnection` de Java no soporta ese método HTTP.

## Configuración (variables de entorno Docker)

| Variable | Descripción |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | URL de conexión a PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos |
| `FEIGN_CLIENT_ORDER_URL` | URL del servicio de órdenes |
| `FEIGN_CLIENT_NOTIFICATION_URL` | URL del servicio de notificaciones |

## Tecnologías

- Java 25 · Spring Boot 4.0.6
- Spring Data JPA · Hibernate 7
- Spring Cloud OpenFeign + Apache HttpClient 5
- Flyway (migraciones)
- PostgreSQL 16
- Lombok · Bean Validation
