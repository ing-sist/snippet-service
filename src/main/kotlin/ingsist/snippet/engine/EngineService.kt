package ingsist.snippet.engine

import ingsist.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.dtos.ExecuteResDTO
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class EngineService(private val engineRestClient: RestClient): EngineServiceInterface {

    override fun parse(snippet: ExecuteReqDTO): ExecuteResDTO {
        val response = engineRestClient.post()
            .uri("/v1/engine/parse")
            .body(snippet)
            .retrieve()
            .toEntity(ExecuteResDTO::class.java)

        return response.body ?: throw Exception("Engine parse failed with status code: ${response.statusCode}")
    }
}