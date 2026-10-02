package com.example

import com.example.domain.detector.JumpDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class JumpDetectorTest {

    private lateinit var detector: JumpDetector

    @Before
    fun setUp() {
        detector = JumpDetector(phonePosition = "pocket", sensitivity = "medium")
    }

    @Test
    fun testRestingStateNoJumpDetected() {
        // Flat gravity signal at 9.8 m/s^2
        for (i in 0..50) {
            val event = detector.processSample(0f, 9.8f, 0f, i * 20L)
            assertNull(event)
        }
    }

    @Test
    fun testSingleJumpPeakDetection() {
        // Feed baseline samples to settle gravity filter
        for (i in 0..30) {
            detector.processSample(0f, 9.8f, 0f, i * 20L)
        }

        // Simulate upward impulse -> peak -> downward crest
        detector.processSample(0f, 15f, 0f, 700L)
        detector.processSample(0f, 25f, 0f, 720L)
        detector.processSample(0f, 28f, 0f, 740L)
        val crestEvent = detector.processSample(0f, 18f, 0f, 760L) // falling edge after peak

        assertNotNull("Jump peak should be detected on crest falling edge", crestEvent)
    }

    @Test
    fun testRefractoryPeriodRejectsRapidNoise() {
        // Feed baseline
        for (i in 0..30) {
            detector.processSample(0f, 9.8f, 0f, i * 20L)
        }

        // Jump 1
        detector.processSample(0f, 25f, 0f, 700L)
        detector.processSample(0f, 28f, 0f, 720L)
        val event1 = detector.processSample(0f, 15f, 0f, 740L)
        assertNotNull(event1)

        // Rapid secondary glitch only 50ms later (less than 250ms refractory period)
        detector.processSample(0f, 24f, 0f, 770L)
        detector.processSample(0f, 28f, 0f, 780L)
        val eventGlitch = detector.processSample(0f, 15f, 0f, 790L)

        assertNull("Rapid peak within refractory period must be rejected", eventGlitch)

        // Valid jump 300ms after jump 1
        detector.processSample(0f, 25f, 0f, 1050L)
        detector.processSample(0f, 28f, 0f, 1070L)
        val event2 = detector.processSample(0f, 15f, 0f, 1090L)
        assertNotNull("Subsequent jump after refractory window must be accepted", event2)
    }

    @Test
    fun testSensitivityLowRequiresHigherImpulse() {
        detector.sensitivity = "low"
        // Baseline
        for (i in 0..30) {
            detector.processSample(0f, 9.8f, 0f, i * 20L)
        }

        // Small amplitude that would trigger on high but should fail on low
        detector.processSample(0f, 13f, 0f, 700L)
        detector.processSample(0f, 14f, 0f, 720L)
        val event = detector.processSample(0f, 10f, 0f, 740L)

        assertNull("Low sensitivity should ignore mild impulses", event)
    }
}
