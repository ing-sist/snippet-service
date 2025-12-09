package ingsist.snippet.redis.producer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ingsist.snippet.runner.snippet.dtos.FormattingRulesDTO
import ingsist.snippet.runner.snippet.dtos.LintingRulesDTO
import ingsist.snippet.runner.snippet.dtos.OwnerConfigDto
import ingsist.snippet.runner.snippet.dtos.StreamReqDto
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.redis.core.RedisTemplate
import java.util.UUID

class RedisProducerTest {
    @Test
    fun `should publish formatting snippet`() {
        val redisTemplate = Mockito.mock(RedisTemplate::class.java) as RedisTemplate<String, String>
        val streamOps =
            Mockito.mock(
                org.springframework.data.redis.core.StreamOperations::class.java,
            ) as org.springframework.data.redis.core.StreamOperations<String, String, String>
        Mockito.`when`(redisTemplate.opsForStream<String, String>()).thenReturn(streamOps)

        val objectMapper = jacksonObjectMapper()
        val producer = FormattingSnippetProducer("stream-key", redisTemplate, objectMapper)

        val lintingRules = LintingRulesDTO("camelCase", true, true)
        val formattingRules = FormattingRulesDTO(4, true, true, true, true, true, 1, true, true, true, 1, true)
        val config = OwnerConfigDto(lintingRules, formattingRules)
        val dto = StreamReqDto(UUID.randomUUID(), "asset-key", "1.0", "kotlin", config)

        producer.publishSnippet(dto)

        Mockito.verify(redisTemplate).opsForStream<String, String>()
    }

    @Test
    fun `should publish linting snippet`() {
        val redisTemplate = Mockito.mock(RedisTemplate::class.java) as RedisTemplate<String, String>
        val streamOps =
            Mockito.mock(
                org.springframework.data.redis.core.StreamOperations::class.java,
            ) as org.springframework.data.redis.core.StreamOperations<String, String, String>
        Mockito.`when`(redisTemplate.opsForStream<String, String>()).thenReturn(streamOps)

        val objectMapper = jacksonObjectMapper()
        val producer = LintingSnippetProducer("stream-key", redisTemplate, objectMapper)

        val lintingRules = LintingRulesDTO("camelCase", true, true)
        val formattingRules = FormattingRulesDTO(4, true, true, true, true, true, 1, true, true, true, 1, true)
        val config = OwnerConfigDto(lintingRules, formattingRules)
        val dto = StreamReqDto(UUID.randomUUID(), "asset-key", "1.0", "kotlin", config)

        producer.publishSnippet(dto)

        Mockito.verify(redisTemplate).opsForStream<String, String>()
    }
}
