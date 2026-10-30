# New percentage clarification — October 30, 2026

Source: owner response in this development session. This is **new clarification**, not recovered historical approval text. The original SRS and provenance documents remain unchanged.

Use basic consumer-calculator context-sensitive percentages for the specified simple forms:

| Expression | Result |
|---|---|
| `10%` | `0.1` |
| `200+10%` | `220` |
| `200-10%` | `180` |
| `200*10%` | `20` |
| `200/10%` | `2000` |
| `200+10%+10%` | `242` |
| `200-10%+5%` | `189` |
| `200+10%-10%` | `198` |
| `100-20%+5%` | `84` |

For addition/subtraction, calculate the percentage relative to the accumulated left-hand value. Apply each change sequentially to the updated result. For multiplication/division, interpret the percentage operand as its value divided by 100.

Complex percentage expressions involving parentheses, mixed precedence, nested/repeated percentages, a percentage on the left, or other forms beyond this clarification remain `NEEDS-VERIFICATION`. The engine rejects such percentage expressions with an editable inline error rather than returning an assumed result. Ordinary expressions without percentages retain their existing parser grammar.

The automated acceptance suite covers every supplied example and verifies rejection of representative unresolved forms. This clarification does not authorize changes to any other approved requirement.
