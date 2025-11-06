package ingsist.snippet.domain
import jakarta.persistence.*

@Entity
data class SnippetEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: String,
    val name: String,
    val language: String,
    val version: String,
    val description: String,
    val assetKey: String,
)