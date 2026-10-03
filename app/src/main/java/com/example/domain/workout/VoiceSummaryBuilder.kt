package com.example.domain.workout

data class RoundSummaryConfig(
    val voiceRoundSummaryEnabled: Boolean = true,
    val speakJumps: Boolean = true,
    val speakJpm: Boolean = true,
    val speakStreak: Boolean = true,
    val speakRestDuration: Boolean = true
)

object VoiceSummaryBuilder {

    /**
     * Builds round summary text for voice and display.
     * Guarded against division by zero (activeSecondsInRound <= 0).
     */
    fun buildRoundSummary(
        round: Int,
        roundJumps: Int,
        activeSecondsInRound: Int,
        bestStreakInRound: Int,
        restDurationSec: Int,
        config: RoundSummaryConfig = RoundSummaryConfig()
    ): Pair<String, String> {
        val jpm = if (activeSecondsInRound > 0) {
            ((roundJumps.toFloat() / activeSecondsInRound) * 60f).toInt()
        } else {
            0
        }

        // English parts
        val enParts = mutableListOf<String>()
        enParts.add("Round $round complete.")
        if (config.speakJumps) {
            val jumpWord = if (roundJumps == 1) "jump" else "jumps"
            enParts.add("$roundJumps $jumpWord.")
        }
        if (config.speakJpm) {
            enParts.add("$jpm jumps per minute.")
        }
        if (config.speakStreak && bestStreakInRound > 0) {
            enParts.add("Best streak $bestStreakInRound.")
        }
        if (config.speakRestDuration && restDurationSec > 0) {
            enParts.add("Rest $restDurationSec seconds.")
        }
        val enText = enParts.joinToString(" ")

        // Khmer parts
        val kmParts = mutableListOf<String>()
        kmParts.add("ជុំទី $round បានបញ្ចប់។")
        if (config.speakJumps) {
            kmParts.add("លោតបាន $roundJumps ដង។")
        }
        if (config.speakJpm) {
            kmParts.add("$jpm ដងក្នុងមួយនាទី។")
        }
        if (config.speakStreak && bestStreakInRound > 0) {
            kmParts.add("ជាប់គ្នាល្អបំផុត $bestStreakInRound ដង។")
        }
        if (config.speakRestDuration && restDurationSec > 0) {
            kmParts.add("សម្រាក $restDurationSec វិនាទី។")
        }
        val kmText = kmParts.joinToString(" ")

        return Pair(enText, kmText)
    }

    /**
     * Builds final workout summary text spoken when workout reaches FINISHED phase.
     */
    fun buildFinalWorkoutSummary(
        totalJumps: Int,
        activeSeconds: Int,
        bestStreak: Int
    ): Pair<String, String> {
        val avgJpm = if (activeSeconds > 0) {
            ((totalJumps.toFloat() / activeSeconds) * 60f).toInt()
        } else {
            0
        }
        val mins = activeSeconds / 60
        val secs = activeSeconds % 60
        val timeEn = if (mins > 0 && secs > 0) {
            "$mins minutes $secs seconds"
        } else if (mins > 0) {
            "$mins minutes"
        } else {
            "$secs seconds"
        }

        val timeKm = if (mins > 0 && secs > 0) {
            "$mins នាទី $secs វិនាទី"
        } else if (mins > 0) {
            "$mins នាទី"
        } else {
            "$secs វិនាទី"
        }

        val jumpWord = if (totalJumps == 1) "jump" else "jumps"
        val enText = "Workout complete! Total $totalJumps $jumpWord. Average $avgJpm jumps per minute. Total time $timeEn."
        val kmText = "ការហាត់បានបញ្ចប់! សរុប $totalJumps ដង។ មធ្យម $avgJpm ដងក្នុងមួយនាទី។ រយៈពេលសរុប $timeKm។"

        return Pair(enText, kmText)
    }
}
