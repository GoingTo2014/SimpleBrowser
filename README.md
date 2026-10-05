# SimpleBrowser

SimpleBrowser is a lightweight Android web browser built around the Android WebView API.

It is designed to keep the application simple, self-contained, and compatible with older Android devices while still supporting modern Android SDKs.

## Features

- Web browsing with Android WebView
- Multiple tabs and tab overview
- Normal and incognito browsing modes
- Browser history and download history
- Cookie management
- Built-in settings pages
- Built-in error and demo pages
- Custom user-agent profiles and desktop mode
- Localization support
- Security information for the current page
- Support for opening HTTP/HTTPS links and local HTML documents

## Compatibility

- **Minimum Android version:** Android 4.4 (API 19)
- **Target SDK:** 35
- **Compile SDK:** 35
- The app uses Android framework APIs directly and does not declare third-party runtime libraries in the app module.

The API 19 minimum is intentional. Code that is added to the project should continue to work on Android 4.4 unless the minimum SDK is deliberately changed.

## Building from source

See [Developer Documentation](docs/DEVELOPERS.md) for the full source-code guide.

The repository does not currently include a Gradle wrapper. The project's GitHub Actions build uses:

- JDK 17
- Gradle 8.10.2
- Android SDK Platform 35

A debug APK can be built with:

```bash
gradle assembleDebug --no-daemon
```

The APK is produced at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Project structure

The Android application is under `app/src/main`.

Important source files include:

| File | Purpose |
| --- | --- |
| `MainActivity.java` | Main browser activity and UI coordinator |
| `TabManager.java` | Creates, manages, restores, and switches tabs |
| `BrowserTab.java` | Per-tab state and WebView |
| `BrowserWebViewClient.java` | Navigation, WebView page handling, and network/error hooks |
| `BrowserChromeClient.java` | WebView title, progress, JavaScript/Chrome events, and windows |
| `BrowserPage.java` | Registry/router for built-in `browser://` pages |
| `BrowserSettings.java` | Persistent browser preferences |
| `CookieStore.java` | Cookie storage/edit/delete support |
| `CookiesPage.java` | Cookie-management UI |
| `BrowserHistory.java` | Browser history storage |
| `DownloadHistory.java` | Download history storage |
| `Localization.java` | Language selection and translation fallback |
| `languages/*.java` | Individual language packs |
| `SettingsPage.java` | Settings UI |
| `SecurityManager.java` | Page security information/UI |
| `DefaultPage.java` | Built-in new-tab page |
| `ErrorPage.java` | Built-in error page |
| `DemoPage.java` | Built-in HTML/WebView feature demo |

## Built-in browser pages

SimpleBrowser provides internal pages using `browser://` routes. Current routes include:

- `browser://default`
- `browser://history`
- `browser://downloads`
- `browser://error`
- `browser://settings`
- `browser://cookies`
- `browser://demo`

When adding an internal page, register its route in `BrowserPage.java` and handle the route through the existing page/navigation system. Do not expose the WebView's private internal base URL as a user-visible browser address.

## Localization

Translations are implemented as Java language packs under:

```text
app/src/main/java/com/example/simplebrowser/languages/
```

To add or change a translation:

1. Add the same key to the language pack.
2. Keep the English language pack as the source of truth for available strings.
3. Use `Localization.translate(...)` for user-visible strings instead of hard-coding new UI text.
4. When adding a new language, register the `LanguagePack` in `Localization.java` and add its language ID to the settings logic.

Missing translations fall back to English, and missing English entries ultimately fall back to the key.

## Compatibility rules for contributors

Please preserve these project conventions when modifying the source:

- Keep Android 4.4 / API 19 compatibility.
- Prefer Android framework APIs already available to the minimum SDK.
- Avoid adding unnecessary third-party dependencies.
- Keep browser functionality separated into focused classes where possible.
- Preserve the existing icon assets and icon behavior unless an icon change is specifically required.
- Use the existing localization system for user-visible text.
- Be careful with WebView behavior: some APIs differ substantially between Android 4.4 and newer releases, so guard newer APIs with version checks where necessary.
- Do not expose private internal WebView URLs as normal browser URLs.
- Test both ordinary and incognito tabs when changing navigation, storage, cookies, or settings behavior.

## Automated builds

GitHub Actions provides a manual debug build workflow at `.github/workflows/build.yml`.

The workflow:

1. Checks out the repository.
2. Installs JDK 17.
3. Sets up Gradle 8.10.2.
4. Runs `gradle assembleDebug --no-daemon`.
5. Uploads the generated debug APK as the `SimpleBrowser-debug` artifact.

## License

SimpleBrowser is released under the **Zero-Clause BSD (0BSD)** license. See [LICENSE](../LICENSE).

The license is intentionally permissive and allows the source code to be used, copied, modified, and redistributed with minimal conditions. Third-party software, platform components, and trademarks remain subject to their respective terms.
