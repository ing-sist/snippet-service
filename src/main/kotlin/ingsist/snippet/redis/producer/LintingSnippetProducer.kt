package ingsist.snippet.redis.producer

import SnippetEventProducer
import StreamReqDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.austral.ingsis.redis.RedisStreamProducer
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class LintingSnippetProducer
    @Autowired
    constructor(
        @Value("\${stream.linting.key}") streamKey: String,
        redis: RedisTemplate<String, String>,
        val objectMapper: ObjectMapper,
    ) : SnippetEventProducer, RedisStreamProducer(streamKey, redis) {
        companion object {
            private const val CORRELATION_ID_KEY = "correlation-id"
        }

        override fun publishSnippet(snippet: StreamReqDto) {
            val corrId = MDC.get(CORRELATION_ID_KEY)
            val snippetWithId = snippet.copy(correlationId = corrId)
            val json = objectMapper.writeValueAsString(snippetWithId)
            emit(json)
        }
    }
