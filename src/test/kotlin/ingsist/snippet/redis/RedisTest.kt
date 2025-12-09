package ingsist.snippet.redis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ingsist.snippet.redis.consumer.ConsumerConformanceStreamService
import ingsist.snippet.redis.consumer.LintingConformanceConsumer
import ingsist.snippet.runner.snippet.domain.ConformanceStatus
import ingsist.snippet.runner.snippet.dtos.LintingConformanceStatusDTO
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.core.RedisTemplate
import java.util.UUID

class RedisTest {
    @Test
    fun `should consume linting conformance`() {
        val redisTemplate = Mockito.mock(RedisTemplate::class.java) as RedisTemplate<String, String>
        val conformanceService = Mockito.mock(ConsumerConformanceStreamService::class.java)
        val objectMapper = jacksonObjectMapper()

        val consumer =
            LintingConformanceConsumer(
                redisTemplate,
                "stream-key",
                "group-id",
                conformanceService,
                objectMapper,
            )

        val dto = LintingConformanceStatusDTO(UUID.randomUUID(), ConformanceStatus.COMPLIANT)
        val json = objectMapper.writeValueAsString(dto)

        val record = Mockito.mock(ObjectRecord::class.java) as ObjectRecord<String, String>
        Mockito.`when`(record.value).thenReturn(json)

        val method = LintingConformanceConsumer::class.java.getDeclaredMethod("onMessage", ObjectRecord::class.java)
        method.isAccessible = true
        method.invoke(consumer, record)

        Mockito.verify(conformanceService).saveConformance(org.mockito.kotlin.any())
    }
}
