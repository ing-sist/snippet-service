package ingsist.snippet.runner.snippet.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

enum class ConformanceStatus {
    PENDING,
    COMPLIANT,
    NON_COMPLIANT,
    EXTERNAL_ERROR,
}

@Entity
@Table(name = "snippet_metadata")
data class SnippetMetadata(
    @Id
    val id: UUID,
    val name: String,
    val language: String,
    @Column(name = "lang_version")
    val langVersion: String,
    val description: String,
    @Column(name = "owner_id")
    val ownerId: String,
    @Enumerated(EnumType.STRING)
    var conformance: ConformanceStatus = ConformanceStatus.PENDING,
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @OneToMany(mappedBy = "snippet", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val versions: MutableList<SnippetVersion> = mutableListOf(),
)

@Entity
@Table(name = "snippet_version")
data class SnippetVersion(
    @Id
    val versionId: UUID,
    var assetKey: String,
    val createdDate: Date,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snippet_id", nullable = false)
    val snippet: SnippetMetadata,
)

@Entity
@Table(name = "owners_config")
data class OwnerConfig(
    @Id
    @Column(name = "owner_id")
    val ownerId: String,
    // linting rules
    val noExpressionsInPrintLine: Boolean,
    val noUnusedVars: Boolean,
    val noUndefVars: Boolean,
    val noUnusedParams: Boolean,
    // formatting rules
    val indentation: Int,
    val openIfBlockOnSameLine: Boolean,
    val maxLineLength: Int,
    val noTrailingSpaces: Boolean,
    val noMultipleEmptyLines: Boolean,
)
