CREATE TYPE delivery_state AS ENUM
    (
        'CREATED', 'IN_PROGRESS', 'DELIVERED', 'FAILED', 'CANCELLED'
        );

CREATE TABLE IF NOT EXISTS delivery
(
    delivery_id  VARCHAR PRIMARY KEY DEFAULT gen_random_uuid(),
    total_volume DECIMAL(10, 2),
    total_weight DECIMAL(10, 2),
    fragile      BOOLEAN,
    address_from  VARCHAR REFERENCES address (address_id),
    address_to    VARCHAR REFERENCES address (address_id),
    state        delivery_state,
    order_id     VARCHAR

);

CREATE TABLE IF NOT EXISTS address
(
    address_id VARCHAR PRIMARY KEY DEFAULT gen_random_uuid(),
    country    VARCHAR,
    city       VARCHAR,
    street     VARCHAR,
    house      VARCHAR,
    flat       VARCHAR,
    CONSTRAINT unique_address UNIQUE (country, city, street, house, flat)
);