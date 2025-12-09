package ingsist.snippet.runner.snippet.dtos

import com.fasterxml.jackson.annotation.JsonProperty

data class FormattingRulesDTO(
    @JsonProperty("Indentation")
    val indentation: Int,
    @JsonProperty("SpaceBeforeColon")
    val spaceBeforeColon: Boolean,
    @JsonProperty("SpaceAfterColon")
    val spaceAfterColon: Boolean,
    @JsonProperty("SpaceAroundAssignment")
    val spaceAroundAssignment: Boolean,
    @JsonProperty("SpaceAroundOperators")
    val spaceAroundOperators: Boolean,
    @JsonProperty("MaxSpaceBetweenTokens")
    val maxSpaceBetweenTokens: Boolean,
    @JsonProperty("LineBreakBeforePrintln")
    val lineBreakBeforePrintln: Int,
    @JsonProperty("LineBreakAfterSemiColon")
    val lineBreakAfterSemiColon: Boolean,
    @JsonProperty("InlineBraceIfStatement")
    val inlineBraceIfStatement: Boolean,
    @JsonProperty("BelowLineBraceIfStatement")
    val belowLineBraceIfStatement: Boolean,
    @JsonProperty("BraceLineBreak")
    val braceLineBreak: Int,
    @JsonProperty("KeywordSpacingAfter")
    val keywordSpacingAfter: Boolean,
)
