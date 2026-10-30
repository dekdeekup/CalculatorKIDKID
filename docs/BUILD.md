# Build and installation guide

## Android Studio

1. Install a complete JDK 17 and Android Studio. Open the project root containing `settings.gradle.kts`.
2. In SDK Manager install Android SDK Platform 35 and Build Tools 34.0.0 and 35.0.0; accept the SDK license. Select JDK 17 as Gradle JDK.
3. Let Gradle synchronize. This project pins Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21 and Room 2.6.1. The wrapper distribution is SHA-256 verified.
4. Run unit tests before connecting a physical device, then run the app on an Android 9+ device.

## Windows 10 with 4 GB RAM

Use a terminal to reduce IDE memory use. Set `JAVA_HOME` to the complete JDK 17 directory. Set `ANDROID_HOME` to the installed Android SDK directory, or put `sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk` in ignored `local.properties`.

```bat
gradlew.bat --no-daemon --max-workers=1 :app:testDebugUnitTest
gradlew.bat --no-daemon --max-workers=1 :app:lintDebug :app:assembleDebug
```

The project limits the Gradle JVM heap to 1536 MB. If memory remains tight, close the IDE and other applications. Do not run an emulator on the 4 GB machine at the same time; use the physical device after the unit tests pass.

## Cloud workspace

The toolchain is outside the checkout in `/workspace/.tools`: verified Temurin JDK 17, Android SDK 35, Build Tools 34.0.0 (the AGP 8.7.3 default) and 35.0.0 (artifact inspection), Gradle 8.9. Proxy and trusted CA configuration is in `/workspace/.gradle/gradle.properties`. Never disable TLS verification to resolve a download error.

```sh
export JAVA_HOME=/workspace/.tools/jdk-17.0.17+10
export GRADLE_USER_HOME=/workspace/.gradle
export ANDROID_HOME=/workspace/.tools/android-sdk
./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Existing checkouts are already isolated: use `/workspace/CalculatorKIDKID`; do not create a Git worktree. No development server or background service is required. Emulator/physical-device access is separate from JVM-based Robolectric tests.

## Installing the test APK

After a successful build use Android Studio Run, or enable USB debugging on your phone and run:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Verify the device identifier before installation. A release signed with a different key cannot update a debug install. Back up/export any important test data before uninstalling; uninstalling removes local calculator data. No device installation has been implied by these instructions.

## Release signing and AAB

A debug key is for local tests only. Do not use it as a Play upload key. Keep the upload keystore outside the project, with an encrypted offline backup and passwords stored separately in a password manager. Never put a keystore or password in Git or a delivery ZIP. Use Play App Signing and retain the upload-key recovery instructions from your Play account.

When all release acceptance requirements are met, use Android Studio **Build → Generate Signed App Bundle / APK → Android App Bundle**. Create or select the owner's upload keystore, choose `release`, and securely enter the key credentials. Store the signed AAB outside source control. The owner must control key creation and backups; no upload key has been generated here.

`./gradlew :app:bundleRelease` can verify release compilation without credentials, but its output is **unsigned and cannot be uploaded to Play**. No signing configuration with embedded secrets is included. Validate the signed artifact with `jarsigner -verify -verbose -certs <signed.aab>` and test an APK delivered through Play's testing track before production.

A public privacy-policy URL, actual Play Console account rules and any required closed testing must be handled before publication. Internal testing is not production. Play approval is a separate milestone.

## Data upgrades

Room schema version 1 is exported in `app/schemas`. There is no earlier shipped schema in the archive. For future changes supply an explicit, tested Room migration; never use destructive migration fallback. Settings reset affects settings keys only. Corruption errors must leave the original history database intact.
