ALTER TABLE snippet_metadata
    ADD compliance VARCHAR(255);

ALTER TABLE snippet_metadata
    ADD created_at TIMESTAMP WITHOUT TIME ZONE;