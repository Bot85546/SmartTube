# PiTube Preview — Galaxy Tab S11 Ultra

This is a separate **tablet testing build** of the same PiTube Cinema UI.

## Why a separate APK?

- App label: **PiTube Preview**.
- Package name: `dev.pradi.pitube.preview`.
- TV version: `dev.pradi.pitube` — untouched, and can still be built at any time.
- Both builds use the same card layouts, home spotlight row, fonts, colors,
  playback features, and focus animation code.
- The tablet build appears in the regular app drawer instead of requiring an
  Android TV launcher; keyboard/D-pad navigation uses the TV interaction model.
- System-generated rectangular focus highlights are disabled on the top-row
  round buttons and search controls, preserving PiTube's circular focus style.
- The profile orb has no focus tooltip. Sidebar labels remain stationary while
  a single glass focus capsule moves behind the selected item.
- Search from Browse opens inside the same activity with a short compositor-
  only transition; it is no longer a separate task switch.

## 16:9 TV preview on a 16:10 tablet

The 2960 x 1848 Galaxy Tab S11 Ultra display has a taller aspect ratio than
the BRAVIA's usual 16:9 TV picture. The preview launches in landscape and
centers the UI within a 16:9 canvas with narrow black bands above and below.
PiTube's existing width-based display-density scaling makes the UI layout close
to the TV's 1920 x 1080 logical proportions. Nothing is stretched.

## How to test

1. Install `PiTube-Tab-S11-Ultra-Preview.apk` on the tablet and open it from the
   app drawer. Allow APK installation only for the file-manager or browser you
   trust; keep Play Protect enabled.
2. Hold the tablet landscape. The Cinema interface should be centered inside
   black top and bottom bands.
3. Use the attached keyboard's arrow keys and Enter to navigate the sidebar;
   Home and the remaining labels should all begin beneath the top toolbar.
   The labels should stay fixed as the glass capsule moves between them.
   Move from video content to the menu with Left; the menu should begin opening
   immediately, without the previous 150 ms pause.
4. Open Search and press Back/Escape; the existing Browse screen should return
   without a separate activity transition. Check the search-field focus.
5. Send screenshots and feedback on spacing, animations, navigation, and text.

## If PiTube crashes on startup

PiTube v10 retains the v8 fix for the earlier `IllegalArgumentException:
parameter must be a descendant of this view` in the sidebar highlight.
The crash reporter remains enabled in case another issue is found.

1. Launch **PiTube Preview** again. The crash reporter should open before
   starting the normal home-screen interface.
2. Choose **Copy report**, **Save to file**, or **Share report** and send the
   saved stack trace for diagnosis. The report stays on this device unless you
   explicitly copy, save or share it.
3. Choose **Safe start** to launch with the original Browse container and
   navigation focus behavior. This provides a fallback while diagnosing
   experimental layout problems.
4. On later launches, the recovery screen lets you choose **Try normal start**
   to turn off safe mode and return to the full experimental PiTube interface.

No laptop is needed for those steps. This report mechanism captures uncaught
Java exceptions, not all possible Android process failures.

## Limitations

- Tablet touch behavior is not identical to Sony's D-pad remote focus model.
- The tablet's chipset, decoder, refresh rate, and physical size differ from
  your BRAVIA; smoothness on the tablet does not guarantee smoothness on TV.
- This APK was compiled and packaging-verified, but not run on the user's
  physical Tab S11 Ultra.
- Source remains available as a single source tree with two product flavors:
  `assembleStpitubeDebug` for TV and `assembleStpitubetabletDebug` for tablet.
- No 'revert' of the TV interface is needed. The tablet-only manifest and
  16:9 preview logic do not activate in the TV product flavor.
