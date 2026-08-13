-- Respostas: parent_id sempre aponta para a raiz da thread. O service normaliza
-- responder-a-uma-resposta para o avô, então a árvore nunca passa de 2 níveis.
ALTER TABLE comments ADD COLUMN parent_id  BIGINT REFERENCES comments(id) ON DELETE CASCADE;
ALTER TABLE comments ADD COLUMN deleted_at TIMESTAMPTZ;
CREATE INDEX idx_comments_parent ON comments (parent_id);

CREATE TABLE comment_reactions (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comment_id BIGINT      NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    emoji      VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    -- Rede para dois cliques simultâneos: vira 409, não duas linhas iguais.
    CONSTRAINT uq_comment_reaction UNIQUE (comment_id, user_id, emoji)
);
CREATE INDEX idx_comment_reactions_comment ON comment_reactions (comment_id);
