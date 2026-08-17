# Database Documentation

Local store: Room over SQLite, DB file `MiddleEast_Container.db`, schema version 1
(new v1 line; no import from the legacy DB — Q5). Column names mirror the legacy tables 1:1
so the upload mapper reproduces legacy payloads exactly. All access is parameterized
(no SQL injection).

## Tables

### Container — inspection master
`Id` (PK), `Name` (UNIQUE — ISO 6346 number), `Type`, `Date` (dd-MMMM-yyyy),
`Status`, `Username`, `IMEInum` (install-UUID), `Status1`, `CreatedDate` (yyyy-MM-dd).
`Status1` drives upload lifecycle: `Upload` → `Done`.

### CImages — per-side image path
`Id` (PK), `Remarks`, 11 side columns (`Front`, `Front_Bottom`, `Front_Top`, `Back`,
`Back_Bottom`, `Back_Top`, `Left`, `Right`, `Inside_btf`, `Inside_ftb`, `Under_Floor`),
`Name` (UNIQUE), `CreatedDate`. Side columns store **relative file paths** into app-scoped
storage; Base64 is produced only at upload time.

### Remarks — per-side remark text
Same 11 side columns, `Name` (UNIQUE), `CreatedDate`.

### Tag — per-side capture state
Same 11 columns; a side is `"Capture"` once photographed. `Name` (UNIQUE), `CreatedDate`.

### EImages — extra images
`Id` (PK), `Name`, `Image` (relative path), `Remarks`, `Time`, `Status` (`Upload`/`Done`),
`Type` (category), `CreatedDate`.

### Images — flat per-image record (legacy compatibility)
`Id` (PK), `Imei_Num`, `C_Num`, `U_Name`, `Type`, `Image`, `Remarks`, `Tag`, `Side`,
`Time`, `Status`, `CreatedDate`.

## Access patterns

- Container `Name` is the join key across all tables.
- Side columns are written via `SideColumnMapper` (exhaustive `when(side)` + entity copy),
  never dynamic SQL — the injection-free replacement for the legacy behaviour.
- Retention (Q7): `purgeUploadedBefore(cutoff)` deletes only `Status1='Done'` rows with
  `CreatedDate < cutoff`. Never deletes pending work.

## Storage layout (app-scoped, L11)

```
<externalFilesDir>/OCRimages/YYYY/MM/<yyyy-MM-dd>/<container>/
    <container> <yyyy-MM-dd> [HH-mm-ss].png
```
