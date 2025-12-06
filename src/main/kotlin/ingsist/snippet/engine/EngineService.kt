package ingsist.snippet.engine

import ingsist.snippet.runner.snippet.dtos.ValidateReqDto
import ingsist.snippet.runner.snippet.dtos.ValidateResDto
import ingsist.snippet.shared.exception.ExternalServiceException
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class EngineService(private val engineRestClient: RestClient) : EngineServiceInterface {
    override fun parse(snippet: ValidateReqDto): ValidateResDto {
        val response =
            engineRestClient.post()
                .uri("/engine/validate")
                .body(snippet)
                .retrieve()
                .onStatus({ status -> status.isError }) { _, response ->
                    throw ExternalServiceException("Engine parse failed with status code: ${response.statusCode}")
                }
                .toEntity(ValidateResDto::class.java)
        println(snippet)
        return response.body ?: throw ExternalServiceException(
            "Engine parse returned empty body",
        )
    }

    override fun getSnippetContent(assetKey: String): String {
        return engineRestClient.get()
            .uri("/engine/code/$assetKey")
            .retrieve()
            .onStatus({ status -> status.isError }) { _, response ->
                throw ExternalServiceException(
                    "Failed to fetch snippet code from Engine. Status: ${response.statusCode}",
                )
            }
            .body(String::class.java)
            ?: ""
    }

    override fun deleteSnippet(assetKey: String) {
        engineRestClient.delete()
            .uri("/engine/code/$assetKey")
            .retrieve()
            .onStatus({ status -> status.isError }) { _, response ->
                throw ExternalServiceException(
                    "Failed to delete snippet code from Engine. Status: ${response.statusCode}",
                )
            }
            .toBodilessEntity()
    }
}
