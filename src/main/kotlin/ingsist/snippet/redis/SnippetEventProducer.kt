import java.util.UUID

interface SnippetEventProducer {
    fun publishSnippet(snippetId: UUID)
}
