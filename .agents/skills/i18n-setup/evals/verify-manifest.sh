#!/bin/bash
set -uo pipefail

# Usage: verify-manifest.sh
# Static verifier for skills/i18n-setup/manifest.json. Three layers:
#   1. JSON-Schema validation against manifest.schema.json (skipped with a
#      warning when the jsonschema module is absent — the schema still
#      validates live in any editor that honors the $schema key)
#   2. Every path-shaped string in the manifest resolves on disk — every
#      string, wherever it sits, so a path under a new key is checked the
#      day the key is invented
#   3. Cross-file invariants a schema cannot express
# Runs standalone against a checkout — no fixture, no agent, no network.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "--- Layer 1: JSON-Schema validation ---"
if python3 -c "import jsonschema" 2>/dev/null; then
  if OUT=$(python3 - "$SKILL_DIR" <<'PY' 2>&1
import json, pathlib, sys
import jsonschema
d = pathlib.Path(sys.argv[1])
schema = json.loads((d / "manifest.schema.json").read_text())
manifest = json.loads((d / "manifest.json").read_text())
errors = sorted(
    jsonschema.Draft202012Validator(schema).iter_errors(manifest),
    key=lambda e: list(e.absolute_path),
)
for e in errors[:20]:
    loc = "/".join(str(x) for x in e.absolute_path) or "<root>"
    print(f"{loc}: {e.message}")
sys.exit(1 if errors else 0)
PY
  ); then
    echo "PASS: manifest.json validates against manifest.schema.json"
  else
    echo "$OUT"
    echo "FAIL: schema validation"
    exit 1
  fi
else
  echo "WARN: python3 jsonschema module not installed - layer 1 skipped (pip install jsonschema)"
fi

echo "--- Layers 2 and 3: path resolution and cross-file invariants ---"
python3 - "$SKILL_DIR" <<'PY'
import json, pathlib, re, sys

skill_dir = pathlib.Path(sys.argv[1])
manifest = json.loads((skill_dir / "manifest.json").read_text())
failures = []

def check(ok, label):
    print(("PASS" if ok else "FAIL") + ": " + label)
    if not ok:
        failures.append(label)

# Layer 2: walk EVERY string value; anything shaped like a skill-relative
# path must resolve. Checking only known blocks goes blind the day a new
# key carries a path.
def strings(node):
    if isinstance(node, dict):
        for v in node.values():
            yield from strings(v)
    elif isinstance(node, list):
        for v in node:
            yield from strings(v)
    elif isinstance(node, str):
        yield node

path_like = [s for s in strings(manifest) if re.match(r"^(\./)?references/|^\./", s)]
missing = [s for s in path_like if not (skill_dir / s).is_file()]
check(not missing, f"all {len(path_like)} path-shaped strings resolve on disk"
      + (f" — missing: {missing}" if missing else ""))

schema_ref = manifest.get("$schema", "")
check(bool(schema_ref) and (skill_dir / schema_ref).is_file(),
      "$schema names a schema file that exists")

# Layer 3: invariants across entries and across files.
stacks = manifest["stacks"]
catalogs = manifest["catalogs"]

variants = [s["variant"] for s in stacks]
check(len(variants) == len(set(variants)), "stack variants are unique")

catalog_ids = [c["id"] for c in catalogs]
check(len(catalog_ids) == len(set(catalog_ids)), "catalog ids are unique")

dangling = [s["variant"] for s in stacks if s["catalog"] not in catalog_ids]
check(not dangling, "every stack's catalog names an existing catalogs[] row"
      + (f" — dangling: {dangling}" if dangling else ""))

bad_translation = [c["id"] for c in catalogs if "%locale%" not in c["translation"]]
check(not bad_translation, "every translation pattern carries %locale%"
      + (f" — violations: {bad_translation}" if bad_translation else ""))

bad_source = [c["id"] for c in catalogs if "%locale%" in c["source"]]
check(not bad_source, "no source path carries a language placeholder"
      + (f" — violations: {bad_source}" if bad_source else ""))

# A role list naming real skills (anything that is not an internal
# reference file) needs the install commands the dependency gate hands out.
for s in stacks:
    roles = s["skills"]
    named = [e for r in ("setup", "convert", "recall")
             for e in roles[r] if not e.startswith("references/")]
    if named:
        check("install" in roles and "installClaudeCode" in roles,
              f"{s['variant']}: role lists name skills, so both install commands are present")

# Goldens agree with the manifest: a plan expectation that names a variant
# must name one that exists.
for golden in sorted((skill_dir / "evals" / "expectations" / "plan").glob("*.json")):
    data = json.loads(golden.read_text())
    v = data.get("variant")
    if v is not None:
        check(v in variants, f"expectations/plan/{golden.name}: variant '{v}' exists in the manifest")

sys.exit(1 if failures else 0)
PY
STATUS=$?

if [ $STATUS -eq 0 ]; then
  echo "PASS: manifest verification"
else
  echo "FAIL: manifest verification"
fi
exit $STATUS
