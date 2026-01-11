CREATE TYPE payment_state AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAILED'
    );

CREATE TABLE IF NOT EXISTS payment
(
    payment_id     VARCHAR PRIMARY KEY DEFAULT gen_random_uuid(),
    total_product  DECIMAL(10, 2),
    delivery_total DECIMAL(10, 2),
    total_payment  DECIMAL(10, 2),
    state          payment_state,
    free_total     DECIMAL(10, 2),
    order_id       VARCHAR
);