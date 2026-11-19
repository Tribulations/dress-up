# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.2] - 2026-11-19

Names, several categories, reviews, cut-outs and outfits. From a tester's wishes.

### Added
- **Name and description** for a garment (both optional), e.g. "Coola favvis tröjan". The garment page shows the name
  as its title with the categories (and colour) underneath.
- **Several categories per garment**, e.g. Sweater and Sportswear. The first one picked is the main one: shown first,
  used for sorting, and the title when there is no name. Filtering on any of them finds the garment.
- **Rate the details:** besides the overall rating, rate fit, comfort, quality, style and easy care, each ½–5 stars.
- **Your review** of a garment.
- **Remove background** on the garment page: cuts the garment out of its photo on the phone (ML Kit, via Google Play
  services, which downloads its model once). The cut-out is shown in the grid, on the page and in outfits; **Use the
  original photo** undoes it. The app still has no internet permission.
- **Outfits** tab: pick garments from a gallery of all your garment pictures and lay them out as a collage (drag,
  pinch or slide to resize, tap to bring to the front). Outfits can be named, edited and deleted.
- The test user now has names, descriptions, several categories, aspect ratings, reviews, cut-outs and three outfits.

### Changed
- Deleting a garment also removes it from the outfits it is in (the dialog says so).
- Database version 2. An update from 0.1.1 keeps every garment: its one category becomes its main category.

### Development
- New modules `feature:outfits` and the optional adapter `adapter:cutout-mlkit` (switch: `dressup.cutout` in
  `gradle.properties`), behind the `BackgroundRemover` interface in `core:data`. R8 keep rules for ML Kit. 58 unit
  tests.

## [0.1.1] - 2026-11-19

Profiles and a test user.

### Added
- **Profiles.** On the first start the app asks for your name and makes your profile. More profiles can be added,
  renamed, switched between and deleted in **Settings → Profiles**. Each profile has its own garments, categories and
  photos. An update from 0.1.0 keeps everything in your first profile.
- **Test user:** a profile with a prefilled, made-up wardrobe (32 garments with drawn pictures, ratings, likes, own
  categories, garments without photo or colour) to try the app with. It can be reset to its original state at any
  time.
- **Settings** menu behind the gear on the wardrobe: Profiles, Categories and the app version. The wardrobe's title
  shows whose wardrobe it is.

### Development
- New modules `feature:settings` and `feature:profiles`; `DressUpProfiles` in `core:data` gives each profile its own
  `DressUpData`. 37 unit tests.

## [0.1.0] - 2026-11-18

The first version (milestone M1, the wardrobe).

### Added
- Add a garment with a photo **taken with the camera from inside the app**, or picked from the gallery. Only a
  category is required; a colour is optional.
- Categories: a built-in list (T-shirt, Shirt, Sweater, Hoodie, Jacket, Coat, Pants, Jeans, Shorts, Skirt, Dress,
  Shoes, Accessory) plus your own. Built-in ones can be hidden; your own can be renamed and deleted (unless in use).
  New categories can also be made right from the editor.
- Wardrobe grid of photo tiles with filters (liked, category, colour) and sorting (newest, oldest, highest rated, by
  category).
- Garment page with a big photo, category, colour and date added; a ½–5 star rating and a separate like; edit and
  delete.
- Photos are downscaled and kept in the app's private storage. No permissions, no network, no account.
- Own look: dark aubergine theme with a mustard accent and a coat-hanger icon.

### Development
- Android project: Kotlin, Jetpack Compose, Room, Coil, convention plugins in `build-logic/`, modules `app`,
  `core:{model,data,ui}`, `feature:{wardrobe,garment,categories}`.
- 24 unit tests; `scripts/run-on-device.sh` and `scripts/emulator-ui.sh` for emulator testing.
- Release APKs signed with a dedicated release key (AGENTS.md "Releases").

### Documentation
- Initial project documentation: `README.md`, `AGENTS.md` (and `CLAUDE.md`, which imports it), `GUIDELINES.md`,
  `ARCHITECTURE.md`, `BRAINSTORMING.md`, `STATUS.md`, `CHANGELOG.md`, plus `.editorconfig` and `.gitignore`, following
  the workflow of the user's other projects (`../noty`, `../d-player`, `../svensk-statistik-app`).
- Main decisions recorded in `ARCHITECTURE.md`: Android only; category required and colour optional; built-in plus
  user-defined categories; star rating (with halves) plus a like; the diary logs single garments; photos taken with
  the camera from inside the app and linked to the garment.

[Unreleased]: https://github.com/Tribulations/dress-up/compare/v0.1.2...HEAD
[0.1.2]: https://github.com/Tribulations/dress-up/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/Tribulations/dress-up/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/Tribulations/dress-up/releases/tag/v0.1.0
