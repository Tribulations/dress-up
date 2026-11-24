# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **Code (no visible change):** export, import and the test user share one mapping between the database and
  `profile.yaml`; the test user is now filled through the importer, so every Reset also tests the import.
- **Code:** garments and outfits share one ratings type (overall, aspects, review, like) from the model to the screens.
- **Code:** the editors keep their unsaved inputs as one value (`SavedDraft`) instead of a key per field.

## [0.2.2] - 2026-11-23

### Added
- **Several colours per garment:** tap blue and red (and more) in the garment editor. The garment shows up under the
  wardrobe filter of each of its colours. Multicolour stays, for patterns, and can be combined with others.
- **Layers in the outfit editor:** a strip of the outfit's garments under the board, the front one first. Tap one to
  select it on the board; **Forward** and **Back** move the selected garment one layer.
- **Lock** a garment on the outfit board, e.g. a coat in the background: touching it on the board neither selects nor
  moves it. Unlock it from the layer strip. Saved with the outfit and in exports.

### Changed
- On the outfit board, once a garment is selected, dragging, pinching and twisting **anywhere on the board** move only
  that garment, so a crowded board no longer picks up the wrong one mid-gesture. A quick tap selects another garment.
- Selecting a garment no longer brings it to the front (use Forward and Back).
- Exports write `colours:` as a list; files with the older single `colour:` still import.
- Database version 5 (added by itself on update; a garment's colour is kept).

## [0.2.1] - 2026-11-19

### Added
- **Turn garments** on the outfit board: twist with two fingers, or use the new **Turn** slider (with Straighten).
- **Outfit backgrounds:** a colour from a palette, or your own picture from the camera or the gallery. Shown wherever
  the outfit is, and included in exports.
- **Sort by Most worn and Least worn** in the wardrobe (counted from the diary; never-worn garments count as least).
- **Import a profile on the first start**, next to "What's your name?": a new phone can start with the wardrobe from
  an export.

### Changed
- Cut-out garments are zoomed in to fill their frames (grid, garment page, pickers), whole but with almost no margin.
- Database version 4 (added by itself on update).

### Fixed
- After changing the sort or a filter, the wardrobe grid stayed scrolled to where the old first garment was.

## [0.2.0] - 2026-11-19

M2: the diary, outfit ratings, and export and import.

### Added
- **Diary** tab: every wear in a list, newest first, under month headers, with the day, a small collage of what was
  worn, the occasion and the stars. Tap an entry for its page (date, occasion, note, what was worn), and go on from
  there to the outfit and each of its garments.
- **Log wear** on garment and outfit pages (today, one tap away) and **+** in the Diary: pick an outfit or single
  garments, the date (Today, Yesterday or any past day), an optional **occasion** ("Christmas Eve", "Sam's birthday";
  the ones used before are suggested) and an optional note.
- **Wear counts:** garment and outfit pages show "Worn 7 times · last 24 Dec 2025" and the entries they were worn in.
- **Outfit page** with the collage (tap a garment to open it), its garments, and the same ratings as a garment: stars,
  heart, details (comfort, style, fit, for the occasion) and a review. The Outfits grid shows the stars and heart.
- **Export & import** in Settings: save a profile as one .zip file wherever you like (a `profile.yaml` you can read and
  edit, plus an `images/` folder with the pictures), and import such a file as a new profile. No permission needed;
  existing profiles are never changed by an import.
- The test user now has a diary with occasions and notes, and rated outfits.

### Changed
- Tapping an outfit opens its page; editing it is the pencil there.
- Logging an outfit records its garments too, so editing the outfit later doesn't change the diary, and deleting an
  outfit keeps its diary entries.
- Database version 3 (added by itself on update; nothing is lost).

### Fixed
- Leaving the Profiles screen while the test user was being filled or reset stopped it half-way.

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
