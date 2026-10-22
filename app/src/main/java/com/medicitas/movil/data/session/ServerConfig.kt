package com.medicitas.movil.data.session

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/** Dónde se guarda la URL del servidor elegida por el usuario (SharedPreferences en la app). */
interface ServerUrlStore {
    fun read(): String?
    fun write(url: String)
}

/**
 * URL base de la API. Por defecto la de BuildConfig (http://10.0.2.2:3000/ para el emulador);
 * desde la pantalla de inicio de sesión se puede cambiar, p. ej. a http://192.168.1.50:3000/
 * cuando la APK se instala en un celular de la misma red que el equipo que corre MediCitas.
 */
class ServerConfig(private val defaultUrl: String, private val store: ServerUrlStore) {

    @Volatile
    var baseUrl: HttpUrl = (store.read() ?: defaultUrl).toHttpUrlOrNull() ?: defaultUrl.toHttpUrlOrNull()!!
        private set

    val display: String get() = baseUrl.toString()

    /** Devuelve false si el texto no es una URL http(s) válida. */
    fun update(raw: String): Boolean {
        val text = raw.trim().let { if (it.endsWith("/")) it else "$it/" }
        val parsed = text.toHttpUrlOrNull() ?: return false
        baseUrl = parsed
        store.write(parsed.toString())
        return true
    }
}

/** Reescribe esquema, host y puerto de cada petición con la URL base vigente. */
class BaseUrlInterceptor(private val config: ServerConfig) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val base = config.baseUrl
        val original = chain.request()
        val url = original.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()
        return chain.proceed(original.newBuilder().url(url).build())
    }
}
