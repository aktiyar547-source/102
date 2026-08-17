# Deployment Guide

## Distribution
Private/enterprise (MDM), not Play Store (Q1). Ship a signed `prodRelease` APK/AAB.

## Signing
1. Generate a keystore (see `keystore.properties.sample`), store it securely off-repo.
2. `cp keystore.properties.sample keystore.properties` and fill in (gitignored).
3. `./gradlew assembleProdRelease` (or `bundleProdRelease` for AAB). The release build is
   minified, resource-shrunk, and signed when `keystore.properties` is present.

## Environments
- `staging` build → wire-validation against the staging host (R1). Confirm both endpoints
  accept the payloads before promoting.
- `prod` build → set the production host in the `prod` flavor; **prefer HTTPS** and, once the
  server has TLS, drop the cleartext exception in `network_security_config.xml`.

## Release gate
- All JVM tests green (incl. wire compatibility).
- Instrumented DAO test green on a device.
- `staging` build confirmed accepted by the backend.
- ProGuard mapping file archived for crash de-obfuscation.

## Versioning
Bump `versionCode` (integer, monotonic) and `versionName` per release. Legacy shipped
versionCode 1; this line starts at 2.
