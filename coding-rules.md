# RB Coding Rules
### Adapted from *The Power of Ten* — NASA/JPL Laboratory for Reliable Software

> **Core Philosophy:** Simple code is reliable code. If it is hard to read, it is hard to trust.
> These rules are not obstacles — they are the seat-belt you stop noticing after a week.

---

## Background

This document adapts Gerard J. Holzmann's *Power of Ten* rules (NASA/JPL) for commercial
mobile development in Kotlin and Swift. The original rules were written for safety-critical
C code aboard spacecraft. The spirit carries over: **fewer surprises, fewer crashes, happier
users, faster debugging.**

Unlike the original, these rules acknowledge commercial trade-offs. They are not Draconian —
they are the minimum discipline that separates maintainable products from unmaintainable ones.

---

## The Rules

---

### Rule 1 — Keep Control Flow Simple

**Avoid complex or non-linear control flow.**

- No deeply nested callbacks, promise chains, or reactive pipelines that are hard to trace
- No mutual recursion or unbounded recursive calls — use iterative solutions instead
- Avoid `goto`-equivalent patterns: excessive labeled breaks, `continue` inside complex loops,
  or exception-driven flow for non-exceptional paths
- Each function or method should have an obvious, traceable path from entry to exit

**In practice (Kotlin):**
```kotlin
// ✅ Clear, linear flow
fun processResult(data: Data?): Result {
    requireNotNull(data) { "data must not be null" }
    val parsed = parse(data) ?: return Result.Failure("parse failed")
    return Result.Success(parsed)
}

// ❌ Avoid: nested lambdas hiding control flow
fun processResult(data: Data?) =
    data?.let { parse(it)?.let { p -> transform(p)?.let { t -> Result.Success(t) } } }
        ?: Result.Failure("unknown")
```

**In practice (Swift):**
```swift
// ✅ Use guard for early exits, keep the happy path unindented
func processResult(_ data: Data?) -> Result<Parsed, Error> {
    guard let data = data else { return .failure(AppError.noData) }
    guard let parsed = parse(data) else { return .failure(AppError.parseFailed) }
    return .success(parsed)
}
```

---

### Rule 2 — Bound All Loops and Collections

**Every loop or collection traversal must have a knowable upper limit.**

- Prefer `for item in collection` over `while` with manual index management
- When using `while`, always include a guard or counter that guarantees termination
- Never poll or retry indefinitely — always set a max attempt count
- Streaming or paginated data must have an explicit page limit or cancellation condition

**In practice:**
```kotlin
// ✅ Bounded retry with explicit limit
val maxRetries = 3
var attempt = 0
while (attempt < maxRetries) {
    val result = tryConnect()
    if (result.isSuccess) break
    attempt++
}

// ❌ Avoid: no upper bound
while (!isConnected()) {
    tryConnect()
}
```

---

### Rule 3 — Manage Memory and Resources Deliberately

**Acquire resources explicitly; release them explicitly or via structured scoping.**

- Use structured concurrency (`async`/`await`, `coroutineScope`) — avoid fire-and-forget
  coroutines or threads that outlive their owner
- Avoid retaining heavy objects (bitmaps, streams, sensors, BLE connections) beyond the
  scope where they are needed
- Prefer `use { }` (Kotlin) and `defer` (Swift) blocks to guarantee resource cleanup
- Do not cache data structures of unbounded size in memory — set size limits on caches

**In practice (Kotlin):**
```kotlin
// ✅ Resource cleaned up automatically
FileInputStream(file).use { stream ->
    processStream(stream)
}

// ✅ Coroutine tied to lifecycle scope
viewModelScope.launch {
    val result = repository.fetchData()
    _uiState.value = result
}
```

**In practice (Swift):**
```swift
// ✅ Explicit cancellation on deinit
private var cancellables = Set<AnyCancellable>()

// ✅ Use defer for guaranteed cleanup
func readFile() throws -> Data {
    let handle = try FileHandle(forReadingFrom: url)
    defer { handle.closeFile() }
    return handle.availableData
}
```

---

### Rule 4 — Keep Functions and Methods Short

**A function should do one thing and fit in one screen (~60 lines max).**

- If a function needs a comment to explain what each section does, it should be split
- Functions longer than ~40 lines are a signal to refactor, not a hard stop — but they
  require justification
- A function name should fully describe what it does; if you need "and" in the name,
  split it

**Guideline:**
```
< 20 lines  — ideal
20–40 lines — acceptable
40–60 lines — review carefully; consider splitting
> 60 lines  — refactor required
```

---

### Rule 5 — Assert Invariants and Validate State

**Use preconditions, postconditions, and assertions to catch bugs at the boundary.**

- Validate inputs at the entry of every public function/method
- Use language-native assertion tools rather than silent failures
- Never silently swallow errors — log at minimum, handle explicitly when possible
- Assert the things that *should never be false* — they document intent and catch regressions

**In practice (Kotlin):**
```kotlin
fun updateDevice(device: BluetoothDevice, payload: ByteArray) {
    require(payload.isNotEmpty()) { "Payload must not be empty" }
    require(payload.size <= MAX_PAYLOAD_SIZE) { "Payload exceeds max size: ${payload.size}" }
    check(isConnected) { "Device must be connected before updating" }
    // ... proceed
}
```

**In practice (Swift):**
```swift
func updateDevice(_ device: CBPeripheral, payload: Data) {
    precondition(!payload.isEmpty, "Payload must not be empty")
    precondition(payload.count <= maxPayloadSize, "Payload exceeds max size")
    assert(isConnected, "Device must be connected before updating")
    // ... proceed
}
```

> Use `require`/`precondition` for caller errors (bugs in calling code).
> Use `check`/`assert` for internal invariants (bugs in this code).

---

### Rule 6 — Minimize Variable Scope

**Declare variables as close to their use as possible, in the narrowest scope that works.**

- Prefer `val`/`let` (immutable) over `var`/`var` — mutability is opt-in, not default
- Do not reuse variables for different purposes within the same function
- Avoid class-level state for data that only needs to exist within a single operation
- Keep mutable state centralized and documented (ViewModel, StateFlow, `@Published`)

**In practice:**
```kotlin
// ✅ Scoped to where it's needed
fun buildPacket(command: Int, data: ByteArray): ByteArray {
    val header = byteArrayOf(0xAA.toByte(), command.toByte())
    val checksum = computeChecksum(data)
    return header + data + byteArrayOf(checksum)
}

// ❌ Avoid: class-level field that only serves one method
class PacketBuilder {
    private var tempHeader: ByteArray? = null  // only used in buildPacket()
    ...
}
```

---

### Rule 7 — Handle Every Return Value and Error

**Ignoring a return value is a decision that must be deliberate and documented.**

- Handle `Result`, nullable returns, and thrown exceptions at every call site
- Never use bare `try { } catch { }` with an empty body
- In Kotlin, do not chain `.let {}` or `?.` so deeply that error paths become invisible
- In Swift, do not silence `try?` unless the failure is genuinely irrelevant, and comment why

**In practice (Kotlin):**
```kotlin
// ✅ Explicit handling
val result = repository.sync()
when (result) {
    is Result.Success -> handleSuccess(result.data)
    is Result.Failure -> handleError(result.error)
}

// ❌ Avoid: silent discard
try {
    repository.sync()
} catch (e: Exception) { }
```

**In practice (Swift):**
```swift
// ✅ Handle or explicitly acknowledge
if let data = try? loadCachedData() {
    use(data)
} else {
    // Cache miss is acceptable; will fetch from network
}

// ❌ Avoid: error swallowed, state unknown
_ = try? saveData(record)
```

---

### Rule 8 — Limit Feature Flags and Build Variants

**Each active feature flag doubles the number of configurations to test.**

- Keep the number of active feature flags small at any given time
- Feature flags should be short-lived — merge or delete within a release cycle
- Avoid deeply nested `if (featureEnabled)` logic scattered across the codebase;
  use a single injection point (factory, provider, or constructor)
- Build variants (debug/release, staging/prod) are acceptable but must not diverge
  in business logic — only in configuration values

**Guideline:**
```
0–3 active feature flags  — manageable
4–6 active feature flags  — review and retire old ones
> 6 active feature flags  — significant test surface risk; audit required
```

---

### Rule 9 — Keep References and Dependencies Explicit

**Avoid hidden dependencies, ambient state, and implicit coupling.**

This is the mobile equivalent of the original rule on pointers. In Kotlin/Swift, "pointers"
are references, singletons, global state, and implicit context.

- Pass dependencies explicitly (constructor injection); avoid reaching into global
  singletons from business logic
- Avoid storing `Context`, `Activity`, or `ViewController` references in long-lived objects
  — use `WeakReference` or redesign
- Prefer concrete types over opaque callbacks when the data flow needs to be traceable
- Static/global mutable state is permitted only for infrastructure (logging, analytics);
  never for business state

**In practice:**
```kotlin
// ✅ Explicit injection
class SyncManager(
    private val bleService: BleService,
    private val repository: DeviceRepository
)

// ❌ Avoid: hidden global reach
class SyncManager {
    private val bleService = AppContainer.getInstance().bleService
}
```

---

### Rule 10 — Zero Warnings, Run the Linters

**Treat warnings as errors from day one. Let the tools do the mechanical checking.**

- All code must compile with zero warnings in production builds
- Run language linters on every commit or PR:
  - **Android/Kotlin:** `ktlint`, `detekt`, Android Lint
  - **iOS/Swift:** `SwiftLint`, Xcode warnings
- A suppressed warning must be accompanied by a comment explaining why it is safe to suppress
- Static analysis findings are bugs until proven otherwise — do not dismiss without review

**Setup recommendation:**
```bash
# Android — run on CI
./gradlew lint detekt ktlintCheck

# iOS — run on CI
swiftlint lint --strict
xcodebuild ... | xcbeautify
```

> A codebase with zero warnings is a codebase where *new* warnings are immediately visible.
> That is the entire point.

---

## Quick Reference

| # | Rule | Mobile Keyword |
|---|------|----------------|
| 1 | Simple control flow, no deep recursion | **Traceable paths** |
| 2 | Every loop has an upper bound | **Bounded iteration** |
| 3 | Manage resources with structured scoping | **No leaks** |
| 4 | Functions ≤ 60 lines, one purpose | **One job** |
| 5 | Assert invariants, validate inputs | **Fail loud, fail early** |
| 6 | Narrowest scope, prefer immutability | **Immutable by default** |
| 7 | Handle every return value and error | **No silent failures** |
| 8 | Minimize feature flags and variants | **Limit test surface** |
| 9 | Explicit dependencies, no hidden state | **Visible data flow** |
| 10 | Zero warnings, run linters always | **Tools find bugs cheaper** |

---

## When to Break a Rule

Every rule has legitimate exceptions. The cost of breaking a rule is:

1. A short comment at the site explaining why
2. A second pair of eyes (code review) confirming the exception is justified

No exception should be silent.

---

*Adapted from "The Power of Ten – Rules for Developing Safety Critical Code" by Gerard J. Holzmann, NASA/JPL Laboratory for Reliable Software. Original rules target C; this document adapts them for commercial Kotlin/Swift mobile development with a focus on simplicity and reliability.*
