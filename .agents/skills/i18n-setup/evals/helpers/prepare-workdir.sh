#!/usr/bin/env bash
# Usage: prepare-workdir.sh <fixture-name> <workdir>
set -euo pipefail
EVALS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FIXTURE="$1"; WORKDIR="$2"
SPEC=$(jq -e --arg f "$FIXTURE" '.[$f]' "$EVALS_DIR/fixtures.json")
TYPE=$(jq -r '.type' <<<"$SPEC")
case "$TYPE" in
  local)
    cp -R "$EVALS_DIR/$(jq -r '.path' <<<"$SPEC")/." "$WORKDIR/" ;;
  derived)
    cp -R "$EVALS_DIR/$(jq -r '.base' <<<"$SPEC")/." "$WORKDIR/"
    while IFS= read -r ov; do
      cp -R "$EVALS_DIR/$ov/." "$WORKDIR/"
    done < <(jq -r '.overlays[]' <<<"$SPEC") ;;
  *) echo "unknown fixture type: $TYPE" >&2; exit 2 ;;
esac
rm -rf "$WORKDIR/node_modules" "$WORKDIR/dist"
git -C "$WORKDIR" init -q && git -C "$WORKDIR" add -A && git -C "$WORKDIR" -c user.email=eval@local -c user.name=eval commit -qm baseline
