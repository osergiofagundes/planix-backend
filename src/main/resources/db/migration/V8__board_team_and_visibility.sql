ALTER TABLE boards ADD COLUMN team_id BIGINT REFERENCES teams(id) ON DELETE CASCADE;

ALTER TABLE boards ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'RESTRICTED';
ALTER TABLE boards ADD CONSTRAINT ck_board_visibility
    CHECK (visibility IN ('TEAM', 'RESTRICTED'));

UPDATE boards b
   SET team_id = (SELECT min(t.id) FROM teams t WHERE t.owner_id = b.owner_id);

INSERT INTO team_members (team_id, user_id, role, created_at)
SELECT DISTINCT b.team_id, bm.user_id, 'MEMBER', now()
  FROM board_members bm
  JOIN boards b ON b.id = bm.board_id
ON CONFLICT (team_id, user_id) DO NOTHING;

ALTER TABLE boards ALTER COLUMN team_id SET NOT NULL;
CREATE INDEX idx_boards_team ON boards (team_id);

ALTER TABLE boards ALTER COLUMN visibility SET DEFAULT 'TEAM';
