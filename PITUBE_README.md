# PiTube — Android TV edition

PiTube is a personal Android TV customization of the open-source
[SmartTube](https://github.com/yuliskov/SmartTube) project. It is intended for
Sony BRAVIA VH1 hardware. It is **not** an official YouTube or Sony application.

## Included in this customization

- Original PiTube launcher icon, banner, and separate package ID
  (`dev.pradi.pitube`). It can coexist with upstream SmartTube.
- Reworked TV-first, Apple TV-inspired look: navy glass-style navigation rail,
  persistent translucent top toolbar, deep-slate content, brighter glass focus
  pills, and a matching search experience. Other upstream themes remain selectable.
- Lightweight remote-focus animation: Leanback's native card zoom and a
  190 ms fade of an animated aurora *shadow beneath* the selected card.
  Overlapping filled shapes create a continuous soft gradient with an inner
  underlap, so no focus stroke or pixel gap can appear atop the thumbnail.
  Only the focused item animates at 20 fps; decoration registration is
  deferred until after RecyclerView layout on older TVs, avoiding
  layout-state exceptions during the first focused video. There are no software blur passes,
  intermediate bitmaps, or network calls.
- Native circular focus with a blue-white glass rim on title-bar controls,
  without Android's additional rectangular keyboard-focus highlight. The
  profile button no longer displays the redundant "None" tooltip.
- Search/profile/time stay legible over scrolling videos thanks to a
  translucent, 205dp-tall softly fading top header veil behind the video
  content (but below its controls), without a hard bottom divider.
  The navigation rail uses one colour from its top inset to its bottom edge,
  without exposed dark margins. The search glyph is high contrast on a dark orb.
- Navigation labels stay stationary while each focused row paints its own
  animated glass background behind the icon and label. The menu only scrolls
  when it genuinely overflows, below the persistent toolbar.
- Search from Browse now opens an animated overlay **in the same activity**,
  instead of launching a new Android task. Search keeps its existing keyboard,
  voice and suggestion logic but uses matching dark-glass colors and round
  focus treatment without Android's rectangular focus indicators.
- Glass-inspired gradient focus pills and gentle transform animations for the
  sidebar, plus softly lit rounded settings cards. Animations change only
  alpha, translation, or scale, keeping them inexpensive on the BRAVIA.
- First-run sidebar defaults to Home, Music, Channels, Subscriptions, History,
  Playlists, and Settings. Additional sections can be enabled in Settings.
- A simplified context menu, with less-used commands accessible in Settings.
- SponsorBlock enabled by default; sponsor and self-promotion segments skip
  automatically and silently. Other segment types are not skipped by default.
- BRAVIA VH1 video preset prefers up to 2160p/60 VP9 where available and
  supported; playback quality and speed remain adjustable.
- Upstream ad-free playback and watch-history/account/playlist functionality.
- Disabled automatic upstream APK-update prompts so the custom app does not
  accidentally install an unrelated build.
- Removed upstream app-installation and package-deletion permissions from the
  final manifest; the custom app does not manage APK updates.
- A separately installable **PiTube Preview** flavor for Galaxy Tab S11 Ultra,
  with a 16:9 landscape testing canvas and touch-enabled TV navigation. This
  flavor does not alter the Sony BRAVIA APK or its installed preferences.
- A branded animated launch view and native first-window background make
  cold starts feel responsive while account and service initialization runs.
- Offline crash reports saved in app-private storage when an uncaught Java
  exception terminates PiTube. The next launcher start presents an independent
  recovery interface with Copy, Save to file and Share options. A separate
  safe-start mode turns off the experimental focus capsule and inline search.
  Crash reports include app-version/device details, recent startup milestones,
  and the exception stack; no system-wide logcat capture or network upload.
- v8 repairs a startup crash in the floating sidebar focus capsule: the focus
  resolver now returns only actual grid children, and highlight positioning
  uses safe window coordinates rather than assuming a child always belongs to
  a particular layout parent.
- v9 reserves layout space below the top toolbar instead of scrollable padding,
  keeping Home and the other labels visible. The 150 ms built-in sidebar-opening
  delay is removed, and the transition now lasts 185 ms. The focus capsule gets
  a soft Siri-inspired cyan/indigo/violet/rose gradient, luminous glass edge,
  precomputed subtle noise texture, and 175 ms spring navigation. The glass
  texture is created only once per sidebar: no live blur or perpetual animation.
- v10 fixes the remaining cause of sliding menu labels: SmartTube's modified
  Leanback grid forcibly switched **every** D-pad press back to an aligned
  scroll strategy. PiTube now tags only the sidebar for item-visibility
  scrolling, so all seven default labels stay in place and the glass focus
  moves independently. Video grids retain their original scrolling behaviour.
- v11 reserves the sidebar's toolbar clearance in the actual Leanback XML
  layout, including on devices where a later layout callback does not run.
  The glass capsule is a non-interactive translucent overlay; its observer
  attaches after the view joins the window, with a selected-row fallback.
- v12 compacts the top bar and sidebar clearance, paints the entire navigation
  dock to remove the exposed black strip, and places the focus glass behind
  the header icons and labels. Smaller 16:9 cards and tighter spacing display
  more videos per row. Focused video cards, title-bar orbs, settings tiles,
  and the navigation capsule gain animated gradient/noise glass outlines;
  off-focus items do not perform animation work.
- v15 replaces the faulty floating sidebar capsule with row-owned focus glass,
  which stays aligned with the text and is never hidden by the headers grid.
  The navigation rail background now follows the sliding fragment instead of
  painting its stationary dock. Removing the dock's unnecessary 50 dp trailing
  padding eliminates the coloured band over the first video column when the
  sidebar is closed. The top toolbar is slimmer and focused header rows use a
  small scale response with animated colour/noise that stops when unfocused.
- GitHub Actions builds both Android TV and tablet-preview variants on pushes
  to `pitube-v10`, and publishes direct APK downloads in a test prerelease.

## Build requirements

- OpenJDK 17.
- Android SDK Platform 34 and Build Tools 30.0.3 (and their accepted licenses).
- Gradle 7.5 (the upstream wrapper downloads it automatically).
- A network connection for Gradle dependencies.
- Some configurations may require Android NDK 21.0.6113669.

The customization patch targets upstream commit
`4b3f333e540a1403429d1a9d86dec758e59288f9`.
For the downloadable source kit, first obtain the matching SmartTube source:

```bash
git clone https://github.com/yuliskov/SmartTube.git
cd SmartTube
git checkout 4b3f333e540a1403429d1a9d86dec758e59288f9
git submodule update --init --recursive
git apply /path/to/PiTube.patch
```

The patch already contains the rendered launcher assets. The bundled
`render_branding.py` script can regenerate them if Pillow is installed.
Then, from the project root, build the APK:

```bash
bash gradlew :smarttubetv:assembleStpitubeDebug
```

For the tablet-testing build:

```bash
bash gradlew :smarttubetv:assembleStpitubetabletDebug
```

See `PITUBE_TABLET_TESTING.md` for tablet setup and the limits of screen/input
equivalence with a physical Sony TV.

Local debug APKs use the developer's local debug keystore unless
`keystore.properties` points to the permanent PiTube signing keystore.
The universal APK should appear under
`smarttubetv/build/outputs/apk/stpitube/debug/`, named similarly to
`PiTube_pitube_32.68-pitube.20_universal.apk`. Keep the debug keystore if you
want to update the same installation later. For distribution, use a dedicated
release signing key.

The GitHub Actions workflow has two modes:

- **Updateable PiTube:** Configure the single GitHub Actions secret
  `PITUBE_SIGNING_BUNDLE_B64` using the private base64 signing bundle previously
  saved as `PiTube-signing-secret.txt`. Every subsequent build uses fixed TV
  and tablet application IDs (`dev.pradi.pitube` and
  `dev.pradi.pitube.preview`), the same private certificate, and an increasing
  version code. Releases use tags `pitube-update-N`; install once, then tap
  Install on newer APKs to update in place with app data retained.
- **Isolated test fallback:** Without the secret, builds continue using
  `dev.pradi.pitube.testN` and `dev.pradi.pitube.preview.testN` and release
  as `pitube-test-N`. These install alongside previous builds but cannot
  replace them.

To enable permanent updates: open your repository's **Settings > Secrets and
variables > Actions > New repository secret**. Name it
`PITUBE_SIGNING_BUNDLE_B64`, paste the entire base64 line from your private
signing bundle, and save. Then run the **PiTube APKs** workflow once in GitHub
Actions. Keep a private backup of the signing bundle. Never commit it or paste
it into public issues. This permanent version is a *different package* from
the earlier `.testN` apps, so the first installation is one-time; future
updates do not need a reinstall.

## Build verification

The updated `assembleStpitubeDebug` build completed successfully on 29 October 2026
using OpenJDK 17, Gradle 7.5, and Android SDK 34. The universal APK was
verified to declare `dev.pradi.pitube`, advertise an Android TV launcher,
contain ARMv7 and ARM64 libraries, and pass APK signature checks for v1/v2.
This verifies packaging, not live YouTube playback or appearance on a TV.
The current version is `32.68-pitube.20`; GitHub builds derive an increasing
`versionCode` from the workflow run number (starting at 2470). APKs update
previous PiTube installations only when the signing certificate matches.

PiTube v16 replaces the flat horizontal focus gradient with overlapping,
two-dimensionally drifting colour fields. The sidebar rail is uniform, with
no contrasting right edge. Video tiles now show a soft animated aurora
**outside** the selected card instead of an inset glass border.
PiTube v17 moves the video halo from the card's clipped drawing layer to
Leanback's containing grid overlay, so it can actually render outside the
thumbnail. The card interior is excluded from the glow with a reusable path.
PiTube v18 adds persistent signed-update support to GitHub Actions; the
rendering stays identical to v17.

## Installation

1. Transfer the APK to the Sony TV using a trusted local transfer method.
2. Permit installation by the file manager you choose in Android TV Settings.
3. Open the transferred APK, install, then launch **PiTube**.
4. In Settings, review SponsorBlock, video presets, playback speed, and
   **Edit categories** for the sidebar/home screen.

## On-device crash recovery

1. If PiTube unexpectedly closes, launch it again using its normal app icon.
2. A minimal recovery screen appears **before** the full SmartTube interface
   initializes, even if application initialization crashed previously.
3. Copy or save the error report and send it privately for diagnosis. It can
   contain crash messages or navigation details; review it before sharing.
4. Choose **Safe start** to bypass the experimental Browse overlay and
   floating focus capsule. Or choose **Try normal start** to test the full UI.
5. Safe mode stays active across restarts. The recovery launcher remains
   available so **Try normal start** can turn it off later.

The recorder handles uncaught Java exceptions. Hardware resets, app
force-stops, out-of-memory process kills and native aborts may not produce a
report. The report cannot pop up in the terminated Android process: the user
must reopen the app to view it.

## Important limitations

- This source customization has not yet been run on the user's physical TV.
- Ad-free playback depends on upstream SmartTube's YouTube integrations, which
  may stop working when YouTube changes its interfaces.
- The theme is an extensively reworked Leanback-based interface. Search from
  the Browse screen uses an in-activity panel; some other deep links may still
  invoke the original standalone search screen.
- Since this is a custom build, upstream update APKs must **not** be installed
  on top of it. Rebuild and sign new PiTube versions yourself.
- SmartTube disclosed a 2025 compromise involving its build environment and
  signing keys. Prefer source builds or carefully verified recent builds.

## Attribution and license

SmartTube is copyright its respective contributors and distributed under the
MIT License in `LICENSE`. The custom artwork and adjustments in this project
are original to this PiTube build. Original upstream files retain their
licenses; see nested module license notices for other dependencies.
