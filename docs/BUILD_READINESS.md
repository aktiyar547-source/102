# Build Readiness & Test Depth — Milestone 6

This milestone hardens the project for a real build and deepens the test suite. **It was
authored without an Android SDK/Gradle in the sandbox, so nothing here was compiled or run
by me** — the audit below is a careful static review, and the first real `./gradlew` runs on
your machine or CI. What I *could* verify offline (algorithm logic, payload order, retention
math, wire-field order) is covered by the JVM tests.

## Static build-readiness audit — issues found and fixed

| # | Issue | Severity | Fix |
|---|---|---|---|
| A1 | `MecrcDatabase` used `fallbackToDestructiveMigrationOnDowngrade(false)` — no boolean overload in Room 2.6.1 | Compile error | Removed (not needed at schema v1) |
| A2 | Missing Hilt binding for `ContainerOcrEngine` (injected by `OcrViewModel`) | Dagger compile error | Added `@Binds bindOcrEngine` in `RepositoryModule` |
| A3 | Camera screens never requested runtime `CAMERA` permission; declared permission makes `ACTION_IMAGE_CAPTURE` throw until granted (Android 10+) | Runtime crash | Added `rememberCameraCapture` permission gate; wired into OCR, single-side, add-extra |
| A4 | Wrong import `androidx.compose.ui.menu.MenuAnchorType` | Compile error | Corrected to `androidx.compose.material3.MenuAnchorType` |
| A5 | `collectAsStateWithLifecycle` used without `lifecycle-runtime-compose` | Unresolved reference | Added the dependency |
| A6 | Back-arrow icon needed `material-icons-core` | Unresolved reference | Added the dependency |
| A7 | Bitmap decode ran on the composition (main) thread in `FileImage` | Jank/ANR risk | Wrapped decode in `Dispatchers.IO` |

Verified by cross-check: **all 10 injected interfaces** (`SessionRepository`, `ContainerRepository`,
`SideCaptureRepository`, `ExtraImageRepository`, `UploadRepository`, `ContainerOcrEngine`, `Clock`,
`DispatcherProvider`, `AppConfig`, `MecrcApi`) resolve to a `@Binds`/`@Provides`. Room column names
that are SQL keywords (`Left`, `Right`) are safe because Room backtick-quotes all generated column
references.

## Test suite (what runs where)

**JVM unit tests** (`./gradlew testDevDebugUnitTest`) — run anywhere, no device:
- `ValidateContainerNumberUseCaseTest` — ISO 6346 accept/reject parity.
- `ContainerPayloadBuilderTest` — frozen `/container/test` field set + order.
- `MecrcApiWireCompatibilityTest` — **MockWebServer**: asserts the real HTTP request (method, path,
  form-encoding, field order) for both endpoints. The executable compatibility gate.
- `SideColumnMapperTest` — injection-free side-column writer, 1:1 mapping.
- `CreateContainerUseCaseTest`, `PurgeOldUploadedUseCaseTest` — outcomes + retention cutoff.

**Instrumented tests** (`./gradlew connectedDevDebugAndroidTest`, needs emulator/device):
- `ContainerDaoTest` — Room lifecycle: `markDone` flips status; retention purge removes only
  uploaded rows before the cutoff.

## Residual risks (unverifiable without the toolchain)

- Annotation processing (Hilt/Room/KSP) correctness is reasoned-through but unrun.
- Compose compiler ↔ Kotlin 2.0.20 handled by the `kotlin-compose` plugin (versions aligned in the
  catalog), but unbuilt.
- material3 `menuAnchor(MenuAnchorType)` and AutoMirrored `ArrowBack` assume Compose BOM
  `2024.09.03` (material3 1.3.0) — both APIs exist there.
- Unused-import warnings may remain (non-fatal); run `./gradlew lintDevDebug`.

## How to build

```bash
# 1. Point to your SDK
cp local.properties.sample local.properties   # then edit sdk.dir

# 2. Generate the Gradle wrapper jar (binary; not committed)
gradle wrapper --gradle-version 8.9

# 3. Build + test
./gradlew testDevDebugUnitTest        # fast JVM gate (incl. wire-compatibility)
./gradlew lintDevDebug
./gradlew assembleDevDebug            # produces app/build/outputs/apk/debug/
./gradlew connectedDevDebugAndroidTest  # instrumented (needs a device/emulator)
```

## CI

`.github/workflows/android.yml` runs JDK 17 + Gradle, generates the wrapper, then runs unit tests,
lint, and `assembleDevDebug`, uploading `app/build/reports/` as an artifact on every push/PR to `main`.

## Still needs you (not blocking Milestone 6)

- A reachable **staging host** (or one captured legacy upload) to validate the revived
  `/container/test` path against the real server (R1). The wire *shape* is proven; server
  *acceptance* is not.
- The **enterprise signing** channel/keystore for release builds (Milestone 7).
