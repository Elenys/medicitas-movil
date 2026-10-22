package com.medicitas.movil.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medicitas.movil.di.AppContainer
import com.medicitas.movil.ui.appointments.AppointmentsScreen
import com.medicitas.movil.ui.appointments.AppointmentsViewModel
import com.medicitas.movil.ui.booking.BookingScreen
import com.medicitas.movil.ui.booking.BookingViewModel
import com.medicitas.movil.ui.detail.AppointmentDetailScreen
import com.medicitas.movil.ui.detail.AppointmentDetailViewModel
import com.medicitas.movil.ui.login.LoginScreen
import com.medicitas.movil.ui.login.LoginViewModel
import com.medicitas.movil.ui.notifications.NotificationsScreen
import com.medicitas.movil.ui.notifications.NotificationsViewModel
import com.medicitas.movil.ui.theme.MediColors
import kotlinx.coroutines.launch

/** Mapa de navegación de la app (rutas). */
object Routes {
    const val LOGIN = "login"
    const val APPOINTMENTS = "citas"
    const val BOOKING = "agendar"
    const val NOTIFICATIONS = "avisos"
    const val DETAIL = "cita/{id}"
    fun detail(id: Int) = "cita/$id"
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab(Routes.APPOINTMENTS, "Mis citas", Icons.Outlined.CalendarMonth),
    Tab(Routes.BOOKING, "Agendar", Icons.Outlined.AddCircleOutline),
    Tab(Routes.NOTIFICATIONS, "Avisos", Icons.Outlined.Notifications),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediCitasNavHost(container: AppContainer) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    var askLogout by remember { mutableStateOf(false) }

    fun goToLogin() = nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }

    // 401 en cualquier petición protegida: la sesión venció o la cuenta se desactivó.
    LaunchedEffect(Unit) {
        container.sessionEvents.expired.collect { if (nav.currentDestination?.route != Routes.LOGIN) goToLogin() }
    }

    Scaffold(
        topBar = {
            if (route != null && route != Routes.LOGIN) {
                TopAppBar(
                    title = {
                        Text(
                            when (route) {
                                Routes.APPOINTMENTS -> "MediCitas"
                                Routes.BOOKING -> "Agendar cita"
                                Routes.NOTIFICATIONS -> "Avisos"
                                else -> "Detalle de la cita"
                            },
                        )
                    },
                    navigationIcon = {
                        if (route == Routes.DETAIL) {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { askLogout = true }) {
                            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Cerrar sesión")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MediColors.PrimaryDark,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                )
            }
        },
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar(containerColor = MediColors.Surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MediColors.Success,
                                selectedTextColor = MediColors.Success,
                                indicatorColor = MediColors.SuccessLight,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.LOGIN, modifier = Modifier.padding(padding)) {
            composable(Routes.LOGIN) {
                val vm: LoginViewModel = viewModel(
                    factory = viewModelFactory { initializer { LoginViewModel(container.authRepository, container.serverConfig) } },
                )
                LoginScreen(vm, onLoggedIn = {
                    nav.navigate(Routes.APPOINTMENTS) { popUpTo(Routes.LOGIN) { inclusive = true } }
                })
            }
            composable(Routes.APPOINTMENTS) {
                val vm: AppointmentsViewModel = viewModel(
                    factory = viewModelFactory { initializer { AppointmentsViewModel(container.appointmentRepository) } },
                )
                AppointmentsScreen(
                    viewModel = vm,
                    greetingName = container.authRepository.currentUser?.firstName,
                    onOpen = { nav.navigate(Routes.detail(it)) },
                    onBook = { nav.switchTab(Routes.BOOKING) },
                )
            }
            composable(Routes.BOOKING) {
                val vm: BookingViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { BookingViewModel(container.catalogRepository, container.appointmentRepository) }
                    },
                )
                BookingScreen(
                    viewModel = vm,
                    onOpenAppointment = { id ->
                        nav.switchTab(Routes.APPOINTMENTS)
                        nav.navigate(Routes.detail(id))
                    },
                    onFinished = { nav.switchTab(Routes.APPOINTMENTS) },
                )
            }
            composable(Routes.NOTIFICATIONS) {
                val vm: NotificationsViewModel = viewModel(
                    factory = viewModelFactory { initializer { NotificationsViewModel(container.notificationRepository) } },
                )
                NotificationsScreen(vm, onOpenAppointment = { nav.navigate(Routes.detail(it)) })
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                val id = entry.arguments?.getInt("id") ?: 0
                val vm: AppointmentDetailViewModel = viewModel(
                    factory = viewModelFactory { initializer { AppointmentDetailViewModel(id, container.appointmentRepository) } },
                )
                AppointmentDetailScreen(vm)
            }
        }
    }

    if (askLogout) {
        AlertDialog(
            onDismissRequest = { askLogout = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Desea salir de MediCitas en este dispositivo?") },
            confirmButton = {
                TextButton(onClick = {
                    askLogout = false
                    scope.launch {
                        container.authRepository.logout()
                        goToLogin()
                    }
                }) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { askLogout = false }) { Text("Cancelar") } },
        )
    }
}

/** Cambia de pestaña sin apilar pantallas repetidas. */
private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(Routes.APPOINTMENTS) { saveState = route != Routes.APPOINTMENTS }
    launchSingleTop = true
    restoreState = route != Routes.APPOINTMENTS
}
