CREATE TABLE snippet_test
(
    test_id     UUID NOT NULL,
    test_name   VARCHAR(255),
    snippet_id  UUID NOT NULL,
    version_tag VARCHAR(255),
    CONSTRAINT pk_snippet_test PRIMARY KEY (test_id)
);

CREATE TABLE snippet_test_expected_outputs
(
    test_id        UUID    NOT NULL,
    expected_value TEXT,
    output_order   INTEGER NOT NULL,
    CONSTRAINT pk_snippet_test_expected_outputs PRIMARY KEY (test_id, output_order)
);

CREATE TABLE snippet_test_inputs
(
    test_id     UUID    NOT NULL,
    input_value TEXT,
    input_order INTEGER NOT NULL,
    CONSTRAINT pk_snippet_test_inputs PRIMARY KEY (test_id, input_order)
);

ALTER TABLE snippet_test
    ADD CONSTRAINT FK_SNIPPET_TEST_ON_SNIPPET FOREIGN KEY (snippet_id) REFERENCES snippet_metadata (id);

ALTER TABLE snippet_test_expected_outputs
    ADD CONSTRAINT fk_snippet_test_expected_outputs_on_snippet_test FOREIGN KEY (test_id) REFERENCES snippet_test (test_id);

ALTER TABLE snippet_test_inputs
    ADD CONSTRAINT fk_snippet_test_inputs_on_snippet_test FOREIGN KEY (test_id) REFERENCES snippet_test (test_id);