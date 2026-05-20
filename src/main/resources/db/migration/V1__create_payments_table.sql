CREATE TABLE payments (
    id         BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id   BIGINT         NOT NULL,
    user_id    BIGINT         NOT NULL,
    amount     DOUBLE PRECISION NOT NULL,
    method     VARCHAR(30)    NOT NULL,
    status     VARCHAR(20)    DEFAULT 'PENDING' NOT NULL,
    CONSTRAINT uq_payments_order_id UNIQUE (order_id)
);
