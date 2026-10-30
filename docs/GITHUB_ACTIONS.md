# Manual GitHub Actions builds and downloads

## Source provenance and toolchain

- Repository base: `b5929584115aeda37c96008fc36d632167b915ee` (initial README only).
- Reviewed source-import commit: `12a74245613144f30d66aaaa0ea7050a57145210`.
- Source: the previously delivered `CalculatorKidKid-0.1.0-source.zip`, SHA-256 `bc3850f93e068fb744b0a1783414198adcc36a676a1c922678f0342e5b11a0a1`. Application source, tests, schema and approved SRS are preserved byte-for-byte from that delivery. CI/setup documentation and exclusions are added separately.
- Gradle 8.9 with pinned distribution SHA-256; Android Gradle Plugin 8.7.3; Kotlin/kapt 2.0.21; Temurin JDK 17.0.17+10; Android compile/target 35; minimum 28; SDK Build Tools 34.0.0 (AGP default) and 35.0.0 (artifact inspection).
- Room 2.6.1; JUnit 4.13.2; Robolectric 4.14.1. Exact built commit, runtime versions, installed SDK package revisions and test counts are recorded in the generated `BUILD_INFO.json`.
- Historical validation reports describe the earlier cloud milestone. The current workflow's logs, reports and BUILD_INFO are the evidence for its own run, not a claim that all historical acceptance gates passed.

## Workflow configuration

`.github/workflows/android-manual-build.yml` runs **only** on `workflow_dispatch`. It has no push, PR, tag or release trigger. It does not create/publish releases, merge branches, upload to Play, sign production artifacts or read repository secrets.

Permissions are `contents: read`. Checkout does not persist credentials. Actions checkout/setup-java/upload-artifact are pinned to immutable full commit SHAs. The Ubuntu 24.04 GitHub-hosted runner supplies Android command-line tools; the workflow installs required SDK packages, runs unit tests and lint before builds, verifies the debug signature/absence of requested permissions, checks the AAB is unsigned, then packages and scans outputs.

`actions/upload-artifact` uses GitHub Actions runtime authentication; it does not need a personal access token, a custom secret, or repository `contents: write`. No `gh release upload` step is used. Artifacts expire after 14 days. The existing empty draft Release is not changed by this workflow.

The public source branch contains no existing APK/AAB/delivery ZIP. The standard Gradle wrapper JAR is a build-bootstrap dependency, not an existing application deliverable. Generated output, debug keystores, machine-local configuration, caches and credentials stay out of the repository and source archive.

## Review and first execution

1. Review the development PR and its source/SRS preservation evidence. Do not merge until you approve it.
2. GitHub requires a `workflow_dispatch` workflow to exist on the **default branch** to enable normal manual dispatch. A workflow present only in this unmerged development branch may not yet appear or run. Do not add automatic triggers or merge merely to bypass that limitation.
3. After your approved merge to `main`, open the repository's **Actions** tab. If GitHub Actions is disabled, enable it for this repository under your account policy.
4. Select **Android manual test build**, click **Run workflow**, choose `main` (or another approved ref containing the workflow), and click the green **Run workflow** button. This builds the selected commit; it does not publish a release.
5. Wait for the run to show a green success status. A failed run may have a `CalculatorKidKid-failure-reports-<commit>` artifact instead of installable outputs; inspect logs and do not claim a passing APK build.

No hosted run before merge is claimed merely because the workflow YAML is valid or local builds pass.

## Downloading and installing

1. Sign in to GitHub and open `dekdeekup/CalculatorKIDKID` → **Actions** → the successful manual run.
2. On its **Summary**, scroll to **Artifacts** and click `CalculatorKidKid-0.1.0-test-<full commit SHA>`.
3. GitHub downloads an artifact ZIP. Extract it. It contains the debug APK, UNSIGNED AAB, exact-commit source ZIP, docs, BUILD_INFO.json, SHA256SUMS, reports and a combined `CalculatorKidKid-0.1.0-All-Deliverables.zip`.
4. Transfer `CalculatorKidKid-0.1.0-debug.apk` to the Armor 30 Pro. Permit installation from that chosen transfer/browser source if Android requests it, install the test build, then restore that setting when appropriate.
5. A debug signature cannot update an app installed under a different signing key. Do not uninstall an installation containing important data without an owner-approved preservation plan. There is no approved in-app export feature to assume.
6. Use `docs/PHYSICAL_DEVICE_ACCEPTANCE.md` to record device results. Automated JVM/Robolectric tests do not replace physical-device acceptance. Do not try to install an AAB directly.

Because the repository is public, treat its workflow logs and downloadable artifacts as public to users with access to those GitHub features. Never include private input examples or device identifiers in reports. Commit/source archive scanners reduce known credential risks but are not a universal guarantee that arbitrary private information can be detected automatically.

## Signing and release boundaries

The APK uses only an ephemeral development/debug key generated on the runner, which is never packaged. Each fresh runner may create a different debug certificate, so cross-run APK updates can fail signature matching. A stable test-signing arrangement requires a separately approved secure workflow; no permanent key is created or embedded here.

The release AAB remains unsigned. Owner-controlled release signing, missing semantic decisions, physical acceptance, privacy URL and applicable Play policies remain outstanding as documented. This workflow requests no personal access token and has no automatic Release or Google Play publication.
