package ingsist.snippet.domain
<<<<<<< HEAD
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
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
    val versions: MutableList<SnippetVersion> = mutableListOf(),
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
    val snippet: SnippetMetadata,
)
=======
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
>>>>>>> 2d4a23d (feat/useCase#1: created snippet controller, service repo & connected to ps parser (not finished yet))
