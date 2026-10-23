#!/usr/bin/env bash
# Layer B: prefilled plan -> run phases 3-4 fully, phase 5 through the offline gates.
# Usage: run-layer-b.sh <fixture-name>   (KEEP_WORKDIR=1 to keep the temp dir)
set -euo pipefail
EVALS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_DIR="$(dirname "$EVALS_DIR")"
FIXTURE="$1"
WORKDIR=$(mktemp -d)
cleanup() { [ "${KEEP_WORKDIR:-0}" = 1 ] && echo "workdir kept: $WORKDIR" || rm -rf "$WORKDIR"; }
trap cleanup EXIT

"$EVALS_DIR/helpers/prepare-workdir.sh" "$FIXTURE" "$WORKDIR"
mkdir -p "$WORKDIR/.claude/skills"
cp -R "$SKILL_DIR" "$WORKDIR/.claude/skills/i18n-setup"
rm -rf "$WORKDIR/.claude/skills/i18n-setup/evals"
[ -d "$EVALS_DIR/prefills/$FIXTURE" ] || { echo "ERROR: no prefill for fixture '$FIXTURE'; Layer B needs one." >&2; exit 1; }
# Delegated ecosystem skills (composition rule): local checkout wins, else shallow clone.
LINGUI_SKILLS_DIR="${LINGUI_SKILLS_DIR:-}"
if [ -z "$LINGUI_SKILLS_DIR" ]; then
  git clone -q --depth 1 https://github.com/lingui/skills "$WORKDIR/.lingui-skills-src"
  LINGUI_SKILLS_DIR="$WORKDIR/.lingui-skills-src"
fi
lingui_skill_found=0
for d in "$LINGUI_SKILLS_DIR"/skills/*/; do
  [ -d "$d" ] || continue
  lingui_skill_found=1
  cp -R "$d" "$WORKDIR/.claude/skills/$(basename "$d")"
done
if [ "$lingui_skill_found" = 0 ]; then
  echo "ERROR: no skill directories found under '$LINGUI_SKILLS_DIR/skills/'." >&2
  echo "Set LINGUI_SKILLS_DIR to a checkout of https://github.com/lingui/skills that has a top-level skills/ directory, or unset it to let this script shallow-clone one." >&2
  exit 1
fi
mkdir -p "$WORKDIR/.crowdin"
cp -R "$EVALS_DIR/prefills/$FIXTURE/." "$WORKDIR/.crowdin/"
rm -f "$WORKDIR/.crowdin/NOTE.md"
( cd "$WORKDIR" && npm install )

PROMPT="Resume the i18n setup at .crowdin/. The plan is approved. Execute phases 3 and 4 to completion, then phase 5 up to and including 'crowdin config sources'. Do NOT run 'crowdin upload' (no credentials in this environment) and do NOT create a Crowdin project. Work unguided; do not ask questions."
( cd "$WORKDIR" && { claude -p "$PROMPT" --dangerously-skip-permissions 2>&1 || true; } | tee .eval-agent-output.txt )

exec "$EVALS_DIR/library-checks/lingui.sh" "$WORKDIR"
