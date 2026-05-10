# Functions

## The Golden Rule: Code Should Read Like Well-Written Prose

The ultimate goal is code that a non-programmer could almost follow. Every function name is a sentence fragment. Every call site reads like a paragraph. When you read clean code top-down, it tells you a story.

```kotlin
// The entry point reads like a sentence:
fun countIslands(ocean: Array<CharArray>): Int {
    if (ocean.isEmpty()) return 0
    return IslandCounter(ocean).count()
}

// The class encapsulates context, so every method reads cleanly:
class IslandCounter(private val ocean: Array<CharArray>) {

    private val totalRows = ocean.size
    private val totalCols = ocean[0].size

    fun count(): Int {
        var islandCount = 0

        for (row in ocean.indices) {
            for (col in ocean[0].indices) {
                if (isLand(row, col)) {
                    islandCount++
                    submergeIsland(row, col)
                }
            }
        }

        return islandCount
    }

    private fun submergeIsland(row: Int, col: Int) {
        if (!isLand(row, col)) return

        markAsVisited(row, col)

        exploreNorth(row, col)
        exploreSouth(row, col)
        exploreWest(row, col)
        exploreEast(row, col)
    }

    private fun exploreNorth(row: Int, col: Int) = submergeIsland(row - 1, col)
    private fun exploreSouth(row: Int, col: Int) = submergeIsland(row + 1, col)
    private fun exploreWest(row: Int, col: Int) = submergeIsland(row, col - 1)
    private fun exploreEast(row: Int, col: Int) = submergeIsland(row, col + 1)

    private fun isWithinOcean(row: Int, col: Int): Boolean =
        row in 0 until totalRows && col in 0 until totalCols

    private fun isLand(row: Int, col: Int): Boolean =
        isWithinOcean(row, col) && ocean[row][col] == '1'

    private fun markAsVisited(row: Int, col: Int) {
        ocean[row][col] = '0'
    }
}
```

Read `count()` out loud: "For each cell in the ocean, if it is land, increment the island count and submerge that island." Read `submergeIsland`: "If it's not land, stop. Otherwise, mark as visited, then explore north, south, west, east."

You don't need comments. The code IS the comment.

Notice how encapsulating `ocean` in the class eliminates it from every method signature. `isLand(row, col)` instead of `isLand(ocean, row, col)`. The class gives shared context a home, and every function gets cleaner because of it.

## Core Principles

1. **Reads Like English** - Function names form sentences; call sites form paragraphs
2. **Small** - Ideally 5-20 lines; one screen, one glance
3. **Do One Thing** - Each function does one thing, does it well, does it only
4. **One Level of Abstraction** - All statements at the same conceptual level
5. **Descriptive Names** - Long, descriptive names beat short, enigmatic ones

## Functions as Vocabulary

Think of each function as a **word** in your domain's language. The more precise your vocabulary, the clearer your prose.

```kotlin
// Each function is a precisely defined word in our "ocean" vocabulary:

private fun isWithinOcean(row: Int, col: Int): Boolean =
    row in 0 until totalRows && col in 0 until totalCols

private fun isLand(row: Int, col: Int): Boolean =
    isWithinOcean(row, col) && ocean[row][col] == '1'

private fun markAsVisited(row: Int, col: Int) {
    ocean[row][col] = '0'
}
```

The principle at work:
- **Domain over generic** — `isWithinOcean` not `checkBounds`. The name speaks the problem's language, not programming-generic language.
- **Concept over value** — `isLand` not `isOne`. The name describes what the value MEANS, not what it IS.
- **Intent over mechanism** — `markAsVisited` not `setToZero`. The name describes WHY we do it (tracking traversal), not HOW (changing a char).
- **Class as shared context** — No `ocean` parameter anywhere. The class holds shared state, keeping signatures focused on what varies.

### The Naming Litmus Test

Read the call site out loud. If it sounds like a sentence, you've named it right:

```kotlin
if (isLand(row, col)) {     // "if is land..."
    submergeIsland(row, col) // "submerge island"
}
```

Compare with poor naming:

```kotlin
if (check(i, j)) {     // "if check i j" — meaningless
    process(i, j)       // "process i j" — process how?
}
```

## The Stepdown Rule: Tell a Story Top-Down

Code reads like a narrative. The entry point function appears FIRST in the file — before any class or helper it delegates to. The reader encounters "what this does" before "how it works". Then each subsequent level defines the terms used above:

```
TO count islands, we scan each cell — if it is land, we count it and submerge it.
  TO submerge an island, if it's not land we stop; otherwise mark it visited, then explore all four directions.
    TO explore north, we submerge the cell above.
    TO check if something is land, we check it's within the ocean and the cell is '1'.
      TO check within ocean, we check the row and col are in bounds.
```

Each level is a function. Each function defines a word used by the level above:

```kotlin
// Level 0: Entry point — one sentence
fun countIslands(ocean: Array<CharArray>): Int {
    if (ocean.isEmpty()) return 0
    return IslandCounter(ocean).count()
}

// Level 1: The story
fun count(): Int { /* for each cell, if isLand, count++ and submergeIsland */ }

// Level 2: Key plot points
private fun submergeIsland(row: Int, col: Int) {
    if (!isLand(row, col)) return

    markAsVisited(row, col)

    exploreNorth(row, col)
    exploreSouth(row, col)
    exploreWest(row, col)
    exploreEast(row, col)
}

// Level 3: Supporting details
private fun exploreNorth(row: Int, col: Int) = submergeIsland(row - 1, col)
private fun exploreSouth(row: Int, col: Int) = submergeIsland(row + 1, col)
private fun exploreWest(row: Int, col: Int) = submergeIsland(row, col - 1)
private fun exploreEast(row: Int, col: Int) = submergeIsland(row, col + 1)

// Level 4: Primitives
private fun isWithinOcean(row: Int, col: Int): Boolean =
    row in 0 until totalRows && col in 0 until totalCols

private fun isLand(row: Int, col: Int): Boolean =
    isWithinOcean(row, col) && ocean[row][col] == '1'

private fun markAsVisited(row: Int, col: Int) {
    ocean[row][col] = '0'
}
```

## Function Size: Small Enough to Be Obvious

### The First Rule: Small

```kotlin
// Bad — a wall of code, multiple responsibilities
fun processUserRegistration(userData: UserData) {
    // Validate input (20 lines)
    // Check database (15 lines)
    // Send email (10 lines)
    // Log activity (5 lines)
}

// Good — reads like a checklist
fun processUserRegistration(userData: UserData) {
    validateUserInput(userData)
    val user = createUserAccount(userData)
    sendWelcomeEmail(user)
    logRegistration(user)
}
```

### Blocks and Indenting

Indent level should be 1-2 at most. If you're nesting, you're missing a function:

```kotlin
// Bad — nested logic obscures intent
fun renderPageWithSetups(isSuite: Boolean) {
    if (isTestPage) {
        if (isSuite) {
            val suiteSetup = getSuiteSetup()
            if (suiteSetup != null) {
                // more nesting...
            }
        }
    }
}

// Good — extracted and named
fun renderPageWithSetups(isSuite: Boolean) {
    if (isTestPage) includeSetupPages(isSuite)
}
```

## Do One Thing

**A function should do one thing, do it well, and do it only.**

How to tell if a function does more than one thing:
- Can you extract another function with a name that's not a restatement?
- Are all statements at the same level of abstraction?

```kotlin
// Bad — does three things: iterates, checks, and pays
fun payEmployees() {
    val employees = getAllEmployees()
    for (emp in employees) {
        if (emp.isPayday()) {
            val pay = emp.calculatePay()
            emp.deliverPay(pay)
        }
    }
}

// Good — each function does one thing
fun payEmployees() {
    val employees = getAllEmployees()
    for (emp in employees) {
        payIfNecessary(emp)
    }
}

fun payIfNecessary(employee: Employee) {
    if (employee.isPayday()) calculateAndDeliverPay(employee)
}
```

## One Level of Abstraction

Don't mix low-level mechanics with high-level intent:

```kotlin
// Bad — string concatenation mixed with business logic
fun renderHtml(): String {
    var pageHtml = "<html>"        // Low level
    includePageSetup()             // High level
    pageHtml += "<body>"           // Low level
    return pageHtml
}

// Good — consistent abstraction
fun renderHtml(): String {
    return HtmlBuilder()
        .addHeader()
        .addBody()
        .addFooter()
        .build()
}
```

## Function Arguments

### Argument Count

- **Zero (niladic)** — Ideal
- **One (monadic)** — Good
- **Two (dyadic)** — Acceptable, but consider wrapping
- **Three (triadic)** — Avoid when possible
- **More than three** — Requires special justification; use an object

```kotlin
// Bad — too many arguments, hard to remember order
fun createUser(name: String, email: String, age: Int, address: String, phone: String, role: String) { }

// Good — wrapped in a domain object
fun createUser(userData: UserData) { }
```

### Common Monadic Forms

Three reasons for a single argument:

1. **Asking a question**: `isValidEmail(email)`, `fileExists(path)`
2. **Transforming**: `parseJson(jsonString)`, `toUpperCase(name)`
3. **Event**: `logError(message)`, `passwordAttemptFailed(username)`

### Flag Arguments: Never

Passing a boolean means the function does two things. Split it:

```kotlin
// Bad — what does `true` mean at the call site?
render(true)

// Good — intent is obvious
renderForSuite()
renderForSingleTest()
```

### Argument Objects

When a function needs related arguments, group them:

```kotlin
// Bad — x and y are a concept pretending to be two things
fun createCircle(x: Double, y: Double, radius: Double) { }

// Good — the concept has a name
fun createCircle(center: Point, radius: Double) { }
```

## Have No Side Effects

A function should do what its name promises — nothing more, nothing less:

```kotlin
// Bad — "check" shouldn't initialize anything
fun checkPassword(username: String, password: String): Boolean {
    val user = User.findByName(username)
    if (user.password == password) {
        Session.initialize()  // SIDE EFFECT — the name lied to you
        return true
    }
    return false
}

// Good — name matches behavior
fun login(username: String, password: String): Boolean {
    if (checkPassword(username, password)) {
        Session.initialize()
        return true
    }
    return false
}
```

## Command Query Separation

Functions should either **do something** (command) or **answer something** (query), not both:

```kotlin
// Bad — sets a value AND returns success status
fun setAndCheckAttribute(name: String, value: String): Boolean {
    if (attributeExists(name)) {
        setAttribute(name, value)
        return true
    }
    return false
}

// Good — separated concerns
fun setAttribute(name: String, value: String) { }
fun attributeExists(name: String): Boolean { }
```

## Prefer Exceptions to Error Codes

```kotlin
// Bad — nested error code checking
fun deletePage(page: Page): Int {
    if (deletePageFromDb(page) == E_OK) {
        if (registry.deleteReference(page.name) == E_OK) {
            return E_OK
        }
    }
    return E_ERROR
}

// Good — clean flow with exceptions
fun deletePage(page: Page) {
    try {
        deletePageFromDb(page)
        registry.deleteReference(page.name)
    } catch (e: Exception) {
        logger.log(e)
        throw
    }
}
```

## Extract Try/Catch Blocks

Error handling is one thing. A function that handles errors should do nothing else:

```kotlin
// Good — the try/catch function only orchestrates error handling
fun deletePage(page: Page) {
    try {
        deletePageAndReferences(page)
    } catch (e: Exception) {
        handleDeletionError(e)
    }
}
```

## Don't Repeat Yourself (DRY)

Duplication is the root of many evils. Extract common logic:

```kotlin
// Bad — identical logic duplicated
fun processAdminUser(user: User) {
    validateEmail(user.email)
    saveToDatabase(user)
    sendWelcomeEmail(user)
}

fun processRegularUser(user: User) {
    validateEmail(user.email)
    saveToDatabase(user)
    sendWelcomeEmail(user)
}

// Good — shared logic extracted
fun processUser(user: User) {
    validateUser(user)
    saveUser(user)
    notifyUser(user)
}
```

## Structured Programming

For small functions, multiple early returns improve readability:

```kotlin
fun isValidUser(user: User?): Boolean {
    if (user == null) return false
    if (user.email.isBlank()) return false
    if (!user.isActive) return false
    return true
}
```

## How to Write Functions Like This

1. **Write it messy** — get it working first, correctness before cleanliness
2. **Write tests** — so you can refactor fearlessly
3. **Extract and name** — pull out concepts, give them precise domain names
4. **Read it out loud** — if it doesn't sound like English, rename
5. **Repeat** — keep refining until reading the code tells you the story
