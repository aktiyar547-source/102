"""
Lightweight Kotlin sanity check: strips comments and string literals, then
verifies bracket balance and detects unterminated strings. Catches the whole
class of damage that scripted text edits can cause.
"""
import os, sys

def strip(src):
    out, i, n = [], 0, len(src)
    state = None  # None | line | block | str | rawstr | char
    while i < n:
        c = src[i]; nxt = src[i+1] if i+1 < n else ""
        if state is None:
            if c == "/" and nxt == "/": state = "line"; i += 2; continue
            if c == "/" and nxt == "*": state = "block"; i += 2; continue
            if src.startswith('"""', i): state = "rawstr"; i += 3; out.append(" "); continue
            if c == '"': state = "str"; i += 1; out.append(" "); continue
            if c == "'": state = "char"; i += 1; out.append(" "); continue
            out.append(c); i += 1; continue
        if state == "line":
            if c == "\n": state = None; out.append("\n")
            i += 1; continue
        if state == "block":
            if c == "*" and nxt == "/": state = None; i += 2; continue
            if c == "\n": out.append("\n")
            i += 1; continue
        if state == "rawstr":
            if src.startswith('"""', i): state = None; i += 3; continue
            if c == "\n": out.append("\n")
            i += 1; continue
        if state == "str":
            if c == "\\": i += 2; continue
            if c == '"': state = None; i += 1; continue
            if c == "\n": return None, "unterminated string"
            i += 1; continue
        if state == "char":
            if c == "\\": i += 2; continue
            if c == "'": state = None; i += 1; continue
            i += 1; continue
    if state in ("str", "char"): return None, "unterminated string at EOF"
    if state == "block": return None, "unterminated block comment"
    if state == "rawstr": return None, "unterminated raw string"
    return "".join(out), None

problems = []
checked = 0
for root, _, files in os.walk("."):
    if "/build/" in root or ".git" in root: continue
    for fn in files:
        if not fn.endswith((".kt", ".kts")): continue
        path = os.path.join(root, fn)
        src = open(path, errors="ignore").read()
        code, err = strip(src)
        checked += 1
        if err:
            problems.append((path, err)); continue
        for open_c, close_c, label in [("{","}","braces"), ("(",")","parens"), ("[","]","brackets")]:
            d = code.count(open_c) - code.count(close_c)
            if d != 0:
                problems.append((path, f"{label} unbalanced by {d:+d}"))
        if "package " not in src and not fn.endswith(".gradle.kts"):
            problems.append((path, "no package declaration"))

print(f"Checked {checked} Kotlin files")
if problems:
    for p, m in problems: print(f"  *** {p}: {m}")
    sys.exit(1)
print("  All files: balanced brackets, terminated strings, package present.")
