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
