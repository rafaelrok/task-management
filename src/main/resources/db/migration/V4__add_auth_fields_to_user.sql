-- Adiciona campos de senha e role na tabela users
ALTER TABLE users
    ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT 'changeme';
ALTER TABLE users
    ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER';

-- Ajusta valores iniciais (recomendado alterar senhas manualmente depois com hashing)
UPDATE users
SET password = 'changeme',
    role     = 'ROLE_USER'
WHERE password = 'changeme';
