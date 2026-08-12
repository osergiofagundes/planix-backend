CREATE TABLE teams (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    icon        VARCHAR(50),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_teams_owner ON teams (owner_id);

CREATE TABLE team_members (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    team_id    BIGINT      NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_team_member    UNIQUE (team_id, user_id),
    CONSTRAINT ck_team_member_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);
CREATE INDEX idx_team_members_user ON team_members (user_id);

INSERT INTO teams (owner_id, name, created_at, updated_at)
SELECT u.id, left('Equipe de ' || u.name, 150), now(), now()
  FROM users u;

INSERT INTO team_members (team_id, user_id, role, created_at)
SELECT t.id, t.owner_id, 'OWNER', now()
  FROM teams t;
