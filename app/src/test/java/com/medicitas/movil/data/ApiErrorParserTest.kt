package com.medicitas.movil.data

import com.medicitas.movil.data.remote.ApiErrorParser
import com.medicitas.movil.domain.model.ApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ApiErrorParserTest {

    @Test
    fun `lee code, message y details del formato de MediCitas`() {
        // Respuesta real de POST /api/v1/appointments con cuerpo vacío.
        val body = """{"error":{"code":"VALIDATION_ERROR","message":"Revise los datos ingresados.",
            "details":[{"field":"doctorId","message":"Invalid input"},{"field":"startsAt","message":"Fecha y hora inválidas"}]}}"""
        val e = ApiErrorParser.parse(400, body)
        assertEquals("VALIDATION_ERROR", e.code)
        assertEquals("Revise los datos ingresados.", e.message)
        assertEquals(400, e.httpStatus)
        assertEquals(2, e.details.size)
        assertEquals(ApiException.FieldError("startsAt", "Fecha y hora inválidas"), e.details[1])
    }

    @Test
    fun `sin details la lista queda vacia`() {
        val e = ApiErrorParser.parse(401, """{"error":{"code":"INVALID_CREDENTIALS","message":"Correo o contraseña incorrectos."}}""")
        assertEquals("INVALID_CREDENTIALS", e.code)
        assertEquals("Correo o contraseña incorrectos.", e.message)
        assertTrue(e.details.isEmpty())
    }

    @Test
    fun `ignora campos desconocidos`() {
        val e = ApiErrorParser.parse(409, """{"error":{"code":"SLOT_TAKEN","message":"Ese horario acaba de ser reservado.","extra":1},"x":true}""")
        assertEquals("SLOT_TAKEN", e.code)
    }

    @Test
    fun `cuerpo que no es JSON da un mensaje generico en espanol`() {
        val e = ApiErrorParser.parse(502, "<html>Bad gateway</html>")
        assertEquals(ApiException.UNKNOWN_ERROR, e.code)
        assertEquals("El servidor tuvo un problema. Inténtelo más tarde.", e.message)
    }

    @Test
    fun `401 sin cuerpo se trata como sesion vencida`() {
        val e = ApiErrorParser.parse(401, null)
        assertTrue(e.isUnauthenticated)
        assertEquals("Su sesión terminó. Inicie sesión de nuevo.", e.message)
    }

    @Test
    fun `fallo de red se traduce a NETWORK_ERROR`() {
        val e = ApiErrorParser.fromThrowable(IOException("timeout"))
        assertEquals(ApiException.NETWORK_ERROR, e.code)
        assertEquals(ApiErrorParser.NETWORK_MESSAGE, e.message)
    }
}
