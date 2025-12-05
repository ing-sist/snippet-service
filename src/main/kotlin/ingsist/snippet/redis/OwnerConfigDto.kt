package ingsist.snippet.redis

data class OwnerConfigDto(
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
