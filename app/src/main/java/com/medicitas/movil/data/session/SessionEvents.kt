package com.medicitas.movil.data.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Avisa a la interfaz cuando el servidor responde 401 a una petición protegida (token vencido o
 * cuenta desactivada, 04-api §1.4): la app borra la sesión y vuelve a la pantalla de ingreso.
 */
class SessionEvents {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()
    fun notifyExpired() { _expired.tryEmit(Unit) }
}

class UnauthorizedInterceptor(
    private val jar: SessionCookieJar,
    private val events: SessionEvents,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val isAuthEndpoint = request.url.encodedPath.startsWith("/api/v1/auth/")
        if (response.code == 401 && !isAuthEndpoint) {
            jar.clear()
            events.notifyExpired()
        }
        return response
    }
}
