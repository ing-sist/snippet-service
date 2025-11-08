package ing.sist.snippet.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "snippets")
data class Snippet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    var name: String,
    @Column
    var description: String? = null,
    @Column(nullable = false)
    var language: String,
    @Column(nullable = false)
    var languageVersion: String,
    // Este es el ID de usuario de Auth0 (el 'sub' del JWT)
    @Column(nullable = false)
    val ownerId: String,
)
