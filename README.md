<div align="center">

# 🐝 LinkHive

**An offline-first Android app to save, auto-organize, and instantly retrieve your links — with a little help from Gemini.**

Save a link in one tap, and LinkHive names it, sorts it into the right bucket, summarizes it, tags it, and makes it findable — by keyword *or* by plain-English question.

</div>

---

## Why LinkHive?

Every link-saver dies the same death: the **graveyard**. You save things "for later," and later never comes. LinkHive is built around the opposite idea — that a saved link should *pay off*. Its number-one job is **fast retrieval**, backed by three principles:

- **Zero-friction capture** — paste a URL or share it from any app; LinkHive does the organizing.
- **Intelligent organization** — automatic categories, titles, summaries, and tags, so you never have to file anything by hand.
- **Effortless retrieval** — instant offline search, plus an AI "Ask" mode that finds links from a description, not just exact words.

It works fully offline and stores everything locally on your device.

## Features

### Capture
- **Paste to save** with an instant, editable preview of the detected name and category.
- **Share to save** — share a link from your browser or any app straight into LinkHive (Android share sheet → auto-categorized in the background).
- **Optimistic saving** — the link appears immediately; enrichment happens in the background.

### Organize
- **Automatic categorization** into `Jobs · Socials · Videos · Articles · Uncategorized`, driven by an **editable rule table** (add or remove your own `domain → category` rules).
- **Custom categories** — create your own buckets with a custom emoji and color.
- **Smart titles** — clean, Title-Cased names derived from the URL (skips opaque IDs, fixes stopwords and acronyms like UI/UX/API/iOS).
- **AI enrichment (Gemini)** — a concise summary and 2–4 topical tags for every link, plus a category sanity-check.
- **Rich video cards** — real titles and thumbnails for YouTube & Vimeo via oEmbed.

### Retrieve
- **Instant offline search** across name, source, URL, category, tags, and summary.
- **AI "Ask" search** — type a description ("that offline-first PWA article I saved") and Gemini reasons over your library to surface the right links, each with a one-line explanation of *why* it matched.

### Act (anti-graveyard)
- **Read-state triage** — cycle each link through `Unread → Reading → Done`, with an **Unread-only** filter to turn your pile into a queue.
- **Jobs pipeline** — job links carry an application status (`Not Applied → Applied → Rejected`) you can tap to cycle or set directly.
- **Pin** important links to the top, and **delete** with a tap.

## How it works

```
Paste / Share a URL
        │
        ▼
  Heuristic pass (instant, offline)
  • rule-based category   • derived title   • derived source
        │  ← saved & visible immediately (optimistic)
        ▼
  Enrichment pass (background)
  • oEmbed title + thumbnail (YouTube / Vimeo)
  • Gemini: summary, tags, category check
        │
        ▼
  Stored in Room (offline) → searchable instantly
```

The heuristic layer means the app is fully usable **without** an API key; the Gemini layer adds summaries, tags, and natural-language search when a key is configured.

## Tech stack

| Layer | Technology |
|---|---|
| Language / UI | Kotlin · Jetpack Compose · Material 3 |
| Architecture | MVVM (ViewModel · Repository · StateFlow) |
| Local storage | Room (offline-first) |
| Networking | Retrofit · OkHttp · Moshi |
| Images | Coil |
| AI | Google Gemini API (structured JSON output) |
| Secrets | Secrets Gradle plugin (`.env`) |
| Testing | JUnit · Robolectric · Roborazzi (screenshot tests) |

## Data model

Each link is a Room `LinkRecord`:

| Field | Notes |
|---|---|
| `id` | UUID |
| `url` | raw URL |
| `name` | editable, auto-derived title |
| `category` | `jobs · socials · videos · articles · uncategorized` (or a custom one) |
| `source` | `@handle` for socials, otherwise the domain |
| `status` | jobs only — `applied · not_applied · rejected` |
| `readState` | `unread · reading · done` |
| `pinned` | floats the link to the top |
| `tags` | AI-suggested topical tags |
| `summary` | AI-generated 1–2 sentence summary |
| `thumbnail` | preview image (e.g. video thumbnail) |
| `searchBlob` | precomputed lowercase string for fast offline search |

## Getting started

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Clone the repo and open the folder in Android Studio (`File → Open`). Let it sync Gradle and resolve any SDK prompts.
2. Create a `.env` file in the project root and add your Gemini API key (see [`.env.example`](.env.example)):
   ```
   GEMINI_API_KEY=your_key_here
   ```
   Get a key from [Google AI Studio](https://aistudio.google.com/apikey).
3. Run the app on an emulator or a physical device (Android 7.0 / API 24+).

> **Tip:** If a release-signing config blocks a local debug run, remove the `signingConfig = signingConfigs.getByName("debugConfig")` line from `app/build.gradle.kts` and use the default debug signing.

### Running without a key
LinkHive still works without a Gemini key — you'll get rule-based categorization, derived titles, offline search, and oEmbed video titles. Summaries, AI tags, and the AI "Ask" search require a key.

## Project structure

```
app/src/main/java/com/example/
├─ data/
│  ├─ Categorizer.kt            # rule-based category + title/source derivation
│  ├─ api/ApiService.kt         # Gemini + oEmbed clients
│  ├─ database/                 # Room DB, LinkDao, CategoryRuleDao
│  ├─ model/                    # LinkRecord, CategoryRule
│  └─ repository/LinkRepository.kt   # save → enrich orchestration
├─ ui/
│  ├─ screens/MainScreen.kt     # Compose UI
│  ├─ theme/                    # colors, type, category colors
│  └─ viewmodel/LinkViewModel.kt# state, filtering, search
└─ MainActivity.kt              # entry point + share-intent handling
```

## Roadmap

- Accounts & multi-device sync
- Dead-link detection with archived snapshots
- Shareable / public collections
- Full in-app management UI for the categorizer rule table

---

<div align="center">
<sub>Built with <a href="https://ai.studio/apps/1a76b725-31a0-4593-82ea-768e3b4b32fa">Google AI Studio</a>.</sub>
</div>
