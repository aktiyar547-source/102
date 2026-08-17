"""CHECKS 7-11: Gradle aliases, screen/VM contracts, Room, navigation, syntax."""
import sys, re, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
from lib import load, strip_code

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
fail = []
def rep(name, ok, detail=""):
    print(f"  [{'PASS' if ok else 'FAIL'}] {name}" + (f" — {detail}" if detail and not ok else ""))
    if not ok: fail.append(name)

print("CHECK 7 — Gradle version-catalog aliases resolve")
catalog = open(os.path.join(PROJECT, 'gradle', 'libs.versions.toml')).read()
lib_keys = set(re.findall(r'^([\w-]+)\s*=\s*\{', catalog, re.M))
plugin_sec = catalog.split("[plugins]")[1] if "[plugins]" in catalog else ""
plugin_keys = set(re.findall(r'^([\w-]+)\s*=\s*\{', plugin_sec, re.M))
gradle = open(os.path.join(APP, 'build.gradle.kts')).read() + open(os.path.join(PROJECT, 'build.gradle.kts')).read()
used_libs = set(re.findall(r'libs\.((?!plugins\.)[\w.]+)', gradle))
used_plugins = set(re.findall(r'libs\.plugins\.([\w.]+)', gradle))
missing = sorted(a for a in used_libs if a.replace('.', '-') not in lib_keys)
missing_p = sorted(a for a in used_plugins if a.replace('.', '-') not in plugin_keys)
rep(f"library aliases ({len(used_libs)} used)", not missing, str(missing))
rep(f"plugin aliases ({len(used_plugins)} used)", not missing_p, str(missing_p))

print("\nCHECK 8 — screens only use members their ViewModel exposes")
vms = {}
for p, f in files.items():
    if not p.endswith("ViewModel.kt"): continue
    for m in re.finditer(r'class\s+(\w+ViewModel)', f["code"]):
        vms[m.group(1)] = (set(re.findall(r'\bfun\s+(\w+)\s*\(', f["code"]))
                           | set(re.findall(r'\b(?:val|var)\s+(\w+)\s*[:=]', f["code"])))
bad = []
for p, f in files.items():
    if "/ui/" not in p or p.endswith("ViewModel.kt"): continue
    vm = re.search(r'viewModel:\s*(\w+ViewModel)', f["code"])
    if not vm or vm.group(1) not in vms: continue
    for m in re.finditer(r'viewModel(?:::|\.)(\w+)', f["code"]):
        if m.group(1) not in vms[vm.group(1)]:
            bad.append(f"{os.path.basename(p)}: viewModel.{m.group(1)}")
rep(f"viewModel members exist ({len(vms)} ViewModels)", not bad, str(bad[:4]))

print("\nCHECK 9 — UI state fields exist")
bad = []
for p, f in files.items():
    if not p.endswith("ViewModel.kt"): continue
    for dm in re.finditer(r'data class\s+(\w+UiState)\s*\(([\s\S]*?)\n\)', f["code"]):
        fields = set(re.findall(r'va[lr]\s+(\w+)\s*:', dm.group(2)))
        screen = os.path.basename(p).replace("ViewModel.kt", "Screen.kt")
        for sp, sf in files.items():
            if os.path.basename(sp) != screen: continue
            cleaned = sf["code"].replace("viewModel.state.", "VM.")
            for u in re.finditer(r'(?<![\w.])state\.(\w+)', cleaned):
                if u.group(1) not in fields:
                    bad.append(f"{screen}: state.{u.group(1)}")
rep("state.X fields declared", not bad, str(bad[:4]))

print("\nCHECK 9b — ViewModels only use fields their own UiState declares")

def copy_args(code, start):
    """Top-level named arguments of the .copy( beginning at `start`.

    Written as a paren scan rather than a regex: a copy call routinely contains
    nested constructor calls, and a regex cannot tell those arguments from the
    copy's own. An earlier regex version reported four false positives, which is
    worse than no check at all.
    """
    i, depth, args, token = start, 0, [], ""
    while i < len(code):
        ch = code[i]
        if ch == "(":
            depth += 1
            if depth == 1:
                token = ""
                i += 1
                continue
        elif ch == ")":
            depth -= 1
            if depth == 0:
                break
        if depth == 1:
            if ch == ",":
                token = ""
            else:
                token += ch
                if ch == "=" and not token.rstrip("=").rstrip().endswith(("=", "!", "<", ">")):
                    name = token[:-1].strip()
                    if name.isidentifier():
                        args.append(name)
                    token = ""
        i += 1
    return args

bad = []
for path, f in files.items():
    if not path.endswith("ViewModel.kt"):
        continue
    for dm in re.finditer(r'data class\s+(\w+UiState)\s*\(([\s\S]*?)\n\)', f["code"]):
        fields = set(re.findall(r'va[lr]\s+(\w+)\s*:', dm.group(2)))
        for m in re.finditer(r'(?:_state\.value|it)\.copy', f["code"]):
            for arg in copy_args(f["code"], m.end()):
                if arg not in fields:
                    bad.append(f"{os.path.basename(path)}: copy({arg} = ...)")
rep("copy() arguments exist on the UiState", not bad, str(sorted(set(bad))[:4]))

print("\nCHECK 10 — Room: DAO queries match entity columns")
ent = {}
for p, f in files.items():
    if "/entity/" not in p: continue
    tm = re.search(r'tableName = "(\w+)"', f["src"])
    if tm:
        body = f["code"][f["code"].find("data class"):]
        ent[tm.group(1)] = set(re.findall(r'va[lr]\s+(\w+)\s*:', body))
bad = []
for p, f in files.items():
    if "/dao/" not in p: continue
    for q in re.finditer(r'@Query\("([^"]+)"\)', f["src"]):
        sql = q.group(1)
        tm = re.search(r'FROM\s+(\w+)|UPDATE\s+(\w+)', sql, re.I)
        if not tm: continue
        t = tm.group(1) or tm.group(2)
        if t not in ent: continue
        for col in set(re.findall(r'\b(\w+)\s*=\s*:', sql)) | set(re.findall(r'SET\s+(\w+)\s*=', sql, re.I)):
            if col not in ent[t]:
                bad.append(f"{os.path.basename(p)}: {t}.{col}")
rep(f"DAO columns exist ({len(ent)} entities)", not bad, str(bad[:4]))

print("\nCHECK 11 — navigation arguments wired")
routes = {}
for p, f in files.items():
    if not p.endswith("Routes.kt"): continue
    for m in re.finditer(r'const val (\w+) = "([^"]+)"', f["src"]):
        a = re.findall(r'\{(\w+)\}', m.group(2))
        if a: routes[m.group(1)] = a
nav = "".join(f["src"] for p, f in files.items() if p.endswith("MecrcNavGraph.kt"))
bad = [f"{r}:{a}" for r, args in routes.items() for a in args
       if f'navArgument("{a}")' not in nav]
rep(f"every route arg has a navArgument ({len(routes)} routes)", not bad, str(bad))
# SavedStateHandle keys must be real route args
allargs = {a for args in routes.values() for a in args}
bad = []
for p, f in files.items():
    for k in re.findall(r'savedStateHandle\["(\w+)"\]', f["src"]):
        if k not in allargs:
            bad.append(f"{os.path.basename(p)}: '{k}'")
rep("SavedStateHandle keys match route args", not bad, str(bad))

print("\n" + "="*54)
print("ALL CHECKS PASSED" if not fail else f"{len(fail)} FAILURES: {fail}")
