#!/bin/sh
# Runs check-drafts.sh against the fixtures and asserts exit codes and reported lines.
set -u
here=$(cd "$(dirname "$0")" && pwd)
script="$here/../scripts/check-drafts.sh"
fx="$here/fixtures"
fail=0
check() { # name expected_exit actual_exit
  if [ "$2" = "$3" ]; then echo "PASS: $1"; else echo "FAIL: $1 (expected exit $2, got $3)"; fail=1; fi
}
contains() { # name haystack needle
  case "$2" in *"$3"*) echo "PASS: $1";; *) echo "FAIL: $1 (missing: $3)"; fail=1;; esac
}

out=$("$script" "$fx/ledger-clean.jsonl" --categories one,few,many,other 2>&1); check "clean ledger with uk categories exits 0" 0 $?
[ -z "$out" ] && echo "PASS: clean ledger prints nothing" || { echo "FAIL: clean ledger printed: $out"; fail=1; }

out=$("$script" "$fx/ledger-clean.jsonl" 2>&1); check "clean ledger without categories exits 0" 0 $?

out=$("$script" "$fx/ledger-broken.jsonl" --categories one,few,many,other 2>&1); check "broken ledger exits 1" 1 $?
contains "renamed placeholder is reported" "$out" "Hello {name}	placeholders	source: {name}	target: {Name}"
contains "dropped tag is reported" "$out" "welcome	placeholders	source: </0> <0>	target: "
contains "dropped %d in a plural form is reported" "$out" "items_count	placeholders in form other	source: %d	target: "
contains "wrong plural category set is reported" "$out" "items_count	plural categories	source: few,many,one,other	target: one,other"
contains "plain source with plural target is reported" "$out" "shape	shape mismatch"
contains "%% is not a placeholder but dropping it is reported" "$out" "pct	placeholders"
n=$(printf '%s\n' "$out" | grep -c "	pct	"); [ "$n" = 1 ] && echo "PASS: pct reported once" || { echo "FAIL: pct reported $n times"; fail=1; }
contains "dropped select branch is reported" "$out" "gender	select branches	source: male,other	target: other"
n=$(printf '%s\n' "$out" | grep -c "	gender	"); [ "$n" = 1 ] && echo "PASS: select is not held to plural categories" || { echo "FAIL: gender reported $n times"; fail=1; }
contains "a form carrying another source form's placeholder is reported" "$out" "seats	placeholders in form one	source: {1}	target: {2}"
contains "a form without a source counterpart carrying a foreign placeholder is reported" "$out" "stray	placeholders in form few	source: {count}	target: {count} {x}"
contains "a form without # whose source counterpart has none is not held to #, but a form the source lacks follows other" "$out" "devices	# missing in form few	source: #	target: "
n=$(printf '%s\n' "$out" | grep -c "	devices	"); [ "$n" = 1 ] && echo "PASS: devices reported once (one is not held to #)" || { echo "FAIL: devices reported $n times"; fail=1; }

"$script" "$fx/ledger-invalid.jsonl" >/dev/null 2>&1; check "invalid JSON exits 2" 2 $?
"$script" >/dev/null 2>&1; check "no arguments exits 2" 2 $?
"$script" "$fx/does-not-exist.jsonl" >/dev/null 2>&1; check "missing file exits 2" 2 $?
"$script" "$fx/ledger-clean.jsonl" --bogus >/dev/null 2>&1; check "unknown option exits 2" 2 $?
PATH=/nonexistent "$script" "$fx/ledger-clean.jsonl" >/dev/null 2>&1; check "missing jq exits 2" 2 $?

exit $fail
