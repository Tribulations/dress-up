# dress-up

*(working title)*

A personal **wardrobe tracker**: photograph and catalogue the clothes you own, log what you wear in a dated
diary, rate and review your garments, put them in lists and see statistics about how you dress. The UI takes
inspiration from **Letterboxd**: what Letterboxd does for the films you watch, dress-up does for the clothes in
your wardrobe.

> **Current state (2026-11-19): v0.1.1 released** (milestone M1, the wardrobe, plus profiles and a test user). Download the APK from
> [GitHub releases](https://github.com/Tribulations/dress-up/releases). Tested on the emulator; not yet on the
> phone (STATUS.md). Next: the diary and backup (M2).

## Features

| Feature | State |
|---|---|
| Add a garment: take a photo from inside the app (or pick one from the gallery), pick a category (required) and a colour (optional) | 🧪 Built in v0.1.0, tested on the emulator |
| Categories: built-in list (pants, shirt, …) plus your own; hide, rename, delete | 🧪 Built in v0.1.0, tested on the emulator |
| Wardrobe grid: photo tiles, filter (liked, category, colour) and sort | 🧪 Built in v0.1.0, tested on the emulator |
| Garment page: big photo, details, star rating (with halves) and a like, edit, delete | 🧪 Built in v0.1.0, tested on the emulator |
| Profiles: your name on the first start; add, rename, switch and delete profiles in Settings | 🧪 Built in v0.1.1, tested on the emulator |
| Test user: a prefilled, made-up wardrobe to try the app with, can be reset | 🧪 Built in v0.1.1, tested on the emulator |
| Wear history on the garment page | 📝 Planned (M2) |
| Diary: log which garments you wore on a date, with an optional note | 📝 Planned (M2) |
| Statistics: most and least worn, cost per wear, year in review | 📝 Planned (M3) |
| Lists (e.g. "Summer", "Work", "To donate") and a wishlist | 📝 Planned (M3) |
| Export and backup of the whole wardrobe | 📝 Planned (M2) |
| Outfits: combine garments and log an outfit at once | 📝 Idea (BRAINSTORMING.md) |
| Automatic background removal / AI tagging of photos | 📝 Idea (BRAINSTORMING.md) |

Legend: 📝 Planned · 🚧 In progress · 🧪 Implemented, untested · ✅ Done & tested

## Tech stack

An **Android** app in **Kotlin** with **Jetpack Compose**, offline first, with **Room** for data, the phone's camera
app opened from inside dress-up for photos, and **Coil** to show them (ARCHITECTURE.md §2). No permissions, no network.

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
