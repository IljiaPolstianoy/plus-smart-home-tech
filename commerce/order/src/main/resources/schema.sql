CREATE TYPE order_state AS ENUM (
    'NEW',
    'ON_PAYMENT',
    'ON_DELIVERY',
    'DONE',
    'DELIVERED',
    'ASSEMBLED',
    'PAID',
    'COMPLETED',
    'DELIVERY_FAILED',
    'ASSEMBLY_FAILED',
    'PAYMENT_FAILED',
    'PRODUCT_RETURNED',
    'CANCELED'
    );

CREATE TABLE IF NOT EXISTS orders
(
    order_id         VARCHAR PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    shopping_cart_id VARCHAR(255) REFERENCES shopping_cart (shopping_cart_id) ON DELETE CASCADE,
    payment_id       VARCHAR(255),
    delivery_id      VARCHAR(255),
    state            order_state,
    delivery_weight  DECIMAL(10, 2),
    delivery_volume  DECIMAL(10, 2),
    fragile          BOOLEAN,
    total_price      DECIMAL(10, 2),
    delivery_price   DECIMAL(10, 2),
    product_price    DECIMAL(10, 2)
);

CREATE TABLE IF NOT EXISTS orders_products
(
    order_id   VARCHAR REFERENCES orders (order_id) ON DELETE CASCADE,
    product_id VARCHAR REFERENCES product (product_id) ON DELETE CASCADE,
    quantity   BIGINT NOT NULL,
    PRIMARY KEY (order_id, product_id)
);