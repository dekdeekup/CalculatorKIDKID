# Requirements traceability — v0.1.0

Baseline: unchanged documents in `Requirements/`. Initial audit: repository had only README; supplied scaffold explicitly incomplete and unverified. Each item below remains an acceptance obligation, even when not yet verified.

| SRS section | Binding checklist | Initial assessment |
|---|---|---|
| 1 Scope | Native Kotlin/API 28; offline/no permissions, backup/services/ads/accounts disabled; Thai/English; themes; icon; offline About/Privacy; hide converter; measure size/launch. | Partial scaffold; implementation/verification required |
| 2 UI | Portrait/landscape/narrow/split-screen/font scaling; TalkBack; color and shape; ordered keys; no repeat; asynchronous preview with stale-result protection; inline errors and C. | Partial scaffold; implementation/verification required |
| 3 Editing | Cursor/selection/hardware/copy; undo/redo 50; expression500/operand200/depth50; precedence/unary/implicit/percent; partial preview; approved equals examples and deduplication. | Partial scaffold; implementation/verification required |
| 4 Numbers | BigDecimal >=50; places0–20; HALF_UP/UP/DOWN; full internal values; scientific formatting after rounding; exponent±1000; no negative zero; result copy; errors. | Partial scaffold; implementation/verification required |
| 5 Clipboard | Thai digits/operators/whitespace/locale groups; atomic invalid rejection; combined length; validated truncation; no split scientific tokens; warning; single undo. | Partial scaffold; implementation/verification required |
| 6 Memory | Independent durable M1/M2; partial preview storage; cursor insertion and four mandatory examples; valid length truncation; long-press irreversible clear; truthful failures. | Partial scaffold; implementation/verification required |
| 7 History | Session20; Room newest-first; successful equals only; repeat dedup; calendar days7/30/90 and max100/300/500 defaults30/300; prune; delete/undo while screen; confirmed clear; corruption isolation; migrations. | Partial scaffold; implementation/verification required |
| 8 Settings | Thai/English; System/Light/Dark; decimal places and rounding; retention; locale formatting; autosave; confirmed settings-only reset; failures. | Partial scaffold; implementation/verification required |
| 9 Lifecycle | Same-process return; rotation includes cursor/preview/page/stacks/history scroll; process/task relaunch resets session; other recreation clears stacks; navigation rules. | Partial scaffold; implementation/verification required |
| 10 Delivery | Unit tests then device; correct numbers and usable UI; Armor30Pro/API28+; Android Studio/Windows4GB CLI guide; APK; signed AAB; secure key procedure; Play requirements. | Partial scaffold; implementation/verification required |

## NEEDS-VERIFICATION

- SRS §3 and provenance source gaps: contextual percent truth tables, unary/implicit multiplication edge cases are unavailable. Simple cases now follow `PERCENT_CLARIFICATION.md`; complex cases remain a release blocker.
- SRS §4: exact scientific threshold edge cases and significant digit interpretation require historical evidence.
- Hardware shortcut mappings and sign-toggle operand scope are not specified. Existing whole-expression sign behavior is retained.
- Public privacy URL, Play account/testing obligations and upload signing credentials are unavailable. Never invent these.
- Physical Armor 30 Pro and Android 9 device verification require devices.

## Conflicts

No conflicting approved requirements found. Scaffold tests expecting missing-parenthesis failure conflict with the approved equals auto-close example only if used at the equals layer; strict parser evaluation remains valid, and equals/preview completion must be tested separately. File delivered is a 7z archive rather than the ZIP name in the request; all 12 files were extracted and inspected.

## Implementation and verification map

“Implemented” means code is present; it does not replace device acceptance. Baseline limitations and release gates remain binding.

| Requirement / source | Implementation | Automated evidence / remaining verification |
|---|---|---|
| §1 Kotlin / API 28+ / package / version | Gradle manifests and native Activity | Build, API 28/35 Robolectric; physical API 28+ pending |
| §1 offline / no ads / no accounts / no collection / no services | Permission-free manifest; no SDKs for these services | Manifest inspection; APK permission inspection |
| §1 no Auto Backup | Manifest disables backup and full backup | APK manifest; device-transfer behavior still needs device audit |
| §1 size <= ~30MB / launch <=2s | Minimal native Views and Room | APK size reported; installed size and cold launch unmeasured |
| §1 gold calculator/arrows icon | Vector drawable | Visual review pending |
| §1 converter hidden | No converter page | Source inspection; future requirements retained unchanged |
| §1 offline About / Privacy | Bilingual offline page | Runtime History navigation/confirmation tested; About visual device review pending; public URL unavailable |
| §1 sound/haptics off | Buttons disable feedback | Device audit pending |
| §2 portrait / landscape / narrow / split-screen / fonts | Responsive display/keypad, minimum touch sizes and scroll fallback | Rendered 360×640 portrait, 640×360 landscape and 2× fonts tested; physical matrix pending |
| §2 TalkBack and color + shape | Descriptions; rounded digit vs edged command backgrounds | TalkBack and contrast audit pending |
| §2 ordered input / no auto-repeat | UI-thread edits, normal click controls | Button workflow test; physical rapid-input audit pending |
| §2 expensive work / busy / stale previews | Serial worker; generation check; busy text | API 28/35 stale-preview runtime tests |
| §2 inline errors / editing clears / serious error C | Inline status; input preserved; C handler | Calculation/error tests; memory-pressure fault audit pending |
| §3 cursor / selection / keyboard / expression copy | EditText; shared editor; native text selection | Editor selection test; device keyboard/selection audit pending |
| §3 Undo / Redo 50 / C | Editor transaction stacks | Undo limit, selection undo/redo, paste and auto-close tests |
| §3 500 chars / 200 digits / depth50 | Editor + strict parser guards | Boundary tests |
| §3 precedence / unary / implicit / contextual percent | Preserve ordinary scaffold grammar; simple context-sensitive percent per new clarification | Existing parser tests; complex percent rules NEEDS-VERIFICATION |
| §3 partial preview | Only documented trailing plus and valid auto-close completion | Approved partial, ambiguous, auto-close tests; other cases NEEDS-VERIFICATION |
| §3 equals unchanged partial / auto-close undo / dedup | Completion metadata and session save marker | Pure completion tests and button/Room workflow test |
| §4 BigDecimal / >=50 precision | 60-digit recurring conversion; exact rational intermediates avoid premature loss | Precision, exact cancellation, terminating/recurring tests |
| §4 places0–20 / HALF_UP UP DOWN | Settings + formatter | Rounding and settings persistence tests |
| §4 internal values separate from display | BigDecimal preview, memory and history values | Tiny nonzero/internal continuation tests |
| §4 scientific after rounding / trailing zeros / negative zero | Formatter | Formatting tests; exact historical threshold edges NEEDS-VERIFICATION |
| §4 exponent±1000 / zero division / malformed | Strict parser and result exponent guard | Bounds, malformed and zero division tests |
| §4 copying full terminating / rounded recurring / dot/no grouping | Rational denominator classification then plain numeric output | Terminating, recurring and cancellation copy tests |
| §5 Thai digits / operators / whitespace / grouping | Editor normalization | Thai/US/German grouping tests |
| §5 atomic invalid rejection / normalized combined cap | Validates before transaction | Atomic rejection and combined-length tests |
| §5 valid partial paste / no split exponent / Thai warning | Whole scientific-token boundaries; inline localized warning | Truncation and scientific-token tests; device clipboard notification audit pending |
| §5 paste one undo / no separate clipboard notification | Shared Editor transaction; Android clipboard only | One-undo test and source inspection |
| §6 two durable full-precision registers / partial preview | Preferences commit; same preview value | Persistence tests; API 28/35 partial-preview memory runtime tests |
| §6 insertion cursor / negative wrap / zero replacement | Editor memory insertion | All four mandatory examples and selection test |
| §6 cap/truncation / long press irreversible clear / display-only formatting | Shared insertion; separate preference keys; formatter | Model and persistence tests; physical long-press audit pending |
| §6 truthful storage failure | UI applies durable changes only after commit; error reporting | API 28/35 failed-commit reporting runtime tests |
| §7 session20 / new session reset | Process-only Session, bounded deque | API 28/35 rotation, recreation, new-launch and in-flight operation runtime tests |
| §7 Room newest-first / only successful equals / dedup | DAO and equals save marker | Room sorting and button workflow tests |
| §7 local-calendar retention / max / prune open/history | DAO pruning with local date cutoff | Calendar/DST, pruning and cap tests; exact inclusive boundary NEEDS-VERIFICATION |
| §7 latest delete undo only on History / clear confirmed irreversible | Session deletion slot invalidated on navigation; confirmation | API 28/35 deletion/confirmation/navigation runtime tests |
| §7 corruption isolate / never delete | Non-destructive SQLite corruption callback and guarded worker | Corrupt-file preservation runtime test |
| §7/§8 preserve upgrade data / migrations | Schema v1 exported, no destructive fallback | No earlier released schema supplied; future schema changes require migration tests |
| §8 language/themes/places/rounding/retention / autosave | Spinner changes committed on worker; no Save button | Preference tests; API 28/35 rapid autosave and rotation-while-saving tests |
| §8 reset only settings / confirm / failure | Separate settings keys; confirmed reset | Memory/settings persistence test; failed-commit reporting tested |
| §9 return / rotation / History scroll / UndoRedo | Process-only task session and saved orientation marker | API 28/35 rotation/recreation runtime tests; actual device task management pending |
| §9 process death / task removal new session | No expression restored from Bundle; new launch resets Session | New-launch reset tested; actual OS process kill / Recents swipe still needs device acceptance |
| §9 other recreation clears stacks | Saved orientation comparison | API 28/35 non-rotation recreation test |
| §9 Back behavior / delete undo invalidation / autosave | Calculator task moved back; page navigation invalidates delete slot | API 28/35 deletion/confirmation/navigation runtime tests; previous-app device check pending |
| §10 unit tests before physical device / all numerical errors fixed | Tests run before any device installation | No physical tests or publication attempted |
| §10 source / beginner Windows4GB CLI / APK / signed AAB | Wrapper + BUILD guide; debug APK; release compilation | Signed AAB blocked on owner-controlled signing; device acceptance outstanding |
| §10 actual Play rules / closed testing / approval separate | Release guide | Play account requirements and public privacy URL NEEDS-VERIFICATION |
| §11 decisions01–30 | All mapped above to their source sections | No approved decision removed |
| §12 final clarifications1–3 | Partial completion, safe truncation, no converter placeholder | Tests/inspection above |
| §13 English technical docs / Thai+English UI / safe secrets | English docs, paired resources, no upload key created | Resource/source audit; Thai native-speaker review pending |

## Input ordering correction

`=` now queues subsequent button/hardware input until its asynchronous evaluation completes. The queue is held in the process session and completion is delivered to the current Activity across rotation. Tests exercise rapid auto-close → edit → equals and rotation during that sequence, preserving both resulting History entries. Obsolete queued previews are skipped before calculation and stale callbacks cannot overwrite newer input. Pending settings snapshots and completion callbacks likewise survive rotation.

## Release acceptance documentation follow-up

See [remaining verification register](REMAINING_VERIFICATION.md) for D01–D12 (missing decisions), E01–E09 (device/evidence gates), and P01–P04 (external prerequisites); [physical-device checklist](PHYSICAL_DEVICE_ACCEPTANCE.md) for 43 explicitly unrun cases; and [owner-controlled signing procedure](SIGNED_AAB_RELEASE.md). This documentation review changes no application source, tests, approved numeric rules or localization. Existing 71-test results are prior completed automated evidence, not a newly executed device test or test rerun in this milestone.
