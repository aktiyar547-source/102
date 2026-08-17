"""CHECK 1 — every capitalised symbol used is imported, local, or same-package."""
import sys, re, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
from lib import load

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



files = load(ROOT)

# Everything declared anywhere in the project, grouped by package
by_pkg = {}
for p, f in files.items():
    by_pkg.setdefault(f["pkg"], set()).update(f["decls"])

BUILTIN = set("""
LongArray IntArray ByteArray CharArray ShortArray FloatArray DoubleArray BooleanArray
Charsets Regex Triple Sequence Comparator
BuildConfig StringBuilder Base64 Bundle
String Int Long Boolean Unit List Map Set Pair Triple Any Nothing Throwable Exception
Float Double Byte Char Short Array MutableList MutableMap MutableSet LinkedHashMap
HashMap ArrayList Result Comparable Number Iterable Collection Sequence Regex Math
System Thread Runtime Error RuntimeException IllegalStateException IllegalArgumentException
IndexOutOfBoundsException NullPointerException Comparator Lazy Function0 Function1
Deprecated JvmStatic JvmField Suppress OptIn Volatile Synchronized Throws
TODO Enum Companion Instant Duration UUID Date Calendar Locale File IOException
""".split())

problems = []
for p, f in files.items():
    code = f["code"]
    # Remove annotation names, named arguments, and member access after a dot
    scan = re.sub(r'@\w+', ' ', code)
    scan = re.sub(r'\.\s*\w+', ' ', scan)          # member access
    scan = re.sub(r'(?<![\w.])(\w+)\s*=(?!=)', ' ', scan)  # named args / assignment
    used = set(re.findall(r'(?<![\w.])([A-Z]\w+)', scan))

    # enum entries and CONSTANTS declared in this file
    # enum entries: at line start OR comma-separated on one line, any case
    enum_entries = set(re.findall(r'(?:^|,|\{)\s*([A-Z]\w*)\s*[({,;]', code, re.M))
    enum_entries |= set(re.findall(r'(?:^|,|\{)\s*([A-Z]\w*)\s*(?=[,\n}])', code, re.M))
    # named arguments, including multi-line calls
    # Named arguments, including ones on their own line inside a multi-line call.
    named_args = set(re.findall(r'(?:^|[(,]|\n)\s*(\w+)\s*=\s*', code, re.M))
    # Properties of entities/data classes read through an implicit receiver
    # inside `private fun X.toDomain()` — valid Kotlin, and invisible to a
    # regex that only sees a bare capitalised name.
    receiver_props = set()
    for m in re.finditer(r'fun\s+(\w+)\.\w+\s*\(', code):
        for cls_file in files.values():
            for cm in re.finditer(
                r'(?:data\s+)?class\s+' + m.group(1) + r'\s*\(([\s\S]*?)\n\)',
                cls_file["code"],
            ):
                receiver_props |= set(re.findall(r'va[lr]\s+(\w+)\s*:', cm.group(1)))

    known = (set(f["imports"]) | f["decls"] | BUILTIN
             | by_pkg.get(f["pkg"], set()) | enum_entries | named_args | receiver_props)
    # star imports make everything from that package fair game
    stars = [i for i in re.findall(r'^import\s+([\w.]+)\.\*', f["src"], re.M)]
    for s in stars:
        known |= by_pkg.get(s, set())
    # generic type params declared in this file
    known |= set(re.findall(r'<\s*([A-Z])\s*[,>]', code))

    unknown = sorted(u for u in used if u not in known)
    if unknown:
        problems.append((p.replace(ROOT + '/', ''), unknown))

print(f"CHECK 1 — symbol resolution across {len(files)} files")
if not problems:
    print("  PASS: every symbol resolves")
else:
    for p, u in problems:
        print(f"  ? {p}: {', '.join(u[:8])}")
print(f"  files with unresolved: {len(problems)}")
