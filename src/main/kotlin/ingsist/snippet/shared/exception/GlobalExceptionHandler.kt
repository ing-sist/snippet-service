package ingsist.snippet.shared.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
)

class SnippetNotFoundException(message: String) : RuntimeException(message)

class SnippetAccessDeniedException(message: String) : RuntimeException(message)

class InvalidSnippetException(val errors: List<String>) : RuntimeException(errors.joinToString(", "))

class ExternalServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(SnippetNotFoundException::class)
    fun handleSnippetNotFound(
        ex: SnippetNotFoundException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val error =
            ErrorResponse(
                status = HttpStatus.NOT_FOUND.value(),
                error = "Not Found",
                message = ex.message ?: "Snippet not found",
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }

    @ExceptionHandler(SnippetAccessDeniedException::class)
    fun handleAccessDenied(
        ex: SnippetAccessDeniedException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val error =
            ErrorResponse(
                status = HttpStatus.FORBIDDEN.value(),
                error = "Forbidden",
                message = ex.message ?: "Access denied",
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
    }

    @ExceptionHandler(InvalidSnippetException::class)
    fun handleInvalidSnippet(
        ex: InvalidSnippetException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val error =
            ErrorResponse(
                status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
                error = "Unprocessable Entity",
                message = ex.errors.joinToString("; "),
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errors =
            ex.bindingResult.fieldErrors
                .map { "${it.field}: ${it.defaultMessage}" }
                .joinToString("; ")

        val error =
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Bad Request",
                message = errors,
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val error =
            ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                error = "Internal Server Error",
                message = ex.message ?: "An unexpected error occurred",
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }

    @ExceptionHandler(ExternalServiceException::class)
    fun handleExternalServiceException(
        ex: ExternalServiceException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val error =
            ErrorResponse(
                status = HttpStatus.BAD_GATEWAY.value(),
                error = "Bad Gateway",
                message = ex.message ?: "External service error",
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error)
    }
}
