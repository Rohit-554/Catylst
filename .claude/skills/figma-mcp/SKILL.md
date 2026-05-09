---
name: figma-mcp
description: Implement a Figma design into production-ready Compose Multiplatform code. Use when translating Figma screens or components into Kotlin Compose code with strict design system compliance, component library reuse, and pixel-perfect accuracy through a structured 5-pass workflow.
argument-hint: "[screen or component name]"
---

Implement the Figma design for **$ARGUMENTS** into production-ready Compose Multiplatform code for the Slotli app.

<objective>
Implement a Figma design into production-ready Compose Multiplatform code for the Slotli app. You will use the Figma MCP tools to inspect the design, then translate it into pixel-perfect Kotlin Compose code that strictly adheres to the existing design system, component library, and architectural patterns.

This skill exists because manually translating Figma designs is error-prone and time-consuming. The goal is a systematic, multi-pass approach that produces code indistinguishable from what a senior developer would write after carefully studying the Figma file — reusing every existing component and design token possible, and only creating new ones through the proper channels.
</objective>

<context>
This is the Slotli app — a Kotlin Multiplatform (KMP) Compose project targeting Android and iOS.

**Architecture**: MVVM + Presenter Pattern (BaseViewModel → Presenter → Repository)
**Base package**: `app.slotli.de.slotli`
**Design system location**: `composeApp/src/commonMain/kotlin/app/slotli/de/slotli/presentation/theme/`
**Component library location**: `composeApp/src/commonMain/kotlin/app/slotli/de/slotli/presentation/screens/components/`
**Screens location**: `composeApp/src/commonMain/kotlin/app/slotli/de/slotli/presentation/screens/`

Read these files FIRST before doing anything:
- @.claude/ARCHITECTURE.md — for architecture patterns
- @.claude/CODE_STYLE.md — for coding conventions
- @composeApp/src/commonMain/kotlin/app/slotli/de/slotli/presentation/theme/Color.kt — all design system colors
- @composeApp/src/commonMain/kotlin/app/slotli/de/slotli/presentation/theme/Typography.kt — all typography styles
- @composeApp/src/commonMain/kotlin/app/slotli/de/slotli/presentation/theme/Spacing.kt — spacing tokens, border widths, elevations
- @composeApp/src/commonMain/kotlin/app/slotli/de/slotli/presentation/theme/Dimensions.kt — corner radii, icon sizes, avatar sizes, button heights, animation durations
- @composeApp/src/commonMain/kotlin/app/slotli/de/slotli/presentation/theme/Theme.kt — theme composition and AppTheme access object

Read ALL existing components to know what is already built:
- @composeApp/src/commonMain/kotlin/app/slotli/de/slotli/presentation/screens/components/ — scan every file in this directory
</context>

<workflow>
You MUST follow this multi-pass workflow. Do not skip passes. Each pass builds on the previous one.

## PASS 1: Design Audit & Inventory
Goal: Fully understand the Figma design before writing any code.

1. Use the Figma MCP tools to fetch the design for the screen/component the user specifies
2. Create a complete inventory of every visual element:
   - All colors used (hex values)
   - All text styles (font size, weight, line height, letter spacing)
   - All spacing values (padding, margins, gaps)
   - All corner radii
   - All icons and images
   - All interactive states (hover, pressed, disabled, focused)
   - All shadow/elevation values
   - Layout structure (rows, columns, stacks, scroll behavior)
3. Document the component hierarchy — what is a screen, what is a section, what is a reusable component

**Output of Pass 1**: Present the full inventory to yourself as a structured checklist. Do not write any code yet.

## PASS 2: Design System Compliance Check
Goal: Map every design element to existing design tokens or flag gaps.

For EACH element from the Pass 1 inventory:

### Colors
1. Compare every color hex value against `Color.kt` (AppColors, DarkColors, SemanticColors)
2. If a color EXISTS in the design system → note the token name to use (e.g., `AppColors.Primary`)
3. If a color DOES NOT EXIST → **STOP and flag it to the user**:
   - Show the hex value and where it appears in the design
   - Ask: "This color `#XXXXXX` is not in the design system. Should I add it as `[suggested semantic name]` to Color.kt? Or is it a one-off that should use a different existing token?"
   - Only proceed after the user confirms
   - If approved, add the color to the appropriate section of Color.kt (AppColors for base colors, SemanticColors for semantic usage, DarkColors if dark variant is provided)

### Typography
1. Compare every text style against `Typography.kt` (MaterialTheme.typography and AppTextStyles)
2. If a style matches → note the token name
3. If a style does NOT match → flag it: "This text style (size: Xsp, weight: W, lineHeight: Ysp) doesn't match any existing typography token. Should I add it to AppTextStyles?"

### Spacing & Dimensions
1. Compare all spacing values against `Spacing.kt` grid values and `Dimensions.kt`
2. Flag any values that don't align with the 8dp grid or existing tokens
3. Suggest the nearest existing token or ask to add a new one

### Icons & Assets
1. Check `composeApp/src/commonMain/composeResources/drawable/` for existing assets
2. List any new icons/images needed from the Figma file
3. For icons: check if Material Icons Extended has the icon before requesting an export

**Output of Pass 2**: A complete mapping table (design element → design token) and a list of all flagged items with user decisions.

## PASS 3: Component Library Audit
Goal: Maximize reuse of existing components before creating anything new.

1. Review EVERY existing component in `presentation/screens/components/`:
   - `AppButton.kt` — Primary, Secondary, Disabled variants with icons
   - `AppBottomNav.kt` — 5-item grid navigation
   - `AppTextField.kt` — Text input with error states
   - `AppOtpTextField.kt` — Circle-based OTP input
   - `AppSearchTextFields.kt` — Search input variant
   - `AppSwitch.kt` — Custom toggle switch
   - `AppSegmentButton.kt` — Segmented filter buttons
   - `AppRangeSlider.kt` — Range slider
   - `AppDialog.kt` — Dialog with hero icon
   - `AppBottomSheet.kt` — Bottom sheet with drag handle
   - `OnboardingIndicator.kt` — Page dots
   - `AuthTopAppBar.kt` — Auth screen app bar

2. For each UI element in the Figma design, determine:
   - **Exact match**: Existing component can be used as-is → use it
   - **Variant needed**: Existing component needs a new variant/parameter → extend it
   - **New component needed**: Nothing similar exists → will create new

3. For new components, classify them:
   - **Small/reusable** (used across multiple screens like cards, chips, badges, list items) → create in `presentation/screens/components/`
   - **Feature-specific** (used only within one feature's screens) → create in `presentation/screens/[feature]/components/`
   - **Screen-level** (the full screen composable) → create in `presentation/screens/[feature]/`

**Output of Pass 3**: A component plan listing every component to use/extend/create with its target location.

## PASS 4: Implementation
Goal: Write production-quality code.

### Component Creation Order
Build bottom-up — smallest components first, then compose them into larger ones:
1. Add any approved new design tokens (colors, typography, spacing)
2. Create new small reusable components in `components/`
3. Create feature-specific components in `screens/[feature]/components/`
4. Create/modify the screen composable in `screens/[feature]/`
5. Create/modify ViewModel and state classes following BaseViewModel pattern
6. Register ViewModel in Koin module if new
7. Add navigation route if new screen

### Code Standards (MANDATORY)
- Follow `App[ComponentName]` naming convention for all new components
- Use `AppTheme.colors`, `AppTheme.spacing`, `AppTheme.dimensions` — NEVER hardcode values
- Use `MaterialTheme.typography` or `AppTextStyles` — NEVER hardcode text styles
- Use named parameters for all Compose function calls
- Keep composables small and focused (< 300 lines per file)
- Add **extensive `@Preview` functions** for all new components (see Preview Requirements below)
- Use `Modifier` as first optional parameter in component signatures
- Follow immutable state pattern with data classes
- Use `updateState {}` for state mutations in Presenters

### Preview Requirements (MANDATORY)
Every new or modified composable MUST have extensive previews. Previews are first-class citizens — they serve as living documentation, visual regression tests, and design verification tools.

**For every component, create previews covering:**

1. **Default state** — The component with minimal/default parameters
2. **All visual variants** — Every distinct appearance (e.g., Primary, Secondary, Outlined for buttons)
3. **All interactive states** — Enabled, Disabled, Pressed, Focused, Loading (where applicable)
4. **Content extremes** — Short text, long/wrapping text, empty content, max content
5. **Dark theme** — Wrap in `SlotliTheme(darkTheme = true)` to verify dark mode
6. **RTL layout** — For components with directional layout, add an RTL preview
7. **Size variations** — If the component adapts to different sizes, preview each

**Preview naming convention:**
```kotlin
@Preview(name = "Default")
@Composable
private fun App[ComponentName]Preview() { ... }

@Preview(name = "Disabled")
@Composable
private fun App[ComponentName]DisabledPreview() { ... }

@Preview(name = "Dark Theme")
@Composable
private fun App[ComponentName]DarkPreview() { ... }

@Preview(name = "Long Text")
@Composable
private fun App[ComponentName]LongTextPreview() { ... }
```

**Preview grouping:** Use `@Preview(group = "App[ComponentName]")` to group related previews together.

**For screen composables:** Create a preview with mock data that represents a realistic populated state, not just empty/loading states. Include a second preview showing the loading/empty state.

### File Organization
```
screens/[feature]/
├── [Feature]Screen.kt           # Screen composable
├── [Feature]ViewModel.kt        # ViewModel (extends BaseViewModel)
├── [Feature]Presenter.kt        # Presenter (extends Presenter)
├── [Feature]UiState.kt          # State data class
├── [Feature]UiEvents.kt         # Sealed class of UI events
└── components/                  # Feature-specific components
    ├── [Feature]Card.kt
    ├── [Feature]Header.kt
    └── ...
```

## PASS 5: Visual Verification
Goal: Ensure pixel-perfect accuracy.

1. Re-fetch the Figma design using MCP tools
2. Compare your implementation element-by-element against the design:
   - Are all colors correct and using design system tokens?
   - Are all text styles correct?
   - Are all spacing values correct?
   - Are all corner radii correct?
   - Are all shadows/elevations correct?
   - Is the layout structure correct (alignment, distribution, overflow)?
   - Are all interactive states handled?
3. If ANY discrepancy is found, fix it immediately
4. Document any intentional deviations (e.g., accessibility improvements) and explain why
</workflow>

<constraints>
### ABSOLUTE RULES — Never violate these:

1. **NEVER hardcode colors** — Every color must reference `AppColors`, `SemanticColors`, or `DarkColors` from Color.kt. If the color doesn't exist, flag it and get approval before adding it.

2. **NEVER hardcode dimensions** — Use `AppTheme.spacing`, `AppTheme.dimensions`, or add new tokens. Raw `dp` values are only acceptable in component-internal layout calculations that don't represent design tokens.

3. **NEVER hardcode text styles** — Use `MaterialTheme.typography` or `AppTextStyles`. No inline `TextStyle()` with raw values.

4. **NEVER duplicate existing components** — If `AppButton` already does what you need, use it. If it needs a small extension, modify it. Only create new components when truly necessary.

5. **NEVER create a component without checking the library first** — Pass 3 is mandatory. Skipping it creates technical debt.

6. **ALWAYS flag design system gaps** — If something in the Figma doesn't map to existing tokens, ask the user. Never silently add tokens or silently use raw values.

7. **ALWAYS follow the file organization pattern** — Small reusable components go in `components/`, feature components go in `screens/[feature]/components/`, screens go in `screens/[feature]/`.

8. **ALWAYS use the App prefix** — New components must be named `App[ComponentName].kt` to maintain consistency with AppButton, AppTextField, AppSwitch, etc.

9. **ALWAYS preserve existing component APIs** — When extending a component, add new optional parameters with defaults. Never break existing call sites.

10. **ALWAYS use Koin for dependency injection** — New ViewModels must be registered in the viewModelModule.

11. **ALWAYS write extensive previews** — Every composable must have previews covering: default state, all variants, disabled/loading states, content extremes (short/long text), and dark theme. A component without thorough previews is not complete.
</constraints>

<design_system_flagging>
When you encounter something in the Figma that doesn't match the design system, use this exact format to flag it:

```
DESIGN SYSTEM GAP DETECTED

Element: [What it is — e.g., "Background color of the promo card"]
Figma value: [The exact value — e.g., "#FF6B35"]
Closest existing token: [Nearest match — e.g., "AppColors.Warning (#FB8C00)"]
Location in design: [Where in the Figma — e.g., "Home screen → Featured section → Promo card"]

Options:
A) Add as new token: `AppColors.[SuggestedName]` = Color(0xFFFF6B35)
B) Use closest existing token: `AppColors.Warning`
C) This is a one-off, use raw value with comment

Which option should I use?
```

Do NOT proceed past this point until the user responds. Collect ALL gaps and present them together if there are multiple, so the user can make decisions in batch.
</design_system_flagging>

<component_creation_guidelines>
When creating NEW components:

### Small Reusable Components (→ `components/`)
Criteria: Will be used across 2+ features/screens
Examples: Cards, chips, badges, list items, section headers, rating displays, avatar groups

Template:
```kotlin
/**
 * [Description of component and its purpose]
 * Pixel-matched to Figma design: [frame name/link if available]
 */
@Composable
fun App[ComponentName](
    modifier: Modifier = Modifier,
    // Required parameters first
    // Optional parameters with defaults last
) {
    // Implementation using only design system tokens
}

object App[ComponentName]Defaults {
    // Default values as constants if needed
}

// --- Extensive Previews ---

@Preview(name = "Default", group = "App[ComponentName]")
@Composable
private fun App[ComponentName]Preview() {
    SlotliTheme {
        App[ComponentName](/* default params */)
    }
}

@Preview(name = "Variant B", group = "App[ComponentName]")
@Composable
private fun App[ComponentName]VariantBPreview() {
    SlotliTheme {
        App[ComponentName](/* variant params */)
    }
}

@Preview(name = "Disabled", group = "App[ComponentName]")
@Composable
private fun App[ComponentName]DisabledPreview() {
    SlotliTheme {
        App[ComponentName](/* disabled state params */)
    }
}

@Preview(name = "Long Text", group = "App[ComponentName]")
@Composable
private fun App[ComponentName]LongTextPreview() {
    SlotliTheme {
        App[ComponentName](/* long/wrapping text params */)
    }
}

@Preview(name = "Dark Theme", group = "App[ComponentName]")
@Composable
private fun App[ComponentName]DarkPreview() {
    SlotliTheme(darkTheme = true) {
        App[ComponentName](/* default params */)
    }
}
```

### Feature-Specific Components (→ `screens/[feature]/components/`)
Criteria: Only used within one feature but extracted for readability
Examples: A specific card layout for a feature, a custom header for one screen

### Screen Composables (→ `screens/[feature]/`)
Criteria: The top-level screen that is a navigation destination
Must follow: ViewModel + Presenter + UiState + UiEvents pattern from ARCHITECTURE.md
</component_creation_guidelines>

<success_criteria>
The implementation is complete when ALL of these are true:
1. Every visual element from the Figma design is implemented in code
2. Every color, typography, spacing, and dimension value uses a design system token (no hardcoded values)
3. Every reusable element from the existing component library is reused (not recreated)
4. All new components follow the `App[Name]` naming convention and are in the correct directory
5. All design system gaps have been flagged to and resolved by the user
6. The Pass 5 visual verification confirms pixel-perfect accuracy
7. New ViewModels are registered in Koin, new routes are added to navigation
8. Code follows all patterns in ARCHITECTURE.md and CODE_STYLE.md
9. All new composables have **extensive** @Preview functions covering: default state, all variants, disabled/loading states, content extremes, and dark theme
10. Screen composables have previews with realistic mock data (populated state) and loading/empty states
</success_criteria>

<verification>
After implementation, verify by:
1. Re-reading the Figma design via MCP and comparing every element
2. Grep for any hardcoded color hex values (`Color(0x`) outside of Color.kt — there should be NONE
3. Grep for any hardcoded dp values that should be design tokens
4. Confirm all new files are in the correct package/directory
5. Confirm all new ViewModels are registered in the Koin module
6. Confirm navigation routes are properly connected
7. Check that no existing component call sites were broken by modifications
8. Verify every new composable has extensive @Preview functions (default, variants, disabled, long text, dark theme at minimum)
9. Confirm screen previews include both populated and loading/empty states
</verification>