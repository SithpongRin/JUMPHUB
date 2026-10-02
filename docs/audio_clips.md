# JUMPHUB — Audio Clip Registry & Sound Asset Specification

This document defines the naming conventions, supported languages, clip identifiers, and speech text for audio cues in JUMPHUB.

The audio system supports independent voice language selection (**Khmer** and **English**) and multiple voice cue modes.

---

## 1. Directory Structure

Audio files and resources are structured with language-specific prefixes:

- Khmer clips: `km_` prefix
- English clips: `en_` prefix
- Sound effects / beeps: `sfx_` prefix

---

## 2. Priority Hierarchy System

To prevent audio overlap and maintain workout flow:

| Level | Category | Events | Interruption Rule |
| :--- | :--- | :--- | :--- |
| `CRITICAL` | Workout State Controls | Countdown ("3", "2", "1", "Start"), Finish/Stop, Safety, Rest Start/End | Interrupts any active cue immediately; clears lower-priority backlog. |
| `HIGH` | Key Milestones | Target reached (100%), Personal records, 10s remaining warning | Delays or interrupts normal cues; does not drop. |
| `NORMAL` | Progress Cues | Target milestones (25%, 50%, 75%), Every N jumps (e.g. 25, 50, 100), Every N minutes | Serialized execution in priority queue. |
| `LOW` | High-frequency telemetry | Every Jump counting, rhythm metronome beep | Skipped if audio queue is backed up; never causes lag or stutter. |

---

## 3. Voice Cue Modes

Users can configure when the app speaks:

1. **Phase Only (Default)**: Essential events only (Countdown, Start, Rest, Resume, 10s remaining, Goal reached, Finish).
2. **Every Jump**: Announces count on each jump (low priority with queue-dropping to prevent overlapping speech).
3. **Every N Jumps**: Configurable interval (10, 25, 50, 100, custom).
4. **Target Milestones**: 25%, 50%, 75%, 100% of target jumps (dynamically calculated and deduplicated).
5. **Every N Minutes**: Active workout time progress (e.g. "1 minute, 120 jumps").
6. **Custom**: User combines selected triggers freely.

---

## 4. Audio Registry Table

| Identifier | File / Utterance | Language | Spoken Text / Tone | Purpose | Where Used |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `sfx_beep_low` | SoundPool | Non-verbal | 440 Hz short low tone | Warning / Countdown | 3, 2, 1 and rest end |
| `sfx_beep_high` | SoundPool | Non-verbal | 880 Hz bright tone | Round start tone | Start cue |
| `en_count_3` | TTS / Clip | English | "Three" | Countdown 3 | Pre-workout start |
| `en_count_2` | TTS / Clip | English | "Two" | Countdown 2 | Pre-workout start |
| `en_count_1` | TTS / Clip | English | "One" | Countdown 1 | Pre-workout start |
| `en_start` | TTS / Clip | English | "Start" | Start prompt | Jump phase officially begins |
| `en_rest` | TTS / Clip | English | "Rest {seconds} seconds" | Rest prompt | Rest interval start |
| `en_paused` | TTS / Clip | English | "Workout paused" | Pause cue | Pause triggered |
| `en_resumed` | TTS / Clip | English | "Resuming workout" | Resume cue | Resume triggered |
| `en_milestone_25` | TTS / Clip | English | "25 percent, {jumps} jumps" | Milestone 25% | 25% target reached |
| `en_milestone_50` | TTS / Clip | English | "50 percent, {jumps} jumps" | Milestone 50% | 50% target reached |
| `en_milestone_75` | TTS / Clip | English | "75 percent, {jumps} jumps" | Milestone 75% | 75% target reached |
| `en_milestone_100`| TTS / Clip | English | "Target reached! {jumps} jumps"| Milestone 100% | Target completed |
| `en_ten_seconds`| TTS / Clip | English | "Ten seconds remaining" | Final stretch warning | 10s before rest |
| `en_workout_complete` | TTS / Clip | English | "Workout complete! Total {jumps} jumps" | Session finish | Workout conclusion |
| `km_count_3` | TTS / Clip | Khmer | "បី" (Bei) | Countdown 3 | Pre-workout start |
| `km_count_2` | TTS / Clip | Khmer | "ពីរ" (Pii) | Countdown 2 | Pre-workout start |
| `km_count_1` | TTS / Clip | Khmer | "មួយ" (Muoy) | Countdown 1 | Pre-workout start |
| `km_start` | TTS / Clip | Khmer | "ចាប់ផ្តើម" (Chhab pdaoem) | Start prompt | Jump phase officially begins |
| `km_rest` | TTS / Clip | Khmer | "សម្រាក {seconds} វិនាទី" | Rest prompt | Rest interval start |
| `km_paused` | TTS / Clip | Khmer | "បានផ្អាកការហាត់" | Pause cue | Pause triggered |
| `km_resumed` | TTS / Clip | Khmer | "បន្តការហាត់" | Resume cue | Resume triggered |
| `km_milestone_25` | TTS / Clip | Khmer | "២៥ ភាគរយ, {jumps} ដង" | Milestone 25% | 25% target reached |
| `km_milestone_50` | TTS / Clip | Khmer | "៥០ ភាគរយ, {jumps} ដង" | Milestone 50% | 50% target reached |
| `km_milestone_75` | TTS / Clip | Khmer | "៧៥ ភាគរយ, {jumps} ដង" | Milestone 75% | 75% target reached |
| `km_milestone_100`| TTS / Clip | Khmer | "សម្រេចគោលដៅហើយ! {jumps} ដង"| Milestone 100% | Target completed |
| `km_ten_seconds`| TTS / Clip | Khmer | "សល់ដប់វិនាទីទៀត" | Final stretch warning | 10s before rest |
| `km_workout_complete` | TTS / Clip | Khmer | "ការហាត់បានបញ្ចប់! សរុប {jumps} ដង" | Session finish | Workout conclusion |

---

## 5. Playback Architecture

Low-latency playback is driven by `AudioCueEngine`:
- SoundPool for instant sonification tones.
- Android TextToSpeech engine with fallback for Khmer (`Locale("km", "KH")`) and English (`Locale.US`).
- Transient audio focus with ducking (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`) to lower background music during voice cues.
- Queue prioritization prevents overlap and guarantees critical safety and state cues are never lost.
