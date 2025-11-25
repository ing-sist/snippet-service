package ingsist.snippet.engine

import ingsist.snippet.dtos.ExecuteReqDTO
import ingsist.snippet.dtos.ExecuteResDTO

interface EngineServiceInterface {
    fun parse(snippet: ExecuteReqDTO) : ExecuteResDTO
}