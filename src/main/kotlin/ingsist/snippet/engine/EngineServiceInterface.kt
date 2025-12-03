package ingsist.snippet.engine

import ingsist.snippet.dtos.ValidateReqDto
import ingsist.snippet.dtos.ValidateResDto

interface EngineServiceInterface {
    fun parse(snippet: ValidateReqDto): ValidateResDto
}
