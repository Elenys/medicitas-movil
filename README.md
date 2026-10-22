# MediCitas Móvil

App Android nativa (Kotlin + Jetpack Compose) del **módulo del paciente** de MediCitas, el sistema
web de citas médicas del proyecto formativo SENA (Análisis y Desarrollo de Software, ficha 3186702).
Consume la API REST real de MediCitas (`/api/v1/…`), sin datos simulados.

Evidencia GA8-220501096-AA2-EV02 — Carlas Elenys Montalvo Mendoza y Freyder López.

## Funciones

| Pantalla | Endpoints |
|---|---|
| Iniciar sesión | `POST /api/v1/auth/login` (cookie httpOnly `medicitas_session`) |
| Mis citas (Próximas / Historial) | `GET /api/v1/appointments?scope=upcoming\|history` |
| Detalle, confirmar, cancelar con motivo | `GET /api/v1/appointments/{id}`, `PATCH …/confirm`, `PATCH …/cancel` |
| Agendar (especialidad → médico → día → hora → reservar) | `GET /specialties`, `GET /doctors?specialtyId=`, `GET /doctors/{id}/availability?month=`, `GET /doctors/{id}/available-slots?date=`, `POST /appointments` |
| Avisos | `GET /api/v1/notifications`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all` |
| Cerrar sesión | `POST /api/v1/auth/logout` |

Los errores de la API (`{"error":{"code","message","details"}}`) se muestran con su `message` en español.

## Arquitectura

```
com.medicitas.movil
├── domain/model        Modelos (User, Appointment, Doctor, Slot…) y ApiException
├── domain/repository   Interfaces de repositorio (contratos que usa la UI)
├── data/remote         MediCitasApi (Retrofit), DTOs, ApiErrorParser
├── data/session        SessionCookieJar, ServerConfig + BaseUrlInterceptor, SessionEvents (401)
├── data/repository     Implementaciones remotas + mapeo DTO → dominio
├── di                  AppContainer (inyección manual de dependencias)
├── ui/<pantalla>       Pantallas Compose + ViewModel (MVVM, StateFlow)
├── ui/components       Componentes reutilizables (StatusBadge, AppointmentCard, botones…)
├── ui/navigation       Mapa de navegación (Navigation Compose)
└── util                Formato de fechas en America/Bogota
```

## Requisitos y compilación

- Android Studio (probado con 2026.1) o JDK 17+ y Android SDK con `platforms;android-36`.
- `local.properties` con `sdk.dir=/ruta/al/Android/sdk` (no se versiona).
- La API de MediCitas corriendo (`docker compose up` en el repositorio de MediCitas → http://localhost:3000).

```bash
./gradlew assembleDebug          # APK en app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 40 pruebas unitarias JVM
```

URL de la API: por defecto `http://10.0.2.2:3000/` (el equipo anfitrión visto desde el emulador).
Se cambia al compilar con `-Pmedicitas.apiBaseUrl=http://192.168.1.50:3000/` o desde la app
(pantalla de ingreso → "Servidor").

## Seguridad

- La sesión es la cookie httpOnly que emite el servidor; vive solo en memoria y se borra al cerrar
  sesión o ante un 401. La app no guarda contraseñas ni tokens en disco.
- **Texto plano (HTTP) solo en la variante debug** (`src/debug/res/xml/network_security_config.xml`),
  para la API local. La variante release prohíbe el texto plano y no reenvía la cookie `Secure` por HTTP.
- Médicos y administradores no pueden usar esta app: se les cierra la sesión y se les remite a la web.
