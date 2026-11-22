CREATE TABLE tb_cart (
    id SERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE,
    establishment_id BIGINT,
    updated_at TIMESTAMP NOT NULL
);
CREATE TABLE tb_cart_item (
    id SERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES tb_cart(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);
DELETE FROM tb_order;
ALTER TABLE tb_order
    ADD COLUMN establishment_id BIGINT NOT NULL,
    ADD COLUMN provider_id BIGINT NOT NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL,
    ADD COLUMN total_amount DOUBLE PRECISION NOT NULL,
    ADD COLUMN delivery_zip_code VARCHAR(8) NOT NULL,
    ADD COLUMN delivery_street VARCHAR(255) NOT NULL,
    ADD COLUMN delivery_number VARCHAR(20) NOT NULL,
    ADD COLUMN delivery_complement VARCHAR(255),
    ADD COLUMN delivery_district VARCHAR(255) NOT NULL,
    ADD COLUMN delivery_city VARCHAR(255) NOT NULL,
    ADD COLUMN delivery_state CHAR(2) NOT NULL,
    ADD COLUMN payment_id BIGINT,
    ADD COLUMN paid_at TIMESTAMP;
ALTER TABLE tb_order_item ADD COLUMN product_name VARCHAR(255) NOT NULL DEFAULT '';
