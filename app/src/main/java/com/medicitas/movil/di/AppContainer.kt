package com.medicitas.movil.di

import android.content.Context
import com.medicitas.movil.BuildConfig
import com.medicitas.movil.data.remote.MediCitasApi
import com.medicitas.movil.data.repository.RemoteAppointmentRepository
import com.medicitas.movil.data.repository.RemoteAuthRepository
import com.medicitas.movil.data.repository.RemoteCatalogRepository
import com.medicitas.movil.data.repository.RemoteNotificationRepository
import com.medicitas.movil.data.session.BaseUrlInterceptor
import com.medicitas.movil.data.session.ServerConfig
import com.medicitas.movil.data.session.ServerUrlStore
import com.medicitas.movil.data.session.SessionCookieJar
import com.medicitas.movil.data.session.SessionEvents
import com.medicitas.movil.data.session.UnauthorizedInterceptor
import com.medicitas.movil.domain.repository.AppointmentRepository
import com.medicitas.movil.domain.repository.AuthRepository
import com.medicitas.movil.domain.repository.CatalogRepository
import com.medicitas.movil.domain.repository.NotificationRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Inyección de dependencias manual (patrón Service Locator / Composition Root): crea una sola vez
 * el cliente HTTP, Retrofit y los repositorios, y los entrega a los ViewModel.
 */
class AppContainer(context: Context) {

    private val prefs = context.getSharedPreferences("medicitas", Context.MODE_PRIVATE)

    val serverConfig = ServerConfig(
        defaultUrl = BuildConfig.API_BASE_URL,
        store = object : ServerUrlStore {
            override fun read(): String? = prefs.getString("server_url", null)
            override fun write(url: String) = prefs.edit().putString("server_url", url).apply()
        },
    )

    val cookieJar = SessionCookieJar(allowSecureCookiesOverHttp = BuildConfig.DEV_INSECURE_TRANSPORT)
    val sessionEvents = SessionEvents()

    val api: MediCitasApi = createApi(
        baseUrl = BuildConfig.API_BASE_URL,
        client = createHttpClient(cookieJar, sessionEvents, serverConfig, BuildConfig.DEBUG),
    )

    val authRepository: AuthRepository = RemoteAuthRepository(api, cookieJar)
    val appointmentRepository: AppointmentRepository = RemoteAppointmentRepository(api)
    val catalogRepository: CatalogRepository = RemoteCatalogRepository(api)
    val notificationRepository: NotificationRepository = RemoteNotificationRepository(api)

    companion object {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

        fun createHttpClient(
            jar: SessionCookieJar,
            events: SessionEvents,
            serverConfig: ServerConfig?,
            logBodies: Boolean,
        ): OkHttpClient = OkHttpClient.Builder()
            .cookieJar(jar)
            .apply { if (serverConfig != null) addInterceptor(BaseUrlInterceptor(serverConfig)) }
            .addInterceptor(UnauthorizedInterceptor(jar, events))
            .apply {
                if (logBodies) {
                    // Solo en debug, y sin imprimir la cabecera de la cookie de sesión.
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                            redactHeader("Cookie")
                            redactHeader("Set-Cookie")
                        },
                    )
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        fun createApi(baseUrl: String, client: OkHttpClient): MediCitasApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MediCitasApi::class.java)
    }
}
