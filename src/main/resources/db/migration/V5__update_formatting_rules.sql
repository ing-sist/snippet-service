ALTER TABLE owners_config
    DROP COLUMN open_if_block_on_same_line,
    DROP COLUMN max_line_length,
    DROP COLUMN no_trailing_spaces,
    DROP COLUMN no_multiple_empty_lines;

ALTER TABLE owners_config
    ADD COLUMN space_before_colon BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN space_after_colon BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN space_around_assignment BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN space_around_operators BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN max_space_between_tokens BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN line_break_before_println INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN line_break_after_semi_colon BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN inline_brace_if_statement BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN below_line_brace_if_statement BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN brace_line_break INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN keyword_spacing_after BOOLEAN NOT NULL DEFAULT TRUE;
