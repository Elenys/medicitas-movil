package com.medicitas.movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.medicitas.movil.ui.navigation.MediCitasNavHost
import com.medicitas.movil.ui.theme.MediCitasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MediCitasApp).container
        setContent {
            MediCitasTheme {
                MediCitasNavHost(container)
            }
        }
    }
}
