# Calculator Kid Kid — Single Codex Handoff

Official Thai app name (only Thai prose in the handoff): เครื่องคิดเลข คิดคิด

This archive contains all **currently recoverable** requirements and the initial Kotlin Android project in one ZIP. It is a consolidation, not a change request.

## Reading order
1. `Requirements/SRS_CalculatorKidKid_v0.1.0_EN.md` — consolidated technical requirements.
2. `Requirements/APPROVED_DECISIONS_AND_PROVENANCE.md` — 30 approved answers, exact examples, final clarifications, and evidence limits.
3. `AndroidProject/CalculatorKidKid/` — existing source to inspect and continue.

## Non-negotiable handoff rules
- ChatGPT-to-Codex communication and all technical documentation must be in English. Thai text is permitted here only for the official Thai app name. This does **not** remove the requirement for Thai-language UI or Thai digit input.
- Translation only; **no requirements changes**. Preserve all approved behaviors.
- Do not infer that missing verbatim interview rounds were recovered. Do not fabricate missing semantics.
- Inspect actual source, implement incrementally, write automated tests, and report real build/test status.
- Current source is incomplete, unbuilt and unverified. This archive contains no APK or AAB.
- Never place private signing keys or credentials in the project or handoff archive.
