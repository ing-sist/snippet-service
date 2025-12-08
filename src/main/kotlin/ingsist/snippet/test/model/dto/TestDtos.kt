package ingsist.snippet.test.model.dto

import java.util.UUID

data class CreateTestRequest(
    val name: String,
    val inputs: List<String> = emptyList(),
    val expectedOutputs: List<String> = emptyList(),
)

data class SnippetTestResponse(
    val id: UUID,
    val name: String,
    val inputs: List<String>,
    val expectedOutputs: List<String>,
    val version: String,
)

data class RunTestResponse(
    val status: RunStatus,
    val outputs: List<String>,
    val errors: List<String>,
    val failures: List<FailureDetail>,
    val version: String,
)

data class FailureDetail(
    val index: Int,
    val expected: String?,
    val obtained: String?,
    val reason: String,
)

enum class RunStatus {
    SUCCESS,
    FAIL,
}
