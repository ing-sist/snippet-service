package ingsist.snippet.engine

import ingsist.snippet.runner.dtos.ValidateReqDto
import ingsist.snippet.runner.dtos.ValidateResDto

interface EngineServiceInterface {
    fun parse(snippet: ValidateReqDto): ValidateResDto

    fun getSnippetContent(assetKey: String): String
}
