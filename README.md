# Calculator Kid Kid / เครื่องคิดเลข คิดคิด

Native Kotlin Android calculator, package `com.kitec.calculatorkidkid`, version 0.1.0, Android 9 (API 28) and later. **Development build; not accepted for production.**

The original approved specification and provenance are preserved under [docs/Requirements](docs/Requirements). See [traceability](docs/TRACEABILITY.md), [build instructions](docs/BUILD.md), and [validation and limitations](docs/VALIDATION.md). Missing historical requirements remain `NEEDS-VERIFICATION`; they have not been removed from the release criteria.

Implemented: decimal arithmetic with at least 60 significant digits for recurring values and exact terminating arithmetic; editable expressions; approved partial/auto-close examples; undo/redo; normalized paste; M1/M2; Room history; settings; Thai/English; themes; offline About/Privacy. Simple context-sensitive percentages follow the owner’s [new clarification](docs/PERCENT_CLARIFICATION.md); complex percent expressions remain blocked pending explicit semantics.

No Internet permission, ads, accounts, analytics or background services. Android backup is disabled. All calculation data stays on the device.

## Quick build

Install a complete JDK 17 and Android SDK Platform 35 / Build Tools 34.0.0 and 35.0.0. Set `JAVA_HOME` and `ANDROID_HOME`, or create ignored `local.properties` with `sdk.dir=<your SDK path>`.

```sh
./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

On Windows use `gradlew.bat`. Unit tests include Robolectric Android runtime checks; first execution needs dependency downloads. The installed calculator is offline even though development tools download dependencies.

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Do not publish it as a completed release. See the validation document for remaining acceptance requirements.

## Release acceptance

See [remaining verification decisions](docs/REMAINING_VERIFICATION.md), [physical-device checklist](docs/PHYSICAL_DEVICE_ACCEPTANCE.md), and [owner-controlled signed AAB procedure](docs/SIGNED_AAB_RELEASE.md). Physical checks remain unrun; no production signing key creation or Play submission is authorized.

## GitHub Actions downloads

The manual-only **Android manual test build** workflow produces the APK, unsigned AAB, exact-commit source ZIP and combined delivery as GitHub Actions artifacts. It has read-only repository permissions and performs no Release/Play publication. See [setup, first-run limitation and download instructions](docs/GITHUB_ACTIONS.md). Its first normal manual dispatch requires the reviewed workflow to be on the default branch; this PR is not merged automatically.
