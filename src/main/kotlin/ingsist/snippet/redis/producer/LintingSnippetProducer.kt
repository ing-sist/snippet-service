package ingsist.snippet.redis.producer

import SnippetEventProducer
import StreamReqDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.austral.ingsis.redis.RedisStreamProducer
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
        override fun publishSnippet(snippet: StreamReqDto) {
            // aca deberia mandar el pending a la ui
            val json = objectMapper.writeValueAsString(snippet)
            emit(json)
        }
    }
