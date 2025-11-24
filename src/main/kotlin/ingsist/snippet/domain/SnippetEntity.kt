package ingsist.snippet.domain
import jakarta.persistence.*
import java.util.UUID

@Entity
data class SnippetEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: UUID,
    val name: String,
    val language: String,
    val version: String,
    val description: String,
    val assetKey: String,
)