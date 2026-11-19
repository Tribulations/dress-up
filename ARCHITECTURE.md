# Architecture & Design

The decided technical design. **This is a living draft:** we work agile, and sections grow as features are built.
Sections marked **TBD** are still open; open questions are in BRAINSTORMING.md.

## 1. Constraints & targets

| | |
|---|---|
| Device | Samsung Galaxy S20 (Android 13) |
| SDK | minSdk 29 (Android 10), targetSdk 35, compileSdk 37 (`gradle/libs.versions.toml`) |
| Network | **Offline first.** Everything works with no network and no account. The app has no INTERNET permission (the one ML Kit asks for is removed, §7.5) |
| Permissions | **None.** The camera app takes the photo (§7.1) and the system photo picker reads the gallery |
| Data | Stays on the device; Android cloud backup is switched off (`data_extraction_rules.xml`); export/import comes in M2 |
| Distribution | Personal use / sideloaded APK from GitHub releases (AGENTS.md "Releases") |

## 2. Tech stack

| Layer | Choice | Status |
|---|---|---|
| Platform | **Android only** | Decided 2026-11-18 |
| Language / UI | Kotlin, Jetpack Compose, Material 3 with an own theme (`core:ui` `DressUpTheme`) | Decided 2026-11-18 |
| Database | Room, schema exported to `core/data/schemas/` | Built (v0.1.0) |
| Photos | The **phone's camera app, opened from inside dress-up** (`ActivityResultContracts.TakePicture`), or the system photo picker. The photo is downscaled (longest edge 1600 px, JPEG 85) into app-private storage and linked to the garment. Coil loads it | Built (v0.1.0). A CameraX viewfinder inside the app stays possible later (BRAINSTORMING.md) |
| DI | **Manual:** the `Application` holds one `DressUpProfiles`, which gives each profile its own `DressUpData`; routes get the active one | Built (v0.1.0; profiles 2026-11-19) |
| Navigation | Navigation Compose with string routes, all in `app` | Built (v0.1.0) |
| Settings | The profile list lives in SharedPreferences (`ProfileStore`); no other settings yet (DataStore when needed) | Built (2026-11-19) |
| Build | Gradle with a version catalog and convention plugins (`build-logic/`), AGP 9 built-in Kotlin, KSP for Room | Built (v0.1.0) |
| Background removal | **Optional adapter** `adapter:cutout-mlkit`: ML Kit subject segmentation (Google Play services), on the device. Switched with `dressup.cutout` in `gradle.properties` (§7.5) | Built (v0.1.2) |

**Why the camera app and not CameraX (2026-11-18):** it gives the S20's full camera (focus, flash, HDR) with no
camera permission and much less code, and the photo still goes straight into the garment. The one weak spot, Android
stopping dress-up while the camera is open, is handled (§7.1).

## 3. High-level overview

```
Compose screens (feature:*)  →  ViewModels (feature:*)  →  repositories (core:data)  →  Room + photo files
           ↑ navigation, wiring: app                                                    (filesDir/photos)
```

Screens are stateless (`FooScreen(uiState, onAction)`); each feature has a `FooRoute` that creates its ViewModel with a
`viewModelFactory` from the repositories in `DressUpData`.

## 4. Module structure

| Module | Contents |
|---|---|
| `app` | `DressUpApplication` (holds `DressUpProfiles`), `MainActivity` (welcome or the active profile), `ProfileScope`, `DressUpNavHost` (with the tab bar), launcher icon, signing; `src/cutout/` or `src/noCutout/` wires the optional adapter in or out |
| `core:model` | `Garment`, `GarmentDraft`, `GarmentAspect`, `Category`, `Rating`, `GarmentColour`, `Outfit`, `OutfitItem`, `OutfitDraft`, `Profile`, `ProfilesState` (plain Kotlin) |
| `core:data` | Room database and migrations, DAOs, `GarmentRepository`, `CategoryRepository`, `OutfitRepository`, `PhotoStorage`, the `BackgroundRemover` interface, `DressUpData`, the camera FileProvider; `profile/` (`DressUpProfiles`, `ProfileRepository`, `ProfilePaths`, `ProfileStore`); `testuser/` (the test wardrobe and the painter of its pictures) |
| `core:ui` | `DressUpTheme`, `RatingBar`, `LikeButton`, `GarmentPhoto` (photo or cut-out), `ColourSwatch`, `NameDialog`, `TextDialog`, `DressUpTabBar` and its icons, `garmentTitle`/`garmentSubtitle`, `Profile.displayName()` |
| `feature:wardrobe` | The photo grid with filters and sorting |
| `feature:garment` | Garment page (details, ratings, review, cut-out) and the add/edit editor (camera, gallery) |
| `feature:outfits` | The Outfits tab, the outfit editor with its collage board, and the gallery of garments to pick from |
| `adapter:cutout-mlkit` | Optional: `MlKitBackgroundRemover` (GUIDELINES.md §2.2). Only `app` depends on it |
| `feature:categories` | Managing categories |
| `feature:settings` | The settings menu |
| `feature:profiles` | First start ("What's your name?") and the Profiles screen with the test user |

Convention plugins: `dressup.android.application`, `dressup.android.library`, `dressup.android.compose`,
`dressup.android.feature` (library + Compose + the three core modules).

## 5. Data model

Decided 2026-11-18 (the user's answers in BRAINSTORMING.md "Open questions"); built in v0.1.0. Room schema version 2
since v0.1.2 (`MIGRATION_1_2`: the old garments table is renamed, copied into the new one and dropped only after every
row and its category link are in the new tables).

- **Garment** (`garments`): id, **name (optional**, e.g. "Coola favvis tröjan", at most 40 characters),
  **description (optional**, 500), colourKey (optional), photoFileName (optional), **cutoutFileName** (optional, the
  photo with its background removed, §7.5), ratingHalfStars (optional, 1..10), **review (optional**, 2000), liked,
  createdAtMillis. Blank texts are stored as null. More optional fields (brand, size, price, material) can be added
  when a feature needs them, with a migration.
- **A garment's categories** (`garment_categories`: garmentId, categoryId, position): **at least one, possibly
  several** (e.g. Sweater + Sportswear). Position 0 is the **main** category: it is shown first, used for sorting, and
  stands in as the title when the garment has no name. Added 2026-11-19 (before: exactly one category per garment).
- **Aspect ratings** (`garment_aspect_ratings`: garmentId, aspectKey, halfStars): besides the overall rating, a
  garment can be rated ½–5 stars on a **fixed list** of aspects (`GarmentAspect`: fit, comfort, quality, style, easy
  care), so the same aspects can be compared across the wardrobe. No row means not rated; keys never change.
- **Category** (`categories`): id, name, builtInKey, hidden, sortOrder. A **built-in list** (T-shirt, Shirt, Sweater,
  Hoodie, Jacket, Coat, Pants, Jeans, Shorts, Skirt, Dress, Shoes, Accessory) is seeded when the database is created;
  `builtInKey` marks them and must never change. **The user's own** categories have `builtInKey = null`. Built-in
  ones can only be hidden; the user's own can also be renamed and deleted. Names are unique (case-insensitive), at
  most 30 characters.
- The garment-category link's category foreign key is **RESTRICT**: a category in use can't be deleted, so no garment
  ever loses its last category. Deleting a garment removes its links and aspect ratings (CASCADE).
- **Rating:** **both** a star rating (½ to 5 stars, in half steps) **and** a separate like (heart), each optional.
- **Colour:** one of a fixed list (`GarmentColour`: black, white, grey, beige, brown, red, pink, orange, yellow, green,
  blue, navy, purple, multicolour). The stored `key` must never change.
- **WearEntry** (M2, not built): id, garment id, date, optional note. The diary logs **single garments**, one entry per
  garment worn (logging a whole outfit at once is an idea, BRAINSTORMING.md).
- **List** (M3, not decided): id, name, ordered garment ids.

### 5.1 Profiles (built 2026-11-19)

- A **profile** is a whole wardrobe: its own Room database and photo folder, so profiles never mix and deleting one
  is deleting its files. `ProfilePaths`: the first profile (`main`) keeps v0.1.0's names (`dress-up.db`,
  `files/photos/`), so an update loses nothing; any other profile uses `dress-up-<id>.db` and
  `files/profiles/<id>/photos/`.
- The list (id, name, test-user flag) and the active id live in SharedPreferences, written synchronously.
- **First start** (also the first start after updating from v0.1.0): "What's your name?" creates the `main` profile.
- **Test user** (id `test`): made on first use by `TestWardrobe.fill`, which wipes that profile's data and inserts 32
  made-up garments (every built-in category, two own categories, ratings with halves, likes, some without photo or
  colour, dates over the past year; since v0.1.2 also names, descriptions, several categories, aspect ratings, reviews
  and drawn cut-outs for most of them) and three outfits. The pictures are **drawn** by `TestPicturePainter` (Android `Canvas`), so no
  photo files are in the repo or the APK, and none are real (GUIDELINES.md §4). It's added to the list only after the
  fill, so an interrupted fill is just redone. **Reset** runs the same fill again.
- Rules: names are 1–30 characters, unique among your own profiles (ignoring case). The active profile can't be
  deleted, and you always keep one profile of your own. The test user can't be renamed.

### 5.2 Outfits (built 2026-11-19)

- **Outfit** (`outfits`): id, name (optional, 40), createdAtMillis.
- **Outfit item** (`outfit_items`: outfitId, garmentId, position, x, y, scale): one garment on the outfit's collage
  board. x and y are the centre of its picture as fractions of the 3:4 board (0..1), scale its width as a fraction of
  the board's width (0.15–1.2), position the drawing order (last on top). Fractions keep the collage the same at any
  size (the small cards in the list, the big editor board).
- Deleting an outfit leaves its garments alone; deleting a garment removes it from every outfit (CASCADE), and the
  garment's delete dialog says so.

## 6. Settings

A settings menu (the wardrobe's gear) with **Profiles** (§5.1), **Categories** and the app version. No preferences of
their own yet.

## 7. Key flows

### 7.1 Add or edit a garment (built in v0.1.0)

1. **Take photo** opens the camera app with a FileProvider URI in `cache/captures/` (`PhotoStorage.newCaptureUri`).
   **From gallery** opens the system photo picker instead.
2. The result is imported at once: decoded with `ImageDecoder` (applies the EXIF rotation), downscaled, written to
   `files/photos/<uuid>.jpg`.
3. Optionally a **name** and a **description**; one or more **categories** (required; the first picked is the main one,
   marked "(main)"; **New category** adds one from the editor); optionally a colour. Then **Save**. A new garment then
   opens its page; an edit returns to it.

Never losing work or photos:

- The editor's inputs live in `SavedStateHandle`, and the pending camera URI in `rememberSaveable`, so if Android
  stops dress-up while the camera is open, the editor comes back with the category, colour and new photo.
- On edit, the old photo file is deleted only **after** the database points at the new one.
- A photo taken in the editor and then replaced before saving is deleted at once. A photo from an editor that was left
  without saving is removed by `DressUpData.cleanUpUnusedPhotos()`, which runs only on a **fresh** app start (not when
  Android restores the app, when an open editor may still need it).
- Deleting a garment (after a confirmation dialog) deletes its photo and cut-out.
- Replacing the photo removes the old photo's cut-out too (it no longer matches).

### 7.2 Rate, like and review (built in v0.1.0; aspects and review 2026-11-19)

On the garment page: tap or drag across the stars (half steps); tapping the current rating again clears it. The heart
is a separate toggle. Under **Rate the details**, each aspect (§5) has its own smaller star bar that works the same way.
**Your review** opens a text dialog; saving it empty removes the review. Everything saves immediately.

### 7.3 Browse the wardrobe (built in v0.1.0)

A grid of 2:3 photo tiles (adaptive, about three columns on a phone), newest first. Filter chips: **Liked**, then
each category in use, then each colour in use (only chips that give a result). Several chips of one kind mean "any of
them"; different kinds combine. A garment with several categories matches the chip of any of them. Sort: newest, oldest, highest rated (unrated
last), by (main) category. A tile shows the cut-out when there is one (whole, on the app's own soft backdrop),
otherwise the photo cropped to fill.

### 7.4 Switch profile (built 2026-11-19)

`MainActivity` shows the welcome screen until a profile exists, then the active profile's `DressUpNavHost` inside a
`ProfileScope`. The scope gives that profile its own `ViewModelStore` (kept in an activity ViewModel, so it survives
rotation) and keys the navigation on the profile id. Switching profile therefore starts a fresh back stack on the new
wardrobe and clears every ViewModel of the old one: no screen can keep showing or editing the old profile's data.
Opened `DressUpData`s stay open until the profile is deleted. Unused photos are cleaned up on a fresh start and right
after a switch (never while an editor might be open, §7.1).

### 7.5 Remove a photo's background (built 2026-11-19)

1. **Remove background** on the garment page (shown when the garment has a photo and the build has the adapter).
2. `GarmentRepository.cutOut` decodes the photo and hands it to the `BackgroundRemover`. `MlKitBackgroundRemover`
   first makes sure the segmentation model is on the phone (`ModuleInstallClient`; Google Play services downloads it
   once, in its own process, with up to two minutes' wait), then runs ML Kit subject segmentation **on the device**.
   The photo never leaves the phone.
3. The foreground (the garment, on transparency) is cropped to the visible pixels plus a small margin
   (`opaqueBounds`) and saved as a WebP next to the photo; the garment's `cutoutFileName` points at it.
4. The cut-out is shown instead of the photo everywhere. **Use the original photo** deletes the cut-out; the photo was
   never changed, so this is a full undo. No garment found, no Play services, or a failure each show their own
   message.

Privacy: ML Kit's libraries ask for INTERNET and ACCESS_NETWORK_STATE only to send Google usage statistics. The app's
manifest removes both (`tools:node="remove"`), so the app itself still never goes online. Switching the adapter off
(`dressup.cutout=false`) leaves ML Kit out of the APK; the app then hides the button (`BackgroundRemover.None`).

R8: ML Kit 16.0.0-beta1 ships no keep rules and breaks when minified, so `adapter/cutout-mlkit/consumer-rules.pro`
keeps its classes.

### 7.6 Put an outfit together (built 2026-11-19)

1. Outfits tab → **+** opens the **gallery**: every garment's picture (cut-outs where there are any) in a grid. Tap to
   pick or unpick, then **Done**.
2. The picked garments land on the **board** in two columns, fully visible. Drag a garment to move it, pinch to resize
   it; tapping one selects it (outlined) and brings it to the front. The selected garment gets a **Size** slider (for
   resizing without pinching) and **Remove**. **Pick garments** reopens the gallery: still-picked garments keep their
   place, new ones are laid out, unpicked ones leave.
3. Optional name, then **Save**. Tapping an outfit in the list opens the same editor, which can also delete it (with
   confirmation). The editor's state is in `SavedStateHandle`, so it survives Android stopping the app.

**TBD:** log a wear (M2), export/import (M2).

## 8. Navigation & screens

| Route | Screen |
|---|---|
| (before any profile) | "What's your name?" (not a route: shown instead of the navigation) |
| `wardrobe` (start, tab) | Wardrobe grid with the profile's name; + adds a garment, the gear opens Settings, the list icon sorts |
| `outfits` (tab) | Every outfit as a small collage; + makes a new one, the gear opens Settings |
| `outfit?id={id}` | New (no id) or edit an outfit: name, board, gallery |
| `garment/{id}` | Garment page: picture (cut-out or photo) and **Remove background / Use the original photo**; then the **name as the title** with its categories (and colour) underneath, description, date added, rating, like, the aspect ratings and the review; edit and delete |
| `editor?id={id}` | Add (no id) or edit a garment |
| `categories` | Your categories (show/hide, rename, delete) and built-in ones (show/hide) |
| `settings` | Profiles, Categories, version |
| `profiles` | Your profiles (switch, rename, delete, new) and the test user (create/switch, reset, delete) |

The two tabs (Wardrobe, Outfits) share a bottom bar (`DressUpTabBar`, own hanger and outfit icons). Both tabs hang off
the `wardrobe` root, so Back from Outfits goes to the wardrobe, then out of the app.

Visual identity (own, GUIDELINES.md §4.1): dark aubergine background, mustard accent (stars, buttons), rose for
likes, and a mustard coat-hanger launcher icon. The name is still a working title (BRAINSTORMING.md).

## 9. Testing strategy

See GUIDELINES.md §5. Unit tests (JVM): rating and colour models, category-name cleaning, the rating bar's touch
maths, wardrobe filtering/sorting (several categories), the garment editor and page, outfit editor and profiles
ViewModels with fake repositories, the outfit board's maths and default layout, the cut-out crop, profile rules and
paths, and the test wardrobe's contents. The database migration is verified by updating the published previous release
on the emulator with real data (STATUS.md). For manual testing, the **test user** (§5.1) gives a full wardrobe in one tap. Manual tests on the emulator
with `scripts/run-on-device.sh` and `scripts/emulator-ui.sh`.

## 10. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Photos take a lot of storage | Downscaled to 1600 px and JPEG 85 on import (about 20–300 KB each) |
| Losing data on a phone change | Export/import in M2; never drop data in migrations; schemas are committed |
| Android stops the app during a camera trip | Editor state in `SavedStateHandle`; orphan cleanup only on fresh starts (§7.1) |
| A release signed with another key can't update the installed app | One release key, checked before every upload (AGENTS.md "Releases") |
| Looking too much like Letterboxd | Own visual identity (GUIDELINES.md §4.1, §8 above) |
| ML Kit subject segmentation is a beta and needs Google Play services | Behind the `BackgroundRemover` interface and a build switch; the original photo is always kept; R8 keep rules (§7.5) |
