package ingsist.snippet.engine

import ingsist.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.dtos.ExecuteResDTO
import ingsist.snippet.exception.ExternalServiceException
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class EngineService(private val engineRestClient: RestClient) : EngineServiceInterface {
    override fun parse(snippet: ExecuteReqDTO): ExecuteResDTO {
        val response =
            engineRestClient.post()
                .uri("/v1/engine/parse")
                .body(snippet)
                .retrieve()
                .onStatus({ status -> status.isError }) { _, response ->
                    throw ExternalServiceException("Engine parse failed with status code: ${response.statusCode}")
                }
                .toEntity(ExecuteResDTO::class.java)
        return response.body ?: throw ExternalServiceException(
            "Engine parse returned empty body",
        )
    }
}
