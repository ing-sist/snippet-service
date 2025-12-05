import ingsist.snippet.redis.OwnerConfigDto
import java.util.UUID

data class StreamReqDto(
    val id: UUID,
    val assetKey: String,
    val version: String,
    val config: OwnerConfigDto,
)
