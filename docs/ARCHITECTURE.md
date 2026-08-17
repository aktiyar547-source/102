# Architecture Guide

Clean Architecture + MVVM. Dependencies point inward: `ui → domain ← data`.

## Layers

**domain/** — pure Kotlin, no Android/framework deps. Models (`Container`, `Side`,
`ContainerType`, `ExtraImage`, `Session`), repository **interfaces**, use cases
(`ValidateContainerNumberUseCase` = ISO 6346, `CreateContainerUseCase`, `PurgeOldUploadedUseCase`,
session/enqueue), and the `ContainerOcrEngine` interface.

**data/** — implements the domain interfaces. `database/` (Room entities, DAOs,
`SideColumnMapper`), `network/` (`MecrcApi`, `ImageEncoder`, `ContainerPayloadBuilder`),
`storage/` (scoped `ImageFileStore`), `ocr/` (ML Kit), `identity/` (install-UUID),
`session/` (encrypted DataStore/prefs), `sync/` (WorkManager workers + scheduler),
`camera/` (watermark + FileProvider), `repository/` (impls).

**ui/** — Jetpack Compose. One `MainActivity` hosts a `NavHost`; each legacy Activity is a
composable destination with a `@HiltViewModel` exposing `StateFlow`. Shared `StatefulScaffold`
renders Loading/Empty/Offline/Error/Success + Retry.

**di/** — Hilt modules: Database, Network, Dispatchers, Repository (binds + OCR).

## Key flows

- **New project:** camera → watermark → ML Kit OCR → ISO 6346 validate → `CreateContainerUseCase`
  → seed Container/CImages/Remarks/Tag rows → side grid.
- **Capture side:** camera → watermark → import to scoped storage → `SideCaptureRepository.saveSide`.
- **Upload:** `EnqueueUploadUseCase` → unique `UploadContainerWorker` per container (network
  constraint + exponential backoff) → data, then extras, then `markDone`. Offline-durable.
- **Housekeeping:** daily `HousekeepingWorker` → `PurgeOldUploadedUseCase`.

## Contract-freeze boundary

The API field set/order, Room column names, ISO 6346 rule, container-type list, and side
taxonomy are frozen behind repository interfaces, so v2 can evolve UI/backend without breaking
legacy compatibility.
