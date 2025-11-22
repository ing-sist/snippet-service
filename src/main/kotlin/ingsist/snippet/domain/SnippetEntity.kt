package ingsist.snippet.domain
import jakarta.persistence.*

@Entity
@Table(name = "snippet_entity", uniqueConstraints = [
    UniqueConstraint(columnNames = ["name"])
])
data class SnippetEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: String,
    var name: String,
    var language: String,
    var version: String,
    var description: String,
    var assetKey: String,
)