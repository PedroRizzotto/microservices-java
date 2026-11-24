CREATE TABLE tb_pet (
                        id SERIAL PRIMARY KEY,
                        customer_id BIGINT NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        species VARCHAR(20) NOT NULL,
                        breed VARCHAR(255),
                        birth_date DATE,
                        weight DOUBLE PRECISION,
                        notes VARCHAR(1000),
                        active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_pet_customer ON tb_pet(customer_id);

ALTER TABLE tb_appointment ADD COLUMN pet_id BIGINT, ADD COLUMN pet_species VARCHAR(20);
