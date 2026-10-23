# Expo and React Native: an existing screenmap

Reached from `SKILL.md`'s engine table for an Expo, React Native or NativeScript project that already runs [screenmap](https://github.com/aleqsio/screenmap) - a `.screenmap/` directory with a `config.json`, or its GitHub Action in the workflows. screenmap discovers screens from the router, captures deep-linkable ones on a simulator or emulator, replays committed flows for the rest, and leaves a folder of slug-named PNGs: a capture definition and a batch, already there.

This skill does not install screenmap. Its agent lane runs a coding agent headless with permission prompts disabled, which is the project owner's choice to make, not this skill's; a project without it takes the Maestro path in [mobile-maestro.md](mobile-maestro.md).

## Its output is the batch

After a local run (`/screenmap`, or the project's own invocation) the screens are in `.screenmap/out/screens/`, one `<slug>.png` per screen, `<slug>--<state>.png` for state variants, and `ios/` and `android/` subfolders when both platforms were captured. Two things stand between that folder and the upload:

- **Only captures that succeeded are context.** screenmap records a status per screen (`ok`, `auth-wall`, `loading`, `error-boundary`, `not-found`, `empty-state`, `missing`) in its `map.json` and `capture-status.json`. Gather only the `ok` ones; an auth wall or a loading skeleton teaches translators nothing, and `empty-state` is worth keeping only when the plan wants that variant.
- **Platform subfolders collide.** The same slug under `ios/` and `android/` is two files with one basename, which the CLI refuses. Gather with the platform suffix: `ios/settings.png` becomes `settings-ios.png`.

```bash
rm -rf .crowdin/screenshots && mkdir -p .crowdin/screenshots
for p in ios android; do
  for f in .screenmap/out/screens/$p/*.png; do cp "$f" ".crowdin/screenshots/$(basename "$f" .png)-$p.png"; done
done
```

A single-platform run copies `.screenmap/out/screens/*.png` as it is. The slugs stay what screenmap made them: they are the identities Crowdin will key on from now on, so they are not renamed into this skill's naming scheme.

## Language and gaps

screenmap does not set the device language; the simulator or emulator it captures on must be in the source language, checked before the run. And its map is route-driven while this skill's inventory is string-driven: a screen screenmap cannot reach, or a state it never captured, that the coverage query shows strings for gets a Maestro flow next to it. Two engines writing into one folder is fine; the contract is the folder.
