# Development validation — October 30, 2026

Status: **buildable, tested development milestone; full v0.1.0 release acceptance is incomplete.** The original SRS remains binding. No publication or physical-device acceptance is claimed.

## Completed features and changed files

- `CalculatorEngine.kt`: preserves strict parser grammar for ordinary arithmetic, adds approved partial/auto-close orchestration, exact rational intermediates with BigDecimal results, at least 60 significant digits for recurring results, exponent checks, display formatting, result-copy classification, and newly clarified simple contextual percent rules. Unresolved complex percentage expressions produce an editable inline error.
- `Editor.kt` (new): shared cursor/selection editing, 50-step undo/redo, atomic paste normalization, locale grouping validation, length/digit/depth guards, scientific-token-safe truncation, mandatory memory insertion examples, and locale-aware display grouping.
- `Storage.kt` (new): two independently durable full-precision memory slots, settings commits, Room history schema version 1, calendar retention/capping and non-destructive corruption handling. Exported schema is in `app/schemas`.
- `MainActivity.kt`: native calculator, separate landscape layout, enlarged-font/small-window scrolling, asynchronous preview generation guards, ordered input around equals, lifecycle/session retention, memory, session/permanent history, deletion undo/confirmed clear, autosaved settings/confirmed reset, themes, TalkBack descriptions, and offline About/Privacy.
- `res/values/strings.xml`, `res/values-th/strings.xml`, `res/drawable/ic_calculator.xml`, `res/xml/data_extraction_rules.xml`: bilingual UI, gold calculator/arrows icon and backup/transfer exclusions.
- Manifest and Gradle files: Android 9 minimum, compile/target 35, no requested permissions, no services, backup disabled, both languages bundled for offline switching, Room dependencies, pinned wrapper and distribution checksum, JVM and Android-runtime test configuration.
- `AcceptanceTest.kt`, `AndroidWorkflowTest.kt`, `LayoutTest.kt` (new), plus the preserved scaffold `CalculatorEngineTest.kt`: calculation, edge, persistence, lifecycle, fault and layout checks.
- README and `docs/`: unchanged approved baseline copies, source inventory/hashes, detailed traceability, new percentage clarification, beginner Windows/Android Studio instructions, release/signing guide and this report.

## Automated checks actually executed

Final source was validated through the saved reusable setup script, then release bundling:

```sh
/workspace/.tools/setup-calculator.sh
JAVA_HOME=/workspace/.tools/jdk-17.0.17+10 GRADLE_USER_HOME=/workspace/.gradle ANDROID_USER_HOME=/workspace/.android ./gradlew --no-daemon :app:bundleRelease
```

The setup script runs SDK preparation and `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`. It has been exercised successfully more than once in this instance. Latest tests executed after the final source change; they were not merely reported from an empty or skipped run.

| Suite | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|
| AcceptanceTest | 26 | 0 | 0 | 0 |
| AndroidWorkflowTest | 34 | 0 | 0 | 0 |
| CalculatorEngineTest | 8 | 0 | 0 | 0 |
| LayoutTest | 3 | 0 | 0 | 0 |
| **Total** | **71** | **0** | **0** | **0** |

AndroidWorkflowTest runs 17 runtime cases on both API 28 and API 35 under Robolectric. It verifies real button-driven calculations/Room writes, exact approved memory behavior, corrupt-file preservation, failed preference commits, newest-first pruning/capping, reopening persistence, confirmed History clear, deletion undo/navigation, rotation and non-rotation recreation, new-launch session reset, stale preview suppression, rapid settings autosave, rapid input across equals, rotation during equals and rotation while settings are saving.

LayoutTest renders native Android Views under Robolectric: 360×640 portrait fits all 31 buttons at >=48dp touch height, 640×360 landscape fits the rearranged controls, and 2× system fonts keep equals reachable by scrolling. Portrait and landscape PNGs were visually inspected. These are simulated runtime/layout checks, not screenshots from a physical device or emulator.

Lint: **0 errors, 1 warning** (`KaptUsageInsteadOfKsp`, a build-time performance recommendation). Kotlin emits deprecation warnings for Android compatibility APIs and kapt's language-level fallback; builds succeed. No failing check was disabled, no lint baseline was used, and no checksum/TLS/signature verification was bypassed. Robolectric tests first failed because test code called Room on the UI thread; corrected tests use a worker and genuine SQLite corruption assertions. A real rotation/undo defect and an equals/input race were fixed and now have regression tests.

## Build and artifact status

- Debug APK: successful `assembleDebug`; `apksigner verify` passes with APK Signature Scheme v2. This is a development/debug signature, not an owner upload signature.
- Release AAB: successful `bundleRelease`; `jarsigner -verify` explicitly reports **jar is unsigned**. The Gradle task name `signReleaseBundle` does not establish that an upload key was configured. Do not upload this unsigned artifact to Play.
- APK inspection confirms package `com.kitec.calculatorkidkid`, version 0.1.0/code 1, minSdk 28, targetSdk 35, approved Thai app name and Thai/default English resources. `aapt dump permissions` shows **no requested permissions**.
- `git diff --check` passes. Original approved requirement/provenance copies are byte-identical to the supplied archive. Application changes are in the working checkout; no commit or push was made.

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| app-debug.apk | 3014202 | `5c5627f86ef9b5ba30d14175331335b327d04c83e98999a4ffdaab8bb8e98080` |
| app-release.aab | 783904 | `10b8bd7225bc4aa1088581018801fe7aa8b478a4368c8bd81fb8dcf8111da22e` |

APK size is approximately 2.9 MiB. Installed size, cold-launch time and behavior on a 2GB-RAM device remain unmeasured; the size/launch goals are not claimed as verified.

## Remaining requirements / NEEDS-VERIFICATION / blockers

1. **Complex percentage semantics** remain undefined: parentheses, nested/repeated percentages, mixed precedence and forms outside the new simple clarification. They are rejected rather than assigned guessed results. See `PERCENT_CLARIFICATION.md` for the exact new owner instruction and examples.
2. Historical unary/implicit-multiplication edge truth tables, sign-toggle operand scope, exact scientific-format threshold edges and other partial-expression semantics were not recovered. Existing ordinary grammar and whole-expression sign toggle are retained. Partial completion is conservative: documented trailing-plus forms and safely closable complete parenthesized expressions are supported; other ambiguous forms remain suspended. These gaps prevent a claim of complete SRS grammar acceptance.
3. Exact local-calendar retention inclusion at the boundary and whether Clear All also encompasses the separate session list were not explicitly specified. Current behavior prunes before local midnight `today minus retention days`; the clearly labeled permanent-history clear affects permanent records. These choices are disclosed for verification, not represented as recovered historical decisions.
4. No physical device or emulator is attached (`adb devices` returned an empty list). No Ulefone Armor 30 Pro installation/test, actual OS process-kill/Recents-swipe test, real split-screen/device matrix, physical rapid-key/keyboard/clipboard behavior, TalkBack audit or native-speaker localization review has been performed. The environment has no `/dev/kvm`; Robolectric tests do not replace the required physical-device milestone.
5. Full Android 9+ device compatibility, dark-theme readability, extreme font/window sizes, real storage-full behavior, installed size and <=2-second cold launch require device measurements. Injected failed commits and synthetic DB corruption have been tested, but do not establish every device failure mode.
6. No upload keystore or signing credentials were supplied or generated. Signed AAB delivery is blocked until the owner establishes secure signing and backups. Never place those secrets in the project, ZIP, Git or chat. The provided unsigned AAB is only build evidence.
7. A publicly reachable privacy-policy URL, applicable current Play target-API/account rules and any required closed testing remain release prerequisites. Offline privacy content exists. Nothing has been uploaded to Play, and no approval is claimed.
8. No earlier shipped Room schema was supplied. Version 1 is exported; future schema changes must add and test explicit migrations. The app has no destructive migration or corruption-recovery fallback.

## Cloud configuration and next milestone

JDK 17, Android SDK Platform 35/Build Tools 34.0.0 and 35.0.0, platform-tools, Gradle 8.9, trusted CA/proxy configuration and dependency caches are prepared outside source. The setup script and startup/activation instructions were saved to the cloud configuration draft as `install_script` and `start_skill`. No network domains, credentials or secrets were added. Review/save the draft in environment settings and publish the environment to retain the prepared snapshot for future tasks. Publication and a fresh-task restoration have not been performed or verified.

Next: resolve the remaining explicit semantic gaps using approved evidence, then perform the documented physical-device/accessibility/performance acceptance matrix, correct any findings, and produce a securely signed release AAB. The source, debug APK and unsigned build bundle are reviewable now; they do not mark the complete v0.1.0 release accepted.

## Release acceptance documentation follow-up

See [remaining verification register](REMAINING_VERIFICATION.md) for D01–D12 (missing decisions), E01–E09 (device/evidence gates), and P01–P04 (external prerequisites); [physical-device checklist](PHYSICAL_DEVICE_ACCEPTANCE.md) for 43 explicitly unrun cases; and [owner-controlled signing procedure](SIGNED_AAB_RELEASE.md). This documentation review changes no application source, tests, approved numeric rules or localization. Existing 71-test results are prior completed automated evidence, not a newly executed device test or test rerun in this milestone.
