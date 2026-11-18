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

Draft, to be confirmed by the user:

- **M1:** add, edit and delete garments with a photo; wardrobe grid with filter (category, colour) and sort;
  garment page.
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

- **Platform:** Android only (Kotlin/Compose, like the user's other apps), or something else?
- **Garment fields for the MVP:** which are needed from day one (price? size? brand? material?)
- **Categories:** a fixed list (tops, bottoms, outerwear, shoes, accessories, …), user-defined, or both?
- **Rating:** stars (with halves, like Letterboxd), a simple like, or both?
- **Diary unit:** log single garments first, or outfits from the start?
- **Visual identity:** final name, colours and icon.

## Idea log

Newest first. Each entry is dated.

### 2026-11-18 – The original idea

An app to keep track of the clothes you have, with a GUI inspired by Letterboxd.
