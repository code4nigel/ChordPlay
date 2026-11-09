# ChordPlay Localization & Internationalization Guide

ChordPlay supports **18 locales** across all components (Android UI, Settings, Compose Multiplatform, Homepage, and Player Deck).

This guide outlines the localization architecture, instructions for adding or renaming strings, how to keep translations 100% synchronized, and how the CI test gate prevents regressions.

---

## 1. Supported Locales

ChordPlay officially supports:
- **English** (`values` / default)
- **German (Deutsch)** (`values-de`)
- **Spanish (Español)** (`values-es`)
- **French (Français)** (`values-fr`)
- **Hebrew (עברית)** (`values-he`)
- **Hindi (हिन्दी)** (`values-hi`)
- **Indonesian (Bahasa Indonesia)** (`values-id`)
- **Italian (Italiano)** (`values-it`)
- **Japanese (日本語)** (`values-ja`)
- **Polish (Polski)** (`values-pl`)
- **Portuguese (Português)** (`values-pt`)
- **Russian (Русский)** (`values-ru`)
- **Thai (ไทย)** (`values-th`)
- **Turkish (Türkçe)** (`values-tr`)
- **Vietnamese (Tiếng Việt)** (`values-vi`)
- **Chinese Simplified (简体中文)** (`values-zh`, `values-zh-rCN`)
- **Chinese Traditional - Hong Kong (繁體中文 香港)** (`values-zh-rHK`)
- **Chinese Traditional - Taiwan (繁體中文 台灣)** (`values-zh-rTW`, `values-b+zh+Hant`)

---

## 2. Resource Architecture

Strings in ChordPlay are divided into two modules:

### A. Android Module (`app`)
- **Location:** `app/src/main/res/values-*/strings.xml`
- **Scope:** Android Views, Settings screens and sub-settings, Android Auto integration, Notification services, and native dialogs.
- **Access in Code:** `R.string.<key>` or `stringResource(R.string.<key>)`

### B. Compose Multiplatform Module (`sharedUi`)
- **Location:** `sharedUi/src/commonMain/composeResources/values-*/strings.xml`
- **Scope:** Home feeds, Player screen, Library, Speed Dial, Mood chips, and shared cross-platform UI.
- **Access in Code:** `Res.string.<key>` or `stringResource(Res.string.<key>)`

> [!IMPORTANT]
> When adding a UI string, ensure you put it in the module where the UI is rendered (`app` for Settings/Services, `sharedUi` for Home/Player/Library).

---

## 3. Workflow: Adding or Renaming Strings

Whenever you add a new feature, rename a setting, or add new UI text:

### Step 1: Add the English Base String
Add your string with its descriptive name to the base English file:
- `app/src/main/res/values/strings.xml`, or
- `sharedUi/src/commonMain/composeResources/values/strings.xml`

```xml
<string name="my_new_setting">New Setting Title</string>
<string name="my_new_setting_desc">Detailed description of what this setting does</string>
```

### Step 2: Audit Missing Keys
Run the synchronization CLI from the repository root:
```bash
python tools/sync_translations.py --check
```
The script will report which locales are missing your new key(s).

### Step 3: Export Missing Strings for Translation
Export all missing keys across all 18 languages to a JSON file:
```bash
python tools/sync_translations.py --export-missing missing.json
```
This generates a structured JSON dictionary:
```json
{
  "app": {
    "values-de": {
      "my_new_setting": "New Setting Title"
    }
  }
}
```

### Step 4: Fill In & Import Translations
Fill in the translations in `missing.json` (or use a translation assistant / LLM), and import them back into the XML files:
```bash
python tools/sync_translations.py --import-translations missing.json
```
The tool will inject the translations into all respective `values-*/strings.xml` files with proper XML escaping and indentation.

### Step 5: Verify via Test Suite
Verify that all 18 locales have 100% key coverage by running:
```bash
python tools/sync_translations.py --check
```
And run the automated unit test gate:
```bash
./gradlew :app:testProdDebugUnitTest --tests "com.music.bitchord.LocaleStringsTest"
```

---

## 4. Homepage Dynamic Shelf Localization

Remote shelves from YouTube Music (and synthesized app shelves) can arrive in any language, or change when the listener switches languages.

### Shelf Identification (`HomeShelfKey`)
- Defined in `shared/src/jvmSharedMain/kotlin/com/music/bitchord/data/settings/LibraryEnums.kt`.
- `HomeShelfKey.fromTitle(title: String)` matches shelf titles against a comprehensive multilingual dictionary covering all 18 languages.
- When creating a new shelf category, add its keywords across languages to `HomeShelfKey.fromTitle()`.

### Header & Subtitle Formatting (`HomeScreen.kt`)
- `localizeShelfTitle(title)`: Recognizes localized titles (including dynamic "Similar to <artist>" across all languages) and returns the appropriate `stringResource(Res.string...)`.
- `localizeShelfSubtitle(subtitle)`: Translates remote and synthesized subtitles into the active app language.
- `localizeCardSubtitle(subtitle)`: Translates card labels (Song, Single, Playlist, Chart) and counters (views, plays, songs).

### Language Change & Feed Refresh
- When the user changes language via `AppLanguageDialog` (or system Per-App Language settings), `MainViewModel.checkLocaleChange()` detects the locale tag difference, clears the shelf key cache, calls `Innertube.refreshSessionScope()`, and reloads Home and Explore feeds with YouTube's `hl` parameter set to the new language.

---

## 5. Local Verification & Pre-Commit Policy

To guarantee that ChordPlay never regresses without relying on costly cloud CI runners:
1. Run `python tools/sync_translations.py --check` before committing or building an APK.
2. Run the local unit test gate:
   ```bash
   ./gradlew :app:testProdDebugUnitTest --tests "com.music.bitchord.LocaleStringsTest"
   ```
   If any string is missing from any of the 18 locales, or if format arguments (like `%1$s`) do not match the base string, the test fails.
3. Because release APKs are built locally and published directly, all GitHub Actions workflows are disabled/removed to ensure zero cloud runner minutes or budget expenses.

