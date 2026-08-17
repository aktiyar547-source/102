"""
CHECK 14 — Room migrations must mirror the entities exactly.

Room compares the migrated schema against the @Entity definitions the first time
the database is opened after an upgrade, and throws if they differ. A migration
that creates a plausible-looking table is not enough: a wrong table name or a
single wrong column type crashes the app on launch for every existing user,
while a fresh install works perfectly. That asymmetry is what makes it easy to
ship.
"""
import os, re, sys


def _find_app_dir():
    """app/ module root, whether or not it sits under android/."""
    here = os.path.dirname(os.path.abspath(__file__))
    for base in (os.path.dirname(here), here, os.getcwd()):
        for candidate in ("app", os.path.join("android", "app")):
            path = os.path.join(base, candidate)
            if os.path.isfile(os.path.join(path, "build.gradle.kts")):
                return path
    raise SystemExit("check: could not locate the app module")

APP = _find_app_dir()
PROJECT = os.path.dirname(APP)
ROOT = os.path.join(APP, "src", "main")

ROOT = os.path.join(APP, "src/main/java/com/middleeastcontainer/data/database")
KT_TO_SQL = {"Long": "INTEGER", "Int": "INTEGER", "String": "TEXT",
             "Float": "REAL", "Double": "REAL", "Boolean": "INTEGER"}

entities = {}
for dp, _, fns in os.walk(os.path.join(ROOT, "entity")):
    for fn in fns:
        if not fn.endswith(".kt"):
            continue
        src = open(os.path.join(dp, fn)).read()
        for m in re.finditer(r'@Entity\(\s*tableName = "(\w+)"([\s\S]*?)\ndata class (\w+)\(([\s\S]*?)\n\)', src):
            table, _, _, body = m.group(1), m.group(2), m.group(3), m.group(4)
            cols = []
            for c in re.finditer(r'val (\w+): (\w+)(\?)?', body):
                if c.group(2) not in KT_TO_SQL:
                    continue
                cols.append((c.group(1), KT_TO_SQL[c.group(2)], c.group(3) is None))
            entities[table] = cols

mig_path = os.path.join(ROOT, "Migrations.kt")
mig = open(mig_path).read() if os.path.exists(mig_path) else ""

created = {}
for m in re.finditer(r'CREATE TABLE IF NOT EXISTS (\w+) \(([\s\S]*?)"""', mig):
    table, body = m.group(1), m.group(2)
    cols = []
    for line in body.splitlines():
        line = line.strip().rstrip(",")
        cm = re.match(r'^(\w+) (INTEGER|TEXT|REAL)( .*)?$', line)
        if cm and cm.group(1).upper() not in ("FOREIGN", "PRIMARY"):
            cols.append((cm.group(1), cm.group(2), "NOT NULL" in (cm.group(3) or "")))
    created[table] = cols

problems = []
for table, cols in created.items():
    if table not in entities:
        problems.append(f"migration creates '{table}' but no @Entity maps to it")
        continue
    want = {c[0]: c for c in entities[table]}
    got = {c[0]: c for c in cols}
    for name, spec in want.items():
        if name not in got:
            problems.append(f"{table}.{name} missing from migration")
        elif got[name] != spec:
            problems.append(
                f"{table}.{name}: entity {spec[1]}/notNull={spec[2]}, "
                f"migration {got[name][1]}/notNull={got[name][2]}")
    for extra in set(got) - set(want):
        problems.append(f"{table}.{extra} exists only in the migration")

# Every version step must be covered.
versions = sorted(int(v) for v in re.findall(r'Migration\((\d+), \d+\)', mig))
db = open(os.path.join(ROOT, "MecrcDatabase.kt")).read()
current = int(re.search(r'version = (\d+)', db).group(1))
covered = {int(a) for a, b in re.findall(r'Migration\((\d+), (\d+)\)', mig)}
for v in range(1, current):
    if v not in covered:
        problems.append(f"no migration from version {v}")

print(f"CHECK 14 — migrations mirror entities ({len(created)} tables, db v{current})")
if problems:
    for p in problems:
        print(f"  *** {p}")
    sys.exit(1)
print("  [PASS] every migrated table matches its entity, all versions covered")
