CREATE TABLE IF NOT EXISTS products_in_warehouse
(
    product_id VARCHAR PRIMARY KEY,
    fragile    BOOLEAN        NOT NULL,
    width      DECIMAL(10, 2) NOT NULL CHECK (width > 0),
    height     DECIMAL(10, 2) NOT NULL CHECK (height > 0),
    depth      DECIMAL(10, 2) NOT NULL CHECK (depth > 0),
    weight     DECIMAL(10, 2) NOT NULL CHECK (weight > 0)
);

CREATE TABLE IF NOT EXISTS product_quantity
(
    product_quantity_id VARCHAR REFERENCES products_in_warehouse (product_id) PRIMARY KEY,
    quantity            BIGINT
);

CREATE TABLE IF NOT EXISTS order_booking
(
    order_booking_id VARCHAR PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         VARCHAR NOT NULL UNIQUE,
    delivery_id      VARCHAR NULL
);

CREATE TABLE IF NOT EXISTS booked_product
(
    order_booking_id VARCHAR REFERENCES order_booking (order_booking_id) ON DELETE CASCADE,
    product_id       VARCHAR REFERENCES product (product_id) ON DELETE CASCADE,
    quantity         BIGINT,
    PRIMARY KEY (order_booking_id, product_id)
)