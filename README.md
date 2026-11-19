# dress-up

*(working title)*

A personal **wardrobe tracker**: photograph and catalogue the clothes you own, log what you wear in a dated
diary, rate and review your garments, put them in lists and see statistics about how you dress. The UI takes
inspiration from **Letterboxd**: what Letterboxd does for the films you watch, dress-up does for the clothes in
your wardrobe.

> **Current state (2026-11-19): v0.2.1 released** (milestones M1, the wardrobe, and M2, the diary and backup: outfits
> with ratings, a wear diary with occasions, wear counts, export and import, plus profiles, a test user and cut-outs).
> Download the APK from [GitHub releases](https://github.com/Tribulations/dress-up/releases). Tested on the emulator;
> not yet on the phone (STATUS.md). Next: lists and statistics (M3).

## Features

| Feature | State |
|---|---|
| Add a garment: take a photo from inside the app (or pick one from the gallery), pick a category (required) and a colour (optional) | 🧪 Built in v0.1.0, tested on the emulator |
| Garment name and description; the name is the page's title, the categories go underneath | 🧪 Built in v0.1.2, tested on the emulator |
| Several categories per garment (the first is the main one) | 🧪 Built in v0.1.2, tested on the emulator |
| Rate the details (fit, comfort, quality, style, easy care) and write a review | 🧪 Built in v0.1.2, tested on the emulator |
| Remove the background from a garment's photo, on the phone (optional adapter, ML Kit) | 🧪 Built in v0.1.2, tested on the emulator |
| Outfits: pick garments from a gallery and lay them out as a collage | 🧪 Built in v0.1.2, tested on the emulator |
| Turn garments on the outfit board; outfit backgrounds (a colour or your own picture) | 🧪 Built in v0.2.1, tested on the emulator |
| Rate and review outfits like garments (stars, like, details, review) on an outfit page | 🧪 Built in v0.2.0, tested on the emulator |
| Diary: log an outfit or garments worn on a date, with an occasion ("Christmas Eve") and a note; a list by month | 🧪 Built in v0.2.0, tested on the emulator |
| Wear counts and wear history on garment and outfit pages | 🧪 Built in v0.2.0, tested on the emulator |
| Export a profile as a zip (readable `profile.yaml` + images) and import one as a new profile, also on the first start | 🧪 Built in v0.2.0 (first start v0.2.1), tested on the emulator |
| Categories: built-in list (pants, shirt, …) plus your own; hide, rename, delete | 🧪 Built in v0.1.0, tested on the emulator |
| Wardrobe grid: photo tiles, filter (liked, category, colour) and sort (also most and least worn since v0.2.1) | 🧪 Built in v0.1.0, tested on the emulator |
| Garment page: big photo, details, star rating (with halves) and a like, edit, delete | 🧪 Built in v0.1.0, tested on the emulator |
| Profiles: your name on the first start; add, rename, switch and delete profiles in Settings | 🧪 Built in v0.1.1, tested on the emulator |
| Test user: a prefilled, made-up wardrobe to try the app with, can be reset | 🧪 Built in v0.1.1, tested on the emulator |
| Statistics: most and least worn, cost per wear, year in review | 📝 Planned (M3) |
| Lists (e.g. "Summer", "Work", "To donate") and a wishlist | 📝 Planned (M3) |
| A calendar view of the diary | 📝 Idea (BRAINSTORMING.md) |
| AI tagging of photos | 📝 Idea (BRAINSTORMING.md) |

Legend: 📝 Planned · 🚧 In progress · 🧪 Implemented, untested · ✅ Done & tested

## Tech stack

An **Android** app in **Kotlin** with **Jetpack Compose**, offline first, with **Room** for data, the phone's camera
app opened from inside dress-up for photos, and **Coil** to show them (ARCHITECTURE.md §2). Background removal is an
optional adapter using ML Kit on the device. Exports are YAML (kaml) in a zip. No permissions, no network.

## Requirements

- Android 10 or later. Built for a Samsung Galaxy S20 (Android 13).
- Personal use only.
- To build: JDK 21 and the Android SDK (`sdk.dir` in `local.properties`).

## Building

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/dress-up-<version>-debug.apk (id com.dressup.app.debug)
./gradlew testDebugUnitTest      # unit tests
./gradlew assembleRelease        # signed release APK, needs the signing.* lines in local.properties (AGENTS.md)
scripts/run-on-device.sh         # build, boot the emulator if needed, install and launch (debug)
```

The debug build has its own app id, so it can be installed next to the release.

## Documentation

| File | Purpose |
|---|---|
| [BRAINSTORMING.md](BRAINSTORMING.md) | Vision, Letterboxd concepts, ideas, open questions |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Decided technical design |
| [GUIDELINES.md](GUIDELINES.md) | Coding, modularity, privacy and workflow rules |
| [AGENTS.md](AGENTS.md) / [CLAUDE.md](CLAUDE.md) | Where AI agents start: reading order and doc-update rules |
| [STATUS.md](STATUS.md) | What is being worked on, done, tested and untested |
| [CHANGELOG.md](CHANGELOG.md) | Notable changes per version |
