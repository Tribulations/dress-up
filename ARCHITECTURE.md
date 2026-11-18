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
| Platform | Android | Assumed (same as the user's other projects) |
| Language / UI | Kotlin, Jetpack Compose, Material 3 with an own theme | Assumed |
| Database | Room | Assumed |
| Settings | DataStore | Assumed |
| Photos | CameraX or the system photo picker; files in app-private storage; Coil for loading | Assumed |
| DI | Hilt (or manual DI), as in the user's other projects | TBD |
| Build | Gradle with a version catalog and convention plugins (`build-logic/`) | Assumed |

## 3. High-level overview

**TBD.** Expected shape: Compose screens → ViewModels → repositories in `core:data` → Room and photo files.

## 4. Module structure

**TBD.** Draft in GUIDELINES.md §2.1 (`app`, `feature:*`, `core:model`, `core:data`, `core:ui`).

## 5. Data model

**TBD.** First sketch (not decided):

- **Garment:** id, name, category, colours, brand, size, purchase date and price, photo(s), rating, notes,
  status (owned / wishlist / given away).
- **WearEntry:** id, garment id (or outfit id later), date, note.
- **List:** id, name, ordered garment ids.

## 6. Settings

**TBD.**

## 7. Key flows

**TBD.** First candidates: add a garment (photo → details → save), log a wear from the garment page or the diary,
export/import.

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
