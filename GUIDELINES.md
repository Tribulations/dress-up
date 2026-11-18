# Development Guidelines

The rules for how code in this project is written and organised. They apply to all code.
If a change has to break a rule, write down why in the commit message, and update this file if the rule itself
should change.

**This describes the target setup, not what exists.** There is no code in this repo yet. Commands and tooling
mentioned below are what should be true once the project skeleton is built.

The technical structure these rules produce is described in [ARCHITECTURE.md](ARCHITECTURE.md).

---

## 1. Core goals

1. **Never lose the user's wardrobe.** Every garment, photo and diary entry is saved durably before the UI says
   it's saved. Deleting asks for confirmation (or can be undone). Database migrations never drop data.
2. **Modular, with optional parts:** the core (garments, photos, diary) works offline with no account. Each
   optional extra (sync, AI tagging, anything needing a network) can be removed with one switch, and the rest
   still builds and works.
3. **Readable:** code is written for the next reader. Clarity beats cleverness.
4. **Private:** wardrobe photos are personal data and are treated that way everywhere (§4).
5. **A design of its own:** inspired by Letterboxd's concepts and feel, never a copy of its brand (§4.1).

## 2. Modularity rules

### 2.1 Modules

| Module | Examples | May depend on |
|---|---|---|
| `app` | wiring, navigation, theme | everything |
| `feature:*` | `wardrobe`, `garment`, `diary`, `lists`, `stats` | `core:*` only |
| `core:model` | plain Kotlin data types | nothing |
| `core:data` | Room database, repositories, photo storage | `core:model` |
| `core:ui` | design system: theme, photo tile, rating bar, shared components | `core:model` |
| Optional adapters | sync, AI tagging | `core:*` interfaces |

(Names will be revised once ARCHITECTURE.md §4 is decided. The shape, not the names, is what's fixed.)

### 2.2 Dependency rules (non-negotiable)

- **A feature module never depends on another feature module.** Shared code moves into a `core` module.
  Navigation between features goes through `app`.
- **The core never imports an optional adapter.** It defines interfaces; implementations are wired in `app`.
- Every dependency version is pinned in the Gradle version catalog (`gradle/libs.versions.toml`).

## 3. Readability rules

- Follow the official Kotlin coding conventions and the Compose API guidelines, with ktlint/detekt enforced once
  the build exists.
- **Names over comments.** Comment *why*, not what.
- Size guidelines: functions about 30 lines, files about 300 lines, nesting at most 3 levels; prefer early returns.
- Immutable data where possible; explicit result/error types instead of nullable flags.
- **Never swallow an error silently.** A failed save or import shows the user what went wrong.
- Screens are stateless (`FooScreen(uiState, onAction)`); ViewModels expose one `StateFlow<UiState>`.
- All user-facing text lives in string resources, never hard-coded.

## 4. Privacy & secrets

- Photos stay in app-private storage. Nothing is uploaded anywhere unless the user turns on a feature that
  needs it, and that feature says what it sends.
- Never log photo paths or the user's notes above debug level.
- Never commit real wardrobe photos or data. Test fixtures are made up for the purpose.
- Secrets live in an untracked `local.properties`. A missing key disables that adapter; it never crashes the app.

### 4.1 Letterboxd inspiration, not imitation

- Borrow **concepts and interaction patterns**: photo grid, diary, ratings, lists, a statistics screen.
- **Never** use Letterboxd's name, logo, icons, illustrations, exact colour palette or copied text, and don't
  make the app look like it is Letterboxd. Our colours, icons and wording are our own.

## 5. Testing

- Unit-test repositories and ViewModels with fakes; test Room migrations.
- Compose UI tests for the main flows (add garment, log a wear) once they exist.
- Record in STATUS.md how each item was verified, and on what (emulator / phone, Android version).

## 6. Workflow & documentation

- Docs are updated in the same commit as the change they describe (AGENTS.md "Keeping the docs up to date").
- SemVer tags per milestone while on 0.x.
- Decisions go from BRAINSTORMING.md into ARCHITECTURE.md only when the user agrees.

## 7. Git

- Small, focused commits in [Conventional Commits](https://www.conventionalcommits.org/) style:
  `feat(wardrobe): add category filter`, `fix(data): keep photos on garment edit`.
- The app must build at every commit on `main`.
