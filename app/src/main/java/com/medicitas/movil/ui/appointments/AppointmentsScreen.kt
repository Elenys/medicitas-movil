package com.medicitas.movil.ui.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medicitas.movil.domain.model.AppointmentScope
import com.medicitas.movil.ui.UiState
import com.medicitas.movil.ui.components.AppointmentCard
import com.medicitas.movil.ui.components.EmptyState
import com.medicitas.movil.ui.components.ErrorBox
import com.medicitas.movil.ui.components.LoadingBox
import com.medicitas.movil.ui.components.PrimaryButton
import com.medicitas.movil.ui.theme.MediColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    viewModel: AppointmentsViewModel,
    greetingName: String?,
    onOpen: (Int) -> Unit,
    onBook: () -> Unit,
) {
    val scope by viewModel.scope.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Cada vez que la pantalla entra en composición (también al volver del detalle) se recarga.
    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize()) {
        if (greetingName != null) {
            Text(
                "Hola, $greetingName",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )
            Text(
                "Estas son sus citas médicas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediColors.TextMuted,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        PrimaryTabRow(selectedTabIndex = scope.ordinal, containerColor = MediColors.Background) {
            Tab(scope == AppointmentScope.UPCOMING, { viewModel.selectScope(AppointmentScope.UPCOMING) }, text = { Text("Próximas") })
            Tab(scope == AppointmentScope.HISTORY, { viewModel.selectScope(AppointmentScope.HISTORY) }, text = { Text("Historial") })
        }
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.message, onRetry = viewModel::load)
            is UiState.Success ->
                if (s.data.isEmpty()) {
                    if (scope == AppointmentScope.UPCOMING) {
                        EmptyState("No tiene citas próximas", "Agende una cita con el médico de su preferencia.") {
                            PrimaryButton("Agendar cita", onClick = onBook)
                        }
                    } else {
                        EmptyState("Sin historial", "Aquí aparecerán sus citas pasadas y canceladas.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(s.data, key = { it.id }) { appointment ->
                            AppointmentCard(appointment, onClick = { onOpen(appointment.id) })
                        }
                    }
                }
        }
    }
}
