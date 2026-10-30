# Approved Requirements Evidence and Exact Behavioral Examples

## Status and source fidelity
This is a reconstruction of **all approved requirements currently available** in the conversation context, prior summaries, and the existing handoff files. It is not a verbatim export of all older Grill Me rounds 1–50. All recovered facts are included; no missing choices have been fabricated. The product owner approved the SRS on 2026-10-30, after a 30-topic supplementary interview and three final clarifications. Translation of the handoff does not authorize any behavior changes.

## Additional exact examples and constraints
- `12+` followed by `=` displays `12`, keeps the expression as `12+`, and does not append History.
- `2×(3+4` followed by `=` auto-closes the missing `)`, displays `14`, saves History, and Undo restores the prior incomplete expression.
- `2×(3+` followed by `=` does not append History.
- Repeating `=` without editing does not add History.
- Insert M1(3) after expression `2` -> `23`.
- Insert M1(-3) after expression `2` -> `2(-3)`.
- Insert M1(3) after expression `12.` -> `12.3`.
- Insert M1(5) after expression `0` -> `5`.
- Paste normalization must recognize Thai digits and supported operator glyphs; invalid characters reject the entire paste without changing the expression.
- Paste may truncate a validated prefix with warning, but must not split scientific-notation syntax. One successful paste is one Undo step.
- Full precision is maintained for operations and Memory; display rounding is separate. Copying terminating decimals uses full internal value; recurring decimals use configured rounding.
- Nonzero values rounded to zero for display may show `0`, without destroying internal value. No trailing zeros or negative zero in displayed results.
- Maximum expression length 500 characters; numeric operand length 200 digits; parentheses depth 50; scientific exponent magnitude 1000.
- Session History retains 20 entries and resets on a genuinely new session; permanent Room History defaults to 30 local-calendar days / 300 entries and supports 7/30/90 days and 100/300/500 entries.
- History individual delete: only latest deletion undoable and only while remaining on History screen; Clear All is confirmed and irreversible.
- Same-process return preserves expression, cursor, preview, Undo/Redo and page; same-process rotation also preserves History scroll position. Process death or task swipe-away then relaunch starts a new expression/session while preserving persisted data. Non-rotation Activity recreation preserves expression but clears Undo/Redo.
- No Internet permission, ads, accounts, background services, or Android Auto Backup. The app still supports Thai and English UI; only technical handoff language is English.
- No premature claim of passing build/tests, availability of APK/AAB, or successful Google Play submission.

## Approved additional interview (01–30)
- 01. Open directly to calculator.
- 02. Portrait fits expression, result and controls; scroll only long text.
- 03. Accommodate system font scaling; some scrolling allowed.
- 04. Landscape rearranges controls.
- 05. Favor compact APK, speed and native simplicity.
- 06. Same-process return from other apps preserves expression, cursor and preview.
- 07. Process rapid key presses in order without loss.
- 08. Support narrow displays and split-screen.
- 09. Move heavy calculations off UI thread and indicate busy if needed.
- 10. No long-press auto-repeat except specified Memory actions.
- 11. Inline errors; preserve editable expression; no modal dialogs.
- 12. Editing immediately clears error and updates preview.
- 13. TalkBack labels and system font scaling.
- 14. Distinguish number and command controls by both color and shape.
- 15. On serious error preserve expression if possible and keep C available.
- 16. Report Memory/Settings persistence failures truthfully.
- 17. Keep calculator usable if History DB is corrupt; do not wipe DB automatically.
- 18. Settings auto-save on Back; no Save button.
- 19. Confirm Reset Settings and never erase Memory/History.
- 20. Preserve Settings/Memory/History on app update with migrations.
- 21. Parser, rounding, and error Unit Tests pass before physical-device testing.
- 22. Any incorrect numeric result must be fixed before publication.
- 23. Fix UI bugs impairing readability or touch; defer minor cosmetics.
- 24. Test on actual device plus Android 9+ emulators/automated tests as feasible.
- 25. Prioritize minor post-release bugs for future updates without erasing data.
- 26. Deliver complete Android Studio project and beginner documentation.
- 27. Prefer Gradle CLI if Windows 10 / 4 GB RAM struggles with Android Studio.
- 28. Prepare APK, signed AAB and safe signing-key procedures when environment permits.
- 29. Follow actual Play Console testing/release rules, including closed testing if required.
- 30. Delivery requires verified build, device testing, source/APK/AAB/docs; Play approval is a separate milestone.

## Final clarifications (closed)
1. Partial Preview: evaluate only when unambiguous, otherwise hide/suspend, without History insertion.
2. Overlong Paste: validated truncation with warning; never split scientific notation.
3. Unit converter: hide unfinished features in v0.1.0; retain future requirements.

## Source gaps, not new questions
The exact verbatim wording of Grill Me rounds 1–50 was not recovered. Consequently, do not claim to have a byte-for-byte transcript. The earlier approved SRS mentions context-sensitive percent, implicit multiplication, unary minus, and precedence, but this bundle does not recover every exact edge-case truth table. For cases where no exact rule is documented, use `NEEDS-VERIFICATION` rather than silently inventing a rule. This does **not** reopen the interview or authorize changing any existing approved example.
