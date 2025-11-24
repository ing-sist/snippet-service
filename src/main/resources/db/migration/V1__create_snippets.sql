CREATE TABLE snippets (
                          id          VARCHAR(50) PRIMARY KEY,
                          name        VARCHAR(255) NOT NULL,
                          language    VARCHAR(50) NOT NULL,
                          version     VARCHAR(20) NOT NULL,
                          description TEXT,
                          asset_key   VARCHAR(255) NOT NULL,
                          created_at  TIMESTAMPTZ DEFAULT now()
);