ALTER TABLE tb_user DROP CONSTRAINT tb_user_type_check;

ALTER TABLE tb_user ADD CONSTRAINT tb_user_type_check CHECK (type BETWEEN 0 AND 2);
