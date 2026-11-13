CREATE TABLE tb_establishment (
                                  id SERIAL PRIMARY KEY,
                                  provider_id BIGINT NOT NULL UNIQUE,
                                  name VARCHAR(255) NOT NULL,
                                  address VARCHAR(255),
                                  phone VARCHAR(50)
);

CREATE TABLE tb_pet_service (
                                id SERIAL PRIMARY KEY,
                                establishment_id BIGINT NOT NULL,
                                name VARCHAR(255) NOT NULL,
                                duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
                                price DOUBLE PRECISION NOT NULL,
                                active BOOLEAN NOT NULL DEFAULT TRUE,
                                CONSTRAINT fk_pet_service_establishment
                                    FOREIGN KEY(establishment_id)
                                        REFERENCES tb_establishment(id)
);

CREATE TABLE tb_availability (
                                 id SERIAL PRIMARY KEY,
                                 establishment_id BIGINT NOT NULL,
                                 day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
                                 start_time TIME NOT NULL,
                                 end_time TIME NOT NULL,
                                 CHECK (end_time > start_time),
                                 CONSTRAINT fk_availability_establishment
                                     FOREIGN KEY(establishment_id)
                                         REFERENCES tb_establishment(id)
);

CREATE TABLE tb_appointment (
                                id SERIAL PRIMARY KEY,
                                establishment_id BIGINT NOT NULL,
                                pet_service_id BIGINT NOT NULL,
                                customer_id BIGINT NOT NULL,
                                customer_email VARCHAR(255) NOT NULL,
                                pet_name VARCHAR(255) NOT NULL,
                                start_at TIMESTAMP NOT NULL,
                                end_at TIMESTAMP NOT NULL,
                                price_at_booking DOUBLE PRECISION NOT NULL,
                                status VARCHAR(20) NOT NULL,
                                CONSTRAINT fk_appointment_establishment
                                    FOREIGN KEY(establishment_id)
                                        REFERENCES tb_establishment(id),
                                CONSTRAINT fk_appointment_pet_service
                                    FOREIGN KEY(pet_service_id)
                                        REFERENCES tb_pet_service(id)
);
