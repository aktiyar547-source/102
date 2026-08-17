# Release Checklist

- [ ] `./gradlew testDevDebugUnitTest` green (ISO 6346, golden payload, wire compat, retention).
- [ ] `./gradlew connectedDevDebugAndroidTest` green on a device (DAO lifecycle + retention).
- [ ] `./gradlew lintProdRelease` reviewed; no blocking issues.
- [ ] `staging` build run against the confirmed backend; both endpoints accepted (R1 closed).
- [ ] `prod` flavor host set (HTTPS preferred); cleartext exception removed if server has TLS.
- [ ] `keystore.properties` present on the release machine; `assembleProdRelease`/`bundleProdRelease` signed.
- [ ] `versionCode`/`versionName` bumped; `RELEASE_NOTES.md` updated.
- [ ] ProGuard `mapping.txt` archived for crash de-obfuscation.
- [ ] Distributed via the enterprise/MDM channel; install + camera permission verified on a real device.
