package ingsist.snippet.runner.snippet.dtos

data class OwnerConfigDTO(
    val noExpressionsInPrintLine: Boolean,
    val noUnusedVars: Boolean,
    val noUndefVars: Boolean,
    val noUnusedParams: Boolean,
    val indentation: Int,
    val openIfBlockOnSameLine: Boolean,
    val maxLineLength: Int,
    val noTrailingSpaces: Boolean,
    val noMultipleEmptyLines: Boolean,
)
