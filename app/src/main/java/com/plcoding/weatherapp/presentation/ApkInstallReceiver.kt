package com.plcoding.weatherapp.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

class ApkInstallReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1 != null) {
            if (p1.action == "PACKAGE_INSTALL_ACTION") {

                when (p1.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        // Additional user action is required to complete the installation (e.g., user confirmation)
                        // You may notify the user or handle this scenario accordingly
                    }
                    PackageInstaller.STATUS_SUCCESS -> {
                        // Installation successful
                        Toast.makeText(p0, "Installation successful", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        // Installation failed
                        Toast.makeText(p0, "Installation failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}