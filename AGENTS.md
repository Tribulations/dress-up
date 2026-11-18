# AGENTS.md

Onboarding for AI coding agents (and humans) working on **dress-up** (working title): a personal app to keep
track of the clothes you own. Its UI takes inspiration from Letterboxd: a grid of photos of your clothes (the
way Letterboxd shows film posters), a dated diary of what you wore, ratings, notes, lists and yearly statistics,
all applied to your own wardrobe.

## Start here: reading order

1. **[README.md](README.md):** what the project is and its *current state*.
2. **[STATUS.md](STATUS.md):** what is being worked on right now, what is done, tested or untested, and what comes next.
3. **[GUIDELINES.md](GUIDELINES.md):** the rules every change must follow (modularity, readability, privacy, workflow).
4. **[ARCHITECTURE.md](ARCHITECTURE.md):** the decided technical design (modules, data model, flows).
5. **[BRAINSTORMING.md](BRAINSTORMING.md):** ideas and open questions. **Not decided.** Don't implement from here
   without the user agreeing.
6. **[CHANGELOG.md](CHANGELOG.md):** history of notable changes.

## Key rules (summary; details in GUIDELINES.md)

- **Never lose the user's wardrobe.** Garments, photos and the wear diary are stored durably on the device and
  can be exported and backed up. No change ever deletes user data without an explicit confirmation.
- **Offline first, private by default.** The app works fully with no network and no account. Photos of the user's
  clothes (and possibly of the user) are personal and never leave the device unless the user turns that on.
- **Inspired by Letterboxd, never a copy of it.** Borrow concepts (poster grid, diary, ratings, lists, statistics)
  and the general feel. Never use Letterboxd's name, logo, icons, artwork, exact colour palette or text.
  dress-up has its own visual identity.
- **Modular:** features are separate modules on a small core. Optional extras (cloud sync, AI tagging, anything
  needing a network) are removable adapters behind an interface.
- **MVP first.** Build the simplest thing that works and grow it step by step.
- **Readable code** over clever code. Small functions and files, explicit error types.

## Keeping the docs up to date (required)

The docs are the project's memory. After any change:

| What happened | Update |
|---|---|
| Started, implemented or verified work | `STATUS.md` (move the item between sections; say *how* it was tested) |
| A user-visible or documentation change | `CHANGELOG.md` under `[Unreleased]` |
| A design decision was made or changed | `ARCHITECTURE.md` (and remove/mark it in `BRAINSTORMING.md`) |
| A new idea, research finding or open question | `BRAINSTORMING.md` |
| A coding or workflow rule changed | `GUIDELINES.md` |
| A feature changed state | the feature table in `README.md` |

**When the user shares something informally** (an idea, a preference, a constraint), decide on your own whether
it belongs in one of these docs and which one, update it, and tell the user briefly what you changed. Ideas go to
BRAINSTORMING (its Idea log, dated, newest first) unless the user says it's decided. Keep the docs consistent with
each other: for example, an idea that contradicts a listed non-goal means updating the non-goal too.

## Working conventions

- **Agile.** The plan is a draft that grows step by step. Don't push for decisions before a feature needs them;
  note ideas and open questions, and decide together when it's time.
- **Commit and push regularly** to `origin` (GitHub: `Tribulations/dress-up`), in small, focused commits, each of
  which builds.
- Use **Conventional Commits** with scopes named after the module touched: `feat(wardrobe): …`, `feat(diary): …`,
  `fix(data): …`, `docs: …`, `build: …`. Update the docs (STATUS/CHANGELOG) in the same commit as the change
  they describe.
- **Tag** meaningful points with annotated SemVer tags (`git tag -a v0.1.0 -m "…"`, then `git push --tags`):
  - a completed milestone gets a minor bump while we're on 0.x (M1 → `v0.1.0`, M2 → `v0.2.0`, …)
  - a fix release gets a patch bump

  When tagging, move the `[Unreleased]` CHANGELOG entries under a section for the new version, and bump the
  app's `versionName`/`versionCode` (once code exists).
- **Secrets** (API keys, signing config) live in an untracked `local.properties` (git-ignored). Without a key, the
  build still works and that adapter is simply unavailable.
- Before calling something "tested", run the tests and/or verify it for real (emulator or phone), and record in
  STATUS.md *how* it was tested.

## Testing on a device

No scripts exist yet. When adding them, recreate the pattern from the user's other Android projects
(`../d-player/scripts/`) rather than inventing a new one: a seed script with a small set of **made-up** test
garments and photos, a `run-on-device.sh` that targets the emulator by default and the phone only with `--phone`,
and emulator UI helpers for agent-driven checks. Until those exist, use `adb`/Gradle directly and say so in
STATUS.md.

- **Default device:** a local emulator, booted headless if it isn't running. **Always shut it down when you're
  done** (`adb -s emulator-5554 emu kill`, then confirm no `qemu-system` process is left).
- **The user's phone** (Galaxy S20) is only used for tests **when the user says so**, even if it happens to be
  connected. **Never send input to it without checking first**: confirm this app is the resumed activity before
  each tap. Screenshots that show other apps or the user's own photos are private: delete them, never share them.
- **Never touch the user's real wardrobe data** on the phone (no clearing app data, no test imports into it)
  without asking.
- **Memory:** the machine has 16 GB and runs earlyoom. A Gradle build while the emulator runs can get the emulator
  killed. Build first, or `./gradlew --stop` before heavy builds.
- In STATUS.md, record which device was used for a test (phone or emulator, and the Android version).
