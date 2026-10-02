# JUMPHUB — Master Architecture & Implementation Summary

## 1. Project Overview
JUMPHUB is a native personal jump-rope workout Android application built around precision jump detection, automated hands-free operation, offline-first reliability, cloud synchronization, and modern mobile UX.
Key principles:
- **Accuracy**: 50 Hz accelerometer sampling, gravity removal, dynamic thresholding, and refractory peak detection.
- **Hands-Free Operation**: High-contrast display, prominent 3-2-1 countdown, and audio cue system.
- **Offline-First**: Core workout detection, voice cues, local Room persistence, and history operate fully without Internet.
- **Zero Emoji**: UI exclusively utilizes Material Design and vector iconography.
- **Dynamic Floating Navigation**: Ergonomic bottom navigation floating above content with scroll-driven responsiveness.

---

## 2. Start Workout Audio Flow & State Machine
The workout state machine strictly enforces preparation and safety:
- **Flow**: `IDLE` $\to$ `COUNTDOWN` $\to$ "3" $\to$ "2" $\to$ "1" $\to$ "Start" $\to$ `WARMUP` / `JUMPING`.
- **Integrity**: During countdown, the jump detector is disabled, jump timestamps are not saved, and active jumping time is paused.
- **State Persistence**: Governed by `WorkoutForegroundService`, which survives screen rotation, screen lock/off (via WakeLock), and backgrounding.
- **Phases Supported**: `IDLE`, `COUNTDOWN`, `WARMUP`, `JUMPING`, `RESTING`, `PAUSED`, `COOLDOWN`, `FINISHED`.

---

## 3. Voice Cue System
Configurable through DataStore with independent Voice Language and UI Language:
- **Voice Languages**: English (`en`) and Khmer (`km`).
- **Voice Cue Modes**:
  1. `PHASE_ONLY`: Essential workout events only (Countdown, Start, Rest, Resume, 10s remaining, Goal reached, Finish).
  2. `EVERY_JUMP`: Speaks count on each detected jump with queue backpressure drop to avoid speech backlog.
  3. `EVERY_N_JUMPS`: Speaks at intervals of 10, 25, 50, 100, or custom jumps.
  4. `TARGET_MILESTONES`: Speaks at 25%, 50%, 75%, and 100% of target jumps (deduplicated).
  5. `EVERY_N_MINUTES`: Speaks active time & jumps progress at time intervals.
  6. `CUSTOM`: User combines jump progress, time progress, milestones, and phase cues.
- **Audio Priority System**:
  - `CRITICAL`: Countdown, Start, Stop/Finish, Safety, Rest start/end. Interrupts any active low-priority cue.
  - `HIGH`: Target reached, Personal record, 10 seconds remaining.
  - `NORMAL`: Target milestones, Every N jumps, Time intervals.
  - `LOW`: Every Jump count, rhythm metronome beep. Dropped if queue has backlog to prevent audio delay.
- **Audio Focus**: Transient audio focus with ducking (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`) lowers background music.

---

## 4. Jump Detection & Sensor Pipeline
- **Sampling**: Accelerometer (`Sensor.TYPE_ACCELEROMETER`) at approximately 50 Hz (`SENSOR_DELAY_GAME`).
- **Processing**: Offloaded to background coroutines; never blocks UI.
- **Filter**: Magnitude $\sqrt{x^2 + y^2 + z^2}$, exponential baseline gravity tracking, low-pass smoothing.
- **Threshold**: Adaptive threshold scaled by sensitivity (`low`, `medium`, `high`) and phone placement (`pocket` vs `hand`).
- **Refractory Period**: Minimum jump interval $\approx 250\text{ms}$ (~4 jumps/sec) rejecting secondary rebound noise.
- **Manual Correction**: +1 / -1 controls adjust `totalJumps` and `correctedJumps` while preserving `detectedJumps`.

---

## 5. WHO-Aligned Training Plans & Adherence
- **Educational Context**: Programs are designed around WHO physical activity recommendations (150–300 min moderate or 75–150 min vigorous physical activity per week, plus strength activity on $\ge 2$ days). Clearly distinguished as JUMPHUB targets rather than official WHO jump quotas.
- **User-Selected Training Days**: User chooses 2–7 training days per week (e.g. Mon, Wed, Fri, Sat).
- **Reminders**: Only dispatched on planned training days; rest days are not alerted.
- **Training Streak vs Jump Streak**:
  - *Jump Streak*: Uninterrupted jumps in a workout (gap $\le 2\text{s}$).
  - *Training Streak*: Consecutive planned training days completed. **Scheduled rest days do NOT break the training streak.**
  - *Adherence*: Completed sessions / planned sessions $\times 100\%$.

---

## 6. History & Sync-Safe Deletion
- **Workout History**: Displayed in `ProgressScreen` and session details in `SessionDetailScreen`.
- **Metrics Detail**: Total jumps, detected vs corrected, active time, total elapsed time, average RPM, peak 30s rate, peak 60s rate, best streak, estimated calories, RPE, and round breakdown.
- **Deletion Flow**: Delete button with confirmation AlertDialog.
  - Cascades deletion of round records and session from Room.
  - Automatically recomputes stats and personal records.
  - **Sync-Safe Tombstones**: Inserts `tombstone_session_$uuid` into `SyncMetadataEntity` so cloud sync recognizes the deletion and avoids restoring it.

---

## 7. Profile & Weekly Weight Tracking
- **Profile**: Age, Height (cm), Weight (kg) stored in DataStore.
- **Optional Weight**: If weight is omitted, workout operates normally and calories are cleanly marked "unavailable".
- **Calorie Estimation**: $\text{MET} \times \text{weightKg} \times \text{activeHours}$, clearly labelled as an estimate.
- **Weekly Weight Check-in**:
  - Configurable reminder day (Sunday default) and time (08:00).
  - Canvas trend chart showing weekly progress.
  - Change since previous and initial entry.

---

## 8. Database & Schema
- **Room Local Database**: `jumphub_database` (version 1).
  - Non-destructive migration path: All new preference configurations (voice modes, training days, athlete profile) are persisted in DataStore, keeping Room schema backward compatible.
  - Entities: `SessionEntity`, `RoundRecordEntity`, `UserPlanEntity`, `PlanDefinitionEntity`, `ReminderScheduleEntity`, `BodyMetricEntity`, `PersonalRecordEntity`, `DailyStatEntity`, `WeeklyStatEntity`, `MonthlyStatEntity`, `SyncMetadataEntity`.
- **Cloud Firestore Database**:
  - Integrated via `google-services.json` (`project_id: jumphub-95d31`) and `libs.firebase.firestore`.
  - Collections schema: `users/{userId}/sessions/{sessionUuid}` preserving workout records with full metrics.
  - Cloud sync handled via `SyncEngine.kt` with tombstone protection (`syncInboundSession`) ensuring locally deleted workouts are never recreated by incoming cloud sync.

---

## 9. In-App Auto-Update Engine
- **Engine**: Implemented via `com.example.service.update.InAppUpdateManager`.
- **Check Workflow**: Communicates directly with GitHub Releases API (`https://api.github.com/repos/SithpongRin/JUMPHUB/releases/latest`) to detect semantic version upgrades against `BuildConfig.VERSION_NAME`.
- **Download & Install**:
  - Downloads APK directly in background via Android `DownloadManager` with notification visibility into public `DownloadManager` external directory (`Environment.DIRECTORY_DOWNLOADS`) with MIME type `application/vnd.android.package-archive`.
  - Configured `file_paths.xml` with `<external-path>` and `<external-files-path>` for `androidx.core.content.FileProvider`.
  - Resolved "There was a problem parsing the package" error by setting `targetSdk = 35` (stable Android 15 standard, minSdk 26) while keeping `compileSdk = 36` for modern Jetpack libraries, and directing APK downloads to public Downloads storage so Android PackageInstaller has direct file read permissions.
  - Triggers native Android PackageInstaller prompt on download completion via `FileProvider` and `android.permission.REQUEST_INSTALL_PACKAGES`.
  - User can update with a single tap inside **Settings $\to$ About & Updates (Check for Updates)** without uninstalling or losing local workout history.

---

## 10. Test Suite & Verification
Unit and Robolectric tests in `app/src/test/java/com/example/`:
- `WorkoutStateMachineTest`: Countdown sequence (3 $\to$ 2 $\to$ 1 $\to$ Start), verification that jump counting is inactive during countdown, pause/resume, and manual correction.
- `JumpDetectorTest`: Peak detection, refractory period rejection, sensitivity thresholds, baseline gravity settling.
- `WorkoutMetricsCalculatorTest`: Division-by-zero safety on active seconds, standard JPM, calorie formula with/without weight, peak rolling window.
- `TrainingStreakAndAdherenceTest`: Adherence percentage, detailed adherence stats (planned, completed, missed, skipped, completion rate, current/longest streak), scheduled rest days do not break training streak, weekly plan progress calculator.
- `NotificationReminderTest`: Reminder dispatch only on planned training days, reminder suppression on planned rest days, and reminder suppression when workout was already completed today.
- `ExampleRobolectricTest`: Room database initialization, session insert/query, and localized app name verification.

---

## 11. Verification & Build Status
- **Java Runtime**: OpenJDK 21 (Android Studio JBR 21)
- **Android Target**: compileSdk 36, targetSdk 35, minSdk 26
- **Unit Test Execution**: `:app:testDebugUnitTest` executed and passed 100% (49 tasks, 0 failures).
- **Assemble Build**: `:app:assembleDebug` built cleanly with zero compilation errors, generating debug APK (`build/outputs/apk/debug/app-debug.apk`).
- **Signing & Assets**: Debug keystore present at project root, Room schema v1 preserved without destructive migrations.
- **Body Sensors & Activity Recognition**: Declared `BODY_SENSORS`, `ACTIVITY_RECOGNITION`, and `HIGH_SAMPLING_RATE_SENSORS` in Manifest, and requested runtime permissions on launch, completely resolving the OS "JUMPHUB should be granted Body sensors access to function properly" prompt and "No permissions denied" setting issue.
- **Scroll Jank & Smoothness**: Removed scroll-triggered hide/show bottom nav recompositions from all screens, keeping the floating bar stably docked at the bottom with 120dp padding. Heavy view model calculations offloaded to `Dispatchers.Default`.
- **Google Account Authentication**: Native Android `AccountManager.newChooseAccountIntent` integration in Onboarding and Account screens connecting real device Google accounts with Firestore cloud sync.
- **Plan Schedule Synchronization**: Automatic synchronization of training days when choosing a program (e.g. Endurance Builder auto-syncs 4 days), status indicators in schedule picker, and 1-tap "Sync with Plan" button.
- **GitHub Release**: Tag `v1.0.2` with `JUMPHUB-v1.0.2.apk`.

---

## 12. Build & Release Instructions
- Run unit test suite: `.\gradlew.bat :app:testDebugUnitTest`
- Assemble debug APK: `.\gradlew.bat :app:assembleDebug`
- Bump version for update: modify `versionCode` & `versionName` in `app/build.gradle.kts`, rebuild APK, and draft new tag release on GitHub.
