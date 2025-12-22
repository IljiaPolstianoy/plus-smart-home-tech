CREATE TYPE QuantityState AS ENUM ('ENDED', 'FEW', 'ENOUGH', 'MANY');

CREATE TYPE ProductState AS ENUM ('ACTIVE', 'DEACTIVATE');

CREATE TYPE ProductCategory AS ENUM ('LIGHTING', 'CONTROL', 'SENSOR');

CREATE TABLE IF NOT EXISTS product
(
    product_id       VARCHAR PRIMARY KEY,
    product_name     VARCHAR         NOT NULL,
    description      VARCHAR         NOT NULL,
    image_src         VARCHAR,
    quantity_state   QuantityState   NOT NULL,
    product_state    ProductState    NOT NULL,
    product_category ProductCategory NOT NULL,
    price            BIGINT          NOT NULL
);