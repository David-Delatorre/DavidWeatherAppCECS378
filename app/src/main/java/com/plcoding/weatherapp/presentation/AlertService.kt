package com.plcoding.weatherapp.presentation

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.plcoding.weatherapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//Create a service that displays the system alert window
class AlertService : Service() {

    private var windowManager: WindowManager? = null
    private var alertView: View? = null
//    private val coroutineScope = CoroutineScope(Dispatchers.Main)


    //onBind method is called when another component wants to bind with the service
    // by calling bindService(). Since this service doesn't support binding, it
    // returns null.
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    //onStartCommand method is called when the service is started using startService().
    // It's where you put the code to initialize and start the service. In this case,
    // it calls the showOverlay() function to display the system alert window and returns
    // START_STICKY, which tells the system to restart the service if it's killed.
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay()
        return START_STICKY
    }

    //showOverlay function is responsible for displaying the system alert window.
    // It creates a WindowManager.LayoutParams object to specify the properties of
    // the window, such as size, type, and flags. Then, it initializes the windowManager
    // using the system service WINDOW_SERVICE and inflates the layout for the alert window.
    // Finally, it adds the alert view to the window manager, sets a delay using
    // Handler().postDelayed() to close the alert after 1 minute, and stops the service
    // by calling stopSelf().
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("InflateParams")
    private fun showOverlay() {


        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        alertView = inflater.inflate(R.layout.alert_window, null)

        windowManager?.addView(alertView, params)

        // Close the alert after 1 minute using Handler
        Handler().postDelayed({
            stopSelf()
        }, 60000)

        //Close the alert after 1 minute using coroutines
//        coroutineScope.launch {
//            delay(60000)
//            stopSelf()
//        }
    }

    // onDestroy method is called when the service is stopped or destroyed. It's where you release
    // any resources held by the service. In this case, it removes the alert view from the window
    // manager to clean up resources.
    override fun onDestroy() {
        super.onDestroy()
        windowManager?.removeView(alertView)
//        coroutineScope.cancel()
    }
}