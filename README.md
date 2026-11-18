# dress-up

*(working title)*

A personal **wardrobe tracker**: photograph and catalogue the clothes you own, log what you wear in a dated
diary, rate and review your garments, put them in lists and see statistics about how you dress. The UI takes
inspiration from **Letterboxd**: what Letterboxd does for the films you watch, dress-up does for the clothes in
your wardrobe.

> **Current state (2026-11-18): planning.** No code yet. Main decisions made (ARCHITECTURE.md §2, §5): Android
> only, category required and colour optional, built-in plus own categories, star rating and like, diary of
> single garments, photos taken in the app.

## Features

| Feature | State |
|---|---|
| Add a garment: take a photo in the app, pick a category (required) and a colour (optional) | 📝 Planned (M1) |
| Categories: built-in list (pants, shirt, …) plus your own | 📝 Planned (M1) |
| Wardrobe grid: photo tiles, filter and sort (Letterboxd poster-grid style) | 📝 Planned (M1) |
| Garment page: big photo, details, star rating (with halves) and a like, wear history | 📝 Planned (M1) |
| Diary: log which garments you wore on a date, with an optional note | 📝 Planned (M2) |
| Statistics: most and least worn, cost per wear, year in review | 📝 Planned (M3) |
| Lists (e.g. "Summer", "Work", "To donate") and a wishlist | 📝 Planned (M3) |
| Export and backup of the whole wardrobe | 📝 Planned (M2) |
| Outfits: combine garments and log an outfit at once | 📝 Idea (BRAINSTORMING.md) |
| Automatic background removal / AI tagging of photos | 📝 Idea (BRAINSTORMING.md) |

Legend: 📝 Planned · 🚧 In progress · 🧪 Implemented, untested · ✅ Done & tested

## Tech stack

An **Android** app in **Kotlin** with **Jetpack Compose** (decided 2026-11-18), offline first, with **Room**
for data and an in-app camera (CameraX) for photos stored in app storage (ARCHITECTURE.md §2).

## Requirements

- Phone: Samsung Galaxy S20 (Android 13).
- Personal use only.

## Building

Nothing to build yet. This will be filled in once the project skeleton exists (see STATUS.md and
ARCHITECTURE.md §4).

## Documentation

| File | Purpose |
|---|---|
| [BRAINSTORMING.md](BRAINSTORMING.md) | Vision, Letterboxd concepts, ideas, open questions |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Decided technical design |
| [GUIDELINES.md](GUIDELINES.md) | Coding, modularity, privacy and workflow rules |
| [AGENTS.md](AGENTS.md) / [CLAUDE.md](CLAUDE.md) | Where AI agents start: reading order and doc-update rules |
| [STATUS.md](STATUS.md) | What is being worked on, done, tested and untested |
| [CHANGELOG.md](CHANGELOG.md) | Notable changes per version |
