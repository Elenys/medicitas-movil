package com.medicitas.movil.data

import com.medicitas.movil.data.session.SessionCookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCookieJarTest {

    private val http = "http://10.0.2.2:3000/api/v1/auth/login".toHttpUrl()
    private val https = "https://medicitas.example.com/api/v1/auth/login".toHttpUrl()

    /** Igual que la cabecera real: Path=/; Max-Age=28800; Secure; HttpOnly; SameSite=lax. */
    private fun sessionCookie(url: okhttp3.HttpUrl, maxAge: Int = 28800) =
        Cookie.parse(url, "medicitas_session=eyJhbGciOi.jwt; Path=/; Max-Age=$maxAge; Secure; HttpOnly; SameSite=lax")!!

    @Test
    fun `en desarrollo reenvia la cookie Secure por HTTP`() {
        val jar = SessionCookieJar(allowSecureCookiesOverHttp = true)
        jar.saveFromResponse(http, listOf(sessionCookie(http)))
        val sent = jar.loadForRequest("http://10.0.2.2:3000/api/v1/appointments?scope=upcoming".toHttpUrl())
        assertEquals(listOf("medicitas_session"), sent.map { it.name })
        assertTrue(jar.hasSession())
    }

    @Test
    fun `en produccion la cookie Secure no viaja por HTTP`() {
        val jar = SessionCookieJar(allowSecureCookiesOverHttp = false)
        jar.saveFromResponse(http, listOf(sessionCookie(http)))
        assertTrue(jar.loadForRequest(http).isEmpty())
    }

    @Test
    fun `por HTTPS siempre se envia`() {
        val jar = SessionCookieJar(allowSecureCookiesOverHttp = false)
        jar.saveFromResponse(https, listOf(sessionCookie(https)))
        assertEquals(1, jar.loadForRequest("https://medicitas.example.com/api/v1/notifications".toHttpUrl()).size)
    }

    @Test
    fun `no se envia a otro servidor`() {
        val jar = SessionCookieJar(allowSecureCookiesOverHttp = true)
        jar.saveFromResponse(http, listOf(sessionCookie(http)))
        assertTrue(jar.loadForRequest("http://otro-servidor.com/api/v1/appointments".toHttpUrl()).isEmpty())
    }

    @Test
    fun `Max-Age 0 del logout borra la sesion`() {
        val jar = SessionCookieJar(allowSecureCookiesOverHttp = true)
        jar.saveFromResponse(http, listOf(sessionCookie(http)))
        jar.saveFromResponse(http, listOf(sessionCookie(http, maxAge = 0)))
        assertFalse(jar.hasSession())
        assertTrue(jar.loadForRequest(http).isEmpty())
    }

    @Test
    fun `la cookie vencida se descarta`() {
        var now = System.currentTimeMillis()
        val jar = SessionCookieJar(allowSecureCookiesOverHttp = true, now = { now })
        jar.saveFromResponse(http, listOf(sessionCookie(http, maxAge = 60)))
        now += 61_000
        assertTrue(jar.loadForRequest(http).isEmpty())
        assertFalse(jar.hasSession())
    }

    @Test
    fun `clear borra todo`() {
        val jar = SessionCookieJar(allowSecureCookiesOverHttp = true)
        jar.saveFromResponse(http, listOf(sessionCookie(http)))
        jar.clear()
        assertFalse(jar.hasSession())
    }
}
