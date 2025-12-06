package ingsist.snippet.engine

import ingsist.snippet.runner.snippet.dtos.ValidateReqDto
import ingsist.snippet.runner.snippet.dtos.ValidateResDto

interface EngineServiceInterface {
    fun parse(snippet: ValidateReqDto): ValidateResDto

    fun getSnippetContent(assetKey: String): String

    fun deleteSnippet(assetKey: String)
}
