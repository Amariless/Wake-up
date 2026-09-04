# Graph Report - Wake-up  (2026-09-02)

## Corpus Check
- 169 files · ~77,729 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1105 nodes · 2349 edges · 76 communities (60 shown, 11 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 95 edges (avg confidence: 0.85)
- Token cost: 60,000 input · 9,228 output

## Community Hubs (Navigation)
- Weekly Class Schedule
- Class Reminder Alarms
- Folder Data Access (DAO)
- App Update Flow
- Alarm Sound Catalog
- Alarm Timer Service
- Alarm Request Codes
- Alarm Broadcast Receiver
- Screen Time Widget
- Alarm Ringing Activity
- Reels Block Accessibility Service
- Alarm Ringing Foreground Service
- Usage Alert Rules
- App Block Rules
- CI Release Workflow
- Block Overlay Service
- Alarms List Screen
- Timer Screen
- Alarm Controller
- Settings DataStore
- Usage Data Access (DAO)
- Alarm Repository
- Alarm Scheduler
- Task Entity & Repository
- Blockable App Catalog
- Room Type Converters
- Alarm Data Access (DAO)
- Permission Intents
- Screen Time Worker
- App Database Setup
- Task Data Access (DAO)
- Widget Deep Links
- Navigation Host
- Folders Screen
- Main Activity
- Settings ViewModel
- Task List Items UI
- Alarm Sound Preview Player
- Block Surface Types
- Permission Status Checks
- Timer ViewModel
- Alarm Timing Tests
- Stopwatch ViewModel
- Folder Detail ViewModel
- App Reliability Worker
- Dismiss Challenge: Math
- Dismiss Challenge Types
- Dev Tools ViewModel
- Folder Detail Screen
- Alarm Timing Calculation
- Alarm Editor ViewModel
- Navigation Routes
- Screen Time Screen
- Task Editor ViewModel
- Reels Node Detector
- Task Reminder Receiver
- Dismiss Challenge: Shake
- Xiaomi Onboarding ViewModel
- Alarm Ringing ViewModel
- Dismiss Challenge: Draw Pattern
- Dismiss Challenge: Trace Path
- Dev Tools Screens
- Permission Revocation Tracker
- App Ready State
- Alarm Volume Status
- Alarm Sound Picker Dialog
- Subject Editor Screen
- Subject Icons
- Task Creation Session State
- Gradle Wrapper Script
- System Usage Stats Source

## God Nodes (most connected - your core abstractions)
1. `TaskEntity` - 61 edges
2. `AlarmEntity` - 57 edges
3. `Row` - 38 edges
4. `SettingsDataStore` - 36 edges
5. `NotificationHelper` - 36 edges
6. `ClassSessionEntity` - 32 edges
7. `TimerForegroundService` - 31 edges
8. `BlockSurface` - 30 edges
9. `SubjectEntity` - 29 edges
10. `FolderEntity` - 28 edges

## Surprising Connections (you probably didn't know these)
- `Workflow CI: Build y publicar APK` --references--> `GitHub Releases (Amariless/Wake-up)`  [EXTRACTED]
  .github/workflows/release.yml → README.md
- `AlarmRingingScreen()` --calls--> `DismissChallengeContent()`  [INFERRED]
  app/src/main/java/com/fritangui/wakeup/alarm/ui/AlarmRingingScreen.kt → app/src/main/java/com/fritangui/wakeup/alarm/ui/DismissChallengeContent.kt
- `TimerRingingScreen()` --calls--> `DismissChallengeContent()`  [INFERRED]
  app/src/main/java/com/fritangui/wakeup/alarm/ui/TimerRingingScreen.kt → app/src/main/java/com/fritangui/wakeup/alarm/ui/DismissChallengeContent.kt
- `BlockAppRow()` --calls--> `Row`  [INFERRED]
  app/src/main/java/com/fritangui/wakeup/ui/blocking/BlockingScreen.kt → app/src/main/java/com/fritangui/wakeup/ui/blocking/BlockingViewModel.kt
- `AlarmEditorScreen()` --calls--> `Row`  [INFERRED]
  app/src/main/java/com/fritangui/wakeup/ui/clock/alarms/AlarmEditorScreen.kt → app/src/main/java/com/fritangui/wakeup/ui/blocking/BlockingViewModel.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Componentes centrales de la arquitectura de Wake up** — readme_ringingforegroundservice, readme_reelsnodedetector, readme_accessibility_service, readme_room_database, readme_datastore_settings, readme_hilt_di, readme_usagestatsmanager [INFERRED 0.75]
- **Flujo de auto-actualización sin pérdida de datos** — _github_workflows_release_workflow, keystore_wakeup_sideload_jks_signing_key, readme_github_releases, readme_wakeup_app [INFERRED 0.85]

## Communities (76 total, 11 thin omitted)

### Community 0 - "Weekly Class Schedule"
Cohesion: 0.06
Nodes (52): UpcomingClassOccurrence, computeWeeklyClassSchedule(), nextClassDayOfWeek(), WeeklyClassDay, WeeklyClassEntry, Row, amPmSuffix(), ClockTimeText() (+44 more)

### Community 1 - "Class Reminder Alarms"
Cohesion: 0.07
Nodes (17): ClassReminderReceiver, BroadcastReceiver, Context, Intent, Flow, SubjectDao, SubjectWithSessions, ClassSessionEntity (+9 more)

### Community 2 - "Folder Data Access (DAO)"
Cohesion: 0.08
Nodes (11): FolderDao, Flow, FolderEntity, FolderRepository, Flow, FoldersViewModel, StateFlow, ViewModel (+3 more)

### Community 3 - "App Update Flow"
Cohesion: 0.08
Nodes (21): UpdateScreen(), StateFlow, ViewModel, UpdateViewModel, ApkDownloadInstaller, Flow, Intent, UpdateChecker (+13 more)

### Community 4 - "Alarm Sound Catalog"
Cohesion: 0.10
Nodes (20): action, AlarmSounds, BundledAlarmSound, Context, BundledNotificationSound, Context, NotificationSounds, AppTimePickerDialog() (+12 more)

### Community 5 - "Alarm Timer Service"
Cohesion: 0.13
Nodes (13): Context, Intent, Job, LifecycleService, MediaPlayer, PendingIntent, PowerManager, Runnable (+5 more)

### Community 6 - "Alarm Request Codes"
Cohesion: 0.10
Nodes (13): AlarmConstants, BootReceiver, BroadcastReceiver, Context, Intent, WakeUpApp, AlarmManager, PendingIntent (+5 more)

### Community 7 - "Alarm Broadcast Receiver"
Cohesion: 0.10
Nodes (10): AlarmReceiver, BroadcastReceiver, Context, Intent, BroadcastReceiver, Context, Intent, PreAlarmReceiver (+2 more)

### Community 8 - "Screen Time Widget"
Cohesion: 0.14
Nodes (14): ActionCallback, ActionParameters, Color, Context, GlanceAppWidget, GlanceAppWidgetReceiver, GlanceId, Intent (+6 more)

### Community 9 - "Alarm Ringing Activity"
Cohesion: 0.10
Nodes (10): AlarmRingingActivity, Bundle, ComponentActivity, Intent, Bundle, ComponentActivity, TimerRingingActivity, AlarmRingingScreen() (+2 more)

### Community 10 - "Reels Block Accessibility Service"
Cohesion: 0.14
Nodes (10): AccessibilityEvent, AccessibilityService, Command, DetectionDebugInfo, GoHome, Job, SharedFlow, StateFlow (+2 more)

### Community 11 - "Alarm Ringing Foreground Service"
Cohesion: 0.18
Nodes (9): Context, Intent, LifecycleService, MediaPlayer, PowerManager, Runnable, Vibrator, RingingForegroundService (+1 more)

### Community 12 - "Usage Alert Rules"
Cohesion: 0.15
Nodes (9): UsageAlertRuleEntity, todayEpochDay(), AppUsageRow, DayUsage, StateFlow, ViewModel, ScreenTimeViewModel, ScreenTimeRefresher (+1 more)

### Community 13 - "App Block Rules"
Cohesion: 0.14
Nodes (3): BlockRuleEntity, Flow, UsageRepository

### Community 14 - "CI Release Workflow"
Cohesion: 0.14
Nodes (20): actions/checkout@v4, softprops/action-gh-release@v2, actions/cache@v4 (cache de Gradle), actions/setup-java@v4 (JDK 17), Cálculo de versionCode vía conteo de commits, Workflow CI: Build y publicar APK, Llave de firma fija (keystore/wakeup-sideload.jks), AccessibilityService (detección de Reels/TikTok) (+12 more)

### Community 15 - "Block Overlay Service"
Cohesion: 0.22
Nodes (7): BlockOverlayService, Context, Intent, Job, LifecycleService, SharedFlow, View

### Community 16 - "Alarms List Screen"
Cohesion: 0.16
Nodes (13): AlarmRow(), AlarmsListScreen(), androidx, Modifier, repeatSummary(), SectionHeader(), AlarmsViewModel, StateFlow (+5 more)

### Community 17 - "Timer Screen"
Cohesion: 0.17
Nodes (16): formatMillis(), TimerIdleContent(), TimerPhase, IDLE, RINGING, RUNNING, TimerRunningContent(), TimerScreen() (+8 more)

### Community 18 - "Alarm Controller"
Cohesion: 0.15
Nodes (3): AlarmController, computeReminderTriggers(), ReminderPlanningTest

### Community 19 - "Settings DataStore"
Cohesion: 0.12
Nodes (3): Keys, Flow, SettingsDataStore

### Community 20 - "Usage Data Access (DAO)"
Cohesion: 0.21
Nodes (4): Flow, UsageDao, AppUsageDailyEntity, BlockSurfaceUsageEntity

### Community 21 - "Alarm Repository"
Cohesion: 0.19
Nodes (3): AlarmEntity, AlarmRepository, Flow

### Community 22 - "Alarm Scheduler"
Cohesion: 0.21
Nodes (4): AlarmScheduler, AlarmManager, TimeZone, nextClassReminderTrigger()

### Community 23 - "Task Entity & Repository"
Cohesion: 0.23
Nodes (3): TaskEntity, Flow, TaskRepository

### Community 24 - "Blockable App Catalog"
Cohesion: 0.22
Nodes (9): BlockableApp, BlockableAppCatalog, AppIcon(), BlockAppRow(), BlockingScreen(), Context, BlockingViewModel, StateFlow (+1 more)

### Community 25 - "Room Type Converters"
Cohesion: 0.14
Nodes (4): Converters, AlarmKind, ALARM, REMINDER

### Community 27 - "Permission Intents"
Cohesion: 0.38
Nodes (3): Context, Intent, PermissionIntents

### Community 28 - "Screen Time Worker"
Cohesion: 0.17
Nodes (9): Context, CoroutineWorker, Result, ScreenTimeWorker, WidgetRefresher, BroadcastReceiver, Context, Intent (+1 more)

### Community 29 - "App Database Setup"
Cohesion: 0.21
Nodes (4): AppDatabase, DatabaseModule, Context, RoomDatabase

### Community 31 - "Widget Deep Links"
Cohesion: 0.26
Nodes (7): Context, Intent, NextClass, ScreenTime, Subject, Task, WidgetDeepLink

### Community 32 - "Navigation Host"
Cohesion: 0.29
Nodes (12): AnimatedContentTransitionScope, BottomDestination, defaultPopEnterTransition(), defaultPopExitTransition(), isTopLevelSwitch(), androidx, com, MutableState (+4 more)

### Community 33 - "Folders Screen"
Cohesion: 0.32
Nodes (8): android, CreateFolderDialog(), FolderRow(), FoldersScreen(), Modifier, ChecklistItem, ChecklistRow(), XiaomiOnboardingScreen()

### Community 34 - "Main Activity"
Cohesion: 0.27
Nodes (8): Bundle, ComponentActivity, Intent, MutableState, MainActivity, WakeUpRoot(), WakeUpTheme(), WakeUpNavHost()

### Community 35 - "Settings ViewModel"
Cohesion: 0.20
Nodes (3): StateFlow, ViewModel, SettingsViewModel

### Community 36 - "Task List Items UI"
Cohesion: 0.35
Nodes (11): ColorBar(), CompleteWithGradeDialog(), formatDueDate(), formatNumber(), gradeSummary(), Color, monthGroupLabel(), TaskGroupHeader() (+3 more)

### Community 37 - "Alarm Sound Preview Player"
Cohesion: 0.31
Nodes (5): AlarmSoundPreviewPlayer, MediaPlayer, StateFlow, AlarmSoundPreviewHolderViewModel, ViewModel

### Community 38 - "Block Surface Types"
Cohesion: 0.20
Nodes (9): BlockSurface, FACEBOOK, GENERIC_APP_TIME_LIMIT, INSTAGRAM_REELS, REDDIT, SNAPCHAT, TIKTOK_FOR_YOU, TWITTER_X (+1 more)

### Community 40 - "Timer ViewModel"
Cohesion: 0.29
Nodes (4): StateFlow, ViewModel, TimerChallengePref, TimerViewModel

### Community 41 - "Alarm Timing Tests"
Cohesion: 0.38
Nodes (3): AlarmTimingTest, TimeZone, toLocalDateTimeIn()

### Community 42 - "Stopwatch ViewModel"
Cohesion: 0.31
Nodes (5): Job, StateFlow, ViewModel, StopwatchState, StopwatchViewModel

### Community 43 - "Folder Detail ViewModel"
Cohesion: 0.24
Nodes (3): FolderDetailViewModel, StateFlow, ViewModel

### Community 44 - "App Reliability Worker"
Cohesion: 0.33
Nodes (4): AppReliabilityWorker, Context, CoroutineWorker, Result

### Community 45 - "Dismiss Challenge: Math"
Cohesion: 0.36
Nodes (6): generateProblem(), MathChallenge(), MathProblem, ShakeChallenge(), TypePhraseChallenge(), DismissChallengeContent()

### Community 46 - "Dismiss Challenge Types"
Cohesion: 0.22
Nodes (7): DismissChallengeType, DRAW_GESTURE, MATH_PROBLEM, NONE, SHAKE, TRACE_PATH, TYPE_PHRASE

### Community 48 - "Folder Detail Screen"
Cohesion: 0.54
Nodes (7): AlarmsTab(), EmptyState(), FolderDetailScreen(), formatMinutes(), SubjectIndicator(), SubjectsTab(), TasksTab()

### Community 50 - "Alarm Editor ViewModel"
Cohesion: 0.38
Nodes (4): AlarmEditorViewModel, com, StateFlow, ViewModel

### Community 52 - "Screen Time Screen"
Cohesion: 0.57
Nodes (6): AppIconSmall(), formatDuration(), Context, ScreenTimeScreen(), UsageBarRow(), WeeklyBarChart()

### Community 53 - "Task Editor ViewModel"
Cohesion: 0.38
Nodes (3): StateFlow, ViewModel, TaskEditorViewModel

### Community 54 - "Reels Node Detector"
Cohesion: 0.60
Nodes (3): AccessibilityNodeInfo, DetectionResult, ReelsNodeDetector

### Community 55 - "Task Reminder Receiver"
Cohesion: 0.53
Nodes (4): BroadcastReceiver, Context, Intent, TaskReminderReceiver

### Community 56 - "Dismiss Challenge: Shake"
Cohesion: 0.53
Nodes (3): SensorEventListener, Sensor, SensorEvent

### Community 57 - "Xiaomi Onboarding ViewModel"
Cohesion: 0.47
Nodes (3): StateFlow, ViewModel, XiaomiOnboardingViewModel

### Community 58 - "Alarm Ringing ViewModel"
Cohesion: 0.60
Nodes (3): AlarmRingingViewModel, StateFlow, ViewModel

### Community 59 - "Dismiss Challenge: Draw Pattern"
Cohesion: 0.80
Nodes (4): DrawPatternChallenge(), generateDotPositions(), Offset, nearestDotIndex()

### Community 60 - "Dismiss Challenge: Trace Path"
Cohesion: 0.80
Nodes (4): buildCurvyPathSamples(), distanceTo(), Offset, TraceCurvyPathChallenge()

### Community 61 - "Dev Tools Screens"
Cohesion: 0.60
Nodes (3): NodeInspectorScreen(), DevButton(), DevToolsScreen()

### Community 62 - "Permission Revocation Tracker"
Cohesion: 0.50
Nodes (3): Context, PermissionRevocationTracker, Tracked

### Community 65 - "Alarm Sound Picker Dialog"
Cohesion: 0.67
Nodes (3): AlarmEditorScreen(), AlarmSoundPickerDialog(), Row2()

### Community 66 - "Subject Editor Screen"
Cohesion: 0.67
Nodes (3): IconPickerCell(), androidx, SubjectEditorScreen()

### Community 69 - "Gradle Wrapper Script"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **39 isolated node(s):** `GoHome`, `Keys`, `ALARM`, `REMINDER`, `NONE` (+34 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 193 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **11 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AlarmEntity` connect `Alarm Repository` to `Weekly Class Schedule`, `Alarm Data Access (DAO)`, `Alarm Sound Catalog`, `Alarm Broadcast Receiver`, `Alarm Timing Tests`, `Folder Detail ViewModel`, `Dev Tools ViewModel`, `Alarms List Screen`, `Alarm Timing Calculation`, `Alarm Controller`, `Alarm Editor ViewModel`, `Folder Detail Screen`, `Alarm Scheduler`, `Task Entity & Repository`, `Room Type Converters`, `Alarm Ringing ViewModel`, `App Database Setup`?**
  _High betweenness centrality (0.127) - this node is a cross-community bridge._
- **Why does `NotificationHelper` connect `Alarm Broadcast Receiver` to `Weekly Class Schedule`, `Class Reminder Alarms`, `Alarm Timer Service`, `Reels Block Accessibility Service`, `Alarm Ringing Foreground Service`, `App Reliability Worker`, `Dev Tools ViewModel`, `Task Reminder Receiver`, `Widget Deep Links`?**
  _High betweenness centrality (0.122) - this node is a cross-community bridge._
- **Why does `TaskEntity` connect `Task Entity & Repository` to `Weekly Class Schedule`, `Class Reminder Alarms`, `Alarm Sound Catalog`, `Task List Items UI`, `Alarm Broadcast Receiver`, `Folder Detail ViewModel`, `Dev Tools ViewModel`, `Folder Detail Screen`, `Alarm Controller`, `Task Editor ViewModel`, `App Database Setup`, `Task Data Access (DAO)`?**
  _High betweenness centrality (0.119) - this node is a cross-community bridge._
- **Are the 35 inferred relationships involving `Row` (e.g. with `BlockAppRow()` and `AlarmEditorScreen()`) actually correct?**
  _`Row` has 35 INFERRED edges - model-reasoned connections that need verification._
- **What connects `GoHome`, `Keys`, `ALARM` to the rest of the system?**
  _39 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Weekly Class Schedule` be split into smaller, more focused modules?**
  _Cohesion score 0.06398390342052314 - nodes in this community are weakly interconnected._
- **Should `Class Reminder Alarms` be split into smaller, more focused modules?**
  _Cohesion score 0.06521739130434782 - nodes in this community are weakly interconnected._