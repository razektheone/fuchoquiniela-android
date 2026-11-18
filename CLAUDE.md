# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Fuchoquiniela for Android: the Android client for the fuchoquiniela.com football prediction pool ("quiniela") service, a port of the iOS app in `~/dev/personal/fuchoquiniela` (UIKit). The web app and the backend are the Laravel project in `~/dev/personal/fuchito`. All three must stay in sync: same users, pools, picks, scoring, screens and UI strings. Identifiers and strings mix Spanish and English (`Partido` = match, `Pronóstico` = pick, `Marcador` = score, `Local`/`Visita` = home/away).

Kotlin, Jetpack Compose (Material 3), single `:app` module, package and application id `razek.fuchoquiniela`, minSdk 26, target/compile SDK 35, JDK 17. Dependencies are in `gradle/libs.versions.toml`: OkHttp, kotlinx.serialization, Coil, Navigation Compose, AndroidX Browser (Custom Tabs).

## Build & check

The machine's toolchain was installed with Homebrew (`openjdk@17`, `android-commandlinetools`, SDK in `~/Library/Android/sdk`, written to the git-ignored `local.properties`):

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # JVM tests in app/src/test (models, dates, simulator)
```

## Backend API

The app uses the same JSON API as iOS, `/api/v1` (fuchito `routes/api.php`, `app/Http/Controllers/Api/V1`), with Sanctum bearer tokens that carry the `mobile` ability. Every endpoint except `auth/login` needs a token. See the iOS CLAUDE.md for the endpoint table; change the contract on all sides together.

- The base URL is `BuildConfig.API_BASE_URL` (`app/build.gradle.kts`), production by default. For the local fuchito Docker setup on the emulator use `http://10.0.2.2:50800/api/v1`; `res/xml/network_security_config.xml` allows cleartext only for `10.0.2.2` and `localhost`.
- The server owns the rules: whether a match is locked (kickoff in UTC) and scoring (3 points for an exact score, 1 for the right result). The app only reflects them.

## Localization

- Spanish is the default (`res/values/strings.xml`, `resources.properties` sets `unqualifiedResLocale=es`) and English the translation (`res/values-en/strings.xml`). Add every new string to both, with the same wording as the iOS `Localizable.xcstrings`.
- `generateLocaleConfig` lists both languages in Android 13+'s per-app language setting; Más › Idioma opens it.
- `AppLocale` (`data/Formatters.kt`) is the app's language, sent as `Accept-Language`; `DateText` formats dates in it and the device's time zone.
- Server text is not translated: API error messages, round and tournament names, and news are Spanish.

## Architecture

- `data/`
  - `FuchoApi` is a singleton with `suspend` functions; failures throw `ApiError` (`Network`, `Unauthorized`, `Server`, `Decoding`), and `ApiError.describe(context)` gives the user-facing text.
  - JSON is snake_case on the wire, mapped with `JsonNamingStrategy.SnakeCase`, so the `@Serializable` classes in `ApiModels.kt` use camelCase names. Dates stay `String` and are read with `parseDate` (ISO 8601 with or without fraction/offset, or MySQL datetimes in UTC). `Pool.price` accepts a number or a numeric string.
  - A 401 on an authenticated call runs `Session.expire()`, which clears the session and emits `Session.expired`; the root then shows the login screen with a message, so screens don't handle 401s themselves.
  - `Session` keeps the token in SharedPreferences, encrypted with an AES key in the Android Keystore, and the current `User` in a `StateFlow`.
  - `PoolStore` caches pools by uuid so pool screens can be navigated to by route.
  - `LeagueSimulator`/`SimulatorStore` recompute the Liga MX table on the device with the web simulator's rules (3/1/0; points, goal difference, goals scored, name). Scores are saved per season in SharedPreferences and never sent.
- `ui/AppRoot.kt` owns navigation: `LoginScreen` when signed out, otherwise a single `NavHost` whose tab routes (Quinielas, Partidos, Tabla, Noticias, Más) show the bottom bar, while pool, news, simulator and help routes are pushed full screen.
- The look matches the website's dark theme (fuchito `public/assets/css/custom.css`) and the iOS app: `ui/theme/Theme.kt` has the colors (page `#1e2024`, cards `#292c31`, accent `#38a9ff`, lime `#c2ff1f`, live `#ff4757`). The app is dark-only.
- `ui/components/` has the shared pieces: `FuchoCard`, `Badge`, `Avatar`, `SectionHeader`, `EmptyState`, buttons, `SegmentedTabs`/`FilterBar`, `GroupedSection`/`ListRow` (iOS inset-grouped lists), `AlertState`/`AlertHost`, `openWeb`/`openSite` (Custom Tabs), `shareInvite`, `ScoreStepper`/`TeamScoreRow` and the league table rows.
- `ui/Loader.kt` (`rememberLoader`) loads one response per screen with loading/error state. The activity handles configuration changes itself, so screen state lives in `remember`.
- `PoolMatchesScreen` has the pick logic in `PoolPicksState`:
  - Matches are grouped by round with sticky headers, and "Por jugar" hides locked matches. The filter bar shows how many open matches still lack a pick.
  - A +/- tap starts from 0-0 when there's no pick yet. Changes are debounced (0.8 s) and sent as one `PUT .../picks` batch on `FuchoApp.scope`, so the save finishes after leaving the screen. Pending picks are flushed on `ON_STOP` and when the screen closes.
  - Rejected picks revert to the last value the server confirmed. Failed saves offer Reintentar or Descartar.
- Invite links (`https://www.fuchoquiniela.com/pool/{uuid}/join`) open the app through an intent filter (no `autoVerify`, so Android may ask which app to use) and prefill the join dialog.
