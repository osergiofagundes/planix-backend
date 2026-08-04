-- Perfil do usuário: os dados pessoais moram na propria tabela users.
-- O endereço vira um @Embeddable no Java, mas em SQL são só colunas soltas aqui.
ALTER TABLE users
    ADD COLUMN birth_date  DATE,
    ADD COLUMN phone       VARCHAR(20),
    ADD COLUMN bio         VARCHAR(1000),
    ADD COLUMN avatar_path VARCHAR(255),
    ADD COLUMN street      VARCHAR(150),
    ADD COLUMN number      VARCHAR(20),
    ADD COLUMN complement  VARCHAR(100),
    ADD COLUMN city        VARCHAR(100),
    ADD COLUMN state       VARCHAR(2),
    ADD COLUMN zip_code    VARCHAR(9);

-- Redes sociais em tabela própria: acrescentar uma rede nova depois é só um valor de enum,
-- não uma migration. A UNIQUE garante uma URL por rede por usuário.
CREATE TABLE user_social_links (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform   VARCHAR(30)  NOT NULL,
    url        VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_user_social_platform UNIQUE (user_id, platform)
);
CREATE INDEX idx_user_social_links_user ON user_social_links (user_id);

-- Os uploads agora ficam em subpastas por tipo. Os anexos que já existem estavam na raiz;
-- aqui o banco passa a apontar para uploads/documents/, e os arquivos são movidos no disco.
UPDATE attachments
   SET stored_filename = 'documents/' || stored_filename
 WHERE stored_filename NOT LIKE '%/%';
