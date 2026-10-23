#!/usr/bin/env bash
# Layer A: detect -> decide -> plan only. No installs, no network.
# One story, top to bottom: assemble a throwaway workdir, run the agent with
# every question pre-answered, verify what it wrote against the golden.
# Usage: run-layer-a.sh <fixture-name>   (KEEP_WORKDIR=1 to keep the temp dir)
set -euo pipefail
EVALS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_DIR="$(dirname "$EVALS_DIR")"
FIXTURE="$1"
CATEGORY=$(jq -r --arg f "$FIXTURE" '.[$f].category' "$EVALS_DIR/fixtures.json")
WORKDIR=$(mktemp -d)
cleanup() { [ "${KEEP_WORKDIR:-0}" = 1 ] && echo "workdir kept: $WORKDIR" || rm -rf "$WORKDIR"; }
trap cleanup EXIT
FAIL=0

# --- 1. Assemble the workdir: fixture + this skill (minus evals) ------------
"$EVALS_DIR/helpers/prepare-workdir.sh" "$FIXTURE" "$WORKDIR"
mkdir -p "$WORKDIR/.claude/skills"
cp -R "$SKILL_DIR" "$WORKDIR/.claude/skills/i18n-setup"
rm -rf "$WORKDIR/.claude/skills/i18n-setup/evals"
cp "$WORKDIR/package.json" "$WORKDIR/.eval-package-baseline.json"

# --- 2. Run the agent with every question pre-answered ----------------------
if [ "$CATEGORY" = hard-stop ]; then
  PROMPT="Use the i18n-setup skill to internationalize this project. Run the inspection and tell me what you find."
else
  case "$CATEGORY" in
    connect-only|collapse)
      ENTRY_POINT_ANSWER="entry point: accept the entry point you recommend"
      ;;
    *)
      ENTRY_POINT_ANSWER="entry point: full journey"
      ;;
  esac
  PROMPT="Use the i18n-setup skill on this project. Answers to every question, so do not ask me anything: $ENTRY_POINT_ANSWER; library: lingui; source locale: en; target locales: uk, es-ES; product description: BrewLog is a coffee-brewing log: home baristas track the brews they make, log cups as they go, and pick a plan (Home barista or Cafe pro) for more tasting notes and recipe sharing; setup mode: unguided; add-ons: none; Crowdin edition: crowdin.com; skip branch creation. Run detection and planning only. STOP once .crowdin/plan.md is written. Do not execute the plan, do not install any packages, do not modify project files."
fi

( cd "$WORKDIR" && { claude -p "$PROMPT" --dangerously-skip-permissions 2>&1 || true; } | tee .eval-agent-output.txt )

# --- 3a. Verify a hard-stop fixture: the refusal changed nothing ------------
verify_hard_stop() {
  local exp msg f
  exp="$EVALS_DIR/$(jq -r --arg f "$FIXTURE" '.[$f].expectedHardStop' "$EVALS_DIR/fixtures.json")"

  msg=$(jq -r '.messageContains' "$exp")
  grep -qiF "$msg" "$WORKDIR/.eval-agent-output.txt" || { echo "FAIL: output lacks: $msg"; FAIL=1; }
  while IFS= read -r f; do
    [ -e "$WORKDIR/$f" ] || { echo "FAIL: expected $f to exist"; FAIL=1; }
  done < <(jq -r '.mustCreate[]? // empty' "$exp")
  while IFS= read -r f; do
    [ -e "$WORKDIR/$f" ] && { echo "FAIL: $f must not exist"; FAIL=1; }
  done < <(jq -r '.mustNotCreate[]? // empty' "$exp")
  if [ "$(jq -r '.depsMustBeUnchanged' "$exp")" = true ]; then
    diff -q "$WORKDIR/package.json" "$WORKDIR/.eval-package-baseline.json" >/dev/null \
      || { echo "FAIL: package.json changed"; FAIL=1; }
  fi
  [ $FAIL = 0 ] && echo "PASS: $FIXTURE hard-stop" || echo "FAIL: $FIXTURE"
}

# --- 3b. Verify a planning fixture: detection.json + plan.md vs the golden --
verify_orchestration() {
  local spec det_golden plan_exp det plan path want got f step phase variant
  spec=$(jq -e --arg f "$FIXTURE" '.[$f]' "$EVALS_DIR/fixtures.json")
  det_golden="$EVALS_DIR/$(jq -r '.expectedDetection' <<<"$spec")"
  plan_exp="$EVALS_DIR/$(jq -r '.expectedPlan' <<<"$spec")"
  det="$WORKDIR/.crowdin/detection.json"
  plan="$WORKDIR/.crowdin/plan.md"

  [ -f "$det" ] || { echo "FAIL: no detection.json"; FAIL=1; return; }
  [ -f "$plan" ] || { echo "FAIL: no plan.md"; FAIL=1; return; }

  # Leaf-walk the golden's match tree; each leaf must equal the same path in detection.json.
  # NOTE: do not use `getpath(...) // "<missing>"` — jq's `//` swallows a legitimate `false` leaf.
  while IFS= read -r path; do
    want=$(jq -c ".match | getpath($path)" "$det_golden")
    got=$(jq -c "getpath($path)" "$det")   # a missing path yields null, which correctly mismatches
    if [ "$want" != "$got" ]; then
      echo "FAIL: detection $(jq -r 'join(".")' <<<"$path"): want $want, got $got"; FAIL=1
    fi
  done < <(jq -c '.match | paths(type != "object" and type != "array")' "$det_golden")

  # Soft assert: candidate files (warn only).
  while IFS= read -r f; do
    jq -e --arg f "$f" '[.candidateFiles[].path] | index($f)' "$det" >/dev/null \
      || echo "WARN: candidateFiles missing $f"
  done < <(jq -r '.softAssert.candidateFilesMustContain[]? // empty' "$det_golden")

  # Plan checklist assertions: lines look like "- [ ] step_id".
  while IFS= read -r step; do
    grep -Eq "^- \[[ x]\] ${step}" "$plan" || { echo "FAIL: plan missing step $step"; FAIL=1; }
  done < <(jq -r '.planStepsContain[]' "$plan_exp")
  while IFS= read -r step; do
    grep -Eq "^- \[[ x]\] ${step}" "$plan" && { echo "FAIL: plan must NOT contain $step"; FAIL=1; }
  done < <(jq -r '.planStepsAbsent[]? // empty' "$plan_exp")
  while IFS= read -r phase; do
    grep -q "$phase" "$plan" && { echo "FAIL: plan must NOT contain phase heading $phase"; FAIL=1; }
  done < <(jq -r '.phasesAbsent[]? // empty' "$plan_exp")

  variant=$(jq -r '.variant' "$plan_exp")
  grep -q "$variant" "$WORKDIR/.crowdin/manifest-snapshot.json" \
    || { echo "FAIL: manifest-snapshot missing variant $variant"; FAIL=1; }

  [ $FAIL = 0 ] && echo "PASS: $FIXTURE orchestration" || echo "FAIL: $FIXTURE"
}

if [ "$CATEGORY" = hard-stop ]; then
  verify_hard_stop
else
  verify_orchestration
fi
exit $FAIL
