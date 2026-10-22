package com.medicitas.movil.data.remote

import com.medicitas.movil.data.remote.dto.ErrorEnvelopeDto
import com.medicitas.movil.domain.model.ApiException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Convierte cualquier fallo de red o HTTP en una [ApiException] con un mensaje en español.
 * Si el cuerpo trae el formato de MediCitas se usa su `message` tal cual (lo escribe el servidor
 * para el usuario final); si no, se usa un mensaje genérico según el código HTTP.
 */
object ApiErrorParser {

    private val json = Json { ignoreUnknownKeys = true }

    const val NETWORK_MESSAGE =
        "No se pudo conectar con el servidor de MediCitas. Revise su conexión e inténtelo de nuevo."

    fun parse(httpStatus: Int, body: String?): ApiException {
        val envelope = body
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString(ErrorEnvelopeDto.serializer(), it) }.getOrNull() }
        if (envelope != null) {
            val e = envelope.error
            return ApiException(
                code = e.code,
                message = e.message,
                httpStatus = httpStatus,
                details = e.details.map { ApiException.FieldError(it.field, it.message) },
            )
        }
        return ApiException(
            code = if (httpStatus == 401) ApiException.UNAUTHENTICATED else ApiException.UNKNOWN_ERROR,
            message = genericMessage(httpStatus),
            httpStatus = httpStatus,
        )
    }

    fun fromThrowable(t: Throwable): ApiException = when (t) {
        is ApiException -> t
        is HttpException -> parse(t.code(), t.response()?.errorBody()?.string())
        is IOException -> ApiException(ApiException.NETWORK_ERROR, NETWORK_MESSAGE)
        else -> ApiException(ApiException.UNKNOWN_ERROR, "Ocurrió un error inesperado. Inténtelo de nuevo.")
    }

    private fun genericMessage(status: Int): String = when (status) {
        401 -> "Su sesión terminó. Inicie sesión de nuevo."
        403 -> "No tiene permiso para realizar esta acción."
        404 -> "El recurso solicitado no existe."
        in 500..599 -> "El servidor tuvo un problema. Inténtelo más tarde."
        else -> "La solicitud no se pudo completar (código $status)."
    }
}

/** Ejecuta una llamada a la API y la envuelve en Result con una ApiException en caso de error. */
suspend fun <T> apiCall(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (t: Throwable) {
        if (t is kotlinx.coroutines.CancellationException) throw t
        Result.failure(ApiErrorParser.fromThrowable(t))
    }

/** Para endpoints 204: un Response no exitoso se convierte en error con el mismo formato. */
fun retrofit2.Response<Unit>.orThrow() {
    if (!isSuccessful) throw ApiErrorParser.parse(code(), errorBody()?.string())
}
