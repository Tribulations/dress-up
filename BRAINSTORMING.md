# Brainstorming

Ideas and open questions. **Nothing here is decided** unless it has been moved into
[ARCHITECTURE.md](ARCHITECTURE.md), [GUIDELINES.md](GUIDELINES.md) or README.md's feature table. Don't implement
from here without the user agreeing.

We work **agile**: this is a living draft. New ideas are added to the [Idea log](#idea-log) as they come.

## Vision

Know what's in your wardrobe and how you actually use it. Make cataloguing clothes and logging what you wear
as pleasant as logging films on Letterboxd: visual, quick, personal, and rewarding to look back on.

## UX principles

- **Photos first.** The wardrobe is a grid of garment photos, the way Letterboxd shows posters.
- **Logging a wear takes seconds:** one or two taps from the garment page or the diary.
- **Looking back is the reward:** diary, statistics and year in review make the data worth entering.
- A dark-first theme where photos stand out, with our own colours and identity.
- Works offline, no account, no ads.

## Letterboxd concepts mapped to a wardrobe

| Letterboxd | dress-up |
|---|---|
| Film poster grid | Grid of garment photos |
| Film page | Garment page: photo, details, rating, notes, wear history |
| Diary (watched on a date) | Diary: worn on a date |
| Rewatch | Wearing the same garment again; wear count |
| Rating (stars) and like | Rating and a "favourite" heart |
| Review | Notes on a garment (fit, how it washes, …) |
| Lists | Lists: "Summer", "Work", "To donate", … |
| Watchlist | Wishlist: clothes you want to buy |
| Stats / year in review | Most and least worn, cost per wear, colours and categories, year in review |
| Tags | Tags: season, occasion, material |

## Core features (MVP)

Draft. The garment fields, categories, ratings and diary unit are decided (ARCHITECTURE.md §5); the rest of the
milestone split is still a draft:

- **M1:** add, edit and delete garments (photo taken in the app, category, optional colour); wardrobe grid with
  filter (category, colour) and sort; garment page.
- **M2:** diary (log wears), wear history per garment, export/import.
- **M3:** lists, wishlist, ratings, statistics.

## Later / maybe

- **Outfits:** combine garments into an outfit, log the whole outfit as one wear, an outfit grid.
- Automatic background removal on garment photos so the grid looks clean.
- AI tagging: suggest category, colour and material from the photo (on-device if possible).
- "Not worn in a year" reminders and suggestions of what to donate.
- Weather-based suggestions of what to wear.
- Laundry status (clean / in the wash).
- Sync between devices or a backup to the user's own Nextcloud (as in `../d-player`).
- A desktop or web view.

## Explicit non-goals (v1)

- No social features (followers, public profiles, sharing reviews).
- No shopping, affiliate links or ads.
- No account or cloud requirement.

## Open questions

### Answered 2026-11-18 (now in ARCHITECTURE.md §2, §5, §7.1)

- ~~**Platform**~~: **Android only.**
- ~~**Garment fields for the MVP**~~: only **category is required** (pants, shirt, …); **colour is optional**.
- ~~**Categories**~~: **both** a built-in list and the user's own.
- ~~**Rating**~~: **both** stars (with halves) and a like.
- ~~**Diary unit**~~: **single garments** first.
- Added by the user: **take a picture of the garment in the app**, linked to its entry.

### Still open

- **Visual identity:** final name, colours and icon.
- **Cost per wear** (M3 statistics) needs a price. Add an optional price field then, or drop that statistic?
- Should a **diary entry** be able to have its own photo too (e.g. a photo of the day's outfit), or only the
  garment?
- **Camera:** v0.1.0 opens the phone's own camera app from inside dress-up (ARCHITECTURE.md §2). Is that good
  enough, or should the viewfinder be built into the app (CameraX), e.g. with a garment-shaped guide frame?

## Idea log

Newest first. Each entry is dated.

### 2026-11-18 – Take the photo in the app

The user wants to take a picture of the garment with the camera directly in the app, and have it linked to the
garment's entry. Decided, see ARCHITECTURE.md §2 and §7.1.

### 2026-11-18 – The original idea

An app to keep track of the clothes you have, with a GUI inspired by Letterboxd.
