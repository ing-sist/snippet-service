package ingsist.snippet.redis

import org.austral.ingsis.redis.RedisStreamProducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FormattingSnippetProducer
    @Autowired
    constructor(
        @Value("\${stream.key}") streamKey: String,
        redis: RedisTemplate<String, String>,
    ) : SnippetEventProducer, RedisStreamProducer(streamKey, redis) {
        override fun publishSnippet(snippetId: UUID) {
            println("Publishing formatting request for snippet: $snippetId")
            val event = snippetId.toString()
            emit(event)
        }
    }
