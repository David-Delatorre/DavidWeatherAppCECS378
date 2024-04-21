package com.plcoding.weatherapp.presentation

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
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
import com.google.android.gms.location.LocationServices
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
            if (permissions_map[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions_map[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
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

        // Hide the navigation bar and status bar: Immersive mode is activated when the activity
        // is created, providing a fullscreen experience. This mode only hides the system UI
        // temporarily and doesn't prevent the user from accessing it when needed.
        window.decorView.apply {
            systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }

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
                            val fusedLocationClient = LocationServices
                                .getFusedLocationProviderClient(this@MainActivity)
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { location -> location.let{
                                    val lat = location.latitude
                                    val long = location.longitude

                                    val intent = Intent("ACTION_SEND_LOCATION").apply {
                                        putExtra("latitude", lat)
                                        putExtra("longitude", long)
                                    }

                                    sendBroadcast(intent) }
                                }
                        }) {
                            Text(text = "Update weather")
                        }
                        Button(onClick = {
                            // Start the service when the button is clicked and the
                            // "Display over other apps" permission is granted
                            if (Settings.canDrawOverlays(this@MainActivity)){
                                val serviceIntent = Intent(this@MainActivity, AlertService::class.java)
                                startService(serviceIntent)
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    "Please allow Display Over Other Apps",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        }) {
                            Text(text = "Click here for a surprise!")
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


