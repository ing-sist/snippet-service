package ingsist.snippet.runner.snippet.dtos

import com.fasterxml.jackson.annotation.JsonProperty

data class OwnerConfigDto(
    @JsonProperty("linting")
    val linting: LintingRulesDTO,
    @JsonProperty("formatting")
    val formatting: FormattingRulesDTO,
)
