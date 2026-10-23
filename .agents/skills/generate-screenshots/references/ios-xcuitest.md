# Native iOS: XCUITest

Reached from `SKILL.md`'s engine table for a native iOS app that already has a UI test target. The project has chosen its engine; this file adds one test class to it rather than a second tool. Without a UI test target, Maestro is the lighter path - see [mobile-maestro.md](mobile-maestro.md).

## One test class, one helper

`CrowdinScreenshots.swift` in the UI test target: one test per screen in the agreed plan, each navigating with accessibility identifiers (localized labels are the strings being translated; identifiers survive rewording) and ending in a call that writes the PNG straight into the contract's folder:

```swift
import XCTest

final class CrowdinScreenshots: XCTestCase {
    private let app = XCUIApplication()
    private let outputDir = ProcessInfo.processInfo.environment["CROWDIN_SCREENSHOTS_DIR"]

    override func setUp() {
        continueAfterFailure = false
        // The source language, whatever the simulator is set to.
        app.launchArguments += ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
    }

    func testSettings() throws {
        app.tabBars.buttons["settings-tab"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
        try shoot("settings")
    }

    private func shoot(_ name: String) throws {
        let dir = try XCTUnwrap(outputDir, "CROWDIN_SCREENSHOTS_DIR is not set")
        let url = URL(fileURLWithPath: dir).appendingPathComponent("\(name).png")
        try XCUIScreen.main.screenshot().pngRepresentation.write(to: url)
    }
}
```

The direct write works because a UI test runner on the simulator runs on the Mac and sees its disk; a physical device cannot write there, and this skill does not need one. Confirm on the first run that the file appears in the folder. If the runner cannot write there, attach the screenshot with `XCTAttachment` instead and export it from the result bundle with `xcrun xcresulttool export attachments`, then move it under the contract's name; the direct write is the shorter path whenever it works.

The user signs in on the simulator, and the test launches without resetting the app's state. A flow that must start signed out gets its own `launchArguments` the app already understands.

## Running it

The output directory reaches the test runner through `xcodebuild`'s `TEST_RUNNER_` prefix:

```bash
rm -rf .crowdin/screenshots && mkdir -p .crowdin/screenshots
TEST_RUNNER_CROWDIN_SCREENSHOTS_DIR="$PWD/.crowdin/screenshots" \
xcodebuild test -scheme <Scheme> -destination 'platform=iOS Simulator,name=<Device>' \
  -only-testing:<UITestTarget>/CrowdinScreenshots
```

A top-up run names its tests: `-only-testing:<UITestTarget>/CrowdinScreenshots/testSettings`. An existing fastlane `snapshot` lane is a working capture definition already: its output folder, gathered into the contract with the device suffix dropped, is the batch.
