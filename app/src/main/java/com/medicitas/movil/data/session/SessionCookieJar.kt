package com.medicitas.movil.data.session

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Guarda en memoria la cookie de sesión `medicitas_session` (httpOnly, JWT) que emite
 * POST /api/v1/auth/login, y la reenvía en cada petición al mismo servidor.
 *
 * - La cookie nunca se escribe en disco ni la lee la interfaz: muere con el proceso o al cerrar
 *   sesión ([clear]).
 * - La API marca la cookie como `Secure`. En producción solo viaja por HTTPS (regla de OkHttp).
 *   Con [allowSecureCookiesOverHttp] = true (SOLO variante debug, contra la API local por HTTP)
 *   se envía también por HTTP, igual que hace el navegador con localhost.
 */
class SessionCookieJar(
    private val allowSecureCookiesOverHttp: Boolean,
    private val now: () -> Long = System::currentTimeMillis,
) : CookieJar {

    private val cookies = mutableListOf<Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            this.cookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
            if (cookie.expiresAt > now()) this.cookies += cookie // Max-Age=0 (logout) solo borra.
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        cookies.removeAll { it.expiresAt <= now() }
        val httpsEquivalent = if (!url.isHttps && allowSecureCookiesOverHttp) {
            url.newBuilder().scheme("https").build()
        } else {
            url
        }
        return cookies.filter { it.matches(url) || (it.secure && it.matches(httpsEquivalent)) }
    }

    @Synchronized
    fun hasSession(): Boolean = cookies.any { it.name == SESSION_COOKIE && it.expiresAt > now() }

    @Synchronized
    fun clear() = cookies.clear()

    companion object {
        const val SESSION_COOKIE = "medicitas_session"
    }
}
