#!/bin/sh
# Checks a translate drafts ledger (JSONL) for placeholder and plural-category mismatches.
# Usage: check-drafts.sh <ledger.jsonl> [--categories one,few,many,other]
# Exit: 0 clean, 1 mismatches (one line each), 2 usage error, missing jq, or invalid JSON.
# It reads only the ledger; it never opens or rewrites a resource file.
set -u

usage() {
  echo "usage: check-drafts.sh <ledger.jsonl> [--categories a,b,c]" >&2
  exit 2
}

[ $# -ge 1 ] || usage
ledger=$1
shift
categories=""
while [ $# -gt 0 ]; do
  case $1 in
    --categories) [ $# -ge 2 ] || usage; categories=$2; shift 2 ;;
    *) usage ;;
  esac
done

[ -f "$ledger" ] || { echo "check-drafts: no such file: $ledger" >&2; exit 2; }
command -v jq >/dev/null 2>&1 || { echo "check-drafts: jq is required; review the placeholders by hand from the ledger" >&2; exit 2; }

program='
# %% is a literal percent sign; everything else that looks like a placeholder is one.
def placeholders:
  gsub("%%"; "")
  | [ match("\\{\\{[^}]*\\}\\}|\\{[^{}]*\\}|%([0-9]+\\$)?[0-9.]*[A-Za-z@]|</?[A-Za-z0-9][^<>]*>"; "g").string ]
  | sort;
def hashes: [ match("#"; "g") ] | length;
def literal_percents: [ match("%%"; "g") ] | length;
. as $r
| def report($what; $src; $tgt): "\($r.file)\t\($r.key)\t\($what)\tsource: \($src)\ttarget: \($tgt)";
select(has("target"))
| if (.source | type) != (.target | type) then
    report("shape mismatch"; .source | type; .target | type)
  elif (.source | type) == "string" then
    (.source | placeholders) as $s
    | (.target | placeholders) as $t
    | if $s != $t then report("placeholders"; $s | join(" "); $t | join(" "))
      elif (.source | literal_percents) != (.target | literal_percents) then
        report("placeholders"; "%% x\(.source | literal_percents)"; "%% x\(.target | literal_percents)")
      else empty end
  else
    # A source keyed only by CLDR category names is a plural; any other key set is an ICU select.
    ((.source | keys) - ["zero","one","two","few","many","other"] | length == 0) as $plural
    # Per source form, its placeholder set. A target form with a namesake source form must match it
    # exactly (Lingui numbers the same expression differently per branch: {1} in one, {2} in other).
    # A target form the source lacks (uk few/many from en one/other) may use any placeholder some
    # source form uses and must keep every placeholder all source forms share.
    | (.source | with_entries(.value |= (placeholders | unique))) as $forms
    | ([ $forms[] ] | add // [] | unique) as $union
    | ([ $forms[] ] | if length == 0 then [] else reduce .[1:][] as $f (.[0]; . - (. - $f)) end) as $inter
    # Same per-form rule for #: a target form needs it only when its namesake source form has it,
    # and a form the source lacks follows the source other form, whose numbers it carves out.
    | (.source | with_entries(.value |= hashes)) as $sh_forms
    | ($sh_forms.other // ([ $sh_forms[] ] | max)) as $sh_other
    | (
        ( .target | to_entries[]
          | (.value | placeholders | unique) as $t
          | if $forms[.key] != null then
              select($forms[.key] != $t)
              | report("placeholders in form " + .key; $forms[.key] | join(" "); $t | join(" "))
            else
              select((($t - $union) | length) > 0 or (($inter - $t) | length) > 0)
              | report("placeholders in form " + .key; $union | join(" "); $t | join(" "))
            end ),
        ( .target | to_entries[]
          | ($sh_forms[.key] // $sh_other) as $need
          | select($need > 0 and (.value | hashes) == 0)
          | report("# missing in form " + .key; "#"; "") ),
        ( if $plural and $cats != "" then
            ($cats | split(",") | sort) as $want
            | (.target | keys | sort) as $have
            | select($want != $have)
            | report("plural categories"; $want | join(","); $have | join(","))
          else empty end ),
        ( if $plural then empty else
            (.source | keys | sort) as $want
            | (.target | keys | sort) as $have
            | select($want != $have)
            | report("select branches"; $want | join(","); $have | join(","))
          end )
      )
  end
'

out=$(jq -r --arg cats "$categories" "$program" "$ledger") || exit 2
if [ -n "$out" ]; then
  printf '%s\n' "$out"
  exit 1
fi
exit 0
