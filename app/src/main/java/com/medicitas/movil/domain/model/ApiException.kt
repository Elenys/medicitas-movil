package com.medicitas.movil.domain.model

/**
 * Error de la API traducido al formato único de MediCitas (04-api §1.2):
 * `{ "error": { "code", "message", "details" } }`. [message] ya viene en español para el usuario.
 */
class ApiException(
    val code: String,
    override val message: String,
    val httpStatus: Int? = null,
    val details: List<FieldError> = emptyList(),
) : Exception(message) {

    data class FieldError(val field: String, val message: String)

    val isUnauthenticated: Boolean get() = code == UNAUTHENTICATED

    companion object {
        const val UNAUTHENTICATED = "UNAUTHENTICATED"
        const val NETWORK_ERROR = "NETWORK_ERROR"
        const val UNKNOWN_ERROR = "UNKNOWN_ERROR"
    }
}
