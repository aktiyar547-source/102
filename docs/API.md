# API Documentation — Frozen Legacy Contract

The backend is a CodeIgniter-style PHP app. All endpoints are
`application/x-www-form-urlencoded` **POST**s under `<host>/container_web/container/`.
All traffic in the legacy app is **cleartext HTTP**. Responses are opaque (the legacy
client ignored bodies); the client treats HTTP 2xx as success.

> These shapes are frozen. Changing a field name, order, or encoding breaks
> compatibility and must go through the golden/wire compatibility tests.

## Host configuration

Host + scheme are per-flavor `BuildConfig` values (`MAIN_BASE_URL`, `EXTRA_BASE_URL`).
Default `http://testrun.adlibsol.com/container_web/`. Cleartext is permitted only for the
configured legacy host via `res/xml/network_security_config.xml`.

## POST `container/test` — main inspection upload

Implemented by `MecrcApi.uploadContainer(@FieldMap fields)`; the ordered field map is
produced by `ContainerPayloadBuilder`.

Header fields: `IMEInum`, `container_name`, `user_name`, `container_type`.

Per-side pairs, **in this exact order** (recovered from the legacy `Sync` source):

| # | Image field | Remark field |
|---|---|---|
| 1 | `Front` | `Front_Remarks` |
| 2 | `Front_Bottom` | `Front_Bottom_Remarks` |
| 3 | `Front_Top` | `Front_Top_Remarks` |
| 4 | `Back` | `Back_Remarks` |
| 5 | `Back_Top` | `Back_Top_Remarks` |
| 6 | `Back_Bottom` | `Back_Bottom_Remarks` |
| 7 | `Right` | `Right_Remarks` |
| 8 | `Left` | `Left_Remarks` |
| 9 | `Inside_ftb` | `Inside_ftb_Remarks` |
| 10 | `Inside_btf` | `Inside_btf_Remarks` |

Image values are Base64 (`Base64.DEFAULT`, wrapped) of a **PNG at quality 50, no resize**.
`Under_Floor` is **omitted** unless `INCLUDE_UNDER_FLOOR_IN_TEST_PAYLOAD=true` (Q8), in which
case `Under_Floor` / `Under_Floor_Remarks` are appended last.

Field order is asserted by `MecrcApiWireCompatibilityTest`.

## POST `container/extra_images` — extra-image upload

Implemented by `MecrcApi.uploadExtraImage(...)`. Fields, in order:
`IMEInum`, `container_no`, `user_name`, `picture_time`, `ERemarks`, `type`, `ExtraImage`.

`ExtraImage` is Base64 (`Base64.DEFAULT`) of a PNG at **quality 100, downsampled ~600×600**.

## Identity note

`IMEInum` carries a generated, persisted install-UUID (the legacy IMEI is unavailable on
Android 10+). The backend must treat this value as an opaque device string.

## Unverified: server acceptance

The wire *shape* is proven by tests. Server *acceptance* of the revived `container/test`
call is not yet validated against a live server (risk R1) — run a `staging` build against a
reachable host to confirm.
