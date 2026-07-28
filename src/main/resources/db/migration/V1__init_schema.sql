-- Quadros
CREATE TABLE boards (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

-- Listas de um quadro
CREATE TABLE board_lists (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    board_id   BIGINT       NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    name       VARCHAR(150) NOT NULL,
    position   INTEGER      NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_board_lists_board ON board_lists (board_id, position);

-- Cartões de uma lista
CREATE TABLE cards (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    list_id      BIGINT       NOT NULL REFERENCES board_lists(id) ON DELETE CASCADE,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    due_date     TIMESTAMPTZ,
    priority     VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    position     INTEGER      NOT NULL,
    completed    BOOLEAN      NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_cards_list ON cards (list_id, position);

-- Etiquetas reutilizáveis dentro de um quadro
CREATE TABLE labels (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    board_id   BIGINT       NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(30)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_label_board_name UNIQUE (board_id, name)
);
CREATE INDEX idx_labels_board ON labels (board_id);

-- Junção N:N entre cartão e etiqueta
CREATE TABLE card_labels (
    card_id  BIGINT NOT NULL REFERENCES cards(id)  ON DELETE CASCADE,
    label_id BIGINT NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    PRIMARY KEY (card_id, label_id)
);

CREATE TABLE checklist_items (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_id    BIGINT       NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    text       VARCHAR(300) NOT NULL,
    done       BOOLEAN      NOT NULL DEFAULT FALSE,
    position   INTEGER      NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_checklist_card ON checklist_items (card_id, position);

CREATE TABLE comments (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_id    BIGINT      NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    text       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_comments_card ON comments (card_id);

CREATE TABLE card_links (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_id    BIGINT        NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    url        VARCHAR(2000) NOT NULL,
    title      VARCHAR(200),
    created_at TIMESTAMPTZ   NOT NULL,
    updated_at TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_card_links_card ON card_links (card_id);

CREATE TABLE attachments (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_id           BIGINT       NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL,
    content_type      VARCHAR(150),
    size_bytes        BIGINT,
    created_at        TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_attachments_card ON attachments (card_id);

CREATE TABLE card_changes (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_id    BIGINT      NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    field      VARCHAR(50) NOT NULL,
    old_value  TEXT,
    new_value  TEXT,
    changed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_card_changes_card ON card_changes (card_id, changed_at);