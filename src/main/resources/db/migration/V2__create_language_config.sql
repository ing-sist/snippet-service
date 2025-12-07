-- V2__create_language_config.sql

CREATE TABLE language_config (
    id UUID NOT NULL,
    language VARCHAR(255) NOT NULL,
    extension VARCHAR(32) NOT NULL,
    CONSTRAINT pk_language_config PRIMARY KEY (id),
    CONSTRAINT uq_language_config_language UNIQUE (language)
);

CREATE TABLE language_config_versions (
    language_config_id UUID NOT NULL,
    version VARCHAR(255) NOT NULL,
    CONSTRAINT fk_language_config_versions_language_config FOREIGN KEY (language_config_id) REFERENCES language_config (id)
);

CREATE UNIQUE INDEX uq_language_config_version ON language_config_versions(language_config_id, version);

-- Optional seed: PrintScript 1.0 and 1.1
INSERT INTO language_config (id, language, extension) VALUES
    ('00000000-0000-0000-0000-000000000001', 'printscript', 'ps');

INSERT INTO language_config_versions (language_config_id, version) VALUES
    ('00000000-0000-0000-0000-000000000001', '1.0'),
    ('00000000-0000-0000-0000-000000000001', '1.1');
