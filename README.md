# MECRC Mobile Inspection (v1 re-platform)

Kotlin/Compose re-platform of the legacy `com.middleeastcontainer` Android inspection app.
This repository is the **Milestone 3–5 output: scaffold + data layer + repositories/workers
+ the full Compose presentation layer**. The app is now navigable end to end.

## Status

| Layer | State |
|---|---|
| Gradle (Kotlin DSL + version catalog) | ✅ scaffolded |
| Hilt application skeleton | ✅ |
| Domain models + ISO 6346 validator | ✅ + unit tests |
| Room database (6 legacy tables, parameterized DAOs) | ✅ |
| Network contract (`MecrcApi`, `ImageEncoder`, `ContainerPayloadBuilder`) | ✅ + golden-payload tests |
| Scoped `ImageFileStore`, `InstallIdProvider`, `SecurePrefs` | ✅ |
| Repositories + use cases + durable upload/housekeeping workers | ✅ |
| Safe side-column writer (`SideColumnMapper`) | ✅ + tests |
| **Compose UI — theme, state scaffold, navigation, 11 screens + ViewModels** | ✅ M5 |
| **Camera capture + timestamp watermark + ML Kit OCR** | ✅ M5 |
| **Camera runtime-permission gate** | ✅ M6 |
| **Wire-compatibility test (MockWebServer), DAO instrumented test** | ✅ M6 |
| **CI (GitHub Actions), Gradle wrapper config, build-readiness audit** | ✅ M6 |
| **Release signing config, dev/staging/prod flavors, R8 rules** | ✅ M7 |
| **Full documentation set (`docs/`)** | ✅ M7 |
| Staging server validation (R1) / first real build | ⛔ needs your toolchain + backend |

See `docs/` for the full guide set: `ARCHITECTURE`, `API`, `DATABASE`, `INSTALLATION`,
`DEVELOPER_GUIDE`, `DEPLOYMENT`, `TROUBLESHOOTING`, `RELEASE_NOTES`, `RELEASE_CHECKLIST`, and
`BUILD_READINESS` (static audit + build commands).

### Screens (M5)
Login · Menu · New Project (OCR) · Side grid · Single-side capture · Add extra · View extra ·
Preview · Delete · Upload · Settings — single-activity Compose, navigation mirrors the legacy
flow, legacy blue app bar, and every list screen honours the Loading/Empty/Offline/Error/Success
+ Retry contract via `StatefulScaffold`.

## Build & test

Requires Android Studio (JDK 17, Android SDK 35). This project was authored in a
sandbox **without** the Android SDK/Gradle network access, so it has **not been
compiled here** — build and run the tests locally:

```bash
./gradlew :app:testDebugUnitTest      # runs ISO 6346 oracle + golden-payload tests
./gradlew :app:assembleDebug
```

JVM unit tests (the compatibility + correctness gate):
- `ValidateContainerNumberUseCaseTest` — ISO 6346 accept/reject parity (vectors verified against the legacy algorithm, incl. `CSQU3054383`).
- `ContainerPayloadBuilderTest` — locks the exact `/container/test` field set and order recovered from the legacy `Sync` source.
- `SideColumnMapperTest` — proves the safe side-column writer touches exactly one column (no injection, 1:1 mapping).
- `CreateContainerUseCaseTest` — validation → duplicate → created outcomes.
- `PurgeOldUploadedUseCaseTest` — retention cutoff (uploaded-only, never loses pending work).

### Upload / offline model (M4)
Capture writes to Room + scoped storage immediately (never blocks on network). `UploadScheduler`
enqueues a unique `UploadContainerWorker` per container (network-constrained, exponential backoff)
that uploads inspection data → pending extra images → marks Done; failures retry across
connectivity loss and process death. A daily `HousekeepingWorker` runs the Q7 purge.

## Frozen legacy contract (do not change without a compatibility review)

- **Container number:** ISO 6346, 4 letters + 6 digits + 1 check digit; check = `(Σ value(cᵢ)·2^i) % 11 % 10`.
- **Types (11):** `ContainerType` enum wire strings.
- **Sides (11):** `Side` enum (`dbName` = wire/DB column, `label` = on-screen name).
- **`/container/test` field order:** built by `ContainerPayloadBuilder` — Back_Top before Back_Bottom, Right before Left, Inside_ftb before Inside_btf; `Under_Floor` omitted by default.
- **Image encoding:** main payload = PNG(50) Base64 DEFAULT; extra images = ~600×600 PNG(100) Base64 DEFAULT.
- **Status lifecycle:** `Status1` `Upload → Done`.

## Key decisions (delegated to architect — see Milestone 2 doc)

- Identity: generated install-UUID in the `IMEInum` field (IMEI unavailable on Android 10+).
- Storage: app-scoped, same logical `OCRimages/YYYY/MM/date/container` tree.
- Retention: purge only **uploaded** inspections older than `RETENTION_DAYS` (never loses un-uploaded work — a deliberate fix of a legacy data-loss bug).
- Backend host/scheme: configurable via `BuildConfig`; cleartext restricted to the legacy host via `network_security_config`.
- Security: parameterized DB (no injection), `debuggable=false` / `allowBackup=false` in release, encrypted prefs.

## Open item before release

The revived main upload (`/container/test`) is byte-frozen but **not yet verified against a
live server** (R1). Point the build at a reachable staging host (or capture one real legacy
upload) to confirm acceptance and to decide the `INCLUDE_UNDER_FLOOR_IN_TEST_PAYLOAD` flag.
