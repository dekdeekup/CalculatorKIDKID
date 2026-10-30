# Calculator Kid Kid — development scaffold

Package: `com.kitec.calculatorkidkid` · Version: 0.1.0 · minSdk 28.

## What is implemented
- Android Kotlin project structure, manifest without INTERNET permission and with backup disabled.
- Basic on-screen calculator, cursor-aware insert/delete, basic undo/redo, inline preview.
- BigDecimal expression parser with +, -, ×, ÷, unary signs, parentheses, simple postfix %, implicit multiplication and input limits.
- Initial JUnit tests.

## NOT YET IMPLEMENTED / NOT VERIFIED
This is **not** a completed SRS-compliant release. Contextual percent, full partial-expression semantics, automatic closing parentheses, scientific notation exponent sign bounds, paste normalization/truncation, complete memory M1/M2, session/persistent Room history, settings, language/theme, all lifecycle rules, TalkBack audit, layout adaptation, asynchronous preview, complete test coverage, Play listing/privacy materials and release signing are outstanding. Some current parser behavior is intentionally provisional and must be revised against the approved SRS. The initial tests have not been executed.

No APK or AAB is included because Android SDK/Gradle were unavailable in the generation environment. No signing keys were generated.

## Build prerequisites
Install JDK 17, Android SDK platform 35, build tools and Gradle 8.9 or compatible; open the folder in Android Studio (Gradle wrapper not included). From a configured Gradle environment:

```
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

Debug APK output after a successful build: `app/build/outputs/apk/debug/app-debug.apk`.

## Release safety
Do not upload this scaffold to Google Play as a completed calculator. Finish the SRS, run tests, verify on devices, configure a secure upload key and use Play App Signing before producing a signed release AAB. Never commit signing passwords or private keys to the project.
