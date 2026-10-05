# SimpleBrowser Developer Documentation

This document is for developers who want to build, study, modify, or reuse the SimpleBrowser source code.

## 1. Getting started

SimpleBrowser is an Android application whose main browser engine is the Android WebView API.

### Build requirements

Use:

- JDK 17
- Gradle 8.10.2
- Android SDK Platform 35
- An Android SDK/build environment capable of compiling an application with `minSdk 19`

The repository currently does not contain a Gradle wrapper, so a compatible Gradle installation is required.

Build a debug APK with:

```bash
gradle assembleDebug --no-daemon
```

The output is:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The same build configuration is used by the repository's GitHub Actions workflow.

## 2. Source layout

The application source is in:

```text
app/src/main/
```

The main package is:

```text
com.example.simplebrowser
```

### Main components

**MainActivity.java**  
The main activity coordinates the browser UI. It owns the URL bar, toolbar actions, settings UI, built-in pages, security UI, and connections between the other browser components.

**BrowserTab.java**  
Stores state for an individual browser tab, including its WebView and tab-specific flags/state.

**TabManager.java**  
Owns the normal and incognito tab collections, active-tab handling, tab creation/removal/reordering, tab restoration, and the tab strip.

**BrowserWebViewClient.java**  
Controls WebView navigation and page lifecycle behavior. It also recognizes internal `browser://` routes and keeps internal pages from being treated as ordinary web pages.

**BrowserChromeClient.java**  
Handles WebView Chrome callbacks such as loading progress, page titles, geolocation prompts, console messages, and new-window requests.

**BrowserPage.java**  
Acts as the central registry/router for built-in browser pages. It converts public `browser://...` routes to the internal representation used by the WebView.

**BrowserSettings.java**  
Stores persistent browser settings through Android `SharedPreferences`.

**CookieStore.java / CookiesPage.java**  
Provide the browser's cookie inspection and management functionality.

**BrowserHistory.java / HistoryPage.java**  
Store and display browsing history.

**DownloadHistory.java / DownloadsPage.java**  
Store and display download history.

**SettingsPage.java**  
Builds the settings interface and its settings sections.

**SecurityManager.java**  
Handles the security UI associated with the active page.

**Localization.java / languages/**  
Provide the translation system. `LanguagePack.java` is the base class for individual language packs.

**DefaultPage.java / ErrorPage.java / DemoPage.java**  
Render built-in browser documents.

## 3. Built-in pages

Built-in pages use public routes beginning with:

```text
browser://
```

Current routes are defined in `BrowserPage.java`:

```text
browser://default
browser://history
browser://downloads
browser://error
browser://settings
browser://cookies
browser://demo
```

### Adding a new built-in page

A typical implementation should:

1. Add a route constant to `BrowserPage.java`.
2. Add route normalization/recognition there if needed.
3. Create or reuse a dedicated page renderer class.
4. Handle the route in the existing navigation flow rather than treating it as a normal network URL.
5. Make sure the visible URL remains the public `browser://...` route.

The application deliberately keeps the WebView's private internal base URL separate from the public address shown to the user.

### The demo page

`browser://demo` is the built-in WebView/HTML demonstration page. It is useful when testing HTML controls and WebView behavior without depending on an external website.

When changing WebView behavior, test the demo page because it exercises browser-visible HTML elements and JavaScript/UI interactions.

## 4. Navigation and WebView changes

Browser navigation normally flows through `MainActivity`, `TabManager`, the active `BrowserTab`, and its WebView clients.

When changing navigation:

- Test back, forward, reload, and home.
- Test opening links in new tabs.
- Test tab switching and tab restoration.
- Test both normal and incognito browsing.
- Test `browser://` routes separately from normal HTTP/HTTPS navigation.
- Test on Android 4.4 when possible.

Android 4.4 uses an older Chromium-based WebView, so APIs and Web platform behavior can differ from current Android versions. Newer Android APIs should be protected with `Build.VERSION.SDK_INT` checks when required.

## 5. Settings

Persistent preferences are handled by `BrowserSettings`. Keep preference keys centralized there rather than scattering raw `SharedPreferences` access across unrelated classes.

Settings sections currently include the categories used by the browser menu, such as:

- General
- Websites
- Appearance
- Privacy & Security

When adding a setting, update the settings UI, persistence logic, and localization together.

## 6. Localization

User-visible text should go through:

```java
Localization.translate(context, "Text or key")
```

Language packs live in:

```text
app/src/main/java/com/example/simplebrowser/languages/
```

The English pack is the reference set used for translation lookup. Other language packs can provide translated values for the same keys.

The lookup process is designed to avoid blank UI strings:

1. Try the selected language.
2. Fall back to English.
3. Fall back to the key when neither translation exists.

When adding a new string, add its English value first and then add translations to the other supported language packs as appropriate.

## 7. Cookies and storage

Cookie handling is especially sensitive to Android WebView version differences.

`CookieStore.java` contains compatibility code for old and new Android cookie APIs. It also handles cookie scope/domain behavior and synchronization differences between Android versions.

When modifying cookie behavior:

- Test normal cookies.
- Test secure cookies.
- Test host-only and parent-domain cookies.
- Test editing and deleting cookies.
- Test multiple cookies with the same name on different paths/domains.
- Test Android 4.4 specifically when possible.

Do not assume modern Android WebView cookie behavior is identical to Android 4.4.

## 8. Browser settings and WebView compatibility

The app intentionally supports a wide Android version range while targeting a modern SDK.

Important rules:

- `minSdk 19` is the compatibility floor.
- Do not call APIs unavailable on API 19 without guarding them.
- Prefer APIs already used by the project before introducing another abstraction.
- Keep platform-specific compatibility code close to the feature it protects.
- Test both old and new WebView behavior when changing browser functionality.

## 9. Adding a language

To add a language:

1. Create a class extending `LanguagePack` under `languages/`.
2. Give it a stable language ID.
3. Add the translated string keys.
4. Register it in `Localization.java`.
5. Allow the ID in `BrowserSettings.setLanguage()`.
6. Add its human-readable language name to the localization data.
7. Test the language on the settings screen and on built-in pages.

Keep keys consistent across language packs.

## 10. UI and icons

Much of the UI is generated directly from Java code, with the primary toolbar layout in:

```text
app/src/main/res/layout/main.xml
```

The browser's custom toolbar icons are drawn by `BrowserIconDrawable.java`.

Avoid replacing or changing existing icons unless the feature being developed requires it. UI changes should also preserve touch/hover feedback and work on small screens.

## 11. Security-sensitive code

Web browsers process untrusted web content. Changes involving these areas deserve extra testing:

- URL handling
- SSL errors
- WebView settings
- JavaScript
- cookies
- file/content URIs
- geolocation
- downloads
- user-agent changes
- internal `browser://` routing

Do not bypass a security check simply to make a test page load.

## 12. GitHub Actions

The manual build workflow is:

```text
.github/workflows/build.yml
```

It uses JDK 17 and Gradle 8.10.2 and produces a debug APK artifact.

To reproduce the CI build locally, use the same JDK and Gradle versions and run:

```bash
gradle assembleDebug --no-daemon
```

## 13. Reuse and modification

The repository is intentionally licensed under the Zero-Clause BSD (0BSD) license. You may reuse and modify the source with very few conditions.

When reusing the project, keep in mind that:

- Android and Gradle are governed by their own licenses and terms.
- Any third-party material added by a downstream project may have its own license.
- The SimpleBrowser name, branding, icons, or other project-specific assets may not automatically grant trademark rights.

See the repository's [LICENSE](../LICENSE) for the exact software license text.

## 14. Practical development workflow

A good workflow for browser changes is:

1. Identify the owning component instead of putting everything into `MainActivity.java`.
2. Make the smallest change necessary.
3. Run a debug build.
4. Test the normal browser flow.
5. Test the related `browser://` page if the change touches built-in pages.
6. Test normal and incognito modes where relevant.
7. Check API 19 compatibility.
8. Verify localized UI text if user-visible strings changed.

For larger changes, keep reusable behavior in a focused class so the browser remains maintainable as features are added.
