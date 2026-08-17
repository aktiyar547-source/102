"""
Find English prose sitting in Kotlin code — i.e. a comment line that lost its
`//` during an edit. This is exactly how WatermarkUtil.kt once failed a build.

Paren-aware: lines continuing an open argument list are skipped, otherwise
multi-line calls get reported as prose and the checker starts crying wolf.
"""
import os, re, sys

# A line that looks like an English sentence fragment: words and spaces only.
PROSE = re.compile(r"^\s*[A-Za-z][A-Za-z',\- ]{18,}[.,]?\s*$")

# Any hint of Kotlin syntax means it is code, not an orphaned comment.
SYNTAX = re.compile(
    r"[={}()\[\];:<>+*/&|!?@$]"
    r"|^\s*(//|/\*|\*|import|package|fun|val|var|class|object|interface|enum|"
    r"return|if|else|when|for|while|do|try|catch|finally|throw|override|private|"
    r"public|protected|internal|abstract|data|sealed|companion|const|suspend|"
    r"inline|operator|lateinit|by|in|is|as|out|reified|vararg|init)\b"
)

hits = []
scanned = 0

for root, _, files in os.walk("."):
    if "/build/" in root or ".git" in root:
        continue
    for fn in files:
        if not fn.endswith((".kt", ".kts")):
            continue
        path = os.path.join(root, fn)
        scanned += 1
        depth = 0          # unclosed ( or [ carried across lines
        in_block = False
        for i, line in enumerate(open(path, errors="ignore"), 1):
            stripped = line.strip()

            if in_block:
                if "*/" in stripped:
                    in_block = False
                continue
            if stripped.startswith("/*"):
                if "*/" not in stripped:
                    in_block = True
                continue
            if not stripped or stripped.startswith(("//", "*")):
                continue

            # only judge a line that begins a fresh statement
            if depth == 0 and PROSE.match(line) and not SYNTAX.search(line):
                hits.append((path, i, stripped))

            code = re.sub(r'"[^"]*"', '""', line)   # ignore string contents
            code = re.sub(r"//.*$", "", code)       # ignore trailing comment
            depth += code.count("(") - code.count(")")
            depth += code.count("[") - code.count("]")
            depth = max(depth, 0)

print(f"Scanned {scanned} Kotlin files for orphaned prose (broken comments)")
if hits:
    for p, i, t in hits:
        print(f"  *** {p}:{i}  ->  {t[:70]}")
    sys.exit(1)
print("  none found")
