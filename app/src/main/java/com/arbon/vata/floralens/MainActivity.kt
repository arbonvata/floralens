package com.arbon.vata.floralens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arbon.vata.floralens.presentation.PlantDoctorNavGraph
import com.arbon.vata.floralens.ui.theme.FloraLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FloraLensTheme {
                PlantDoctorNavGraph()
            }
        }
    }
}