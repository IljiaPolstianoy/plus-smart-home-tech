CREATE TYPE ShoppingCartState AS ENUM ('ACTIVE', 'DEACTIVATE');

CREATE TABLE IF NOT EXISTS shopping_cart
(
    shopping_cart_id    VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    userName            VARCHAR           NOT NULL,
    shopping_cart_state ShoppingCartState NOT NULL
);

CREATE TABLE IF NOT EXISTS shopping_cart_and_product
(
    shopping_cart_id VARCHAR(36) REFERENCES shopping_cart (shopping_cart_id) ON DELETE CASCADE,
    product_id       VARCHAR REFERENCES product (product_id),
    quantity         BIGINT NOT NULL,
    PRIMARY KEY (shopping_cart_id, product_id)
)