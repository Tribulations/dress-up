# Architecture & Design

The decided technical design. **This is a living draft:** we work agile, and sections grow as features are built.
Sections marked **TBD** are still open; open questions are in BRAINSTORMING.md.

## 1. Constraints & targets

| | |
|---|---|
| Device | Samsung Galaxy S20 (Android 13) |
| SDK | minSdk 29 (Android 10), targetSdk 35, compileSdk 37 (`gradle/libs.versions.toml`) |
| Network | **Offline first.** Everything works with no network and no account. The app has no INTERNET permission |
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
| DI | **Manual:** one `DressUpData` created by the `Application`, handed to the feature routes | Built (v0.1.0) |
| Navigation | Navigation Compose with string routes, all in `app` | Built (v0.1.0) |
| Settings | None needed yet (DataStore when one is) | TBD |
| Build | Gradle with a version catalog and convention plugins (`build-logic/`), AGP 9 built-in Kotlin, KSP for Room | Built (v0.1.0) |

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
| `app` | `DressUpApplication` (holds `DressUpData`), `MainActivity`, `DressUpNavHost`, launcher icon, signing |
| `core:model` | `Garment`, `GarmentDraft`, `Category`, `Rating`, `GarmentColour` (plain Kotlin) |
| `core:data` | Room database, DAOs, `GarmentRepository`, `CategoryRepository`, `PhotoStorage`, `DressUpData`, the camera FileProvider |
| `core:ui` | `DressUpTheme`, `RatingBar`, `LikeButton`, `GarmentPhoto`, `ColourSwatch`, `NameDialog` |
| `feature:wardrobe` | The photo grid with filters and sorting |
| `feature:garment` | Garment page and the add/edit editor (camera, gallery) |
| `feature:categories` | Managing categories |

Convention plugins: `dressup.android.application`, `dressup.android.library`, `dressup.android.compose`,
`dressup.android.feature` (library + Compose + the three core modules).

## 5. Data model

Decided 2026-11-18 (the user's answers in BRAINSTORMING.md "Open questions"); built in v0.1.0, Room schema version 1.

- **Garment** (`garments`): id, **categoryId (required)**, **colourKey (optional)**, **photoFileName (optional)**,
  ratingHalfStars (optional, 1..10), liked, createdAtMillis. More optional fields (name, brand, size, price, material,
  notes) can be added later when a feature needs them, with a migration.
- **Category** (`categories`): id, name, builtInKey, hidden, sortOrder. A **built-in list** (T-shirt, Shirt, Sweater,
  Hoodie, Jacket, Coat, Pants, Jeans, Shorts, Skirt, Dress, Shoes, Accessory) is seeded when the database is created;
  `builtInKey` marks them and must never change. **The user's own** categories have `builtInKey = null`. Built-in
  ones can only be hidden; the user's own can also be renamed and deleted. Names are unique (case-insensitive), at
  most 30 characters.
- The garment → category foreign key is **RESTRICT**: a category in use can't be deleted, so no garment ever loses
  its category.
- **Rating:** **both** a star rating (½ to 5 stars, in half steps) **and** a separate like (heart), each optional.
- **Colour:** one of a fixed list (`GarmentColour`: black, white, grey, beige, brown, red, pink, orange, yellow, green,
  blue, navy, purple, multicolour). The stored `key` must never change.
- **WearEntry** (M2, not built): id, garment id, date, optional note. The diary logs **single garments**, one entry per
  garment worn (outfits are a later idea, BRAINSTORMING.md).
- **List** (M3, not decided): id, name, ordered garment ids.

## 6. Settings

None yet.

## 7. Key flows

### 7.1 Add or edit a garment (built in v0.1.0)

1. **Take photo** opens the camera app with a FileProvider URI in `cache/captures/` (`PhotoStorage.newCaptureUri`).
   **From gallery** opens the system photo picker instead.
2. The result is imported at once: decoded with `ImageDecoder` (applies the EXIF rotation), downscaled, written to
   `files/photos/<uuid>.jpg`.
3. Choose a category (required; **New category** adds one from the editor) and optionally a colour, then **Save**.
   A new garment then opens its page; an edit returns to it.

Never losing work or photos:

- The editor's inputs live in `SavedStateHandle`, and the pending camera URI in `rememberSaveable`, so if Android
  stops dress-up while the camera is open, the editor comes back with the category, colour and new photo.
- On edit, the old photo file is deleted only **after** the database points at the new one.
- A photo taken in the editor and then replaced before saving is deleted at once. A photo from an editor that was left
  without saving is removed by `DressUpData.cleanUpUnusedPhotos()`, which runs only on a **fresh** app start (not when
  Android restores the app, when an open editor may still need it).
- Deleting a garment (after a confirmation dialog) deletes its photo.

### 7.2 Rate and like (built in v0.1.0)

On the garment page: tap or drag across the stars (half steps); tapping the current rating again clears it. The heart
is a separate toggle. Both save immediately.

### 7.3 Browse the wardrobe (built in v0.1.0)

A grid of 2:3 photo tiles (adaptive, about three columns on a phone), newest first. Filter chips: **Liked**, then
each category in use, then each colour in use (only chips that give a result). Several chips of one kind mean "any of
them"; different kinds combine. Sort: newest, oldest, highest rated (unrated last), by category.

**TBD:** log a wear (M2), export/import (M2).

## 8. Navigation & screens

| Route | Screen |
|---|---|
| `wardrobe` (start) | Wardrobe grid; + adds a garment, the gear opens Categories, the list icon sorts |
| `garment/{id}` | Garment page: photo, category, colour, date added, rating, like; edit and delete |
| `editor?id={id}` | Add (no id) or edit a garment |
| `categories` | Your categories (show/hide, rename, delete) and built-in ones (show/hide) |

Visual identity (own, GUIDELINES.md §4.1): dark aubergine background, mustard accent (stars, buttons), rose for
likes, and a mustard coat-hanger launcher icon. The name is still a working title (BRAINSTORMING.md).

## 9. Testing strategy

See GUIDELINES.md §5. Unit tests (JVM): rating and colour models, category-name cleaning, the rating bar's touch
maths, wardrobe filtering/sorting, and the editor ViewModel with fake repositories. Manual tests on the emulator
with `scripts/run-on-device.sh` and `scripts/emulator-ui.sh`.

## 10. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Photos take a lot of storage | Downscaled to 1600 px and JPEG 85 on import (about 20–300 KB each) |
| Losing data on a phone change | Export/import in M2; never drop data in migrations; schemas are committed |
| Android stops the app during a camera trip | Editor state in `SavedStateHandle`; orphan cleanup only on fresh starts (§7.1) |
| A release signed with another key can't update the installed app | One release key, checked before every upload (AGENTS.md "Releases") |
| Looking too much like Letterboxd | Own visual identity (GUIDELINES.md §4.1, §8 above) |
