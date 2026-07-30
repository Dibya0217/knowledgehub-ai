-- V4: Create documents and document_metadata tables

CREATE TABLE IF NOT EXISTS documents (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename      VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_type     VARCHAR(100) NOT NULL,
    file_size     BIGINT       NOT NULL,
    storage_path  TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS document_metadata (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID         NOT NULL UNIQUE REFERENCES documents(id) ON DELETE CASCADE,
    page_count  INTEGER,
    word_count  INTEGER,
    language    VARCHAR(10),
    title       VARCHAR(500),
    author      VARCHAR(255)
);
