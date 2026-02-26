package com.diplom

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.diplom.tuner.TunerUI
import com.diplom.tuner.TunerViewModel
import com.diplom.ui.theme.DiplomTheme


class MainActivity : ComponentActivity() {

    private lateinit var viewModel: TunerViewModel

    private var permissionGranted by mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                permissionGranted = true
                viewModel.start() // старт тюнера после разрешения
            } else {
                permissionGranted = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = TunerViewModel(this)

        setContent {
            DiplomTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {

                        // Проверка разрешения микрофона
                        LaunchedEffect(Unit) {
                            permissionGranted = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!permissionGranted) {
                                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                viewModel.start()
                            }
                        }

                        // UI тюнера
                        if (permissionGranted) {
                            TunerUI(viewModel = viewModel)
                        } else {
                            Text(
                                text = "Мікрофон не дозволено. Надати доступ для роботи тюнера.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stop()
    }
}