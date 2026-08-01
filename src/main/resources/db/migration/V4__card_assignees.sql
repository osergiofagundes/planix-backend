CREATE TABLE card_assignees (
    card_id BIGINT NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (card_id, user_id)
);
CREATE INDEX idx_card_assignees_user ON card_assignees (user_id);
