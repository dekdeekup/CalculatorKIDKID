# Physical-device acceptance checklist — Calculator Kid Kid v0.1.0

**Execution status: NOT RUN.** This is a test procedure, not a record of passing device tests. Approved baseline is unchanged. Complete separately for the owner’s Ulefone Armor 30 Pro and an actual Android 9/API 28 device where available. An Android 9 emulator can supplement unavailable hardware, but must be labeled **emulator**, never physical. Additional Android versions and 2GB-RAM-class hardware extend compatibility evidence.

## Before installation

- [ ] Confirm latest parser/rounding/error automated suite passes before physical testing. Existing evidence: 71 tests passed; see VALIDATION. Rerun if source/binary changes.
- [ ] Identify APK SHA-256 from delivered `SHA256SUMS`; verify it matches the exact file installed.
- [ ] Record tester/date/timezone, model, Android version/API/security patch, RAM, screen size/density, font/display scales, system/app language, theme, navigation mode, APK checksum, package/version and signing-certificate fingerprint. Do not record personal device identifiers in shared reports.
- [ ] Use disposable calculation data. An uninstall deletes local data. The app has no approved export feature; do not assume an in-app backup/export exists. Preserve important owner data by an owner-approved method before destructive tests.
- [ ] Install the debug APK. With ADB use `adb -s <selected-device> install -r <debug.apk>` only after selecting the intended device. A different signing key cannot update an existing install; stop and diagnose rather than automatically uninstalling it.
- [ ] Keep one result sheet per device/configuration. For every case record PASS / FAIL / BLOCKED / NOT RUN, actual result, screenshot/log where helpful, and defect ID. Never mark an undefined D-item numeric result PASS.

## Required tests

| ID | Steps / exact input | Expected behavior from approved baseline | Result / evidence |
|---|---|---|---|
| F01 | Launch from launcher, then airplane mode with Wi-Fi/mobile data off; relaunch and calculate `2+3×4` | Direct calculator launch; result `14`; normal operation offline, no account/ad/network requirement. | NOT RUN |
| F02 | Inspect app permissions and components; open About/Privacy offline | No Internet/unnecessary permissions, ads/accounts/background services; offline truthful About/Privacy available; no converter placeholder. | NOT RUN |
| F03 | Observe launcher/app title and icon | Approved name `เครื่องคิดเลข คิดคิด`; gold calculator/arrows concept; readable icon on device. | NOT RUN |
| F04 | Switch Thai/English and System/Light/Dark; navigate Back and relaunch | Both languages remain available offline; changes auto-save; readable text, controls, errors, menus and dialogs. | NOT RUN |
| F05 | Normal-font portrait; then landscape | Expression, result and calculator controls fit normal portrait, long text scrolls; landscape rearranges controls; no obstruction of touch/readability by system bars. | NOT RUN |
| F06 | Narrow split-screen; largest supported system fonts/display scaling; rotate in each configuration | Every control remains reachable; text readable; limited font-related scrolling allowed; no clipped essential action. Log any ordinary-portrait overflow as a defect. | NOT RUN |
| F07 | Enable TalkBack; traverse expression/result/all controls/M1/M2/History/Settings/dialogs | Meaningful labels, logical reachable traversal; errors announced appropriately; digits vs commands differ by shape and color. | NOT RUN |
| F08 | Type/select/move cursor in `1234`, select middle `23`, insert `9`; undo/redo; copy selected expression | `194`, correct caret/selection; Undo restores prior text/selection; Redo reapplies; selected text copies correctly. | NOT RUN |
| F09 | Attach hardware keyboard; type arithmetic and use implemented shortcuts listed in About | Hardware editing works without lost input. Enter/equals, Ctrl+Z, Ctrl+Shift+Z/Ctrl+Y and Ctrl+V match implementation; final mapping approval remains D10. | NOT RUN |
| F10 | At least 51 separate edits, exercise Undo/Redo and C | Undo/Redo bounded to 50 steps; editing after Undo clears Redo; C clears expression/preview and remains usable after an error. | NOT RUN |
| F11 | `0.1+0.2`; `2×(3+4)`; `2(3+4)` | `0.3`; `14`; `14`. No binary-floating-point artifact. | NOT RUN |
| F12 | Enter `12+`, then `=` | Display `12`; keep `12+` unchanged; no History insertion. | NOT RUN |
| F13 | Enter `2×(3+4`, then `=`; Undo | Close to `2×(3+4)`, display `14`, save History once; Undo restores unclosed expression. | NOT RUN |
| F14 | Enter `2×(3+`, then `=`; edit to a valid expression | No History insertion; expression stays editable; editing clears inline error and refreshes preview. No guessed partial numeric acceptance. | NOT RUN |
| F15 | A valid `=` followed by repeated `=` without editing | No duplicate History entries. | NOT RUN |
| F16 | Rapidly enter `2×(3+4`, `=`, `+2`, `=`; repeat with rotation while evaluation is pending | Input order preserved, final expression closed correctly, results `14` then `16`, both successful calculations recorded once; no stale preview overwrites new input. | NOT RUN |
| F17 | Test all nine percent examples in PERCENT_CLARIFICATION.md | Exact documented results; chains use updated accumulated value. Complex probe forms remain BLOCKED for final numeric acceptance under D01–D04. | NOT RUN |
| F18 | At 2 places: `-1.235` with HALF_UP, UP, DOWN; then `-0.0001` | `-1.24`, `-1.24`, `-1.23`; rounded zero displays `0`, never `-0`; strip unnecessary fractional zeroes. | NOT RUN |
| F19 | At 10 places `1e-20`; store M1, change display precision, recall into a fresh expression and multiply by `1e20` | Display may be `0`; internal/M1 value retained; continuation yields `1`, not zero. Changing settings does not change stored value. | NOT RUN |
| F20 | Copy result `1/8` at 2 places; then `1/3` at 2 places HALF_UP | Clipboard `0.125` for terminating value; `0.33` for recurring value; dot decimal, no grouping. | NOT RUN |
| F21 | Input `1/0`, `1/(2-2)`, `2..3`, and out-of-range `1e1001` / `1e-1000/10` | Editable inline error, no crash/freeze/modal calculation-error dialog; expression retained; no History save. | NOT RUN |
| F22 | Use prepared text to exercise 500/501 expression characters, 200/201 operand digits, 50/51 nested parentheses and ±1000 exponent limits | Approved limits enforced without crash or silent numeric corruption. Distinguish direct input rejection from overlong Paste truncation. | NOT RUN |
| F23 | Paste Thai digits/operators/whitespace and correctly grouped `1,234.5`; attempt `3abc` into existing `12` | Normalize supported input; valid number evaluates correctly; invalid characters reject the entire paste and preserve `12`. | NOT RUN |
| F24 | Existing expression is `1+` repeated 249 times (498 chars); paste ` ๒๓๔๕ ` | Combined normalized length ≤500; validated prefix inserted with partial-paste warning; one Undo restores prior expression. | NOT RUN |
| F25 | Existing expression is `1+` repeated 248 times (496 chars); paste `12e-34` | Never insert a split scientific literal; warn if truncated; preserve editable input. Observe system clipboard notifications only, no extra app notification. | NOT RUN |
| F26 | Empty M1: store preview of `12+`; clear expression; tap M1 | Store full preview value `12`; recall at cursor/selection as one Undo step; register independent of M2. | NOT RUN |
| F27 | M1 values `3`, `-3`, `3`, `5` inserted after `2`, `2`, `12.`, `0`, respectively | Exact expressions `23`, `2(-3)`, `12.3`, `5`; selection replacement correct; each insertion one Undo step. | NOT RUN |
| F28 | Long press M1; attempt Undo; long press other keys | M1 clears immediately and cannot be restored by editing Undo; M2 unchanged; no unwanted key auto-repeat. Sound/haptic feedback defaults off. | NOT RUN |
| F29 | Make 21 successful distinct equals operations; inspect both history lists | Session keeps latest 20; permanent newest first; partial previews/errors absent. | NOT RUN |
| F30 | Delete entry A, then B; Undo; leave History and return | Latest deletion only is undoable on History; leaving invalidates delete Undo; calculator expression preserved. | NOT RUN |
| F31 | Clear All: cancel, then confirm using disposable history | Cancel preserves entries; confirm irreversibly clears the labeled permanent list. Session-list final scope remains D12, not a guessed pass. | NOT RUN |
| F32 | Check retention settings 7/30/90 days and maxima 100/300/500; reopen app/History with controlled fixture records | Defaults 30/300; prune on opening; preserve ordering and configured caps. Boundary-date expected membership remains D11. Use a disposable test profile, not the owner’s normal history. | NOT RUN |
| F33 | Change places, rounding and retention rapidly; Back; rotate during a pending setting change | All intended changes auto-save; no Save button; no silent discard; settings survive relaunch. | NOT RUN |
| F34 | Reset Settings: cancel, then confirm after populating both registers and permanent History | Cancel preserves settings; confirmation resets settings only; Memory and History stay intact. | NOT RUN |
| F35 | Set expression/cursor/preview and Undo/Redo; visit another app and return; repeat from each page | Same-process return preserves all state/current page. If OS killed process, classify as F38 instead of reporting a same-process failure. | NOT RUN |
| F36 | Rotate calculator; rotate History after scrolling; rotate Settings | Preserve same-process expression/cursor/preview/stacks/page and History scroll; pending input/save survives rotation. | NOT RUN |
| F37 | Use Back from calculator, History and Settings | Calculator returns to previous app without clearing expression; History Back invalidates deletion Undo; Settings Back auto-saves. | NOT RUN |
| F38 | Background app and confirm OS process termination, then relaunch; separately swipe task from Recents and relaunch | New empty expression/session; persistent Settings/M1/M2/permanent History remain. Record method; merely rotating, backgrounding or using developer “Don’t keep activities” does not prove process death. | NOT RUN |
| F39 | Trigger non-rotation Activity recreation while process survives, e.g. controlled developer-option test; restore option afterward | Expression survives; Undo/Redo clear. Do not confuse this with F38 or rotation. | NOT RUN |
| F40 | Update a disposable populated install with a same-signature compatible build, without uninstalling | Settings/Memory/permanent History retained. Migration evidence required if schema changes. Different-key replacement is not a data-preserving upgrade test. | NOT RUN |
| F41 | Disposable profile only: constrained storage/controlled failed writes and corrupt DB; keep an untouched copy before corruption | Truthful errors, no false durable-success indication; calculator stays usable; corrupt DB not auto-deleted/reset. Real-device fault evidence supplements existing injected tests. Stop if safe fault setup is unavailable and mark BLOCKED. | NOT RUN |
| F42 | Measure multiple cold launches and installed app size; observe responsiveness under memory pressure | Record launch-to-usable time (goal ≤2s), installed-size method (goal ≈≤30MB), sample range and device RAM. No unmeasured performance guarantee. | NOT RUN |
| F43 | Disposable profile/device-transfer audit with OS tools | No Android Auto Backup or calculator-data restoration via disabled backup/transfer paths. Record actual manufacturer/OS behavior; manifest configuration alone is not a physical pass. | NOT RUN |

## Results and release gate

Attach case IDs, actual outcomes and evidence to defects. Every incorrect numeric result and obstructive UI/touch defect blocks release until corrected and reverified. Keep undefined-rule probes BLOCKED pending the associated D decision; do not replace their expected results with conventional guesses. Minor cosmetics may be tracked separately under the approved SRS.

Run applicable cases in Thai and English and across the device matrix. A partial pass on the Armor does not establish Android 9 compatibility. Mark unavailable hardware/emulator/fault scenarios honestly. After physical acceptance and remaining decisions are resolved, follow SIGNED_AAB_RELEASE.md; no Play upload or permanent key generation is authorized by this checklist.
