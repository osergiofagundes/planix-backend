-- Usuários. O password_hash cabe em VARCHAR(100) com folga: BCrypt sempre gera 60 caracteres.
CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- Refresh tokens: guardamos o SHA-256, nunca o valor. Vazou o banco, não vazou sessão.
CREATE TABLE refresh_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- O dono do quadro. Tudo abaixo (listas, cartões, etiquetas...) herda a posse por aqui.
ALTER TABLE boards ADD COLUMN owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX idx_boards_owner ON boards (owner_id);

-- Autoria: com mais de uma pessoa no sistema, "quem escreveu isso?" precisa de resposta.
-- Os ON DELETE são diferentes de propósito: comentário e anexo somem com a conta; o histórico
-- sobrevive ao autor (auditoria com buraco deixa de ser auditoria) e vira "usuário removido".
ALTER TABLE comments     ADD COLUMN user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE attachments  ADD COLUMN user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE card_changes ADD COLUMN changed_by BIGINT          REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_comments_user    ON comments (user_id);
CREATE INDEX idx_attachments_user ON attachments (user_id);
