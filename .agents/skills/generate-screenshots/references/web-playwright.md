# Web: Playwright

Reached from `SKILL.md`'s engine table for a web app. Playwright is the capture definition because it writes a PNG to a path, reuses a signed-in session without the agent touching credentials, pins viewport and locale, and can start the app itself. Everything about Playwright beyond what this file names - locators, waiting, configuration - is Playwright's own documentation, not this skill's.

## What the project has

`@playwright/test` in `package.json` and a `playwright.config.*` mean the project already has the engine: reuse its config, its test directory, its conventions. Without them, the install is two commands - `npm i -D @playwright/test` and `npx playwright install chromium` (the project's package manager in place of npm) - a dev dependency plus a browser download. Playwright's own Chromium is the default because it is the same everywhere, on a CI runner included. When the machine will not launch it, or the user would rather not download a browser, `channel: 'chrome'` in the config's `use` block drives the Chrome already installed instead; the config then depends on that machine having Chrome, so the choice is recorded in a comment beside it.

## A config of its own

The screenshot run gets its own config file, `playwright.crowdin.config.ts`, so the project's normal `npx playwright test` never runs the screenshot tests and the screenshot run never inherits a project matrix of browsers and devices. It matches one file and pins the framing:

```ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: 'crowdin-screenshots.ts',
  workers: 1,
  outputDir: '.crowdin/test-results',        // Playwright's run bookkeeping, out of the repo root
  use: {
    baseURL: 'http://localhost:3000',      // where the app's dev server listens
    viewport: { width: 1440, height: 900 },  // one framing for every shot; a narrower app picks its own
    colorScheme: 'light',
    locale: 'en-US',                       // the project's source locale
    storageState: '.crowdin/auth.json',    // see "Signing in"
  },
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: true,             // the user's running dev server wins
  },
});
```

`locale` sets the browser's language, which is what apps that negotiate from `Accept-Language` read. An app that selects the language from the URL or a saved preference is captured at its source-language route or with that preference set; the inventory step already knows which the app does.

When the project's existing config already has a `webServer` and a `baseURL`, import and spread it instead of repeating it.

## Signing in

The user signs in once, interactively, and Playwright saves the session:

```bash
npx playwright codegen --save-storage=.crowdin/auth.json http://localhost:3000
```

A browser opens; the user signs in and closes it; cookies and local storage land in `.crowdin/auth.json`, inside the gitignored workspace. The config's `storageState` loads that file into every test. A session that expires shows up as the sign-in page in every screenshot: re-run the command. An app with no sign-in drops the `storageState` line.

## The spec

One file, `e2e/crowdin-screenshots.ts`, one test per screen in the agreed plan, each named after its screen and ending in a screenshot under the contract's name:

```ts
import { test } from '@playwright/test';

const shots = '.crowdin/screenshots';

test('dashboard', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('heading', { name: 'Dashboard' }).waitFor();
  await page.screenshot({ path: `${shots}/dashboard.png` });
});

test('settings-profile', async ({ page }) => {
  await page.goto('/settings');
  await page.getByRole('tab', { name: 'Profile' }).click();
  await page.screenshot({ path: `${shots}/settings-profile.png` });
});

test('invite-dialog', async ({ page }) => {
  await page.goto('/team');
  await page.getByRole('button', { name: 'Invite' }).click();
  await page.getByRole('dialog').waitFor();
  await page.screenshot({ path: `${shots}/invite-dialog.png` });
});
```

Rules that the example shows and the plan relies on:

- **Wait for the screen's own content** before shooting - a heading, the dialog, the list - never a fixed timeout. A screenshot of a spinner is not context.
- **Viewport, not full page, by default.** Translators read a screen the way users see it; a `fullPage: true` capture of a long page is one image with tiny text, and auto-tag reads it worse. A page taller than the viewport is several screens, by one convention: the first is `<name>`, then `<name>-2`, `<name>-3`, each scrolled one viewport less an overlap of about 120px further down, so a line cut at the bottom of one is whole at the top of the next. A helper that loops over `scrollHeight` does it for every page in the list.
- **Locators by role and visible text** are fine here, unlike in locale-switching tests: the run is always in the source language.
- **Variants are their own tests** - `checkout-empty`, `signup-errors` - reached by the state that shows them, with the user's seeded data or a form submitted empty.

What spoils a capture that still passed its test, and the line that keeps it out - put these in one `shoot` helper every test calls:

| Spoiler | Guard |
|---|---|
| Fonts still loading, so the fallback face is in the image | `await page.evaluate(() => document.fonts.ready)` after navigation |
| A dialog, menu or accordion caught mid-animation | wait for finite animations: `document.getAnimations()` filtered to those whose timing has no infinite iterations, then `await` their `finished` |
| A hover state on the last thing clicked | `await page.mouse.move(0, 0)` before the shot |
| An overlay the dev server mounts that is not part of the app (a framework's dev badge, devtools panels, a cookie banner the plan does not want) | `page.screenshot({ path, style: '<selector> { display: none !important }' })` - hidden for the image only |
| Content scrolled to a heading sits under a fixed or sticky header | after `scrollIntoView`, `window.scrollBy(0, -(headerHeight + margin))`, with the height read from the app's own header |

## Running it

```bash
rm -rf .crowdin/screenshots && mkdir -p .crowdin/screenshots
npx playwright test -c playwright.crowdin.config.ts
```

A top-up run names its screens: `npx playwright test -c playwright.crowdin.config.ts -g "settings-profile|billing"`.
