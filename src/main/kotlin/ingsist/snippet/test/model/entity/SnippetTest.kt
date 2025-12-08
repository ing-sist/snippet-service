package ingsist.snippet.test.model.entity

import ingsist.snippet.runner.snippet.domain.SnippetMetadata
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "snippet_test")
data class SnippetTest(
    @Id
    val testId: UUID,
    @Column(name = "test_name")
    val name: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snippet_id", nullable = false)
    val snippet: SnippetMetadata,
    @Column(name = "version_tag")
    val versionTag: String,
    @ElementCollection
    @CollectionTable(name = "snippet_test_inputs", joinColumns = [JoinColumn(name = "test_id")])
    @Column(name = "input_value", columnDefinition = "TEXT")
    @OrderColumn(name = "input_order")
    val inputs: List<String> = emptyList(),
    @ElementCollection
    @CollectionTable(name = "snippet_test_expected_outputs", joinColumns = [JoinColumn(name = "test_id")])
    @Column(name = "expected_value", columnDefinition = "TEXT")
    @OrderColumn(name = "output_order")
    val expectedOutputs: List<String> = emptyList(),
)
