"""Shared Kotlin source utilities: strip comments/strings, collect declarations."""
import os, re

def strip_code(src):
    """Remove comments and string literals, preserving line structure."""
    out, i, n = [], 0, len(src)
    state = None
    while i < n:
        c = src[i]; nxt = src[i+1] if i+1 < n else ""
        if state is None:
            if c == "/" and nxt == "/": state = "line"; i += 2; continue
            if c == "/" and nxt == "*": state = "block"; i += 2; continue
            if src.startswith('"""', i): state = "raw"; i += 3; continue
            if c == '"': state = "str"; i += 1; continue
            if c == "'": state = "chr"; i += 1; continue
            out.append(c); i += 1; continue
        if state == "line":
            if c == "\n": state = None; out.append("\n")
            i += 1; continue
        if state == "block":
            if c == "*" and nxt == "/": state = None; i += 2; continue
            if c == "\n": out.append("\n")
            i += 1; continue
        if state == "raw":
            if src.startswith('"""', i): state = None; i += 3; continue
            if c == "\n": out.append("\n")
            i += 1; continue
        if state == "str":
            if c == "\\": i += 2; continue
            if c == '"': state = None; i += 1; continue
            if c == "\n": state = None; out.append("\n")
            i += 1; continue
        if state == "chr":
            if c == "\\": i += 2; continue
            if c == "'": state = None; i += 1; continue
            i += 1; continue
    return "".join(out)

def kt_files(root):
    for dp, _, fns in os.walk(root):
        if "/build/" in dp or "/.git" in dp: continue
        for fn in fns:
            if fn.endswith(".kt"):
                yield os.path.join(dp, fn)

def load(root):
    """path -> {src, code, pkg, imports, decls}"""
    files = {}
    for p in kt_files(root):
        src = open(p, errors="ignore").read()
        code = strip_code(src)
        pkg = re.search(r'^package\s+([\w.]+)', src, re.M)
        files[p] = {
            "src": src,
            "code": code,
            "pkg": pkg.group(1) if pkg else "",
            "imports": {m.split(".")[-1]: m for m in re.findall(r'^import\s+([\w.]+)', src, re.M)},
            "decls": set(re.findall(r'\b(?:class|interface|object|enum class)\s+(\w+)', code))
                     | set(re.findall(r'\bfun\s*(?:<[^>]+>\s*)?(\w+)\s*\(', code))
                     | set(re.findall(r'\b(?:val|var)\s+(\w+)', code)),
        }
    return files
