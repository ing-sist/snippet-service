CREATE TABLE snippet
(
    id          UUID NOT NULL,
    name        VARCHAR(255),
    language    VARCHAR(255),
    description VARCHAR(255),
    CONSTRAINT pk_snippet PRIMARY KEY (id)
);

CREATE TABLE snippet_version
(
    version_id   UUID NOT NULL,
    asset_key    VARCHAR(255),
    created_date TIMESTAMP WITHOUT TIME ZONE,
    snippet_id   UUID NOT NULL,
    CONSTRAINT pk_snippet_version PRIMARY KEY (version_id)
);

ALTER TABLE snippet_version
    ADD CONSTRAINT FK_SNIPPET_VERSION_ON_SNIPPET FOREIGN KEY (snippet_id) REFERENCES snippet (id);