CREATE TABLE board_members (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    board_id   BIGINT      NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_board_member UNIQUE (board_id, user_id)
);
CREATE INDEX idx_board_members_user ON board_members (user_id);

INSERT INTO board_members (board_id, user_id, created_at)
SELECT id, owner_id, now() FROM boards;

CREATE TABLE board_invites (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    board_id   BIGINT      NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    created_by BIGINT      NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    max_uses   INTEGER     NOT NULL,
    uses       INTEGER     NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_board_invite_token UNIQUE (token_hash),
    CONSTRAINT ck_board_invite_uses  CHECK (uses >= 0 AND uses <= max_uses)
);
CREATE INDEX idx_board_invites_board ON board_invites (board_id);
