package ingsist.snippet.engine

import ingsist.snippet.dtos.ValidateReqDto
import ingsist.snippet.dtos.ValidateResDto
import ingsist.snippet.exception.ExternalServiceException
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
}
