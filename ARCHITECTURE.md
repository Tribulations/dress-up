# Architecture & Design

No code is built yet. **This is a draft:** we work agile, and the sections below are filled in as decisions are
made. Sections marked **TBD** are still open; open questions are in BRAINSTORMING.md.

## 1. Constraints & targets

| | |
|---|---|
| Device | Samsung Galaxy S20 (Android 13) |
| Network | **Offline first.** Everything works with no network and no account |
| Data | Stays on the device; export/import for backup |
| Distribution | Personal use / sideloaded |

## 2. Tech stack

| Layer | Choice | Status |
|---|---|---|
| Platform | **Android only** | Decided 2026-11-18 |
| Language / UI | Kotlin, Jetpack Compose, Material 3 with an own theme | Decided 2026-11-18 |
| Database | Room | Assumed |
| Settings | DataStore | Assumed |
| Photos | **Taken in the app with the camera** (CameraX) and attached to the garment; picking an existing photo from the gallery as an alternative. Files in app-private storage; Coil for loading | Decided 2026-11-18 (in-app camera); library assumed |
| DI | Hilt (or manual DI), as in the user's other projects | TBD |
| Build | Gradle with a version catalog and convention plugins (`build-logic/`) | Assumed |

## 3. High-level overview

**TBD.** Expected shape: Compose screens → ViewModels → repositories in `core:data` → Room and photo files.

## 4. Module structure

**TBD.** Draft in GUIDELINES.md §2.1 (`app`, `feature:*`, `core:model`, `core:data`, `core:ui`).

## 5. Data model

Decided 2026-11-18 (the user's answers in BRAINSTORMING.md "Open questions"):

- **Garment:** id, **category (required)**, **colour (optional)**, **photo** (taken with the camera, §2), rating
  (optional), liked (optional), created date. Nothing else is required. More optional fields (name, brand, size,
  price, material, notes) can be added later when a feature needs them.
- **Category:** a **built-in default list** (e.g. pants, shirt, t-shirt, sweater, jacket, dress, skirt, shoes,
  accessories) **plus categories the user creates**. Built-in ones can be hidden but not deleted while in use.
- **Rating:** **both** a star rating (½ to 5 stars, in half steps, like Letterboxd) **and** a separate like
  (heart). Each is optional and independent.
- **WearEntry:** id, garment id, date, optional note. The diary logs **single garments**, one entry per garment
  worn (outfits are a later idea, BRAINSTORMING.md).

Not decided yet: **List:** id, name, ordered garment ids (M3).

## 6. Settings

**TBD.**

## 7. Key flows

### 7.1 Add a garment (decided 2026-11-18, details TBD)

Open the camera in the app → take a photo of the garment → choose a category (required) and optionally a colour
→ save. The photo is linked to that garment. A garment can also be saved without a photo and get one later.

**TBD:** log a wear from the garment page or the diary, export/import.

## 8. Navigation & screens

**TBD.** See BRAINSTORMING.md "Letterboxd concepts mapped to a wardrobe".

## 9. Testing strategy

See GUIDELINES.md §5.

## 10. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Photos take a lot of storage | Downscale and compress on import; show storage use in settings |
| Losing data on a phone change | Export/import from M2; never drop data in migrations |
| Looking too much like Letterboxd | Own visual identity (GUIDELINES.md §4.1) |
