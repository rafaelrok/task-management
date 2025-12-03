-- Adiciona campos de senha e role na tabela users
ALTER TABLE users
    ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT '$2a$10$yG.iJEz.FqdkPIsS4/9BaOK90/aYfm5J2lwrGjGwPKW7ztA/9MKfi';
ALTER TABLE users
    ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'MEMBER';

-- Ajusta valores iniciais (recomendado alterar senhas manualmente depois com hashing)
UPDATE users
SET password = '$2a$10$yG.iJEz.FqdkPIsS4/9BaOK90/aYfm5J2lwrGjGwPKW7ztA/9MKfi',
    role     = 'MEMBER'
WHERE password = '$2a$10$yG.iJEz.FqdkPIsS4/9BaOK90/aYfm5J2lwrGjGwPKW7ztA/9MKfi';
