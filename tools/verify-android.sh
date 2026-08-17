#!/usr/bin/env bash
# Every static check over the Android sources. Not a compiler, but it catches
# the classes of error that scripted edits and refactors introduce — including
# ones a green build cannot: Room migrations that do not match their entities
# crash only on upgrade, never on a fresh install.
set -e
cd "$(dirname "$0")/.."
echo "=========================================================="
echo " MECRC — Android static verification"
echo "=========================================================="
for check in kt-sanity prose-scan check1_symbols check2_wiring check3_contracts check4_migrations; do
  python3 "tools/$check.py"
  echo
done
echo "=========================================================="
echo " Clean run: syntax balanced, symbols resolve, DI complete,"
echo " interfaces implemented, Gradle aliases valid, Room/nav/VM"
echo " contracts consistent, migrations mirror entities."
echo "=========================================================="
