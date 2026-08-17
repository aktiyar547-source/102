# Developer Guide

## Conventions
- Kotlin official style; one responsibility per class; constructor injection via Hilt.
- No magic values — see `core/common/Constants.kt`, `DateFormats`, `AppConfig`.
- ViewModels expose `StateFlow<UiState<T>>`; screens are stateless and driven by state.
- Time comes from the injected `Clock` (never `new Date()` in logic) for testability.

## Adding a screen
1. Create `ui/<feature>/<Feature>ViewModel.kt` (`@HiltViewModel`, inject domain interfaces).
2. Create `ui/<feature>/<Feature>Screen.kt` using `MecrcScaffold` + `StatefulScaffold`.
3. Register a route in `Routes.kt` and a `composable(...)` in `MecrcNavGraph.kt`.

## Touching the wire contract
Any change to `MecrcApi`, `ContainerPayloadBuilder`, or `ImageEncoder` must keep
`ContainerPayloadBuilderTest` and `MecrcApiWireCompatibilityTest` green. If a field's name,
order, or encoding must change, that is a backend-compatibility decision, not a refactor.

## Camera capture
Use `rememberCameraCapture(newTarget, onCaptured)` — it handles the CAMERA runtime permission,
file creation, the capture intent, and the watermark step lives in the ViewModel via `WatermarkUtil`.

## Tests
- Pure logic → JVM unit tests under `src/test`.
- DB/framework → instrumented under `src/androidTest`.
- Keep time-dependent tests deterministic with a fixed `Clock`.
