# Troubleshooting Guide

**Camera does nothing / SecurityException.** The CAMERA permission was denied. The app requests
it on first capture; if permanently denied, enable it in system settings for the app.

**Uploads never complete.** Uploads are queued via WorkManager and require connectivity. Check the
device is online; failed attempts retry with backoff. Confirm the flavor's `MAIN_BASE_URL` points
at a reachable host.

**Backend rejects the upload.** The wire shape is frozen and tested, but the server may expect a
value the emulated legacy client never validated (e.g. the `IMEInum` device string). Capture the
request (OkHttp logging is on in debug) and compare against `docs/API.md`.

**"Enter Correct Container No".** The number failed ISO 6346: it must be 4 uppercase letters +
6 digits + a valid check digit (e.g. `CSQU3054383`).

**Duplicate project.** Container `Name` is unique; the number already exists locally.

**Data disappeared after a week.** Expected: the housekeeping job purges *uploaded* inspections
older than the retention window (default 7 days). Pending (un-uploaded) work is never purged.

**Cleartext HTTP blocked.** Only the configured legacy host is allowed cleartext. A new host must
be added to `network_security_config.xml` (or use HTTPS).

**Release build crashes but debug works.** Likely an R8 keep-rule gap. Check `proguard-rules.pro`
and the mapping file; add keeps for any reflectively-accessed class.
