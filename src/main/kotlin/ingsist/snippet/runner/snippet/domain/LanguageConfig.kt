package ingsist.snippet.runner.snippet.domain

import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "language_config")
data class LanguageConfig(
    @Id
    @GeneratedValue
    @UuidGenerator
    val id: UUID? = null,
    @Column(nullable = false, unique = true)
    val language: String,
    @Column(nullable = false)
    var extension: String,
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "version")
    var versions: List<String> = emptyList(),
)
