CREATE TABLE tb_payment (
                            id SERIAL PRIMARY KEY,
                            reference_type VARCHAR(20) NOT NULL,
                            reference_id BIGINT NOT NULL,
                            user_id BIGINT NOT NULL,
                            method VARCHAR(20) NOT NULL,
                            amount DOUBLE PRECISION NOT NULL CHECK (amount > 0),
                            currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
                            status VARCHAR(20) NOT NULL,
                            transaction_code VARCHAR(36) NOT NULL UNIQUE,
                            created_at TIMESTAMP NOT NULL,
                            UNIQUE (reference_type, reference_id)
);
