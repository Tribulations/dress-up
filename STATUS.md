# Status

Tracks what is being worked on and how far each piece has come.
Update this file whenever work starts, finishes or is verified.

**Legend / workflow:** an item moves through
`Next up → Currently working on → Implemented, not yet tested → Done & tested`.
An item only counts as **tested** when it has been verified through automated tests,
on a real device, or both. Record how it was tested.

_Last updated: 2026-11-18_

---

## 🚧 Currently working on

- **Project planning (2026-11-18):** initial docs written (README, AGENTS/CLAUDE, GUIDELINES, ARCHITECTURE,
  BRAINSTORMING, STATUS, CHANGELOG), modelled on the user's other projects (`../noty`, `../d-player`,
  `../svensk-statistik-app`). No code yet.
- **Main decisions made (2026-11-18)**, the user's answers to BRAINSTORMING.md "Open questions", now in
  ARCHITECTURE.md §2, §5, §7.1: Android only; category required, colour optional; built-in plus own categories;
  star rating with halves plus a like; the diary logs single garments; photos taken with the in-app camera.

## 🧪 Implemented, not yet tested

_Nothing yet._

## ✅ Done & tested

_Nothing yet._

## 📝 Next up (roadmap)

Each completed milestone gets a minor version tag (AGENTS.md). The milestones are a draft (BRAINSTORMING.md).

- **Still open** (BRAINSTORMING.md "Open questions"): the visual identity (name, colours, icon); can be decided
  during M1.
- **M1 – wardrobe:** project skeleton, in-app camera, add/edit/delete a garment (photo, category, optional
  colour), built-in and own categories, wardrobe grid with filters, garment page with rating and like.
- **M2 – diary and backup:** log wears by date, diary view, wear history per garment, export/import of the
  whole wardrobe (data + photos).
- **M3 – lists and statistics:** lists, wishlist, ratings, most/least worn, cost per wear, year in review.
- **Later:** outfits, background removal / AI tagging, sync between devices (BRAINSTORMING.md).
