package ingsist.snippet.redis

import SnippetEventProducer
import org.austral.ingsis.redis.RedisStreamProducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class LintingSnippetProducer
    @Autowired
    constructor(
        @Value("\${stream.linting.key}") streamKey: String,
        redis: RedisTemplate<String, String>,
    ) : SnippetEventProducer, RedisStreamProducer(streamKey, redis) {
        override fun publishSnippet(snippetId: UUID) {
            val event = snippetId.toString()
            emit(event)
        }
    }
