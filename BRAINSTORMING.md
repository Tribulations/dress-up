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
- **M2:** ~~diary (log wears), wear history per garment, export/import~~ (built in v0.2.0, with outfit ratings and
  occasions).
- **M3:** lists, wishlist, statistics.

## Later / maybe

- **Outfits:** ~~combine garments into an outfit, an outfit grid~~ (built in v0.1.2), ~~log the whole outfit as one
  wear~~ (built in v0.2.0, ARCHITECTURE.md §5.3).
- **A calendar view of the diary** (the user's first idea for the diary, 2026-11-19; the list came first): a month
  grid with marked days, tap a day for its wears.
- ~~Automatic background removal on garment photos so the grid looks clean.~~ Built in v0.1.2 as a button per garment
  (ARCHITECTURE.md §7.5). Maybe later: offer it right after taking a photo.
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
- ~~**Diary unit**~~: **single garments** first. (Changed 2026-11-19: an entry is an outfit and/or garments.)
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

### 2026-11-19 – Fill with the cut-out, rotate and backgrounds on outfits, sort by wear, import on first start (built in v0.2.1)

From the user:

1. After removing the background, zoom the picture in so the garment fills its frame.
2. Rotate garments on the outfit board, and give an outfit a background: an image or a colour.
3. Sort the wardrobe by most and least worn.
4. Offer the import on a new install, where the app asks for the user's name.

Agreed: the cut-out is scaled to fit (whole, almost no margin) rather than cropped to fill; backgrounds are a fixed
palette of colours or the user's own picture (a free colour picker maybe later). See ARCHITECTURE.md §5.2, §7.3, §7.6,
§7.8.

### 2026-11-19 – Outfit ratings, occasions, diary, calendar, export and import (built in v0.2.0)

From the user:

1. Rate outfits the same way as garments.
2. Asked whether your own categories can be added: yes, Settings → Categories, and **New category** in the garment
   editor (both since v0.1.0).
3. Log occasions and dates when an outfit or garment was worn, e.g. "Christmas Eve" or "Nathalie's birthday".
4. Export and import a profile with everything in it: a simple YAML file that maps to all the data (ratings, image
   paths, …), exported with the images in a folder the YAML points to. The app keeps its database.
5. The diary (M2): log a date and a note when an outfit or garment was worn; wear counts per garment and outfit; a
   calendar view. Minimalistic, intuitive UI and good UX.

Agreed with the user the same day: the diary is a **list of entries** (inspired by how Letterboxd's diary works, not a
copy of it), newest first and grouped by month. Tapping an entry opens it, the entry leads to its outfit, and the outfit
to each garment. **No calendar for now.** Outfit aspects are comfort, style, fit and "for the occasion". Export is one
profile per .zip (profile.yaml plus images/), and import always adds a new profile. Occasions are free text with
suggestions. A photo of its own per diary entry comes later. See ARCHITECTURE.md §5.2, §5.3, §7.6–7.8.

### 2026-11-19 – A tester's wishes (built in v0.1.2)

From someone trying the app (in Swedish, summarised):

1. Name garments and describe them, e.g. "Coola favvis tröjan"; the name on top, the type/category underneath.
2. More than one category per garment.
3. Your own review of a garment, and ratings of specific things about it.
4. Cut the garment out of the photo, so only the garment shows and not the background.
5. A collage/gallery of all garment pictures, and from there put an outfit together by picking garments.

The user said all of it should be built. Choices made while building, to check with the tester: the aspects are a
**fixed list** (fit, comfort, quality, style, easy care) rather than ones you make up; the main category is the **first
one picked**; background removal uses **ML Kit via Google Play services** (on the device, but its model is downloaded
by Play services once); the "gallery" is the picker inside the outfit editor, and the collage is a free board.
See ARCHITECTURE.md §5, §5.2, §7.5, §7.6.

### 2026-11-19 – Profiles and a test user

The user wants a test user to switch to in the app, with a prefilled wardrobe (clothes, pictures and so on), to see
where the app is good and where it isn't. As profiles in the settings: a new install asks for the user's name and uses
it as the profile name. Decided and built, see ARCHITECTURE.md §5.1 and §7.4. Possible later: real (licence-free)
photos in the test wardrobe instead of drawn ones. (Diary entries were added in v0.2.0.)

### 2026-11-18 – Take the photo in the app

The user wants to take a picture of the garment with the camera directly in the app, and have it linked to the
garment's entry. Decided, see ARCHITECTURE.md §2 and §7.1.

### 2026-11-18 – The original idea

An app to keep track of the clothes you have, with a GUI inspired by Letterboxd.
