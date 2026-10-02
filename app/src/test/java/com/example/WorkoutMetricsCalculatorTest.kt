package com.example

import com.example.domain.workout.WorkoutMetricsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutMetricsCalculatorTest {

    @Test
    fun testJpmCalculationZeroSecondsSafe() {
        val jpm = WorkoutMetricsCalculator.calculateJpm(totalJumps = 50, activeSeconds = 0)
        assertEquals(0f, jpm, 0.001f)
    }

    @Test
    fun testJpmCalculationStandard() {
        // 120 jumps in 60 active seconds = 120 RPM
        val jpm = WorkoutMetricsCalculator.calculateJpm(totalJumps = 120, activeSeconds = 60)
        assertEquals(120f, jpm, 0.001f)

        // 60 jumps in 30 active seconds = 120 RPM
        val jpmHalfMin = WorkoutMetricsCalculator.calculateJpm(totalJumps = 60, activeSeconds = 30)
        assertEquals(120f, jpmHalfMin, 0.001f)
    }

    @Test
    fun testCaloriesCalculationWithAndWithoutWeight() {
        // Without weight or weight <= 0: calories must be null (unavailable)
        assertNull(WorkoutMetricsCalculator.calculateEstimatedCalories(activeSeconds = 1200, weightKg = null))
        assertNull(WorkoutMetricsCalculator.calculateEstimatedCalories(activeSeconds = 1200, weightKg = 0f))

        // With 70kg weight for 1 hour (3600s) at MET 11.5: 11.5 * 70 * 1 = 805 kcal
        val cal = WorkoutMetricsCalculator.calculateEstimatedCalories(activeSeconds = 3600, weightKg = 70f, met = 11.5f)
        assertEquals(805f, cal ?: 0f, 0.5f)
    }

    @Test
    fun testPeakWindowRate() {
        // Jumps every 500ms
        val timestamps = (0..20).map { it * 500L }
        // In 5 seconds (5000ms), window fits 11 jumps
        val peakRate = WorkoutMetricsCalculator.calculatePeakWindowRate(timestamps, windowSec = 5)
        assertEquals(11, peakRate)
    }
}
