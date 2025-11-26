package ingsist.snippet.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
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
    val id: UUID,
    val name: String,
    val language: String,
    val description: String,
    @Column(name = "owner_id")
    val ownerId: String,
    @OneToMany(mappedBy = "snippet", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val versions: MutableList<SnippetVersion> = mutableListOf(),
)

@Entity
@Table(name = "snippet_version")
data class SnippetVersion(
    @Id
    val versionId: UUID,
    val assetKey: String,
    val createdDate: Date,
    val versionTag: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snippet_id", nullable = false)
    val snippet: SnippetMetadata,
)
