package ingsist.snippet.runner.snippet.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Embeddable
data class LintingConfig(
    val identifierNamingType: String,
    val printlnSimpleArg: Boolean,
    val readInputSimpleArg: Boolean,
)

@Embeddable
data class FormattingConfig(
    val indentation: Int,
    val spaceBeforeColon: Boolean,
    val spaceAfterColon: Boolean,
    val spaceAroundAssignment: Boolean,
    val spaceAroundOperators: Boolean,
    val maxSpaceBetweenTokens: Boolean,
    val lineBreakBeforePrintln: Int,
    val lineBreakAfterSemiColon: Boolean,
    val inlineBraceIfStatement: Boolean,
    val belowLineBraceIfStatement: Boolean,
    val braceLineBreak: Int,
    val keywordSpacingAfter: Boolean,
)

@Entity
@Table(name = "owners_config")
data class OwnerConfig(
    @Id
    @Column(name = "owner_id")
    val ownerId: String,
    @Embedded
    val linting: LintingConfig,
    @Embedded
    val formatting: FormattingConfig,
)
