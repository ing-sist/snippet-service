ALTER TABLE owners_config
    DROP COLUMN no_expressions_in_print_line,
    DROP COLUMN no_unused_vars,
    DROP COLUMN no_undef_vars,
    DROP COLUMN no_unused_params;

ALTER TABLE owners_config
    ADD COLUMN read_input_simple_arg BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN println_simple_arg BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN identifier_naming_type VARCHAR(255) NOT NULL DEFAULT 'camel';
