package com.plcoding.weatherapp.presentation

import android.location.Location
import com.google.protobuf.DescriptorProtos
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(interval: Long): Flow<Location>

    class LocationException(message: String): Exception()
}