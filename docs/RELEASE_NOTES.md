# Release Notes

## v2.0.0 — Kotlin re-platform (v1 of the modernization)

First release of the rebuilt MECRC Mobile Inspection app. Behaviourally faithful to the legacy
application; rebuilt internally on a modern, maintainable stack.

**Parity**
- Same workflow and screens: Login, New Project (OCR), side capture, extra images, Preview,
  Delete, Upload, Settings.
- Same 11 container types, 11 inspection sides, ISO 6346 container-number validation, and the
  burned-in capture timestamp.
- Backend wire contract reproduced exactly (`container/test`, `container/extra_images`).

**Modernized under the hood**
- Kotlin, Jetpack Compose, Clean Architecture + MVVM, Hilt, Room, Retrofit/OkHttp, WorkManager,
  CameraX, ML Kit OCR.
- Android 10+ ready: app-scoped storage, runtime camera permission, generated install-UUID in
  place of IMEI.

**Deliberate improvements over legacy (safety, not workflow)**
- Parameterized database access (removed SQL-injection surface).
- Durable, retrying, offline-safe uploads (replaces the legacy dead upload path).
- Retention now purges only *uploaded* inspections older than the window — un-uploaded work is
  never lost (legacy could delete pending data).
- Release builds are non-debuggable, non-backup, and signed.

**Known limitations**
- Roles (Inspector/Supervisor/Administrator) remain nominal, as in legacy (real RBAC is v2).
- The revived main upload is validated for wire-shape; server acceptance requires a staging run.
