package ingsist.snippet.runner.snippet.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
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
    @Column(name = "version_tag")
    val versionTag: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snippet_id", nullable = false)
    val snippet: SnippetMetadata,
)

@Embeddable
data class LintingConfig(
    val identifierNamingType: String,
    val printlnSimpleArg: Boolean,
    val readInputSimpleArg: Boolean,
)

@Embeddable
data class FormattingConfig(
    val indentation: Int,
    val spaceBeforeColon: Boolean,
    val spaceAfterColon: Boolean,
    val spaceAroundAssignment: Boolean,
    val spaceAroundOperators: Boolean,
    val maxSpaceBetweenTokens: Boolean,
    val lineBreakBeforePrintln: Int,
    val lineBreakAfterSemiColon: Boolean,
    val inlineBraceIfStatement: Boolean,
    val belowLineBraceIfStatement: Boolean,
    val braceLineBreak: Int,
    val keywordSpacingAfter: Boolean,
)

@Entity
@Table(name = "owners_config")
data class OwnerConfig(
    @Id
    @Column(name = "owner_id")
    val ownerId: String,
    @Embedded
    val linting: LintingConfig,
    @Embedded
    val formatting: FormattingConfig,
)
