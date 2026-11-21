DELETE FROM tb_user WHERE type = 0;
ALTER TABLE tb_user DROP CONSTRAINT tb_user_type_check;
ALTER TABLE tb_user ADD CONSTRAINT tb_user_type_check CHECK (type IN (1, 2));
