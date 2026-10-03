package com.example.domain.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSummaryBuilderTest {

    @Test
    fun `buildRoundSummary produces complete English summary`() {
        val (en, km) = VoiceSummaryBuilder.buildRoundSummary(
            round = 2,
            roundJumps = 120,
            activeSecondsInRound = 75, // 120 / 75 * 60 = 96 JPM
            bestStreakInRound = 45,
            restDurationSec = 30,
            config = RoundSummaryConfig(
                voiceRoundSummaryEnabled = true,
                speakJumps = true,
                speakJpm = true,
                speakStreak = true,
                speakRestDuration = true
            )
        )

        assertEquals("Round 2 complete. 120 jumps. 96 jumps per minute. Best streak 45. Rest 30 seconds.", en)
        assertEquals("ជុំទី 2 បានបញ្ចប់។ លោតបាន 120 ដង។ 96 ដងក្នុងមួយនាទី។ ជាប់គ្នាល្អបំផុត 45 ដង។ សម្រាក 30 វិនាទី។", km)
    }

    @Test
    fun `buildRoundSummary guards against divide by zero when activeSeconds is zero`() {
        val (en, km) = VoiceSummaryBuilder.buildRoundSummary(
            round = 1,
            roundJumps = 0,
            activeSecondsInRound = 0,
            bestStreakInRound = 0,
            restDurationSec = 60
        )

        // JPM should safely be 0 and no division by zero exception thrown
        assertEquals("Round 1 complete. 0 jumps. 0 jumps per minute. Rest 60 seconds.", en)
        assertEquals("ជុំទី 1 បានបញ្ចប់។ លោតបាន 0 ដង។ 0 ដងក្នុងមួយនាទី។ សម្រាក 60 វិនាទី។", km)
    }

    @Test
    fun `buildRoundSummary respects toggles for jumps, jpm, streak`() {
        val config = RoundSummaryConfig(
            voiceRoundSummaryEnabled = true,
            speakJumps = true,
            speakJpm = false,
            speakStreak = false,
            speakRestDuration = true
        )

        val (en, km) = VoiceSummaryBuilder.buildRoundSummary(
            round = 3,
            roundJumps = 100,
            activeSecondsInRound = 60,
            bestStreakInRound = 50,
            restDurationSec = 45,
            config = config
        )

        assertEquals("Round 3 complete. 100 jumps. Rest 45 seconds.", en)
        assertEquals("ជុំទី 3 បានបញ្ចប់។ លោតបាន 100 ដង។ សម្រាក 45 វិនាទី។", km)
    }

    @Test
    fun `buildRoundSummary handles singular jump word in English`() {
        val (en, _) = VoiceSummaryBuilder.buildRoundSummary(
            round = 1,
            roundJumps = 1,
            activeSecondsInRound = 2,
            bestStreakInRound = 1,
            restDurationSec = 30
        )

        assertTrue(en.contains("1 jump."))
    }

    @Test
    fun `buildFinalWorkoutSummary builds accurate EN and KM summary`() {
        val (en, km) = VoiceSummaryBuilder.buildFinalWorkoutSummary(
            totalJumps = 360,
            activeSeconds = 180, // 360 / 180 * 60 = 120 avg JPM, 3 mins
            bestStreak = 90
        )

        assertEquals("Workout complete! Total 360 jumps. Average 120 jumps per minute. Total time 3 minutes.", en)
        assertEquals("ការហាត់បានបញ្ចប់! សរុប 360 ដង។ មធ្យម 120 ដងក្នុងមួយនាទី។ រយៈពេលសរុប 3 នាទី។", km)
    }

    @Test
    fun `buildFinalWorkoutSummary guards against zero active time`() {
        val (en, km) = VoiceSummaryBuilder.buildFinalWorkoutSummary(
            totalJumps = 0,
            activeSeconds = 0,
            bestStreak = 0
        )

        assertEquals("Workout complete! Total 0 jumps. Average 0 jumps per minute. Total time 0 seconds.", en)
        assertEquals("ការហាត់បានបញ្ចប់! សរុប 0 ដង។ មធ្យម 0 ដងក្នុងមួយនាទី។ រយៈពេលសរុប 0 វិនាទី។", km)
    }
}
