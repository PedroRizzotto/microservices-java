INSERT INTO tb_user (name, email, password, type)
SELECT 'Pet Shop Exemplo', 'provider@provider.dev', '$2a$10$kWFBiI34Oa36VJIP5Emsa.BXBGiP52m56iQPK0IuCFUfk4ngtXKV2', 2
WHERE NOT EXISTS (SELECT 1 FROM tb_user WHERE email = 'provider@provider.dev');
