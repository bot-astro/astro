# Recall self-check

Reached for phase 4's `recall_self_check` step: when it runs, what to hand the delegated skill, and what to do with what comes back. The audit itself is run by whichever skill `manifest-snapshot.json`'s `skills.recall` names, invoked with the constraints below — never performed here by hand.

## Why this runs

Phase 1's `candidateFiles` grep (`references/detect/js-ts.md`) only fires where a string literal sits next to one of four syntactic shapes, and its display-copy key list is fixed: a copy field named outside that list, in a file with no JSX to trip the other classes, is walked straight past. Phase 4 wraps only what the plan's `wrap_<path>` steps enumerate — generated from that same grep — so such a miss reaches the end of phase 4 unwrapped, with a green build to show for it. The gap is structural: a fixed key list can be extended but never made exhaustive. `recall_self_check` therefore runs a wider check once the tree is stable enough to survive one — after `build_check` passes, not before.

The delegated skill decides the scan configuration, the skip-list judgment, and the fix loop's bounds; its availability was gated by `verify_skill_dependencies` back in phase 3. For the v1 stack it is `find-unwrapped-strings`, from the `lingui` plugin.

## The delegation call

Invoke the named recall skill under its own contract for being invoked by another workflow, supplying exactly the three inputs it says it accepts — stated as instructions it must honor, not as background it may weigh against its defaults:

- **Scope** — `detection.json`'s `sourceDir`. This narrows what the delegated skill scans; by its own contract, scope never narrows what it reports, so a hit outside that scope still comes back as an out-of-scope finding rather than being dropped.
- **Extra skip-list entries** — the body of the rendered rules file's "What not to wrap" section, `.agents/crowdin-i18n-rules.md`. That section starts from the library-wide skip-list and accumulates this project's own non-translatables as they're found — brand names, product codes, internal labels. Hand it over as-is; it is the project's skip-list, not a summary of it.
- **Known misses** — the gap described above under "Why this runs": phase 1's match classes are pattern-based rather than exhaustive, and the display-copy key list in particular only ever grows by hand, so any field named outside it survives detection regardless of how plainly a user would read it. Pass this as a caution about the *kind* of gap to expect, not as a list of specific strings — this orchestrator doesn't have specific strings to hand over, only the shape of where the list runs out.

Past that handoff, the delegated skill runs the scan, judges each hit wrap-or-skip, and bounds its own fix loop; take its report as given.

## After the delegated run returns

The delegated skill reports which of its own conditions ended the run. What matters from that report is narrower: whether the run ended because the codebase came back clean, with nothing left open, or because the delegated skill stopped at its own bound with residuals still outstanding. Either reading comes off the same report, and both feed the same three steps next — only the residual list's length differs, empty in the first case, populated in the second:

1. **Re-verify the post-conditions on disk.** A delegation that reports success has not finished; a delegation whose output has been read has. Re-check the matched stack's adapter checklist (`references.adapter` in the snapshot) the same way phase 3 and phase 4 already did — extraction and the project build must exit green. The build is the check; a clean recall pass never reaches for a library command of its own to satisfy it, because which commands this project's pipeline needs was settled by the setup delegation.
2. **Record the outcome in the workspace.** Append the delegated skill's report — its termination reason and its full residual list, each entry with `file:line` — to `plan.md`'s notes or to `decisions.md`. This is what lets a resumed run, or a later coverage pass, start from what's actually still open instead of re-discovering it from scratch.
3. **Surface residuals to the user, never drop them.** Every residual the delegated skill reports gets restated back to the user with file and line, in the final report. A run that comes back with residuals it deliberately left unwrapped is a complete, honest report of a bounded pass — not a failure this orchestrator needs to hide, apologize for, or silently retry.
