package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.audio.AudioCueEngine
import com.example.domain.detector.JumpDetector
import com.example.domain.workout.WorkoutPhase
import com.example.domain.workout.WorkoutStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutStateMachineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var audioEngine: AudioCueEngine
    private lateinit var jumpDetector: JumpDetector
    private lateinit var stateMachine: WorkoutStateMachine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        audioEngine = AudioCueEngine(context)
        jumpDetector = JumpDetector()
        stateMachine = WorkoutStateMachine(audioEngine, jumpDetector, testScope)
    }

    @Test
    fun testInitialStateIsIdle() {
        assertEquals(WorkoutPhase.IDLE, stateMachine.workoutState.value.phase)
        assertEquals(0, stateMachine.workoutState.value.totalJumps)
        assertEquals(0, stateMachine.workoutState.value.activeSeconds)
    }

    @Test
    fun testStartWorkoutTransitionsToCountdownFirst() {
        stateMachine.startWorkout(targetRounds = 3, roundDurationSec = 30)
        assertEquals(WorkoutPhase.COUNTDOWN, stateMachine.workoutState.value.phase)
        assertEquals(3, stateMachine.workoutState.value.countdownSeconds)
    }

    @Test
    fun testNoJumpsCountedDuringCountdown() = testScope.runTest {
        stateMachine.startWorkout(targetRounds = 3, roundDurationSec = 30)

        // Simulate sensor events during countdown (t < 3s)
        stateMachine.onSensorSample(0f, 0f, 25f, 500L)
        stateMachine.onSensorSample(0f, 0f, 25f, 1500L)

        // Jumps must strictly remain 0 during countdown!
        assertEquals(0, stateMachine.workoutState.value.totalJumps)
        assertEquals(0, stateMachine.workoutState.value.activeSeconds)

        // Advance 3.5 seconds past countdown (3, 2, 1, Start!)
        advanceTimeBy(3500L)

        // Now state should have transitioned to JUMPING
        assertEquals(WorkoutPhase.JUMPING, stateMachine.workoutState.value.phase)
    }

    @Test
    fun testManualCorrectionAdjustsJumpsPreservingDetected() {
        stateMachine.adjustJumps(5)
        assertEquals(5, stateMachine.workoutState.value.totalJumps)
        assertEquals(5, stateMachine.workoutState.value.correctedJumps)
        assertEquals(0, stateMachine.workoutState.value.detectedJumps)

        stateMachine.adjustJumps(-2)
        assertEquals(3, stateMachine.workoutState.value.totalJumps)
        assertEquals(3, stateMachine.workoutState.value.correctedJumps)
    }

    @Test
    fun testPauseAndResumeWorkout() = testScope.runTest {
        stateMachine.startWorkout(targetRounds = 3)
        advanceTimeBy(3500L)

        stateMachine.pauseWorkout()
        assertEquals(WorkoutPhase.PAUSED, stateMachine.workoutState.value.phase)

        stateMachine.resumeWorkout()
        assertEquals(WorkoutPhase.JUMPING, stateMachine.workoutState.value.phase)
    }

    @Test
    fun testRoundTransitionCreatesLastCompletedRoundSummary() = testScope.runTest {
        stateMachine.startWorkout(targetRounds = 3, roundDurationSec = 5, restDurationSec = 10)
        advanceTimeBy(3500L) // past countdown
        assertEquals(WorkoutPhase.JUMPING, stateMachine.workoutState.value.phase)

        // Simulate 10 jumps
        stateMachine.adjustJumps(10)

        // Advance 5 seconds past round duration
        advanceTimeBy(5500L)

        // Phase should now be RESTING and lastCompletedRoundSummary populated
        assertEquals(WorkoutPhase.RESTING, stateMachine.workoutState.value.phase)
        val summary = stateMachine.workoutState.value.lastCompletedRoundSummary
        org.junit.Assert.assertNotNull(summary)
        assertEquals(1, summary?.round)
        assertEquals(10, summary?.jumps)
        assertEquals(10, summary?.restDurationSec)
    }
}
