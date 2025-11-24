package ingsist.snippet.domain
import jakarta.persistence.*
import java.util.Date
import java.util.UUID

@Entity
@Table(name = "snippet")
data class SnippetMetadata(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID,
    val name: String,
    val language: String,
    val description: String,

    @OneToMany(mappedBy = "snippet", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val versions: MutableList<SnippetVersion> = mutableListOf()
)

@Entity
@Table(name = "snippet_version")
data class SnippetVersion(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val versionId: UUID,
    val assetKey: String,
    val createdDate: Date,
    val versionTag: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snippet_id", nullable = false)
    val snippet: SnippetMetadata
)