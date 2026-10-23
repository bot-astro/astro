# Mobile: Maestro

Reached from `SKILL.md`'s engine table for an iOS, Android, React Native or Flutter app that has no UI automation of its own. Maestro is the mobile default because one YAML dialect drives every mobile stack on a simulator or emulator, and a flow reads as the list of taps it is. Everything about Maestro beyond what this file names - selectors, waiting, its CLI - is Maestro's documentation, not this skill's.

## What the project has

A `.maestro/` directory means the project already runs Maestro: keep its conventions and put the screenshot flows in a subfolder of it. The directory's absence says nothing about the CLI, so check `maestro --version` before proposing an install. When it is missing, the CLI installs with `curl -fsSL "https://get.maestro.mobile.dev" | bash` (Homebrew: `brew tap mobile-dev-inc/tap` then `brew install mobile-dev-inc/tap/maestro`) and needs Java 17 or later.

## The device and the session

The user builds and installs the app on a simulator or emulator the way they always do - Xcode, Android Studio, `expo run:ios`, `flutter run` - and signs in on it. A release-configuration build is the easiest subject: a development client opens on its launcher before the app, and Expo Go is a different app with a different id. Maestro drives the device as it is: `launchApp` restarts the app without clearing its state, so a sign-in the app persists survives it; an app that keeps its session in memory comes back signed out, and that is a stop-and-ask, not a flow to script around. Seeded data is the user's job too.

Device ids come from `xcrun simctl list devices booted` and `adb devices`. Scouting a screen before writing its flow is `maestro hierarchy` for the view tree, or one look at `xcrun simctl io booted screenshot` / `adb exec-out screencap -p` - looking, not capturing.

The device's language is what the app renders, so it must be the source language. Maestro sets a locale only when it starts the device - `maestro start-device --platform ios --device-locale en_US` (`android` likewise) - and never from `maestro test` or inside a flow. A device the user booted themselves is checked, not assumed: one scouting screenshot in the wrong language means every capture is. Appearance is set outside Maestro too - `xcrun simctl ui booted appearance light`, `adb shell cmd uimode night no`.

## The flows

One file per screen in the agreed plan, under `.maestro/crowdin/`, named after the screen. The header's `appId` is the bundle identifier or package name:

```yaml
# .maestro/crowdin/settings.yaml
appId: com.example.app
---
- launchApp
- openLink: example://settings          # a deep link when the app has one
- extendedWaitUntil:
    visible: "Notifications"            # the screen's own content, not the splash
    timeout: 10000
- waitForAnimationToEnd
- takeScreenshot: settings
```

```yaml
# .maestro/crowdin/invite-sheet.yaml
appId: com.example.app
---
- launchApp
- tapOn: "Team"
- tapOn:
    id: "invite-button"                 # an accessibility id survives rewording
- extendedWaitUntil:
    visible: "Invite a teammate"
    timeout: 5000
- takeScreenshot: invite-sheet
```

The `takeScreenshot` value is the screen name without an extension; Maestro adds `.png`. Deep links first, taps only for what no link reaches, a wait on the screen's own content before every shot (a cold launch into a deep link renders the splash first, and `waitForAnimationToEnd` alone will shoot it), and variants as their own flows. When the two platforms have different ids, the header reads `appId: ${APP_ID}` and each run passes `-e APP_ID=<id>`.

## Running and gathering

Maestro does not write into a folder of your choosing: `takeScreenshot` paths resolve inside the run's artifact tree, one session directory per run under the output directory, one folder per flow, the screenshots in its `takeScreenshot/` subfolder, and a path that points elsewhere is rejected. So a run is a fresh output directory, the run, and a gather into the contract's folder, then a look at what landed:

```bash
rm -rf .crowdin/maestro .crowdin/screenshots && mkdir -p .crowdin/screenshots
maestro test --test-output-dir .crowdin/maestro .maestro/crowdin
find .crowdin/maestro -path '*/takeScreenshot/*.png' -exec cp {} .crowdin/screenshots/ \;
ls .crowdin/screenshots
```

The `ls` is the check SKILL.md's step 4 asks for: one PNG per planned screen, or the flow that produced none gets fixed before anything uploads.

An app that ships on both platforms is captured twice, once per device, with the platform suffix applied in the gather so the basenames stay unique. The folders are emptied once, before the first platform, never between them:

```bash
rm -rf .crowdin/maestro .crowdin/screenshots && mkdir -p .crowdin/screenshots
maestro --device <ios simulator id> test --test-output-dir .crowdin/maestro/ios .maestro/crowdin
find .crowdin/maestro/ios -path '*/takeScreenshot/*.png' -exec sh -c 'cp "$1" ".crowdin/screenshots/$(basename "$1" .png)-ios.png"' _ {} \;
maestro --device <android emulator id> test --test-output-dir .crowdin/maestro/android .maestro/crowdin
find .crowdin/maestro/android -path '*/takeScreenshot/*.png' -exec sh -c 'cp "$1" ".crowdin/screenshots/$(basename "$1" .png)-android.png"' _ {} \;
ls .crowdin/screenshots
```

A single-platform app needs no suffix. A top-up run passes the changed flows' paths instead of the folder.
