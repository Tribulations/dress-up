# Status

Tracks what is being worked on and how far each piece has come.
Update this file whenever work starts, finishes or is verified.

**Legend / workflow:** an item moves through
`Next up → Currently working on → Implemented, not yet tested → Done & tested`.
An item only counts as **tested** when it has been verified through automated tests,
on a real device, or both. Record how it was tested.

_Last updated: 2026-11-19_

---

## 🚧 Currently working on

_Nothing. v0.1.2 (names, several categories, reviews, cut-outs and outfits) is released; M2 is next._

## 🧪 Implemented, not yet tested

- **Everything in v0.1.0–v0.1.2 on the user's phone (Galaxy S20).** Only tested on the emulator so far (below). Things
  worth checking on the real phone: the Samsung camera app returns the photo, portrait photos are upright, the grid
  with real photos, **Remove background on real photos** (the emulator only had flat, made-up pictures, and its model
  download is the first one on that phone), pinching on the outfit board with real fingers.
- **The outfit board's pinch-to-resize** was not tested (the emulator's scripted input can't pinch); dragging and the
  Size slider were.

## ✅ Done & tested

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
- **M2 – diary and backup:** log wears by date, diary view, wear history per garment, export/import of the whole
  wardrobe (data + photos).
- **M3 – lists and statistics:** lists, wishlist, most/least worn, cost per wear, year in review.
- **Later:** logging a whole outfit as one wear, AI tagging, sync between devices (BRAINSTORMING.md).
