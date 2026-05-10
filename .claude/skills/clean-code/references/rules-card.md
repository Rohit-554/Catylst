# Clean Code Rules Card — Action Directives

## Before You Write Anything

1. **List the domain vocabulary.** Write down the problem's nouns (entities) and verbs (actions). Use ONLY these words in your code — not programming-generic synonyms.
2. **Place the entry point function FIRST in the file.** Then the class. Then helpers. Then primitives. The reader hits "what" before "how".

## Naming — For Every Name You Choose, Ask:

- **"Does this name describe WHY, or HOW?"** If HOW (mechanism), rename to WHY (intent). `markAsVisited` = why we do it. `setToZero` = how we do it. Always choose WHY.
- **"Is this word from the problem's domain, or from generic programming?"** If generic, replace with domain. `isWithinOcean` = domain. `isWithinBounds` = generic. Always choose domain.
- **"Can a reader understand this without reading the implementation?"** If no, the name is wrong. Rename until yes.
- Classes = nouns (`Customer`, `OrderValidator`). Never `Manager`, `Processor`.
- Functions = verbs (`deletePage`, `saveUser`). Booleans = predicates (`isValid`, `canEdit`).
- One word per concept across the codebase — pick `get` or `fetch`, not both.
- No abbreviations, no `data`/`info`/`temp`, no single-letter vars outside tiny loops.

## Functions — For Every Function You Write, Check:

- **"Does this function do MORE than one thing?"** Extract a helper with a name that is NOT a restatement. If you can, the original did too much.
- **"Are all lines at the same abstraction level?"** If you see a high-level call next to a low-level operation (e.g., `calculateTotal()` next to `array[i] = 0`), extract the low-level part.
- **"Can I name each directional/conditional branch?"** If a function has `row - 1`, `row + 1`, `col - 1`, `col + 1` inline, extract `exploreNorth()`, `exploreSouth()`, `exploreWest()`, `exploreEast()`.
- 5-20 lines max. Max indent level 2. 0-2 parameters (wrap more in an object).
- No side effects — the function does what the name says, nothing hidden.
- No boolean flags — split into two named functions.
- **Encapsulate shared state in a class** to eliminate repeated parameters from every signature.

## File-Level Stepdown — Enforce This Order:

```
1. Entry point function (the "what")
2. Class definition (the "how", organized internally by stepdown)
   2a. Public methods (high-level story)
   2b. Private methods called by public (mid-level)
   2c. Primitive helpers (low-level)
```

## Comments

- **No section-header comments** (`// --- PIN Validation ---`, `// ========== Setup ==========`). Use blank lines between concept groups instead. If a file needs headers to stay navigable, the class has too many responsibilities — split it.

## Classes

- **"Does this class have more than one reason to change?"** If yes, split it.
- High cohesion — methods use most instance variables. If a method ignores most fields, it belongs elsewhere.
- Law of Demeter — never chain: `a.getB().getC().doThing()`. Tell, don't ask.

## Error Handling

- Exceptions, not error codes. Extract try/catch into its own function.
- Never return null — throw or use Optional. Never pass null — validate at boundaries.
- **Validation functions must not return nullable strings** (`String?`). Use a sealed Result type (`Valid`/`Invalid(reason)`) or throw. Nullable-string-as-error is a disguised null return.
- Fail fast — detect problems at entry, not deep in the call stack.

## The Litmus Test

Read every call site aloud. It must sound like an English sentence:
```
"if is land, submerge island"     — GOOD: reads like prose
"if check, process"               — BAD: meaningless
```
If it doesn't read like prose, rename until it does.
