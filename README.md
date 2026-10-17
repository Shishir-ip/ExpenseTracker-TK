<div align="center">

# 💸 Expense Tracker TK

**A local-first expense tracker for Android — your data on your device, synced to your own cloud, with AI that actually knows your spending.**

![Platform](https://img.shields.io/badge/platform-Android_8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-26-blue)
![Release](https://img.shields.io/github/v/release/Shishir-ip/ExpenseTracker-TK?color=6366F1)

*Developed by [SHISHIR](https://github.com/Shishir-ip)*

</div>

---

## ✨ What makes it different

Most expense trackers hold your data hostage or require an account. **Expense Tracker TK** is **local-first**: every expense is written to on-device SQLite instantly, works 100% offline, and syncs to **your own Supabase project** when internet is available. No accounts, no middleman, no monthly fee — and your API keys are yours, entered once in Settings.

- 🎙️ **Voice logging** — say *"lunch 120 and bus 50 yesterday"* and it becomes separate drafts. Parsed by your own AI key (Gemini/OpenRouter) when online, with an offline keyword engine as fallback. Hands-free via a spoken app shortcut.
- 🔄 **Offline-first sync** — changes queue locally and auto-upload when the network returns (connectivity listener + 30-min background job). Schema-adaptive: it reads your tables' columns and adjusts payloads automatically.
- 🧠 **AI insights & chat** — monthly analysis, spend parsing, and a financial chatbot powered by **your** OpenRouter/Gemini key.
- 🎯 **Budget system** — category budgets, daily-limit & monthly-target tracking with an *"avg till yesterday"* status banner, recovery advice, and a **pace-vs-target burn-down chart**.
- 📊 **Deep insights** — spend predictions, 30-day intensity heatmap, weekday patterns, 6-month trend line, interactive year overview with YoY comparison, tappable charts & calendar days.
- 🤝 **Borrow & Lend** — per-person balances, partial settlements, reminder notifications.
- 🔁 **Recurring expenses** — templates that auto-log on a schedule (daily / weekly / 30 days), fully in the background.
- 🏠 **Home widgets** — three home-screen widgets (compact, XL with budget progress, one-tap quick-add) that follow your theme.
- 🎨 **5 themes × 6 accent colors** — System / Light / Dark / Material You dynamic / AMOLED black, plus a seed-color picker that re-tints the whole app.
- 🧩 **Reorderable dashboard** — drag (or arrow) every home card into your own order; layout persists.
- 🔍 **Smart search** — multi-category, amount ranges, and named saved filters.
- 📤 **Exports** — JSON backup (import/export), CSV for Excel/Sheets, and a shareable **monthly PDF report**.
- 🔐 **Optional app lock** — PIN + biometric, off by default.
- ⏪ **Undo everything** — every delete shows an Undo snackbar; deletions only finalize after it dismisses.

<div align="center">

| Home | Insights | History |
|:---:|:---:|:---:|
| sortable dashboard, quick add, burn-down | AI analysis, heatmaps, year view | sticky day headers, bulk edit |

</div>

## 📦 Install

Grab the latest signed APK from [**Releases**](../../releases) and sideload it (Settings → Security → allow installing from this source).

## 🛠️ First-run setup (3 minutes)

1. Open the app → the welcome tour appears once.
2. **Settings → Cloud · Supabase** → paste your project **URL** + **anon key** ([Supabase](https://supabase.com) → Project Settings → API).
3. Set a **Profile key** — the value of the `user_pin` column that identifies your data (default: `main`).
4. Tap **Save & test**. Done — keys are stored on-device and never needed again.
5. Optional: **Settings → AI · OpenRouter** → paste a free key from [openrouter.ai](https://openrouter.ai/keys) to unlock AI insights, chat, and smart voice parsing.

### Database (one-time)

New Supabase project? Settings → **Database setup** → **Copy full SQL** → run once in the Supabase SQL Editor. It creates `expenses`, `budgets` and `borrow_lend` with row-level security enabled.

## 🏗️ Build from source

```bash
git clone https://github.com/Shishir-ip/ExpenseTracker-TK.git
cd ExpenseTracker-TK
```

Requirements: Android Studio (or JDK 17 + Android SDK 35). Then just **Run** ▶️ — debug builds sign automatically.

**Signed release build:** create a `keystore.properties` file in the project root (gitignored):

```properties
storeFile=release.keystore
storePassword=your-password
keyAlias=your-alias
keyPassword=your-password
```

…and run:

```bash
./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

Without `keystore.properties` the release build falls back to debug signing.

## 🧱 Tech stack

| | |
|---|---|
| **UI** | Jetpack Compose · Material 3 · custom Canvas charts · edge-to-edge |
| **Language** | Kotlin 2.1 · Coroutines · StateFlow |
| **Storage** | SQLite (local-first) · DataStore-style SharedPreferences |
| **Sync** | Supabase PostgREST (plain OkHttp, no SDK) · WorkManager · connectivity callbacks |
| **AI** | OpenRouter API (any model — Gemini, Llama, GPT…) |
| **Extras** | Glance-free RemoteViews widgets · Biometric lock · SpeechRecognizer · PdfDocument reports |

```
app/src/main/java/com/expensetracker/tk/
├── data/        # SQLite, Supabase REST, sync engine, AI, parsers, backup, PDF
├── state/       # AppState — single observable source of truth
├── ui/
│   ├── components/   # cards, charts, markdown renderer, shared UI
│   ├── screens/      # Home, History, Insights, Borrow, Settings, Voice, Onboarding
│   └── theme/        # Material 3 themes, accents, typography
├── widget/      # home-screen widgets
├── notify/      # daily summaries, borrow reminders, boot receiver
└── tile/        # quick-settings tile
```

## 🔒 Privacy

- All data lives **on your device**; the cloud copy is in **your own Supabase project** under your control.
- API keys are stored only in app-private storage.
- The database-setup SQL enables **row level security** on all tables.
- No analytics, no tracking, no third-party SDKs.

> Note: the included RLS policies are intentionally open (anon-key access) to match a single-user personal setup. If you share your project, tighten them.

## 📄 License

[MIT](LICENSE) © SHISHIR

---

<div align="center">

**Developed by [SHISHIR](https://github.com/Shishir-ip)** · [github.com/Shishir-ip](https://github.com/Shishir-ip)

⭐ Star this repo if it helped you track your taka!

</div>
