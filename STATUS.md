# Status

Tracks what is being worked on and how far each piece has come.
Update this file whenever work starts, finishes or is verified.

**Legend / workflow:** an item moves through
`Next up → Currently working on → Implemented, not yet tested → Done & tested`.
An item only counts as **tested** when it has been verified through automated tests,
on a real device, or both. Record how it was tested.

_Last updated: 2026-11-24_

---

## 🚧 Currently working on

_Nothing. v0.2.2 (layers, lock and easier gestures in the outfit editor, several colours per garment) is released; M3 is next._

## 🧪 Implemented, not yet tested

- **Everything in v0.1.0–v0.2.2 on the user's phone (Galaxy S20).** Only tested on the emulator so far (below). Things
  worth checking on the real phone: the Samsung camera app returns the photo, portrait photos are upright, the grid
  with real photos, **Remove background on real photos** (the emulator only had flat, made-up pictures, and its model
  download is the first one on that phone), pinching on the outfit board with real fingers, **export to and import
  from Google Drive** (the emulator only had Downloads), and an export of a large real wardrobe (time and size).
- **The outfit board's pinch-to-resize and two-finger turn** were not tested (the emulator's scripted input can't use
  two fingers); dragging and the Size and Turn sliders were. Since v0.2.2 the gesture logic is the board's own
  (pinching with fingers on other garments should still move only the selected one): worth trying on the phone with a
  crowded outfit.
- **A background photo taken with the camera** (only one from the gallery was tested), and the first-start import's
  error message (unit-tested only).
- **Leaving the Profiles screen while the test user is being reset or created** now lets the fill finish
  (`NonCancellable`, the same for an import). Found on the emulator (pressing Back cancelled a reset half-way, an old
  bug); the fix is built and the normal reset was tested again, but leaving mid-way was not repeated.
- In the diary editor, **Choose garments** (the garment gallery, the same component as the outfit editor's) and
  editing an existing entry were covered by unit tests only, not tapped through on the emulator.

## ✅ Done & tested

- **Refactor for readability and extensibility (2026-11-24, unreleased, no visible change).** Four commits:
  one mapping between database rows and `profile.yaml` (`backup/ProfileMapping.kt`), with the test user filled
  through the importer; one `Ratings`/`RatingChange`/`RatingStore` for garments and outfits; editors keep one
  `SavedDraft` (JSON) instead of a key per field, and every ViewModel shares `WhileUiSubscribed`; shared `core:ui`
  delete dialog, snackbar message, empty message, list header and `formatDate(millis)`. Unit tests: 105 JVM tests, 0
  failures (new: the test wardrobe as an export file round-trips through YAML and links up; `SavedDraft` restores a
  draft and ignores a broken one). **Emulator** (Medium_Phone_API_36, Android 16), **release** build (R8): Reset of
  the test user (now through the importer) brought back pictures, cut-outs, likes, ratings, aspects, reviews, the
  locked coat and the diary; on the white tee, overall stars, like, an aspect and the review saved; the shared
  delete dialog showed and cancelled; in the outfit editor a renamed draft with a layer change came back after the
  app's process was killed in the background, and Cancel left the outfit unchanged.
- **v0.2.2 – outfit editor layers, lock and gestures; several colours per garment (2026-11-23).** Unit tests: 102 JVM
  tests pass, 9 of them new or rewritten (colour lists stored and read back, old single colours, the filter with
  several colours, the editor's colour toggles and saved state, colours and lock in `profile.yaml` and old files, the
  touch test `topmostAt` incl. turned and locked garments, layer moves, lock toggling, a locked garment ignoring
  moves). Manual tests on the emulator (Medium_Phone_API_36, Android 16), **release build** (R8, release key, SHA-256
  checked):
  - **Update from v0.2.1** with data: auto-migration 4 → 5, the oxford's old colour (blue) kept.
  - Garment editor: added White and Red to the oxford → its page shows all three; the **Red** filter shows it with
    the red garments.
  - Test user created fresh: Sunday walk's coat is locked (padlock in the strip). Tapping the coat on the board selects
    nothing; tapping the scarf selects it; a drag starting **on the jeans** moved the scarf and left the jeans. With
    nothing selected, a drag on the jeans selected and moved them (and they kept their layer). **Forward** ×2 brought
    the scarf to the front of the strip and the board; selecting the coat in the strip shows **Unlock** and hides the
    sliders. Saved, reopened: the layer order and the lock kept.
  - Export: `colours: [white, navy]` for the Breton, `locked: true` on the coat; imported as "Test user (2)": the
    **Navy** filter shows the Breton (its second colour) and the coat is still locked. Zips deleted afterwards.

- **v0.2.1 – fill, turn, backgrounds, sort by wear, import on the first start (2026-11-19).** Unit tests: 93 JVM tests
  pass, 10 of them new (rotation normalising and adding up, the editor's turn and background in its saved state and
  draft, keeping an outfit's background when editing, wear counts for the whole wardrobe, the two wear sorts, the
  first-start import and its errors). Manual tests on the emulator (Medium_Phone_API_36, Android 16), **release
  build** (R8, release key):
  - **Update from v0.2.0** with data: auto-migration 3 → 4, all outfits there. No crash.
  - Test user **Reset**: Sunday walk on sage with the scarf turned, Midsummer on sky, in the grid and the diary.
  - Cut-outs fill their tiles in the grid (compared with screenshots of v0.2.0).
  - **Most worn** (white sneakers 7, jeans 6, …) and **Least worn** (never-worn first, newest first). Found: the grid
    kept the old first garment in view after re-sorting, so a new sort looked wrong; fixed (scrolls to the top on a
    change) and checked that coming back from a garment page keeps the place.
  - Outfit editor: select the shirt → **Turn** slider (≈33°) → **Background** → **From gallery** (a made-up gradient)
    → Save: the outfit page shows the turned shirt on the picture.
  - Export with the picture and turns in it (`backgroundImage: images/….jpg`, `rotation: 32.9`), then **cleared the
    app's data** (emulator) → the first start showed **Import a profile** → imported → straight into the wardrobe;
    outfits and diary with the picture, colours and turns.
- **v0.2.0 – M2: outfit ratings, the diary, export and import (2026-11-19).** Unit tests: 82 JVM tests pass, 24 of
  them new (diary order and wear counts, the diary editor ViewModel, the diary's month grouping and compact collage, the
  `profile.yaml` round trip, tolerance of unknown keys, star rounding, safe file names and unique import names, the
  export/import ViewModel, the test user's diary, the garment page's wear history). Manual tests on the emulator
  (Medium_Phone_API_36, Android 16, Google Play image), **release build** (R8, release key):
  - **Update from the published v0.1.2 release APK with real data** (the test user and a profile of made-up data):
    auto-migration 2 → 3 ran, the garments and the three outfits were all there, the Diary tab was empty. No crash.
  - Test user **Reset**: 13 diary entries over the past year (Christmas Eve, Sam's birthday, …), outfits with ratings.
  - Diary list: month headers, day and weekday, collages, occasions, stars and hearts, note marks.
  - Entry page → **Open outfit** → outfit page (rating, aspects, review, "Worn 3 times · last …" with its entries) →
    a garment from the strip → garment page with its own wear history.
  - Outfit aspect rating changed (Fit 3.5) and kept.
  - **Log wear** from a garment page: preset garment and today; typing "birth" filtered the occasion chips to "Sam's
    birthday"; Yesterday; a note; Save → back on the garment, "Worn 4 times".
  - Diary **+** → **Choose outfit** (grid) → Midsummer → date picker (2 Nov) → Save: listed under 2 Mon.
  - **Deleting an outfit** keeps its entries, which then show the garments worn ("Midsummer dress + 2 more").
  - **Export** to Downloads through the system dialog: a 1.5 MB zip with `profile.yaml` (read by hand: categories,
    garments, outfits, 15 diary entries) and 53 pictures. **Import** of it: a new profile "Test user" with 32
    garments, 3 outfits and 15 entries; **Switch to it**: pictures, ratings and the diary all came back. A second
    export (picture paths now `images/…`) imported as "Test user (2)", pictures shown.
  - Found and fixed before release: single garments looked tiny in the diary's collages (now a compact layout), the
    entry page's collage pushed the garments off the screen (now narrower), and the reset bug above.
- **v0.1.2 – names, several categories, reviews, cut-outs and outfits (2026-11-19).** Unit tests: 58 JVM tests pass,
  21 of them new (garment page and outfit editor ViewModels, outfit board maths and layout, the cut-out crop, garment
  texts, several-category filtering, the test wardrobe's new data). Manual tests on the emulator (Medium_Phone_API_36,
  Android 16, Google Play image), **release build** (R8, release key) unless noted:
  - **Update from the published v0.1.1 release APK with real data** (a profile with a garment from a made-up gallery
    photo, Sweater, Blue, 4 stars, plus the test user): database migration 1 → 2 ran, the garment kept its photo,
    category, colour and rating, and the test user's 32 garments were all there. No crash.
  - Editor: name and description (the 40-character cap holds), a second category (Sweater (main) · Hoodie); the page
    shows the name as the title with "Sweater · Hoodie" and the colour underneath, and the description.
  - Aspect ratings (4.5 and 3.5 stars) and a review, saved and shown.
  - **Remove background** with ML Kit: in the debug build first (which also had Play services download the model),
    then in the release build with **no INTERNET permission** (`aapt2 dump permissions` lists none): the cut-out
    replaced the photo; **Use the original photo** is offered. The first release build crashed in ML Kit (missing R8
    rules); fixed with `consumer-rules.pro` before release.
  - Test user **Reset**: cut-outs in the grid, three outfits on the Outfits tab.
  - New outfit: + → gallery (3 picked) → board → drag a garment → select (outline, Size slider, Remove) → name → Save;
    open it again → Delete (confirmed). The first default layout cut off the top row; fixed and checked again.
  - Deleting a garment that is in two outfits removes it from both.
- **v0.1.1 – profiles and the test user (2026-11-19).** Unit tests: 37 JVM tests pass, 13 of them new (profile
  rules and paths, the test wardrobe's contents, `ProfilesViewModel`). Manual tests on the emulator
  (Medium_Phone_API_36, Android 16), debug build:
  - **Update from v0.1.0** with garments already saved: the app asked "What's your name?" (a blank name shows an
    error), and the existing garments were all there in the new profile.
  - Settings (gear) → Profiles → **Test user**: the prefilled wardrobe (32 garments, 30 drawn pictures) is made in
    well under a second and shown; the wardrobe's title shows whose wardrobe it is. Garment page of a test garment.
  - **Reset** of the test wardrobe (with confirmation): refilled, old pictures removed.
  - Switching between profiles; a **new profile** starts empty; a duplicate name is refused; **deleting** a profile
    (with confirmation) removes its database and its folder; the last profile of your own can't be deleted (no delete
    button; the rule is also enforced in the data layer); deleting the test user and creating it again.
  - Rotation on the Profiles screen keeps the active profile and the screen stack.
  - **Release build** (R8) from a fresh install: name → Settings → Profiles → Test user → garment page, no crashes.
  - **v0.1.1 release APK** installed as an update over the published v0.1.0 release APK (same key, so Android accepted
    the update): "What's your name?", Settings shows version 0.1.1, test user created, switching back, no crashes.
    (That run had no garments in 0.1.0 because the tap came too early, so "garments are kept on update" is verified
    with the debug build above, not the release.)

- **v0.1.0 – M1 wardrobe (2026-11-18).** Unit tests: 24 JVM tests pass (`./gradlew testDebugUnitTest`). Manual tests on
  the emulator (Medium_Phone_API_36, Android 16), debug build, with `scripts/emulator-ui.sh`:
  - Add a garment: **Take photo** opens the camera app, the photo comes back into the editor and is saved downscaled
    (22 KB) in `files/photos/`; category required (Save disabled until one is chosen), colour optional.
  - **From gallery** with the system photo picker (made-up test image).
  - **New category** from the editor ("Rain gear"), selected at once.
  - **Camera trip with the app killed** (`am kill` while the camera was open): the editor came back with the category
    and the new photo, and saved.
  - Unsaved photo from a cancelled editor removed on the next fresh start (4 files → 3).
  - Garment page: 3.5 stars by tapping, like, edit (added a colour), delete with confirmation (photo file removed).
  - Wardrobe grid: Liked filter, category chip, oldest-first sort.
  - Categories: deleting an in-use category is refused with a message; hiding a built-in one removes it from the
    editor.
  - **Release build** (R8, release key) installed on the emulator: categories seeded, camera → save → garment page →
    grid → categories, no crashes in logcat.

## 📝 Next up (roadmap)

Each completed milestone gets a minor version tag (AGENTS.md). The milestones are a draft (BRAINSTORMING.md).

- **Still open** (BRAINSTORMING.md "Open questions"): final name, colours and icon; optional price for cost per wear;
  a photo per diary entry.
- **M3 – lists and statistics:** lists, wishlist, most/least worn, cost per wear, year in review.
- **Later:** a calendar view of the diary, a photo per diary entry, AI tagging, sync between devices
  (BRAINSTORMING.md).
