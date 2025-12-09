package ingsist.snippet.runner.snippet.repository

import ingsist.snippet.runner.snippet.domain.ConformanceStatus
import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SnippetSpecificationTest {
    @Mock
    private lateinit var root: Root<SnippetMetadata>

    @Mock
    private lateinit var query: CriteriaQuery<*>

    @Mock
    private lateinit var cb: CriteriaBuilder

    @Mock
    private lateinit var pathString: Path<String>

    @Mock
    private lateinit var pathUUID: Path<UUID>

    @Mock
    private lateinit var pathEnum: Path<ConformanceStatus>

    @Mock
    private lateinit var predicate: Predicate

    @Test
    fun `hasAccess should return predicate when sharedIds is empty`() {
        val userId = "user1"
        val sharedIds = emptyList<UUID>()

        Mockito.`when`(root.get<String>("ownerId")).thenReturn(pathString)
        Mockito.`when`(cb.equal(pathString, userId)).thenReturn(predicate)

        val spec = SnippetSpecification.hasAccess(userId, sharedIds)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        Mockito.verify(cb).equal(pathString, userId)
        Mockito.verify(cb, Mockito.never()).or(any(), any())
    }

    @Test
    fun `hasAccess should return or predicate when sharedIds is not empty`() {
        val userId = "user1"
        val sharedIds = listOf(UUID.randomUUID())

        Mockito.`when`(root.get<String>("ownerId")).thenReturn(pathString)
        Mockito.`when`(cb.equal(pathString, userId)).thenReturn(predicate)

        Mockito.`when`(root.get<UUID>("id")).thenReturn(pathUUID)
        Mockito.`when`(pathUUID.`in`(sharedIds)).thenReturn(predicate)
        Mockito.`when`(cb.or(predicate, predicate)).thenReturn(predicate)

        val spec = SnippetSpecification.hasAccess(userId, sharedIds)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        Mockito.verify(cb).or(predicate, predicate)
    }

    @Test
    fun `isShared should return in predicate`() {
        val sharedIds = listOf(UUID.randomUUID())

        Mockito.`when`(root.get<UUID>("id")).thenReturn(pathUUID)
        Mockito.`when`(pathUUID.`in`(sharedIds)).thenReturn(predicate)

        val spec = SnippetSpecification.isShared(sharedIds)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
    }

    @Test
    fun `isOwned should return equal predicate`() {
        val userId = "user1"

        Mockito.`when`(root.get<String>("ownerId")).thenReturn(pathString)
        Mockito.`when`(cb.equal(pathString, userId)).thenReturn(predicate)

        val spec = SnippetSpecification.isOwned(userId)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
    }

    @Test
    fun `nameContains should return like predicate`() {
        val name = "test"

        Mockito.`when`(root.get<String>("name")).thenReturn(pathString)
        Mockito.`when`(cb.lower(pathString)).thenReturn(pathString) // Mock expression
        Mockito.`when`(cb.like(any(), Mockito.eq("%test%"))).thenReturn(predicate)

        val spec = SnippetSpecification.nameContains(name)
        val result = spec?.toPredicate(root, query, cb)

        assertNotNull(result)
    }

    @Test
    fun `nameContains should return null when name is null or blank`() {
        var spec = SnippetSpecification.nameContains(null)
        assertEquals(null, spec)

        spec = SnippetSpecification.nameContains("")
        assertEquals(null, spec)

        spec = SnippetSpecification.nameContains("  ")
        assertEquals(null, spec)
    }

    @Test
    fun `languageEquals should return equal predicate`() {
        val language = "printscript"

        Mockito.`when`(root.get<String>("language")).thenReturn(pathString)
        Mockito.`when`(cb.lower(pathString)).thenReturn(pathString)
        Mockito.`when`(cb.equal(any(), Mockito.eq("printscript"))).thenReturn(predicate)

        val spec = SnippetSpecification.languageEquals(language)
        val result = spec?.toPredicate(root, query, cb)

        assertNotNull(result)
    }

    @Test
    fun `languageEquals should return null when language is null or blank`() {
        var spec = SnippetSpecification.languageEquals(null)
        assertEquals(null, spec)

        spec = SnippetSpecification.languageEquals("")
        assertEquals(null, spec)
    }

    @Test
    fun `conformanceEquals should return equal predicate`() {
        val conformance = "COMPLIANT"

        Mockito.`when`(root.get<ConformanceStatus>("conformance")).thenReturn(pathEnum)
        Mockito.`when`(cb.equal(pathEnum, ConformanceStatus.COMPLIANT)).thenReturn(predicate)

        val spec = SnippetSpecification.conformanceEquals(conformance)
        val result = spec?.toPredicate(root, query, cb)

        assertNotNull(result)
    }

    @Test
    fun `conformanceEquals should return null when invalid or blank`() {
        var spec = SnippetSpecification.conformanceEquals(null)
        assertEquals(null, spec)

        spec = SnippetSpecification.conformanceEquals("")
        assertEquals(null, spec)

        spec = SnippetSpecification.conformanceEquals("INVALID_STATUS")
        assertEquals(null, spec)
    }

    private fun <T> any(): T = Mockito.any()

    private fun assertEquals(
        expected: Any?,
        actual: Any?,
    ) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual)
    }
}
