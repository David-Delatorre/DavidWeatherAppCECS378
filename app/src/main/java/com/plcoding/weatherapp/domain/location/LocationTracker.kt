package com.plcoding.weatherapp.domain.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationTracker {

    suspend fun getCurrentLocation(): Location?
}