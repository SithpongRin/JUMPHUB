package com.example.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.example.JumphubApplication
import com.example.domain.audio.AudioCueEngine
import com.example.domain.detector.JumpDetector
import com.example.domain.workout.WorkoutPhase
import com.example.domain.workout.WorkoutState
import com.example.domain.workout.WorkoutStateMachine
import com.example.service.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorkoutForegroundService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var audioCueEngine: AudioCueEngine
    private lateinit var jumpDetector: JumpDetector
    lateinit var stateMachine: WorkoutStateMachine
        private set

    companion object {
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START_WORKOUT"
        const val ACTION_PAUSE = "ACTION_PAUSE_WORKOUT"
        const val ACTION_RESUME = "ACTION_RESUME_WORKOUT"
        const val ACTION_FINISH = "ACTION_FINISH_WORKOUT"
        const val ACTION_ADJUST_JUMPS = "ACTION_ADJUST_JUMPS"

        const val EXTRA_ROUNDS = "EXTRA_ROUNDS"
        const val EXTRA_ROUND_DURATION = "EXTRA_ROUND_DURATION"
        const val EXTRA_REST_DURATION = "EXTRA_REST_DURATION"
        const val EXTRA_TARGET_JUMPS = "EXTRA_TARGET_JUMPS"
        const val EXTRA_DELTA_JUMPS = "EXTRA_DELTA_JUMPS"

        private val _serviceState = MutableStateFlow(WorkoutState())
        val serviceState: StateFlow<WorkoutState> = _serviceState.asStateFlow()

        var activeInstance: WorkoutForegroundService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this

        val app = application as? JumphubApplication
        val container = app?.container

        audioCueEngine = AudioCueEngine(this)
        jumpDetector = JumpDetector()
        stateMachine = WorkoutStateMachine(audioCueEngine, jumpDetector, serviceScope)

        if (container != null) {
            serviceScope.launch {
                container.preferences.userPreferencesFlow.collectLatest { prefs ->
                    audioCueEngine.voiceLanguage = prefs.voiceLanguage
                    audioCueEngine.voiceEnabled = prefs.voiceEnabled
                    audioCueEngine.voiceVolume = prefs.voiceVolume
                    audioCueEngine.vibrationEnabled = prefs.vibration

                    jumpDetector.phonePosition = prefs.phonePosition
                    jumpDetector.sensitivity = prefs.sensitivity

                    stateMachine.voiceCueMode = prefs.voiceCueMode
                    stateMachine.voiceJumpInterval = prefs.voiceJumpInterval
                    stateMachine.voiceTimeIntervalMinutes = prefs.voiceTimeIntervalMinutes
                    stateMachine.voiceTargetMilestonesEnabled = prefs.voiceTargetMilestonesEnabled
                    stateMachine.voicePhaseCuesEnabled = prefs.voicePhaseCuesEnabled
                    stateMachine.streakGapToleranceSec = prefs.streakGapToleranceSec
                    stateMachine.weightKg = prefs.userWeightKg
                    stateMachine.met = prefs.metValue
                }
            }
        }

        serviceScope.launch {
            stateMachine.workoutState.collectLatest { state ->
                _serviceState.value = state
                updateNotification(state)
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Jumphub:WorkoutWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val rounds = intent.getIntExtra(EXTRA_ROUNDS, 8)
                val roundSec = intent.getIntExtra(EXTRA_ROUND_DURATION, 30)
                val restSec = intent.getIntExtra(EXTRA_REST_DURATION, 60)
                val targetJumps = intent.getIntExtra(EXTRA_TARGET_JUMPS, 500)

                startAsForeground()
                registerAccelerometer()
                wakeLock?.acquire(2 * 60 * 60 * 1000L)

                stateMachine.startWorkout(rounds, roundSec, restSec, targetJumps)
            }
            ACTION_PAUSE -> {
                stateMachine.pauseWorkout()
            }
            ACTION_RESUME -> {
                stateMachine.resumeWorkout()
            }
            ACTION_FINISH -> {
                stateMachine.finishWorkout()
                unregisterAccelerometer()
                if (wakeLock?.isHeld == true) wakeLock?.release()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_ADJUST_JUMPS -> {
                val delta = intent.getIntExtra(EXTRA_DELTA_JUMPS, 0)
                stateMachine.adjustJumps(delta)
            }
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val initialNotification = NotificationHelper.buildWorkoutNotification(
            this,
            jumps = 0,
            activeTimeFormatted = "00:00",
            phase = "Starting"
        )
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, initialNotification, foregroundType)
    }

    private fun updateNotification(state: WorkoutState) {
        val mins = state.activeSeconds / 60
        val secs = state.activeSeconds % 60
        val timeFormatted = String.format("%02d:%02d", mins, secs)
        val phaseName = state.phase.name

        val notification = NotificationHelper.buildWorkoutNotification(
            this,
            jumps = state.totalJumps,
            activeTimeFormatted = timeFormatted,
            phase = phaseName
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun registerAccelerometer() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun unregisterAccelerometer() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val timestampMs = System.currentTimeMillis()
            stateMachine.onSensorSample(event.values[0], event.values[1], event.values[2], timestampMs)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        activeInstance = null
        unregisterAccelerometer()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        audioCueEngine.release()
        serviceScope.cancel()
    }
}
