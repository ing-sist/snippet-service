package ingsist.snippet.test

import ingsist.snippet.domain.SnippetMetadata
import ingsist.snippet.domain.SnippetVersion
import ingsist.snippet.dtos.ShareSnippetDTO
import ingsist.snippet.dtos.SnippetResponseDTO
import ingsist.snippet.dtos.SnippetUploadDTO
import ingsist.snippet.dtos.SubmitSnippetDTO
import java.util.Date
import java.util.UUID

/**
 * DSL builders for creating test data in a fluent and readable way.
 */
class SnippetMetadataBuilder {
    var id: UUID = UUID.randomUUID()
    var name: String = "Test Snippet"
    var language: String = "printscript"
    var description: String = "A test snippet"
    var ownerId: String = "test-user-id"
    private val versions: MutableList<SnippetVersion> = mutableListOf()

    fun withVersion(block: SnippetVersionBuilder.() -> Unit) {
        val builder = SnippetVersionBuilder()
        builder.block()
        // We'll add the version after the metadata is created
    }

    fun build(): SnippetMetadata {
        val metadata =
            SnippetMetadata(
                id = id,
                name = name,
                language = language,
                description = description,
                ownerId = ownerId,
            )
        // Add default version if none specified
        if (versions.isEmpty()) {
            val version =
                SnippetVersion(
                    versionId = UUID.randomUUID(),
                    snippet = metadata,
                    assetKey = "snippet-$id.ps",
                    createdDate = Date(),
                    versionTag = "1.0",
                )
            metadata.versions.add(version)
        }
        return metadata
    }
}

class SnippetVersionBuilder {
    var versionId: UUID = UUID.randomUUID()
    var assetKey: String = "test-asset-key"
    var createdDate: Date = Date()
    var versionTag: String = "1.0"
}

fun snippetMetadata(block: SnippetMetadataBuilder.() -> Unit): SnippetMetadata {
    val builder = SnippetMetadataBuilder()
    builder.block()
    return builder.build()
}

class SnippetResponseDTOBuilder {
    var id: UUID = UUID.randomUUID()
    var name: String = "Test Snippet"
    var language: String = "printscript"
    var description: String = "A test snippet"
    var ownerId: String = "test-user-id"
    var version: String = "1.0"
    var compliance: String = "pending"

    fun build() =
        SnippetResponseDTO(
            id = id,
            name = name,
            language = language,
            description = description,
            ownerId = ownerId,
            version = version,
            compliance = compliance,
        )
}

fun snippetResponse(block: SnippetResponseDTOBuilder.() -> Unit = {}): SnippetResponseDTO {
    val builder = SnippetResponseDTOBuilder()
    builder.block()
    return builder.build()
}

class SubmitSnippetDTOBuilder {
    var code: String = "let x: number = 1;"
    var name: String = "Test Snippet"
    var language: String = "printscript"
    var version: String = "1.1"
    var description: String = "A test snippet"
    var versionTag: String? = "1.0"

    fun build() =
        SubmitSnippetDTO(
            code = code,
            name = name,
            language = language,
            version = version,
            description = description,
            versionTag = versionTag,
        )
}

fun submitSnippet(block: SubmitSnippetDTOBuilder.() -> Unit = {}): SubmitSnippetDTO {
    val builder = SubmitSnippetDTOBuilder()
    builder.block()
    return builder.build()
}

class SnippetUploadDTOBuilder {
    var name: String = "Test Snippet"
    var language: String = "printscript"
    var version: String = "1.1"
    var description: String = "A test snippet"
    var versionTag: String? = "1.0"

    fun build() =
        SnippetUploadDTO(
            name = name,
            language = language,
            version = version,
            description = description,
            versionTag = versionTag,
        )
}

fun snippetUpload(block: SnippetUploadDTOBuilder.() -> Unit = {}): SnippetUploadDTO {
    val builder = SnippetUploadDTOBuilder()
    builder.block()
    return builder.build()
}

class ShareSnippetDTOBuilder {
    var targetUserId: String = "target-user-id"

    fun build() = ShareSnippetDTO(targetUserId = targetUserId)
}

fun shareSnippet(block: ShareSnippetDTOBuilder.() -> Unit = {}): ShareSnippetDTO {
    val builder = ShareSnippetDTOBuilder()
    builder.block()
    return builder.build()
}

data class TestUser(
    val id: String,
    val token: String = "test-token-$id",
)

fun testUser(id: String = "test-user-${UUID.randomUUID()}") = TestUser(id)

object TestUsers {
    val OWNER = TestUser("owner-user-id", "owner-token")
    val COLLABORATOR = TestUser("collaborator-user-id", "collaborator-token")
    val STRANGER = TestUser("stranger-user-id", "stranger-token")
}
