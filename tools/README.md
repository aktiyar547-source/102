# Pre-flight checks

Quick sanity checks to run **before pushing**, so an obvious break is caught in
seconds instead of after a five-minute CI round trip.

```bash
python3 tools/kt-sanity.py     # bracket balance, string termination, package decl
python3 tools/prose-scan.py    # English prose sitting in code (a broken comment)
```

Neither replaces the compiler — they catch the damage that scripted text edits
cause, which is exactly how `WatermarkUtil.kt` lost the `//` from a comment line
and failed a build.
