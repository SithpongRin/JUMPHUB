package com.example.domain.workout

import com.example.domain.audio.AudioCueEngine
import com.example.domain.audio.AudioCueItem
import com.example.domain.audio.AudioPriority
import com.example.domain.detector.JumpDetector
import com.example.domain.detector.JumpEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class WorkoutPhase {
    IDLE,
    COUNTDOWN,
    WARMUP,
    JUMPING,
    RESTING,
    PAUSED,
    COOLDOWN,
    FINISHED
}

@androidx.compose.runtime.Immutable
data class WorkoutState(
    val phase: WorkoutPhase = WorkoutPhase.IDLE,
    val countdownSeconds: Int = 3,
    val totalJumps: Int = 0,
    val roundJumps: Int = 0,
    val detectedJumps: Int = 0,
    val correctedJumps: Int = 0,
    val activeSeconds: Int = 0,
    val elapsedSeconds: Int = 0,
    val currentRound: Int = 1,
    val totalRounds: Int = 8,
    val roundActiveSeconds: Int = 0,
    val roundTargetSeconds: Int = 30,
    val restRemainingSeconds: Int = 60,
    val restTotalSeconds: Int = 60,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val roundBestStreak: Int = 0,
    val currentCadenceJpm: Float = 0f,
    val targetJumps: Int = 500,
    val estimatedCalories: Float? = null,
    val lastJumpTimestampMs: Long = 0L,
    val lastCompletedRoundSummary: CompletedRoundData? = null
)

@androidx.compose.runtime.Immutable
data class CompletedRoundData(
    val round: Int,
    val jumps: Int,
    val jpm: Int,
    val bestStreak: Int,
    val restDurationSec: Int
)

class WorkoutStateMachine(
    private val audioCueEngine: AudioCueEngine,
    val jumpDetector: JumpDetector,
    private val scope: CoroutineScope
) {
    private val _workoutState = MutableStateFlow(WorkoutState())
    val workoutState: StateFlow<WorkoutState> = _workoutState.asStateFlow()

    // Configurable settings
    var voiceCueMode: String = "PHASE_ONLY"
    var voiceJumpInterval: Int = 25
    var voiceTimeIntervalMinutes: Int = 1
    var voiceTargetMilestonesEnabled: Boolean = true
    var voicePhaseCuesEnabled: Boolean = true
    var roundSummaryConfig: RoundSummaryConfig = RoundSummaryConfig()
    var streakGapToleranceSec: Float = 2.0f
    var weightKg: Float? = null
    var met: Float = 11.5f

    // Milestones tracking (deduplicated)
    private val triggeredMilestones = mutableSetOf<Int>()
    private val jumpTimestampsMs = mutableListOf<Long>()

    private var tickerJob: Job? = null
    private var countdownJob: Job? = null

    /**
     * User initiates workout. Transitions: IDLE -> COUNTDOWN -> 3 -> 2 -> 1 -> START -> JUMPING.
     * During COUNTDOWN: jump detector does NOT count, timestamps NOT recorded, active time does NOT count.
     */
    fun startWorkout(targetRounds: Int = 8, roundDurationSec: Int = 30, restDurationSec: Int = 60, targetJumps: Int = 500) {
        if (_workoutState.value.phase != WorkoutPhase.IDLE && _workoutState.value.phase != WorkoutPhase.FINISHED) {
            return
        }

        jumpDetector.reset()
        triggeredMilestones.clear()
        jumpTimestampsMs.clear()

        _workoutState.value = WorkoutState(
            phase = WorkoutPhase.COUNTDOWN,
            countdownSeconds = 3,
            totalJumps = 0,
            detectedJumps = 0,
            correctedJumps = 0,
            activeSeconds = 0,
            elapsedSeconds = 0,
            currentRound = 1,
            totalRounds = targetRounds,
            roundTargetSeconds = roundDurationSec,
            restTotalSeconds = restDurationSec,
            targetJumps = targetJumps
        )

        runCountdownSequence()
    }

    private fun runCountdownSequence() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            // Count 3
            _workoutState.value = _workoutState.value.copy(countdownSeconds = 3)
            audioCueEngine.postCue(
                AudioCueItem(AudioPriority.CRITICAL, textEn = "Three", textKm = "បី", isBeep = true, beepFrequency = 1)
            )
            delay(1000L)

            // Count 2
            _workoutState.value = _workoutState.value.copy(countdownSeconds = 2)
            audioCueEngine.postCue(
                AudioCueItem(AudioPriority.CRITICAL, textEn = "Two", textKm = "ពីរ", isBeep = true, beepFrequency = 1)
            )
            delay(1000L)

            // Count 1
            _workoutState.value = _workoutState.value.copy(countdownSeconds = 1)
            audioCueEngine.postCue(
                AudioCueItem(AudioPriority.CRITICAL, textEn = "One", textKm = "មួយ", isBeep = true, beepFrequency = 1)
            )
            delay(1000L)

            // Start!
            _workoutState.value = _workoutState.value.copy(countdownSeconds = 0, phase = WorkoutPhase.JUMPING)
            audioCueEngine.postCue(
                AudioCueItem(AudioPriority.CRITICAL, textEn = "Start", textKm = "ចាប់ផ្តើម", isBeep = true, beepFrequency = 2)
            )

            // Start active workout timer now that jumping has officially started
            startActiveTicker()
        }
    }

    private fun startActiveTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val current = _workoutState.value
                when (current.phase) {
                    WorkoutPhase.JUMPING -> {
                        val newActiveSec = current.activeSeconds + 1
                        val newRoundActiveSec = current.roundActiveSeconds + 1
                        val jpm = WorkoutMetricsCalculator.calculateJpm(current.totalJumps, newActiveSec)
                        val calories = WorkoutMetricsCalculator.calculateEstimatedCalories(newActiveSec, weightKg, met)

                        _workoutState.value = current.copy(
                            activeSeconds = newActiveSec,
                            elapsedSeconds = current.elapsedSeconds + 1,
                            roundActiveSeconds = newRoundActiveSec,
                            currentCadenceJpm = jpm,
                            estimatedCalories = calories
                        )

                        checkTimeCues(newActiveSec, current.totalJumps)

                        // 10 seconds remaining cue
                        if (voicePhaseCuesEnabled && current.roundTargetSeconds > 15 && current.roundTargetSeconds - newRoundActiveSec == 10) {
                            audioCueEngine.postCue(
                                AudioCueItem(
                                    priority = AudioPriority.HIGH,
                                    textEn = "Ten seconds remaining",
                                    textKm = "សល់ដប់វិនាទីទៀត"
                                )
                            )
                        }

                        // Check round transition if round duration is exceeded
                        if (current.roundTargetSeconds > 0 && newRoundActiveSec >= current.roundTargetSeconds) {
                            if (current.currentRound < current.totalRounds) {
                                enterRestPhase()
                            } else {
                                finishWorkout()
                            }
                        }
                    }
                    WorkoutPhase.RESTING -> {
                        val remainingRest = current.restRemainingSeconds - 1
                        _workoutState.value = current.copy(
                            elapsedSeconds = current.elapsedSeconds + 1,
                            restRemainingSeconds = remainingRest
                        )

                        // Rest warning beeps in final 3 seconds
                        if (remainingRest in 1..3) {
                            audioCueEngine.postCue(
                                AudioCueItem(AudioPriority.HIGH, textEn = "$remainingRest", textKm = "$remainingRest", isBeep = true)
                            )
                        } else if (remainingRest <= 0) {
                            enterNextRound()
                        }
                    }
                    WorkoutPhase.PAUSED -> {
                        _workoutState.value = current.copy(
                            elapsedSeconds = current.elapsedSeconds + 1
                        )
                    }
                    else -> { }
                }
            }
        }
    }

    /**
     * Process accelerometer sample off main thread.
     * Does NOT count jumps during COUNTDOWN, RESTING, PAUSED, IDLE, FINISHED.
     */
    fun onSensorSample(x: Float, y: Float, z: Float, timestampMs: Long) {
        val current = _workoutState.value
        if (current.phase != WorkoutPhase.JUMPING && current.phase != WorkoutPhase.WARMUP) {
            // Jump detector MUST NOT count jumps during countdown or rest!
            return
        }

        val event: JumpEvent? = jumpDetector.processSample(x, y, z, timestampMs)
        if (event != null) {
            recordJumpEvent(event)
        }
    }

    private fun recordJumpEvent(event: JumpEvent) {
        val current = _workoutState.value
        val newTotal = current.totalJumps + 1
        val newRoundJumps = current.roundJumps + 1
        val newDetected = current.detectedJumps + 1
        val newCorrected = current.correctedJumps + 1

        jumpTimestampsMs.add(event.timestampMs)

        // Streak calculation
        val gapSec = if (current.lastJumpTimestampMs > 0) (event.timestampMs - current.lastJumpTimestampMs) / 1000f else 0f
        val newStreak = if (gapSec <= streakGapToleranceSec || current.lastJumpTimestampMs == 0L) {
            current.currentStreak + 1
        } else {
            1
        }
        val bestStreak = maxOf(current.bestStreak, newStreak)
        val roundBestStreak = maxOf(current.roundBestStreak, newStreak)
        val jpm = WorkoutMetricsCalculator.calculateJpm(newTotal, current.activeSeconds)

        _workoutState.value = current.copy(
            totalJumps = newTotal,
            roundJumps = newRoundJumps,
            detectedJumps = newDetected,
            correctedJumps = newCorrected,
            currentStreak = newStreak,
            bestStreak = bestStreak,
            roundBestStreak = roundBestStreak,
            currentCadenceJpm = jpm,
            lastJumpTimestampMs = event.timestampMs
        )

        checkJumpVoiceCues(newTotal, current.targetJumps)
    }

    private fun checkJumpVoiceCues(totalJumps: Int, targetJumps: Int) {
        // Every jump mode
        if (voiceCueMode == "EVERY_JUMP" || voiceCueMode == "CUSTOM") {
            audioCueEngine.postCue(
                AudioCueItem(AudioPriority.LOW, textEn = "$totalJumps", textKm = "$totalJumps")
            )
        }

        // Every N jumps mode
        if (voiceCueMode == "EVERY_N_JUMPS" || (voiceCueMode == "CUSTOM" && voiceJumpInterval > 0)) {
            if (totalJumps % voiceJumpInterval == 0) {
                audioCueEngine.postCue(
                    AudioCueItem(AudioPriority.NORMAL, textEn = "$totalJumps", textKm = "$totalJumps")
                )
            }
        }

        // Target Milestones mode (25%, 50%, 75%, 100%)
        if (voiceTargetMilestonesEnabled && targetJumps > 0) {
            val percentages = listOf(25, 50, 75, 100)
            for (pct in percentages) {
                val milestoneJumps = (targetJumps * (pct / 100f)).toInt()
                if (totalJumps >= milestoneJumps && !triggeredMilestones.contains(pct)) {
                    triggeredMilestones.add(pct)
                    val enText = if (pct == 100) "Target reached! $totalJumps jumps" else "$pct percent, $totalJumps jumps"
                    val kmText = if (pct == 100) "សម្រេចគោលដៅហើយ! $totalJumps ដង" else "$pct ភាគរយ, $totalJumps ដង"
                    audioCueEngine.postCue(
                        AudioCueItem(AudioPriority.HIGH, textEn = enText, textKm = kmText)
                    )
                }
            }
        }
    }

    private fun checkTimeCues(activeSeconds: Int, totalJumps: Int) {
        val intervalSec = voiceTimeIntervalMinutes * 60
        if (intervalSec > 0 && activeSeconds % intervalSec == 0 && (voiceCueMode == "EVERY_N_MINUTES" || voiceCueMode == "CUSTOM")) {
            val mins = activeSeconds / 60
            val enText = "$mins minute, $totalJumps jumps"
            val kmText = "$mins នាទី, $totalJumps ដង"
            audioCueEngine.postCue(
                AudioCueItem(AudioPriority.NORMAL, textEn = enText, textKm = kmText)
            )
        }
    }

    private fun enterRestPhase() {
        val current = _workoutState.value
        val roundJumps = current.roundJumps
        val activeSec = current.roundActiveSeconds
        val bestStreak = current.roundBestStreak
        val restSec = current.restTotalSeconds
        val roundNumber = current.currentRound

        val roundJpm = if (activeSec > 0) ((roundJumps.toFloat() / activeSec) * 60f).toInt() else 0
        val summaryData = CompletedRoundData(
            round = roundNumber,
            jumps = roundJumps,
            jpm = roundJpm,
            bestStreak = bestStreak,
            restDurationSec = restSec
        )

        _workoutState.value = current.copy(
            phase = WorkoutPhase.RESTING,
            restRemainingSeconds = restSec,
            lastCompletedRoundSummary = summaryData
        )

        if (roundSummaryConfig.voiceRoundSummaryEnabled) {
            val (enSummary, kmSummary) = VoiceSummaryBuilder.buildRoundSummary(
                round = roundNumber,
                roundJumps = roundJumps,
                activeSecondsInRound = activeSec,
                bestStreakInRound = bestStreak,
                restDurationSec = restSec,
                config = roundSummaryConfig
            )
            audioCueEngine.postCue(
                AudioCueItem(
                    priority = AudioPriority.CRITICAL,
                    textEn = enSummary,
                    textKm = kmSummary
                )
            )
        } else {
            audioCueEngine.postCue(
                AudioCueItem(
                    priority = AudioPriority.CRITICAL,
                    textEn = "Rest $restSec seconds",
                    textKm = "សម្រាក $restSec វិនាទី"
                )
            )
        }
    }

    private fun enterNextRound() {
        val nextRound = _workoutState.value.currentRound + 1
        _workoutState.value = _workoutState.value.copy(
            phase = WorkoutPhase.JUMPING,
            currentRound = nextRound,
            roundActiveSeconds = 0,
            roundJumps = 0,
            roundBestStreak = 0,
            lastCompletedRoundSummary = null
        )
        audioCueEngine.postCue(
            AudioCueItem(AudioPriority.CRITICAL, textEn = "Round $nextRound of ${_workoutState.value.totalRounds}, start", textKm = "ជុំទី $nextRound ចាប់ផ្តើម")
        )
    }

    fun pauseWorkout() {
        if (_workoutState.value.phase == WorkoutPhase.JUMPING) {
            _workoutState.value = _workoutState.value.copy(phase = WorkoutPhase.PAUSED)
            audioCueEngine.postCue(
                AudioCueItem(AudioPriority.CRITICAL, textEn = "Workout paused", textKm = "បានផ្អាកការហាត់")
            )
        }
    }

    fun resumeWorkout() {
        if (_workoutState.value.phase == WorkoutPhase.PAUSED) {
            _workoutState.value = _workoutState.value.copy(phase = WorkoutPhase.JUMPING)
            audioCueEngine.postCue(
                AudioCueItem(AudioPriority.CRITICAL, textEn = "Resuming workout", textKm = "បន្តការហាត់")
            )
        }
    }

    fun finishWorkout() {
        val current = _workoutState.value
        tickerJob?.cancel()
        countdownJob?.cancel()
        _workoutState.value = current.copy(phase = WorkoutPhase.FINISHED)

        val (enSummary, kmSummary) = VoiceSummaryBuilder.buildFinalWorkoutSummary(
            totalJumps = current.totalJumps,
            activeSeconds = current.activeSeconds,
            bestStreak = current.bestStreak
        )
        audioCueEngine.postCue(
            AudioCueItem(
                AudioPriority.CRITICAL,
                textEn = enSummary,
                textKm = kmSummary
            )
        )
    }

    /**
     * Manual jump correction (+1 or -1 or custom delta).
     * Preserves original detected total.
     */
    fun adjustJumps(delta: Int) {
        val current = _workoutState.value
        val updatedTotal = (current.totalJumps + delta).coerceAtLeast(0)
        val updatedRoundJumps = (current.roundJumps + delta).coerceAtLeast(0)
        val updatedCorrected = (current.correctedJumps + delta).coerceAtLeast(0)
        val jpm = WorkoutMetricsCalculator.calculateJpm(updatedTotal, current.activeSeconds)

        _workoutState.value = current.copy(
            totalJumps = updatedTotal,
            roundJumps = updatedRoundJumps,
            correctedJumps = updatedCorrected,
            currentCadenceJpm = jpm
        )
    }

    fun getJumpTimestamps(): List<Long> = jumpTimestampsMs.toList()

    fun reset() {
        tickerJob?.cancel()
        countdownJob?.cancel()
        jumpDetector.reset()
        triggeredMilestones.clear()
        jumpTimestampsMs.clear()
        _workoutState.value = WorkoutState()
    }
}
