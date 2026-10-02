package com.example.domain.detector

import kotlin.math.sqrt

data class JumpEvent(
    val timestampMs: Long,
    val peakMagnitude: Float,
    val intervalSinceLastMs: Long
)

class JumpDetector(
    var phonePosition: String = "pocket", // "pocket" or "hand"
    var sensitivity: String = "medium"    // "low", "medium", "high"
) {
    companion object {
        const val DETECTOR_VERSION = "1.0"
        const val DEFAULT_REFRACTORY_MS = 250L // Min interval between jumps (~4 jumps/sec)
    }

    private var lastJumpTimestampMs: Long = 0L
    private var gravityEstimate: Float = 9.81f
    private var smoothedMagnitude: Float = 0f

    // Dynamic peak detection state
    private var previousValue: Float = 0f
    private var currentSlopePositive: Boolean = false
    private var potentialPeakValue: Float = 0f
    private var potentialPeakTimestampMs: Long = 0L

    /**
     * Resets detector internal states (e.g. before new workout).
     */
    fun reset() {
        lastJumpTimestampMs = 0L
        gravityEstimate = 9.81f
        smoothedMagnitude = 0f
        previousValue = 0f
        currentSlopePositive = false
        potentialPeakValue = 0f
        potentialPeakTimestampMs = 0L
    }

    /**
     * Process an incoming accelerometer sample (x, y, z in m/s^2) at timestampMs.
     * Returns a JumpEvent if a valid jump peak is detected, or null.
     */
    fun processSample(x: Float, y: Float, z: Float, timestampMs: Long): JumpEvent? {
        val rawMagnitude = sqrt(x * x + y * y + z * z)

        // Gravity / Baseline estimation (alpha filter ~0.05)
        val alphaGravity = 0.05f
        gravityEstimate = (1f - alphaGravity) * gravityEstimate + alphaGravity * rawMagnitude

        // Gravity removed signal (magnitude deviation from baseline)
        val deviation = rawMagnitude - gravityEstimate

        // Low-pass smoothing filter (alpha filter ~0.35)
        val alphaSmooth = 0.35f
        smoothedMagnitude = (1f - alphaSmooth) * smoothedMagnitude + alphaSmooth * deviation

        // Determine dynamic threshold based on sensitivity and phone placement
        val baseThreshold = when (sensitivity.lowercase()) {
            "high" -> 4.5f
            "low" -> 9.0f
            else -> 6.5f // standard medium
        }

        val positionFactor = if (phonePosition.equals("hand", ignoreCase = true)) 1.25f else 1.0f
        val activeThreshold = baseThreshold * positionFactor

        var detectedEvent: JumpEvent? = null

        // Slope / Peak analysis
        val delta = smoothedMagnitude - previousValue
        if (delta > 0) {
            currentSlopePositive = true
            if (smoothedMagnitude > potentialPeakValue) {
                potentialPeakValue = smoothedMagnitude
                potentialPeakTimestampMs = timestampMs
            }
        } else if (delta < 0 && currentSlopePositive) {
            // Reached a local crest/peak
            currentSlopePositive = false
            val elapsedSinceLast = timestampMs - lastJumpTimestampMs

            if (potentialPeakValue >= activeThreshold && elapsedSinceLast >= DEFAULT_REFRACTORY_MS) {
                lastJumpTimestampMs = potentialPeakTimestampMs
                detectedEvent = JumpEvent(
                    timestampMs = potentialPeakTimestampMs,
                    peakMagnitude = potentialPeakValue,
                    intervalSinceLastMs = elapsedSinceLast
                )
            }
            // Reset peak value after falling below crest
            potentialPeakValue = 0f
        }

        previousValue = smoothedMagnitude
        return detectedEvent
    }
}
