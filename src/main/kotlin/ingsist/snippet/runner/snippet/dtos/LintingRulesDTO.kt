package ingsist.snippet.runner.snippet.dtos

data class LintingRulesDTO(
    val noExpressionsInPrintLine: Boolean,
    val noUnusedVars: Boolean,
    val noUndefVars: Boolean,
    val noUnusedParams: Boolean,
)
