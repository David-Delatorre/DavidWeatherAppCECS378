package com.plcoding.weatherapp.presentation

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.plcoding.weatherapp.presentation.ui.theme.DarkBlue
import com.plcoding.weatherapp.presentation.ui.theme.DeepBlue
import com.plcoding.weatherapp.presentation.ui.theme.WeatherAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val locationCollectionRef = Firebase.firestore.collection("location")
    private val mainViewModel: MainViewModel by viewModels()
    private val viewModel: WeatherViewModel by viewModels()
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SYSTEM_ALERT_WINDOW)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions_map ->
            if (permissions_map[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions_map[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                viewModel.loadWeatherInfo()
            }
            permissionsToRequest.forEach { current_permission ->
                mainViewModel.onPermissionResult(
                    permission = current_permission,
                    isGranted = permissions_map[current_permission] == true
                )
            }
        }

        permissionLauncher.launch(permissionsToRequest)
        setContent {
            WeatherAppTheme {
                val dialogQueue = mainViewModel.visiblePermissionDialogQueue

                dialogQueue
                    .reversed()
                    .forEach { permission ->
                        PermissionDialog(
                            permissionTextProvider = when (permission) {
                                Manifest.permission.ACCESS_FINE_LOCATION -> {
                                    FineLocationPermissionTextProvider()
                                }

                                Manifest.permission.RECORD_AUDIO -> {
                                    RecordAudioPermissionTextProvider()
                                }

                                Manifest.permission.CALL_PHONE -> {
                                    PhoneCallPermissionTextProvider()
                                }

                                else -> return@forEach
                            },
                            isPermanentlyDeclined = !shouldShowRequestPermissionRationale(
                                permission
                            ),
                            onDismiss = { mainViewModel::dismissDialog },
                            onOkClick = {
                                mainViewModel.dismissDialog()
                                permissionLauncher.launch(arrayOf(permission))
                            },
                            onGoToAppSettingsClick = ::openAppSettings
                        )
                    }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBlue)
                    ) {
                        WeatherCard(
                            state = viewModel.state,
                            backgroundColor = DeepBlue
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        WeatherForecast(state = viewModel.state)
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = {
//                            Intent(applicationContext, SecondActivity::class.java).also {
//                                startActivity(it)
//                            }
                            Intent(Intent.ACTION_MAIN).also {
                                it.`package` = "com.plcoding.backgroundlocationtracking"
                                try {
                                    startActivity(it)
                                } catch (e: ActivityNotFoundException){
                                    e.printStackTrace()
                                }
                            }
//                            val intent = Intent(Intent.ACTION_SEND).apply {
//                                type = "text/plain"
//                                putExtra(Intent.EXTRA_EMAIL, arrayOf("test@test.com"))
//                                putExtra(Intent.EXTRA_SUBJECT, "Test message")
//                                putExtra(Intent.EXTRA_TEXT, "Content")
//                            }
//                            if(intent.resolveActivity(packageManager) != null){
//                                startActivity(intent)
//                            }
                        }
                        ) {
                            Text(text = "Allow Background Tracking")
                        }
                    }
                    if (viewModel.state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    viewModel.state.error?.let { error ->
                        Text(
                            text = error,
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }


}
    fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}
