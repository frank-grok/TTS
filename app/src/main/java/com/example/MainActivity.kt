package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.BurmeseTtsScreen
import com.example.ui.BurmeseTtsViewModel
import com.example.ui.theme.BurmeseTtsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BurmeseTtsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BurmeseTtsTheme {
                BurmeseTtsScreen(viewModel = viewModel)
            }
        }
    }
}
