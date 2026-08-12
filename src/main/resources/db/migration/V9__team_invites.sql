CREATE TABLE team_invites (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    team_id    BIGINT      NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    created_by BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    role       VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    max_uses   INTEGER     NOT NULL,
    uses       INTEGER     NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_team_invite_token UNIQUE (token_hash),
    CONSTRAINT ck_team_invite_uses  CHECK (uses >= 0 AND uses <= max_uses),
    CONSTRAINT ck_team_invite_role  CHECK (role IN ('ADMIN', 'MEMBER'))
);
CREATE INDEX idx_team_invites_team ON team_invites (team_id);

DROP TABLE board_invites;
