package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ==========================================
// 1. HARDWARE MOTION SENSOR INTEGRATION
// ==========================================
class ApexSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _gForceX = MutableStateFlow(0f)
    val gForceX: StateFlow<Float> = _gForceX

    private val _gForceY = MutableStateFlow(0f)
    val gForceY: StateFlow<Float> = _gForceY

    private val _gForceZ = MutableStateFlow(1f) // Gravity is resting at 1G
    val gForceZ: StateFlow<Float> = _gForceZ

    private val _tiltPitch = MutableStateFlow(0f)
    val tiltPitch: StateFlow<Float> = _tiltPitch

    private val _tiltRoll = MutableStateFlow(0f)
    val tiltRoll: StateFlow<Float> = _tiltRoll

    fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Earth gravity constant is approx 9.81 m/s^2. Convert raw m/s^2 to G-Force values.
                val rawX = event.values[0] / 9.81f
                val rawY = event.values[1] / 9.81f
                val rawZ = event.values[2] / 9.81f

                // Low pass filter to smooth out sensor noise in vehicle chassis
                _gForceX.value = _gForceX.value * 0.8f + rawX * 0.2f
                _gForceY.value = _gForceY.value * 0.8f + rawY * 0.2f
                _gForceZ.value = _gForceZ.value * 0.8f + rawZ * 0.2f
            }
            Sensor.TYPE_GYROSCOPE -> {
                // Keep track of tilt rotations (pitch/roll)
                val pitch = event.values[0]
                val roll = event.values[1]
                _tiltPitch.value = _tiltPitch.value * 0.9f + pitch * 0.1f
                _tiltRoll.value = _tiltRoll.value * 0.9f + roll * 0.1f
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

// ==========================================
// 2. REAL-TIME GPS & LOCATION MANAGER
// ==========================================
class ApexLocationManager(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _currentSpeedMph = MutableStateFlow(0f)
    val currentSpeedMph: StateFlow<Float> = _currentSpeedMph

    private val _totalDistanceFeet = MutableStateFlow(0f)
    val totalDistanceFeet: StateFlow<Float> = _totalDistanceFeet

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private var lastLocation: Location? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            _currentLocation.value = location

            // Speed from location provider in meters/sec. Convert to MPH.
            val rawSpeedMps = if (location.hasSpeed()) location.speed else 0f
            val speedMph = rawSpeedMps * 2.23694f
            _currentSpeedMph.value = speedMph

            // Accumulate distance
            lastLocation?.let { last ->
                if (_isTracking.value) {
                    val distanceMeters = last.distanceTo(location)
                    val distanceFeet = distanceMeters * 3.28084f
                    _totalDistanceFeet.value += distanceFeet
                }
            }
            lastLocation = location
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationTracking() {
        if (_isTracking.value) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 200).apply {
            setMinUpdateIntervalMillis(100)
            setMinUpdateDistanceMeters(0.1f)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            _isTracking.value = true
            _totalDistanceFeet.value = 0f
            lastLocation = null
            Log.d("ApexLocation", "Gps updates activated successfully.")
        } catch (e: Exception) {
            Log.e("ApexLocation", "Could not start hardware location updates: ${e.message}")
        }
    }

    fun stopLocationTracking() {
        if (!_isTracking.value) return
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.e("ApexLocation", "Could not remove hardware location updates: ${e.message}")
        }
        _isTracking.value = false
    }
}
