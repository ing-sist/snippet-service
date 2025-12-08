package ingsist.snippet.redis.producer

import ingsist.snippet.runner.snippet.dtos.StreamReqDto

interface SnippetEventProducer {
    fun publishSnippet(snippet: StreamReqDto)
}
