# Computer use: the agent at the real screen

Reached from `SKILL.md`'s engine table when the agent can drive the user's desktop - computer use in Claude Code or Claude Cowork, enabled in the desktop app on macOS or Windows - and one of these holds: the app is a desktop app no other engine reaches (native macOS or Windows, Electron); the plan has a handful of states that would take longer to script than to click through; or the user wants no test code in the repository. It is also the best scout for any engine, because it sees the real app on the real machine.

It is not the default. A capture definition re-runs in two commands; a computer-use run leaves nothing behind, so the next release is another session of clicking through every screen, at desktop speed, with the mistakes the preview is known for and a permission prompt for each new app. Say that trade-off when offering it, and offer the definition first wherever one of the other references applies.

## Reach with the screen, capture with a command

The agent's screenshots are what it looks at, not files. So the two halves are split: computer use brings the app into the planned state - the route, the open dialog, the tab - and a command on the machine writes the image under the contract's name:

```bash
xcrun simctl io booted screenshot .crowdin/screenshots/settings-ios.png        # iOS simulator
adb exec-out screencap -p > .crowdin/screenshots/settings-android.png          # Android emulator
screencapture -x -R <x,y,w,h> .crowdin/screenshots/settings.png                # a window region, macOS
```

On macOS the region is the app window's frame, read once and reused for every shot so the framing stays one. On Windows the Snipping Tool saves its capture under its own name; the file is moved under the contract's name in the same step. The simulator and emulator commands capture the device, never the window around it, which is why they are preferred for mobile over a region of the screen.

The journey's rules hold unchanged, and a capture here is a file one of these commands wrote this run.

## What the report records instead of a definition

With no spec or flows to commit, the report carries what they would have held: the screen list, each with how it was reached and the capture command, so the next run - by an agent or by hand - starts from a plan rather than from the inventory. Put it where the project keeps its agent notes, or in the report alone when the user prefers nothing in the repository.
