# Garden visual refresh

HeartMatch opens directly on the garden at the player's current level. The garden
provides Play, find-current-level, daily gifts, settings, and realm navigation.
Settings contains Help and a saved option to reduce decorative motion. System back
closes a level preview, returns secondary screens to the garden, and retains the
puzzle's pause behavior.

The shared botanical palette lives in `ui/theme/GardenPalette.kt`. UI line icons,
the scenic backdrop, and sparse edge petals live in `ui/components/GardenChrome.kt`.
The map preserves the backdrop's aspect ratio and projects level markers through
the same centered crop. Realm lighting follows the visible chapter. The existing
garden artwork is reused; this change does not introduce separate realm paintings.

The puzzle has a quieter frame, clearer moves and objectives, and an adaptive
booster toolbar. Pause and result dialogs use one visual style and scroll when
necessary. Daily rewards and Help use the same palette. The total available star
count comes from the packaged level files.

Heart rendering, level data, game rules, rewards, and puzzle animations are
unchanged. Reduced decoration motion affects map petals, the current-level pulse,
and map navigation scrolling only.

## Validation

- Build: `./gradlew assembleDebug`
- Unit suite: `./gradlew testDebugUnitTest`
- Device navigation: `./gradlew connectedDebugAndroidTest --no-configuration-cache`
- `GardenNavigationTest` checks startup, Settings/Help, Play/Pause/Garden, and daily
  gift access without claiming rewards or making moves.

AndroidX Test dependencies were updated to Espresso 3.7.0 and JUnit extensions
1.3.0 so the device tests can run on the installed Android 17 emulator. These are
test-only dependencies. See the [AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test).

Verified during this change: the full 114-test unit suite passed, followed by a
110-test regression run on the final UI source. The Compose navigation run passed
all four device tests. The Android connected-test runner reset the emulator's
local profile; use a disposable emulator for future connected test runs.

The final three navigation tests also passed at 320 × 640 dp, including assertions
that the board fits between the HUD and booster toolbar. The original emulator
screen size is restored after visual inspection.
