package com.example.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.example.data.ApexRepository
import com.example.data.RaceRun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.sqrt

enum class RaceSessionState {
    IDLE, COUNTDOWN, ACTIVE, FINISHED
}

class RaceSessionManager(
    private val context: Context,
    private val repository: ApexRepository,
    private val scope: CoroutineScope
) : LocationListener, SensorEventListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _state = MutableStateFlow(RaceSessionState.IDLE)
    val state: StateFlow<RaceSessionState> = _state

    private val _speedMph = MutableStateFlow(0.0)
    val speedMph: StateFlow<Double> = _speedMph

    private val _gForce = MutableStateFlow(0.0)
    val gForce: StateFlow<Double> = _gForce

    private val _gForceX = MutableStateFlow(0.0)
    val gForceX: StateFlow<Double> = _gForceX

    private val _gForceY = MutableStateFlow(0.0)
    val gForceY: StateFlow<Double> = _gForceY

    private val _liveLatitude = MutableStateFlow<Double?>(null)
    val liveLatitude: StateFlow<Double?> = _liveLatitude

    private val _liveLongitude = MutableStateFlow<Double?>(null)
    val liveLongitude: StateFlow<Double?> = _liveLongitude

    private val _countdown = MutableStateFlow(3)
    val countdown: StateFlow<Int> = _countdown

    data class RaceTrack(
        val name: String,
        val startLat: Double,
        val startLon: Double,
        val finishLat: Double,
        val finishLon: Double,
        val lengthMiles: Double,
        val description: String
    )

    companion object {
        val PRESET_TRACKS = listOf(
            RaceTrack("Mulholland Canyon Sprint", 34.1293, -118.4019, 34.1330, -118.4010, 0.5, "High elevation winding curves challenge"),
            RaceTrack("Valley Bypass ¼ Mile Drag", 34.1200, -118.3940, 34.1235, -118.3910, 0.25, "Continuous high RPM drag sprint"),
            RaceTrack("Angeles Crest Time Attack", 34.1293, -118.4019, 34.1400, -118.4110, 1.25, "Long endurance high speed layout")
        )
    }

    private val _selectedTrack = MutableStateFlow(PRESET_TRACKS[0])
    val selectedTrack: StateFlow<RaceTrack> = _selectedTrack

    private val _userProgress = MutableStateFlow(0f)
    val userProgress: StateFlow<Float> = _userProgress

    private val _opponentProgresses = MutableStateFlow<Map<String, Float>>(emptyMap())
    val opponentProgresses: StateFlow<Map<String, Float>> = _opponentProgresses

    private val _liveLeaderboard = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val liveLeaderboard: StateFlow<List<Pair<String, Float>>> = _liveLeaderboard

    private val activeOpponentsList = listOf("M4_LaunchMonster", "GTR_Steve")

    private val telemetryRecorded = mutableListOf<String>()

    private var startTimeMillis = 0L
    private var endTimeMillis = 0L
    private var startLat: Double? = null
    private var startLon: Double? = null

    init {
        try {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { accSensor ->
                sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (e: Exception) {
            Log.e("RaceSessionManager", "Sensor error: ${e.message}")
        }
        updateLeaderboard()
    }

    fun selectTrack(track: RaceTrack) {
        _selectedTrack.value = track
        _userProgress.value = 0f
        _opponentProgresses.value = emptyMap()
        updateLeaderboard()
    }

    private fun updateLeaderboard() {
        val list = mutableListOf("Apex_Pilot" to _userProgress.value)
        _opponentProgresses.value.forEach { (name, prog) ->
            list.add(name to prog)
        }
        _liveLeaderboard.value = list.sortedByDescending { it.second }
    }

    fun startRaceSession() {
        if (_state.value != RaceSessionState.IDLE) return
        _state.value = RaceSessionState.COUNTDOWN
        _countdown.value = 3
        telemetryRecorded.clear()
        
        _userProgress.value = 0f
        _opponentProgresses.value = emptyMap()
        updateLeaderboard()

        startLat = _selectedTrack.value.startLat
        startLon = _selectedTrack.value.startLon

        scope.launch(Dispatchers.Default) {
            for (i in 3 downTo 1) {
                _countdown.value = i
                kotlinx.coroutines.delay(1000)
            }
            // Trigger Active Session
            startTimeMillis = System.currentTimeMillis()
            _state.value = RaceSessionState.ACTIVE
            
            // Register GPS updates if permissions exist
            try {
                launch(Dispatchers.Main) {
                    try {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            500L,
                            1f,
                            this@RaceSessionManager
                        )
                    } catch (e: Exception) {
                        Log.e("RaceSessionManager", "Fallback GPS loop: ${e.message}")
                    }
                }
            } catch (e: SecurityException) {
                Log.e("RaceSessionManager", "No GPS permission: ${e.message}")
            }

            // Spawn real-time opponent progress updates
            val trackLen = _selectedTrack.value.lengthMiles
            val oppsMap = mutableMapOf<String, Float>()
            activeOpponentsList.forEach { oppsMap[it] = 0f }
            _opponentProgresses.value = oppsMap
            updateLeaderboard()

            launch {
                val delayMs = 150L
                val timeStepHours = delayMs / (1000.0 * 3600.0)
                val rand = java.util.Random()
                while (_state.value == RaceSessionState.ACTIVE) {
                    kotlinx.coroutines.delay(delayMs)
                    val currentOpps = _opponentProgresses.value.toMutableMap()
                    currentOpps.keys.forEach { opp ->
                        val currentProg = currentOpps[opp] ?: 0f
                        if (currentProg < 1.0f) {
                            val baseSpeed = when (opp) {
                                "M4_LaunchMonster" -> 85f
                                "GTR_Steve" -> 80f
                                else -> 72f
                            }
                            val instantaneousSpeed = baseSpeed + rand.nextFloat() * 30f
                            val distanceDelta = instantaneousSpeed * timeStepHours
                            val nextProg = (currentProg + (distanceDelta / trackLen)).toFloat().coerceIn(0f, 1f)
                            currentOpps[opp] = nextProg
                        }
                    }
                    _opponentProgresses.value = currentOpps
                    updateLeaderboard()
                }
            }
        }
    }

    fun stopRaceSession(validationReason: String = "VALID") {
        if (_state.value != RaceSessionState.ACTIVE) return
        _state.value = RaceSessionState.FINISHED
        endTimeMillis = System.currentTimeMillis()

        try {
            locationManager.removeUpdates(this)
        } catch (e: Exception) {
            Log.e("RaceSessionManager", "De-register error: ${e.message}")
        }

        // Save RaceRun log atomically
        scope.launch {
            val durationSec = (endTimeMillis - startTimeMillis) / 1000.0
            val valid = if (telemetryRecorded.any { 
                val parts = it.split("|")
                val s = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                s > 260.0
            }) "ANTI_CHEAT_TRIGGERED" else validationReason

            val finalLeaderboard = _liveLeaderboard.value
            val userPosition = finalLeaderboard.indexOfFirst { it.first == "Apex_Pilot" }
            val won = userPosition == 0

            val run = RaceRun(
                startTime = startTimeMillis,
                endTime = endTimeMillis,
                distanceMiles = _selectedTrack.value.lengthMiles,
                durationSeconds = durationSec,
                avgSpeedMph = 45.0 + (sqrt(durationSec) * 5.0),
                maxSpeedMph = _speedMph.value,
                telemetryTrace = telemetryRecorded.joinToString(","),
                validationStatus = valid
            )
            repository.insertRaceRun(run)

            // Dynamic ELO system rank settlement
            try {
                val myRating = repository.getDriverRating("Apex_Pilot").first() ?: com.example.data.DriverRating("Apex_Pilot", 1650, 34, 11, 4, "Platinum")
                val opponentName = finalLeaderboard.firstOrNull { it.first != "Apex_Pilot" }?.first ?: "M4_LaunchMonster"
                val opponentRating = repository.getDriverRating(opponentName).first() ?: com.example.data.DriverRating(opponentName, 1580, 28, 14, 2, "Platinum")

                val kFactor = 32f
                val expectedMy = 1.0 / (1.0 + java.lang.Math.pow(10.0, (opponentRating.eloRating - myRating.eloRating) / 400.0))
                val actualScore = if (won) 1.0 else 0.0
                
                val newMyElo = (myRating.eloRating + kFactor * (actualScore - expectedMy)).toInt()
                val newOppElo = (opponentRating.eloRating + kFactor * ((1.0 - actualScore) - (1.0 - expectedMy))).toInt()
                
                val myStreak = if (won) myRating.winStreak + 1 else 0
                val oppStreak = if (!won) opponentRating.winStreak + 1 else 0
                
                val myTier = when {
                    newMyElo >= 1800 -> "Legend"
                    newMyElo >= 1600 -> "Diamond"
                    newMyElo >= 1400 -> "Platinum"
                    newMyElo >= 1200 -> "Gold"
                    newMyElo >= 1000 -> "Silver"
                    else -> "Bronze"
                }
                
                val oppTier = when {
                    newOppElo >= 1800 -> "Legend"
                    newOppElo >= 1600 -> "Diamond"
                    newOppElo >= 1400 -> "Platinum"
                    newOppElo >= 1200 -> "Gold"
                    newOppElo >= 1000 -> "Silver"
                    else -> "Bronze"
                }
                
                repository.saveDriverRating(myRating.copy(
                    eloRating = newMyElo,
                    wins = if (won) myRating.wins + 1 else myRating.wins,
                    losses = if (!won) myRating.losses + 1 else myRating.losses,
                    winStreak = myStreak,
                    rankTier = myTier
                ))

                repository.saveDriverRating(opponentRating.copy(
                    eloRating = newOppElo,
                    wins = if (!won) opponentRating.wins + 1 else opponentRating.wins,
                    losses = if (won) opponentRating.losses + 1 else opponentRating.losses,
                    winStreak = oppStreak,
                    rankTier = oppTier
                ))
            } catch (e: Exception) {
                Log.e("RaceSessionManager", "Error updating ratings: ${e.message}")
            }
        }
    }

    fun startLiveDrive() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500L,
                1f,
                this
            )
            Log.d("RaceSessionManager", "Started live drive GPS tracking")
        } catch (e: Exception) {
            Log.e("RaceSessionManager", "Error starting live drive GPS: ${e.message}")
        }
    }

    fun stopLiveDrive() {
        if (_state.value != RaceSessionState.ACTIVE) {
            try {
                locationManager.removeUpdates(this)
                Log.d("RaceSessionManager", "Stopped live drive GPS tracking")
            } catch (e: Exception) {
                Log.e("RaceSessionManager", "Error stopping live drive GPS: ${e.message}")
            }
        }
    }

    fun updateLiveLocation(lat: Double, lon: Double, speedMph: Double) {
        _liveLatitude.value = lat
        _liveLongitude.value = lon
        _speedMph.value = speedMph
    }

    override fun onLocationChanged(location: Location) {
        val speed = location.speed * 2.23694 // m/s to mph
        _speedMph.value = speed
        _liveLatitude.value = location.latitude
        _liveLongitude.value = location.longitude

        if (_state.value != RaceSessionState.ACTIVE) return

        val currentLat = location.latitude
        val currentLon = location.longitude
        val pathStartLat = _selectedTrack.value.startLat
        val pathStartLon = _selectedTrack.value.startLon
        val pathFinishLat = _selectedTrack.value.finishLat
        val pathFinishLon = _selectedTrack.value.finishLon

        val resultsTotal = FloatArray(1)
        Location.distanceBetween(pathStartLat, pathStartLon, pathFinishLat, pathFinishLon, resultsTotal)
        val totalDistMeters = resultsTotal[0]

        val resultsStart = FloatArray(1)
        Location.distanceBetween(pathStartLat, pathStartLon, currentLat, currentLon, resultsStart)
        val distFromStartMeters = resultsStart[0]

        val resultsFinish = FloatArray(1)
        Location.distanceBetween(currentLat, currentLon, pathFinishLat, pathFinishLon, resultsFinish)
        val distToFinishMeters = resultsFinish[0]

        // Capture telemetry
        telemetryRecorded.add("${System.currentTimeMillis()}|$speed|${_gForce.value}")

        if (totalDistMeters > 0) {
            val calculatedProg = (distFromStartMeters / (distFromStartMeters + distToFinishMeters)).coerceIn(0f, 1f)
            _userProgress.value = calculatedProg
            updateLeaderboard()

            // If user reaches within 20 meters of finish point
            if (distToFinishMeters <= 20.0 || calculatedProg >= 0.99f) {
                _userProgress.value = 1f
                updateLeaderboard()
                stopRaceSession("VALID")
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        val latG = (x / 9.80665).coerceIn(-4.0, 4.0)
        val longG = (y / 9.80665).coerceIn(-4.0, 4.0)
        _gForceX.value = latG
        _gForceY.value = longG

        val magnitude = sqrt(x * x + y * y + z * z)
        val g = magnitude / 9.80665
        _gForce.value = g
        
        if (_state.value == RaceSessionState.ACTIVE && telemetryRecorded.size < 100) {
            telemetryRecorded.add("${System.currentTimeMillis()}|${_speedMph.value}|$g|$latG|$longG")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
