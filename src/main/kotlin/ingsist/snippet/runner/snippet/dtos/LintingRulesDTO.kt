package ingsist.snippet.runner.snippet.dtos

import com.fasterxml.jackson.annotation.JsonProperty

data class LintingRulesDTO(
    @JsonProperty("identifierNamingType")
    val identifierNamingType: String,
    @JsonProperty("printlnSimpleArg")
    val printlnSimpleArg: Boolean,
    @JsonProperty("readInputSimpleArg")
    val readInputSimpleArg: Boolean,
)
