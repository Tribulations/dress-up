# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/Tribulations/dress-up/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Tribulations/dress-up/releases/tag/v0.1.0
