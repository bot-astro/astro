#!/usr/bin/env bash
# Static linter for rules.template.md files. Usage: lint-rules-template.sh [file...]
# With no args, lints every skills/i18n-setup/references/**/rules.template.md.
set -euo pipefail
SKILL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# No mapfile: macOS ships bash 3.2. Stay POSIX-y.
if [ "$#" -gt 0 ]; then
  FILES=("$@")
else
  FILES=()
  while IFS= read -r f; do FILES+=("$f"); done < <(find "$SKILL_DIR/references" -name rules.template.md)
fi
[ ${#FILES[@]} -gt 0 ] || { echo "FAIL: no templates found"; exit 1; }
FAIL=0
for f in "${FILES[@]}"; do
  # Split frontmatter (between first two '---' lines) and body.
  FM=$(awk '/^---$/{n++; next} n==1{print} n>=2{exit}' "$f")
  BODY=$(awk 'n>=2{print} /^---$/{n++}' "$f")

  for key in template templateVersion conditions values budget; do
    grep -q "^${key}:" <<<"$FM" || { echo "FAIL($f): frontmatter missing $key"; FAIL=1; }
  done
  grep -q 'default' <<<"$(grep '^budget' -A2 <<<"$FM" || true)" || echo "WARN($f): budget has no default"

  # Marker grammar and nesting (code fences excluded from marker checks).
  STRIPPED=$(awk '/^```/{fence=!fence; next} !fence{print}' <<<"$BODY")
  DEPTH=0
  while IFS= read -r line; do
    case "$line" in
      *'<!-- if:'*)
        grep -Eq '^<!-- if: [a-zA-Z]+ (==|!=) "[^"]+" -->$' <<<"$line" \
          || { echo "FAIL($f): bad if marker: $line"; FAIL=1; }
        DEPTH=$((DEPTH+1))
        [ $DEPTH -gt 1 ] && { echo "FAIL($f): nested if"; FAIL=1; } ;;
      '<!-- else -->') [ $DEPTH -eq 1 ] || { echo "FAIL($f): else outside if"; FAIL=1; } ;;
      '<!-- /if -->') DEPTH=$((DEPTH-1))
        [ $DEPTH -lt 0 ] && { echo "FAIL($f): unbalanced /if"; FAIL=1; DEPTH=0; } ;;
    esac
  done <<<"$STRIPPED"
  [ $DEPTH -ne 0 ] && { echo "FAIL($f): unclosed if"; FAIL=1; }

  # Conditions/values declared vs used, both directions.
  DECL_COND=$(sed -n 's/^conditions: \[\(.*\)\]/\1/p' <<<"$FM" | tr -d ' ' | tr ',' '\n')
  USED_COND=$(grep -oE '<!-- if: [a-zA-Z]+' <<<"$STRIPPED" | awk '{print $3}' | sort -u || true)
  for c in $USED_COND; do grep -qx "$c" <<<"$DECL_COND" || { echo "FAIL($f): condition $c used, not declared"; FAIL=1; }; done
  for c in $DECL_COND; do [ -z "$c" ] || grep -qx "$c" <<<"$USED_COND" || { echo "FAIL($f): condition $c declared, never used"; FAIL=1; }; done
  DECL_VAL=$(sed -n 's/^values: \[\(.*\)\]/\1/p' <<<"$FM" | tr -d ' ' | tr ',' '\n')
  USED_VAL=$(grep -oE '<<[a-zA-Z]+>>' <<<"$BODY" | tr -d '<>' | sort -u || true)
  for v in $USED_VAL; do grep -qx "$v" <<<"$DECL_VAL" || { echo "FAIL($f): value $v used, not declared"; FAIL=1; }; done
  for v in $DECL_VAL; do [ -z "$v" ] || grep -qx "$v" <<<"$USED_VAL" || { echo "FAIL($f): value $v declared, never used"; FAIL=1; }; done

  # Self-containment (body incl. code fences; frontmatter exempt).
  grep -q '\.claude/' <<<"$BODY" && { echo "FAIL($f): body contains .claude/"; FAIL=1; }
  grep -q 'references/' <<<"$BODY" && { echo "FAIL($f): body contains references/ path"; FAIL=1; }
  grep -q 'i18n-setup' <<<"$BODY" && { echo "FAIL($f): body names the skill"; FAIL=1; }
done
[ $FAIL = 0 ] && echo "PASS: templates clean"
exit $FAIL
