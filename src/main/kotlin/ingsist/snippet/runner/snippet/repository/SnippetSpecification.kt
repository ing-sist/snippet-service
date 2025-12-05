package ingsist.snippet.runner.snippet.repository

import ingsist.snippet.runner.snippet.domain.ConformanceStatus
import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import org.springframework.data.jpa.domain.Specification
import java.util.UUID

object SnippetSpecification {
    // Filtro base: ¿El usuario tiene acceso? (Es dueño O está compartido con él)
    fun hasAccess(
        userId: String,
        sharedSnippetIds: List<UUID>,
    ): Specification<SnippetMetadata> {
        return Specification { root, _, cb ->
            val isOwner = cb.equal(root.get<String>("ownerId"), userId)

            if (sharedSnippetIds.isNotEmpty()) {
                val isShared = root.get<UUID>("id").`in`(sharedSnippetIds)
                cb.or(isOwner, isShared)
            } else {
                isOwner
            }
        }
    }

    // Nuevo: Filtro estricto para solo traer lo compartido (excluir lo propio)
    fun isShared(sharedSnippetIds: List<UUID>): Specification<SnippetMetadata> {
        return Specification { root, _, _ ->
            root.get<UUID>("id").`in`(sharedSnippetIds)
        }
    }

    // Nuevo: Filtro estricto para solo traer lo propio
    fun isOwned(userId: String): Specification<SnippetMetadata> {
        return Specification { root, _, cb ->
            cb.equal(root.get<String>("ownerId"), userId)
        }
    }

    // Filtros opcionales
    fun nameContains(name: String?): Specification<SnippetMetadata>? {
        return name?.takeIf { it.isNotBlank() }?.let {
            Specification { root, _, cb ->
                cb.like(cb.lower(root.get("name")), "%${it.lowercase()}%")
            }
        }
    }

    fun languageEquals(language: String?): Specification<SnippetMetadata>? {
        return language?.takeIf { it.isNotBlank() }?.let {
            Specification { root, _, cb ->
                cb.equal(cb.lower(root.get("language")), it.lowercase())
            }
        }
    }

    fun conformanceEquals(conformance: String?): Specification<SnippetMetadata>? {
        return conformance?.takeIf { it.isNotBlank() }?.let { statusStr ->
            val status = ConformanceStatus.values().find { it.name == statusStr.uppercase() }
            status?.let {
                Specification { root, _, cb ->
                    cb.equal(root.get<ConformanceStatus>("conformance"), it)
                }
            }
        }
    }
}
