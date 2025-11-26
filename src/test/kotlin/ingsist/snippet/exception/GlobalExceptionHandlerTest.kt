package ingsist.snippet.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.ServletWebRequest

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {
    private val exceptionHandler = GlobalExceptionHandler()
    private val request = ServletWebRequest(MockHttpServletRequest())

    @Test
    @DisplayName("GIVEN SnippetNotFoundException WHEN handling THEN returns 404")
    fun `handleSnippetNotFound returns 404`() {
        val ex = SnippetNotFoundException("Snippet not found")
        val response = exceptionHandler.handleSnippetNotFound(ex, request)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("Not Found", response.body?.error)
        assertEquals("Snippet not found", response.body?.message)
    }

    @Test
    @DisplayName("GIVEN SnippetAccessDeniedException WHEN handling THEN returns 403")
    fun `handleAccessDenied returns 403`() {
        val ex = SnippetAccessDeniedException("Access denied")
        val response = exceptionHandler.handleAccessDenied(ex, request)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals("Forbidden", response.body?.error)
        assertEquals("Access denied", response.body?.message)
    }

    @Test
    @DisplayName("GIVEN InvalidSnippetException WHEN handling THEN returns 422")
    fun `handleInvalidSnippet returns 422`() {
        val ex = InvalidSnippetException(listOf("Error 1", "Error 2"))
        val response = exceptionHandler.handleInvalidSnippet(ex, request)

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
        assertEquals("Unprocessable Entity", response.body?.error)
        assertEquals("Error 1; Error 2", response.body?.message)
    }

    @Test
    @DisplayName("GIVEN generic Exception WHEN handling THEN returns 500")
    fun `handleGenericException returns 500`() {
        val ex = RuntimeException("Something went wrong")
        val response = exceptionHandler.handleGenericException(ex, request)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Internal Server Error", response.body?.error)
        assertEquals("Something went wrong", response.body?.message)
    }

    @Test
    @DisplayName("GIVEN ExternalServiceException WHEN handling THEN returns 502")
    fun `handleExternalServiceException returns 502`() {
        val ex = ExternalServiceException("Service unavailable")
        val response = exceptionHandler.handleExternalServiceException(ex, request)

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertEquals("Bad Gateway", response.body?.error)
        assertEquals("Service unavailable", response.body?.message)
    }

    @Test
    @DisplayName("GIVEN MethodArgumentNotValidException WHEN handling THEN returns 400")
    fun `handleValidationErrors returns 400`() {
        val bindingResult = mock(BindingResult::class.java)
        val fieldError = FieldError("object", "field", "defaultMessage")
        whenever(bindingResult.fieldErrors).thenReturn(listOf(fieldError))

        val methodParameter = mock(MethodParameter::class.java)
        val ex = MethodArgumentNotValidException(methodParameter, bindingResult)

        val response = exceptionHandler.handleValidationErrors(ex, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Bad Request", response.body?.error)
        assertTrue(response.body?.message?.contains("field: defaultMessage") == true)
    }
}
