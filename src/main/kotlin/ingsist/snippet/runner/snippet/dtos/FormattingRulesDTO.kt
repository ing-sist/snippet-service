package ingsist.snippet.runner.snippet.dtos

data class FormattingRulesDTO(
    val indentation: Int,
    val openIfBlockOnSameLine: Boolean,
    val maxLineLength: Int,
    val noTrailingSpaces: Boolean,
    val noMultipleEmptyLines: Boolean,
)
