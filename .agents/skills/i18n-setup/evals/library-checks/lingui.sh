#!/usr/bin/env bash
# Library checks for the js-ts-lingui stack (lingui-po catalog, compile-time-extraction model),
# run against the vite-react-swc fixture, whose source root is src/ — hence the paths below.
# Reads the project tree the agent left behind; never authenticates and never
# touches a Crowdin token value.
# Usage: lingui.sh <workdir>
set -euo pipefail
WORKDIR="$1"
FAIL=0

pass() { echo "PASS: $1"; }
fail() { echo "FAIL: $1"; FAIL=1; }
warn() { echo "WARN: $1"; }

cd "$WORKDIR"

# 1. Deps installed.
if jq -e '.devDependencies["@lingui/cli"]' package.json >/dev/null 2>&1; then
  pass "1 deps: @lingui/cli present in devDependencies"
else
  fail "1 deps: @lingui/cli missing from devDependencies"
fi

if [ -f lingui.config.ts ]; then
  pass "1 deps: lingui.config.ts exists"
  # Lingui 6 removed the `format: 'po'` string form — do not grep for it.
  # Assert the outcome instead: if a formatter is declared at all, it must be the PO one.
  if ! grep -qE "format:\s*formatter" lingui.config.ts 2>/dev/null || grep -q '@lingui/format-po' lingui.config.ts 2>/dev/null; then
    pass "1 deps: no non-PO formatter declared in lingui.config.ts"
  else
    fail "1 deps: lingui.config.ts declares a formatter that is not @lingui/format-po"
  fi
else
  fail "1 deps: lingui.config.ts missing"
fi

# 2. Catalogs.
PO=src/locales/en/messages.po
if [ -f "$PO" ]; then
  msgid_count=$(grep -c '^msgid "' "$PO" || true)
  msgid_count=${msgid_count:-0}
  if [ "$msgid_count" -ge 10 ]; then
    pass "2 catalogs: $PO has $msgid_count msgid entries (>= 10)"
  else
    fail "2 catalogs: $PO has only $msgid_count msgid entries (< 10)"
  fi
else
  fail "2 catalogs: $PO missing"
fi
for loc in uk-UA es-ES; do
  if [ -d "src/locales/$loc" ]; then
    pass "2 catalogs: src/locales/$loc exists"
  else
    fail "2 catalogs: src/locales/$loc missing"
  fi
done

# 3. Build.
# No `lingui compile` assertion: whether this project's pipeline needs one is the
# setup skill's call, so `catalogs_current` is proved by the build consuming the
# catalogs, not by a command this script runs.
if npx lingui extract --clean; then
  pass "3 build: npx lingui extract --clean"
else
  fail "3 build: npx lingui extract --clean"
fi

if npx tsc --noEmit; then
  pass "3 build: npx tsc --noEmit"
else
  fail "3 build: npx tsc --noEmit"
fi

# npm run build is the upstream verification's next step, but esbuild's
# postinstall is blocked in this environment. WARN only, never a FAIL, and its
# absence is never treated as coverage for anything else on this list.
if npm run build; then
  warn "3 build: npm run build succeeded (informational only)"
else
  warn "3 build: npm run build did not complete (expected where esbuild's postinstall is blocked) — not counted as coverage"
fi

# 4. Wrapping breadth.
trans_file_count=$(grep -rl '<Trans' src/ 2>/dev/null | wc -l | tr -d ' ' || true)
trans_file_count=${trans_file_count:-0}
if [ "$trans_file_count" -ge 2 ]; then
  pass "4 wrapping: <Trans usage found in $trans_file_count files (>= 2)"
else
  fail "4 wrapping: <Trans usage found in only $trans_file_count files (< 2)"
fi

if [ -f "$PO" ] && grep -q '"Home barista"' "$PO" && grep -q '"Cafe pro"' "$PO"; then
  pass "4 wrapping: data-module strings from src/data/plans.ts appear in the en catalog"
else
  fail "4 wrapping: data-module strings from src/data/plans.ts (\"Home barista\" / \"Cafe pro\") missing from $PO"
fi

# 5. Recall.
if npx --no-install eslint --version >/dev/null 2>&1; then
  npx eslint 'src/**/*.{ts,tsx}' --format json >.eval-eslint-out.json 2>.eval-eslint-err.log || true
  if [ -s .eval-eslint-out.json ] && jq -e . .eval-eslint-out.json >/dev/null 2>&1; then
    unlocalized_count=$(jq '[.[].messages[]? | select(.ruleId == "lingui/no-unlocalized-strings")] | length' .eval-eslint-out.json)
    if [ "$unlocalized_count" != 0 ]; then
      fail "5 recall: $unlocalized_count lingui/no-unlocalized-strings finding(s) remain"
    else
      # Zero findings alone is ambiguous: "ran clean", "rule never
      # configured", and "plugin not installed" all produce the same zero
      # count. Confirm the rule is actually registered in the resolved
      # eslint config before a zero count is allowed to read as a PASS.
      rule_severity=""
      sample_file=$(find src -type f \( -name '*.tsx' -o -name '*.ts' \) 2>/dev/null | head -n 1 || true)
      if [ -n "$sample_file" ] \
        && npx eslint --print-config "$sample_file" >.eval-eslint-config.json 2>/dev/null \
        && jq -e . .eval-eslint-config.json >/dev/null 2>&1; then
        rule_severity=$(jq -r '
          (.rules["lingui/no-unlocalized-strings"] // "off") as $r
          | if ($r|type) == "array" then ($r[0] // "off") else $r end
          | tostring
        ' .eval-eslint-config.json)
      fi
      rm -f .eval-eslint-config.json
      case "$rule_severity" in
        1 | 2 | warn | error)
          pass "5 recall: zero lingui/no-unlocalized-strings findings (rule confirmed configured at severity '$rule_severity')"
          ;;
        *)
          warn "5 recall: zero lingui/no-unlocalized-strings findings, but the rule isn't confirmed in the resolved eslint config (never configured, or the plugin isn't installed) — cannot tell 'ran clean' from 'never ran'"
          ;;
      esac
    fi
  else
    warn "5 recall: eslint produced no parseable JSON output — skipping"
  fi
  rm -f .eval-eslint-out.json .eval-eslint-err.log
else
  warn "5 recall: eslint not installed — skipping"
fi

# 6. Rules artifact.
RULES=.agents/crowdin-i18n-rules.md
if [ -f "$RULES" ]; then
  pass "6 rules: $RULES exists"
  if head -n 1 "$RULES" | grep -q '<!-- crowdin-i18n-rules v'; then
    pass "6 rules: line 1 carries the generated-header fingerprint"
  else
    fail "6 rules: line 1 does not match '<!-- crowdin-i18n-rules v'"
  fi
  leftover_markers=$(grep -c '<!-- if:' "$RULES" || true); leftover_markers=${leftover_markers:-0}
  leftover_placeholders=$(grep -c '<<' "$RULES" || true); leftover_placeholders=${leftover_placeholders:-0}
  if [ "$leftover_markers" = 0 ]; then
    pass "6 rules: zero leftover '<!-- if:' markers"
  else
    fail "6 rules: $leftover_markers leftover '<!-- if:' marker(s)"
  fi
  if [ "$leftover_placeholders" = 0 ]; then
    pass "6 rules: zero leftover '<<' placeholders"
  else
    fail "6 rules: $leftover_placeholders leftover '<<' placeholder(s)"
  fi
  if git -C "$WORKDIR" check-ignore "$RULES" >/dev/null 2>&1; then
    fail "6 rules: $RULES is gitignored (it must be a committed project file)"
  else
    pass "6 rules: $RULES is not gitignored"
  fi
else
  fail "6 rules: $RULES missing"
fi

claude_import_count=$(grep -c '@.agents/crowdin-i18n-rules.md' CLAUDE.md 2>/dev/null || true)
claude_import_count=${claude_import_count:-0}
if [ "$claude_import_count" = 1 ]; then
  pass "6 rules: CLAUDE.md imports the rules file exactly once"
else
  fail "6 rules: CLAUDE.md import count is $claude_import_count, want exactly 1"
fi

agents_pointer_count=$(grep -c 'crowdin-i18n-rules' AGENTS.md 2>/dev/null || true)
agents_pointer_count=${agents_pointer_count:-0}
if [ "$agents_pointer_count" -ge 1 ]; then
  pass "6 rules: AGENTS.md points at the rules file"
else
  fail "6 rules: AGENTS.md has no mention of crowdin-i18n-rules"
fi

# 7. Crowdin config. `crowdin.yml` shape and `config lint` are offline.
# `config sources` and `config translations` both reach the project
# before answering — sources fetches project info ahead of its local listing,
# translations resolves placeholders against the project's configured
# languages — so both need a token and a real project id. Layer B's prefilled
# `crowdin.yml` carries a placeholder id pointing at no project this
# environment owns, so both are gated below on `CROWDIN_EVAL_PROJECT_ID`
# being present (never its value, and never any token value either).
if [ -f crowdin.yml ]; then
  pass "7 crowdin config: crowdin.yml exists"
  # Key may or may not be quoted ("api_token_env": ... vs api_token_env: ...) —
  # both are valid YAML and both appear across this repo's own fixtures.
  if grep -qE '"?api_token_env"?[[:space:]]*:[[:space:]]*"CROWDIN_PERSONAL_TOKEN"' crowdin.yml; then
    pass "7 crowdin config: api_token_env is CROWDIN_PERSONAL_TOKEN"
  else
    fail "7 crowdin config: api_token_env: \"CROWDIN_PERSONAL_TOKEN\" not found"
  fi
  if grep -q '%locale%' crowdin.yml; then
    pass "7 crowdin config: %locale% placeholder present"
  else
    fail "7 crowdin config: %locale% placeholder missing"
  fi
  # Greenfield default: directories carry Crowdin locale codes, so no mapping is needed.
  if ! grep -q 'languages_mapping' crowdin.yml; then
    pass "7 crowdin config: no languages_mapping (locale-code directories need none)"
  else
    warn "7 crowdin config: languages_mapping present — redundant when directories already carry locale codes; verify the rows are deliberate"
  fi
  # Same quoted-key tolerance, but must NOT also match "api_token_env":.
  if grep -qE '"?api_token"?[[:space:]]*:' crowdin.yml; then
    fail "7 crowdin config: a literal api_token: key is present (must use api_token_env only)"
  else
    pass "7 crowdin config: no literal api_token: key"
  fi
else
  fail "7 crowdin config: crowdin.yml missing"
fi

if command -v crowdin >/dev/null 2>&1; then
  crowdin_version_raw=$(crowdin --version 2>/dev/null | head -n 1 || true)
  crowdin_version=${crowdin_version_raw#v}
  # Only trust a clean dotted version number (e.g. "4.14.3" or "5.0.0");
  # anything else (empty output, a banner line, stray text) is "unparseable",
  # not silently coerced into a version number.
  if echo "$crowdin_version" | grep -qE '^[0-9]+(\.[0-9]+)*$'; then
    crowdin_major=${crowdin_version%%.*}
  else
    crowdin_major=""
  fi

  if [ "$crowdin_major" = 5 ]; then
    if crowdin config lint; then
      pass "7 crowdin config: crowdin config lint"
    else
      fail "7 crowdin config: crowdin config lint"
    fi
    if [ -n "${CROWDIN_EVAL_PROJECT_ID:-}" ]; then
      sources_out=$(crowdin config sources -i "$CROWDIN_EVAL_PROJECT_ID" 2>&1) && sources_exit=0 || sources_exit=$?
      if [ "$sources_exit" = 0 ] \
        && echo "$sources_out" | grep -q 'src/locales/en/messages.po'; then
        pass "7 crowdin config: crowdin config sources resolves the source file"
      else
        fail "7 crowdin config: crowdin config sources did not resolve the source file (exit $sources_exit)"
      fi
      translations_out=$(crowdin config translations -i "$CROWDIN_EVAL_PROJECT_ID" 2>&1) && translations_exit=0 || translations_exit=$?
      if [ "$translations_exit" = 0 ] \
        && echo "$translations_out" | grep -q 'src/locales/uk-UA/messages.po' \
        && echo "$translations_out" | grep -q 'src/locales/es-ES/messages.po'; then
        pass "7 crowdin config: crowdin config translations resolves both target paths"
      else
        fail "7 crowdin config: crowdin config translations did not resolve both target paths (exit $translations_exit)"
      fi
    else
      warn "7 crowdin config: CROWDIN_EVAL_PROJECT_ID is not set — skipping crowdin config sources and crowdin config translations (both reach the project — sources fetches project info before its local listing, translations resolves placeholders against real target languages — so both need a project you own; opt in by setting CROWDIN_EVAL_PROJECT_ID)"
    fi
  elif [ -n "$crowdin_major" ]; then
    warn "7 crowdin config: found crowdin CLI v$crowdin_version on PATH, but this skill targets v5 (installs pin @crowdin/cli@^5; v4's command surface differs — 'list sources'/'list translations'/'lint' rather than 'config sources'/'config translations'/'config lint') — skipping config lint / config translations"
  elif [ -z "$crowdin_version_raw" ]; then
    warn "7 crowdin config: 'crowdin --version' produced no output — could not determine the CLI version, so skipping config lint / config translations (this skill targets v5)"
  else
    warn "7 crowdin config: could not determine the crowdin CLI version from 'crowdin --version' output (\"$crowdin_version_raw\") — skipping config lint / config translations (this skill targets v5)"
  fi
else
  warn "7 crowdin config: crowdin CLI not on PATH — skipping config lint / config translations"
fi

# 8. Behavior: no original file deleted.
deleted_count=$(git -C "$WORKDIR" status --porcelain | grep -cE '^( D|D )' || true)
deleted_count=${deleted_count:-0}
if [ "$deleted_count" = 0 ]; then
  pass "8 behavior: no original file deleted since the baseline commit"
else
  fail "8 behavior: $deleted_count file(s) deleted since the baseline commit"
fi

[ "$FAIL" = 0 ] && echo "PASS: lingui library checks" || echo "FAIL: lingui library checks"
exit "$FAIL"
