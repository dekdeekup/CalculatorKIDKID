# Software Requirements Specification (SRS)
## Calculator Kid Kid / เครื่องคิดเลข คิดคิด — Version 0.1.0

**Approval:** Approved by the product owner on October 30, 2026. The additional 30/30 interview topics and all 3 final clarifications are closed. No new requirements interview is authorized.

**Android package:** `com.kitec.calculatorkidkid`

**Completeness and provenance:** This document faithfully translates the consolidated Thai SRS available in the conversation, including summarized earlier decisions. It is **not a verbatim transcript** of the original Grill Me rounds 1–50, which were not fully available. Do not invent missing detailed semantics. Mark material gaps as `NEEDS-VERIFICATION` and report them clearly. Preserve the original, already-approved behavior wherever specified.

## 1. Scope and product goals

- Native Android calculator implemented in Kotlin; works entirely offline, is free of ads, requires no account, does not request the `INTERNET` permission, and does not collect personal information.
- Minimum supported OS: Android 9 (API level 28); target devices have at least 2 GB RAM.
- Target installed application size: approximately 30 MB or less; target launch time: within 2 seconds. Both are performance goals to be measured, **not pre-verified guarantees**.
- Version 0.1.0 prioritizes a fully working calculator. Hide any unfinished unit-conversion functionality; preserve converter requirements for a future version and do not add nonfunctional placeholder pages.
- Launch directly into the calculator. Prefer a small, responsive, simple native UI over animations and visual effects.
- App icon concept: gold calculator with arrows. Support Thai and English, System/Light/Dark themes, and locale-aware numeric separators.
- Make About and Privacy information available offline inside the app. Google Play distribution also requires a publicly reachable privacy-policy URL.
- Disable Android Auto Backup. Do not run background services. Sound and haptic feedback default to off. Any local crash logging must avoid personal information.

## 2. User interface, responsiveness, accessibility

- Portrait: show the expression, result, and all calculator buttons on one screen; scroll long text only. With large system fonts, limited scrolling is acceptable where necessary.
- Landscape: rearrange the button layout appropriately.
- Support narrow screens, Android split-screen/multi-window, and system font scaling; every button must remain accessible.
- Provide meaningful TalkBack labels. Distinguish digit and command buttons by both color **and shape**, not color alone.
- Process rapid key presses in order without dropping input events.
- Long press must not auto-repeat except for specifically defined Memory actions.
- Perform expensive calculations off the main/UI thread, with a busy indication if needed. Never let an obsolete asynchronous preview overwrite a newer expression; the exact cancellation/coalescing strategy is an implementation choice.
- Display calculation errors inline near the result, never in modal dialogs. Editing an expression clears the current error immediately and refreshes preview. For serious errors, preserve the expression where possible and allow the user to press C to clear it.

## 3. Expression editing and parser

- Buttons: `0`–`9`, decimal point, `+`, `−`, `×`, `÷`, `%`, `±`, parentheses, `=`, `C`, and Backspace.
- Support cursor movement, selection, insertion and replacement at any position, hardware keyboard input and shortcuts, and copying selected expression text.
- Undo and Redo each support up to 50 editing steps. `C` clears the entire expression and preview. Editing actions should have appropriate undo transaction boundaries.
- Limits: maximum expression length 500 characters; maximum 200 digits per operand; maximum parenthesis nesting depth 50.
- Parser supports arithmetic precedence, unary minus, nested parentheses, implicit multiplication, and context-sensitive percent. **Exact precedence or percent/unary/implicit semantics not fully stated here must be verified against the original SRS before behavior is changed.**
- Incomplete expressions remain editable. Show a partial preview only when the result is unambiguous; otherwise suspend/hide preview. Incomplete preview evaluations do not create History entries.
- Mandatory examples:
  - Expression `12+`, then `=`: display `12`, leave expression `12+` unchanged, and **do not** save History.
  - Expression `2×(3+4`, then `=`: automatically close `)`, evaluate to `14`, save History. Undo must restore the unclosed expression.
  - Expression `2×(3+`, then `=`: do not save History.
  - Repeated `=` with no expression change: do not create duplicate History entries.

## 4. Numeric engine and result formatting

- Use `BigDecimal` as the numeric source of truth, with at least 50 significant digits of working precision. Do not use binary floating-point `Double` as the authoritative value.
- User-configurable displayed decimal places: 0–20, default 10. Supported rounding modes: `HALF_UP` (default), `UP`, and `DOWN`.
- Continue all calculations using the full internal value, never a value rounded for display.
- Use scientific notation when the displayed value exceeds 15 significant digits or when a very small number has six leading zeroes after the decimal point; verify exact threshold edge cases against the original specification if available.
- Supported exponent range: ±1000. Nonzero overflow/underflow is an error and must preserve the expression.
- Apply display rounding before selecting scientific-notation formatting according to the previously approved rules. If a nonzero value rounds to zero for display, show `0` but retain the true internal value for subsequent calculations.
- Strip unnecessary trailing fractional zeroes; never display negative zero (`-0`).
- Copying a result: for terminating decimal values, copy the true internal value; for nonterminating decimals, round according to the current settings. Clipboard numeric format uses a dot (`.`) decimal separator and **no** grouping separators.
- Division by zero or malformed expressions must not crash or freeze the app.

## 5. Paste and clipboard

- On Paste, remove whitespace, normalize Thai numerals and supported operator glyphs, and accept locale-correct grouping separators. Reject input containing any invalid characters **atomically**, leaving the existing expression untouched.
- Enforce the 500-character limit **after normalization and in combination with the existing expression**. If the inserted expression would exceed the limit, insert only a validated prefix and display the previously approved Thai partial-paste warning (Unicode escape sequence `\u0E27\u0E32\u0E07\u0E02\u0E49\u0E2D\u0E04\u0E27\u0E32\u0E21\u0E40\u0E1E\u0E35\u0E22\u0E07\u0E1A\u0E32\u0E07\u0E2A\u0E48\u0E27\u0E19`, English meaning "Only part of the text was pasted").
- Truncating part of an ordinary numeric literal is allowed with the warning, but **never truncate inside scientific-notation syntax**. Automatically closing parentheses is permitted only if space remains and the result is valid.
- One successful Paste operation is one Undo step. Rely on Android's system clipboard notifications; do not create a separate clipboard notification.

## 6. Memory registers M1 and M2

- Provide two independently persistent registers, M1 and M2. Store the complete internal-precision value.
- Tapping an empty register stores the current preview value, including an evaluable partial-expression preview.
- Tapping an occupied register inserts its value at the cursor or replaces the selected text; insertion is one Undo step.
- Mandatory insertion examples:
  - Existing `2` + M1 containing `3` → `23`.
  - Existing `2` + M1 containing `-3` → `2(-3)`.
  - Existing `12.` + M1 containing `3` → `12.3`.
  - Existing `0` + M1 containing `5` → `5`.
- Wrap negative inserted values in parentheses as required by context. When insertion exceeds the expression limit, truncate only to a valid representation, warn, and never split scientific notation.
- Long-pressing a register immediately deletes its stored value and **cannot be undone**. Settings changes may reformat the displayed register value but must not modify the stored numeric value.
- If persisting Memory fails, notify the user and never falsely indicate successful durable storage.

## 7. Calculation history

- Session History contains the 20 most recent items and is cleared at the beginning of a genuinely new session.
- Permanent History uses Room/SQLite, sorted newest first. Save only successfully evaluated `=` operations. Repeated `=` with unchanged expression must not add duplicates.
- Default retention: 30 **local-calendar** days and a maximum of 300 entries. User options: 7/30/90 days and 100/300/500 entries.
- Prune expired/excess permanent entries on application opening or History opening.
- Deleting one entry takes effect immediately. Only the **most recent** deletion can be undone, and only until the user leaves the History screen.
- Clear All requires confirmation and cannot be undone.
- If the History database is corrupted or cannot be read, the calculator must continue to function, show an error, and **must not automatically delete or reset the database**.
- Application upgrades preserve existing records; use explicit database migrations when necessary.

## 8. Settings

- Settings include Thai/English language, System/Light/Dark theme, displayed decimal places 0–20 (default 10), rounding `HALF_UP`/`UP`/`DOWN`, History retention choices, and locale-aware number formatting.
- Changes save automatically. There is no Save button; navigating Back from Settings must not silently discard changes.
- Reset Settings requires confirmation and resets **Settings only**, never M1/M2 or History.
- Persistence failures must be reported and must not be presented as successful durable saves.

## 9. Activity lifecycle and navigation

- Returning from another app while the same process survives: preserve expression, cursor, preview, Undo/Redo, and current page.
- Rotating the screen in the same process: preserve all state, including History scroll position and Undo/Redo.
- If the process is killed and the app is relaunched: start a new expression and Session History; persisted Settings, Memory, and Permanent History remain.
- Swiping the app away from Recents and relaunching: start a new session even if the process happens to survive.
- Activity recreation for a reason other than rotation: preserve the expression but clear Undo/Redo according to the existing rule.
- Back from the calculator returns to the previous app without clearing the expression. Back from History preserves the expression but invalidates History deletion Undo. Back from Settings auto-saves changes.

## 10. Verification, delivery, and release acceptance

- Parser, rounding, and error-handling Unit Tests must pass **before** testing on the physical device.
- Every discovered incorrect numeric result, even in a rare edge case, must be fixed before publication.
- Fix UI defects that obstruct reading or pressing controls before publication; minor cosmetic issues may be deferred.
- Test on the user's Ulefone Armor 30 Pro and check Android 9+ compatibility further using emulators or automated tests as resources permit.
- Prioritize noncritical post-release defects for later updates without erasing user data.
- Development target: Windows 10 PC with 4 GB RAM. If Android Studio is too resource-intensive, use Gradle CLI while keeping the project importable into Android Studio.
- Deliver: complete Android Studio source project, beginner-friendly instructions, a test APK, and a signed AAB for Google Play **when the SDK/build/signing environment is available**. Establish a safe procedure for creating, storing, and backing up the upload signing key.
- Follow the actual Google Play Console requirements applicable to the developer account, including Closed Testing if required. Internal Testing is not equivalent to Production and does not guarantee that an inactive developer account remains open.
- Version 0.1.0 development delivery is complete only after a real successful build, physical-device testing, and delivery of the source, APK, AAB, and documentation. Google Play approval/publication is a separate milestone.

## 11. Approved additional interview decisions — 30/30

| ID | Approved decision |
|---|---|
| 01 | Launch directly into calculator. |
| 02 | Portrait shows expression, result, and controls on one screen. |
| 03 | Support enlarged system font sizes. |
| 04 | Support landscape orientation. |
| 05 | Prioritize a small and fast application. |
| 06 | Preserve state when returning from another app in the same process. |
| 07 | Preserve the order of rapid key presses. |
| 08 | Support narrow screens and split-screen. |
| 09 | Run expensive work off the UI thread. |
| 10 | No automatic long-press repeat except defined Memory actions. |
| 11 | Inline errors; no modal error dialogs. |
| 12 | Editing clears errors and updates preview. |
| 13 | TalkBack support and system font scaling. |
| 14 | Differentiate button types by color and shape. |
| 15 | Preserve expression on serious errors; C remains available. |
| 16 | Report Memory/Settings save failures. |
| 17 | Never auto-delete a corrupt History database. |
| 18 | Auto-save Settings. |
| 19 | Reset Settings without deleting Memory/History. |
| 20 | Preserve data on upgrade and use migrations. |
| 21 | Pass Unit Tests before physical-device testing. |
| 22 | Fix all incorrect calculations before release. |
| 23 | Fix UI issues that impair usability. |
| 24 | Test Android 9+ as resources allow. |
| 25 | Fix minor bugs in later updates without deleting user data. |
| 26 | Deliver the full source project and beginner guide. |
| 27 | Use Gradle CLI when the 4 GB RAM computer struggles. |
| 28 | Prepare APK, AAB, and signing-key handling guidance. |
| 29 | Comply with actual Google Play requirements. |
| 30 | Development delivery requires build, tests, and files; Play approval is separate. |

## 12. Three final clarifications — CLOSED

1. **Partial Preview:** Show a preview only if the partial expression can be evaluated unambiguously; otherwise suspend/hide it. Do not create History for partial previews.
2. **Paste beyond 500 characters:** Insert only a valid truncated portion, warn, and never truncate within scientific notation.
3. **Unit converter:** Hide incomplete conversion functionality in v0.1.0; do not expand the release scope.

## 13. Instructions for Codex

- **The initial source ZIP is incomplete and unverified.** Do not assume it builds or satisfies this SRS.
- Inspect actual project files before editing. Avoid unnecessarily rewriting code that is already correct.
- Treat this SRS as the acceptance checklist; implement automated parser, rounding, persistence, and edge-case tests.
- Do not invent numeric semantics for underspecified grammar. Flag them as `NEEDS-VERIFICATION`, document the impact, and ask for targeted clarification only when genuinely blocking correctness.
- **Do not embed signing keys, passwords, or other secrets in source code, ZIP files, or commits.** The owner must manage signing credentials securely.
- Do not claim APK/AAB builds, on-device tests, or Google Play approval unless actually completed and verified.
- User-facing application strings must still support Thai and English. The fact that this technical SRS is in English does **not** mean the application should be English-only.
