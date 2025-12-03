CREATE TABLE owners_config
(
    owner_id                     VARCHAR(255) NOT NULL,
    no_expressions_in_print_line BOOLEAN      NOT NULL,
    no_unused_vars               BOOLEAN      NOT NULL,
    no_undef_vars                BOOLEAN      NOT NULL,
    no_unused_params             BOOLEAN      NOT NULL,
    indentation                  INTEGER      NOT NULL,
    open_if_block_on_same_line   BOOLEAN      NOT NULL,
    max_line_length              INTEGER      NOT NULL,
    no_trailing_spaces           BOOLEAN      NOT NULL,
    no_multiple_empty_lines      BOOLEAN      NOT NULL,
    CONSTRAINT pk_owners_config PRIMARY KEY (owner_id)
);

CREATE TABLE snippet_metadata
(
    id          UUID NOT NULL,
    name        VARCHAR(255),
    language    VARCHAR(255),
    description VARCHAR(255),
    owner_id    VARCHAR(255),
    CONSTRAINT pk_snippet_metadata PRIMARY KEY (id)
);

CREATE TABLE snippet_version
(
    version_id   UUID NOT NULL,
    asset_key    VARCHAR(255),
    created_date TIMESTAMP WITHOUT TIME ZONE,
    version_tag  VARCHAR(255),
    snippet_id   UUID NOT NULL,
    CONSTRAINT pk_snippet_version PRIMARY KEY (version_id)
);

ALTER TABLE snippet_version
    ADD CONSTRAINT FK_SNIPPET_VERSION_ON_SNIPPET FOREIGN KEY (snippet_id) REFERENCES snippet_metadata (id);