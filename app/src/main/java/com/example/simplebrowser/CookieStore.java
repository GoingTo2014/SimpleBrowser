package com.example.simplebrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.os.Handler;
import android.os.Looper;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public final class CookieStore {

    private static final String PREFS =
            "cookie_sites";

    private static final String KEY =
            "domains";

    private static final String KEY_URLS =
            "urls";

    private static final String KEY_SNAPSHOT =
            "cookie_snapshot";

    private final SharedPreferences preferences;

    public CookieStore(Context context) {
        this(
                context,
                ProfileManager.getActiveProfileId(context));
    }

    public CookieStore(
            Context context,
            String profileId) {

        preferences =
                context.getSharedPreferences(
                        ProfileManager.scopedPrefsName(
                                PREFS,
                                profileId),
                        Context.MODE_PRIVATE);

        try {
            CookieSyncManager.createInstance(
                    context.getApplicationContext());
        } catch (Throwable ignored) {
        }
    }

    public void recordUrl(String url) {

        if (url == null) {
            return;
        }

        recordUrls(
                Collections.singletonList(url));
    }

    public void recordUrls(
            List<String> urls) {

        if (urls == null ||
                urls.isEmpty()) {
            return;
        }

        Set<String> domains =
                new LinkedHashSet<>(
                        preferences.getStringSet(
                                KEY,
                                Collections
                                        .<String>emptySet()));

        Set<String> savedUrls =
                new LinkedHashSet<>(
                        preferences.getStringSet(
                                KEY_URLS,
                                Collections
                                        .<String>emptySet()));

        for (String url : urls) {

            if (url == null ||
                    url.trim().isEmpty()) {
                continue;
            }

            String cleanUrl =
                    url.trim();

            String domain =
                    getDomain(cleanUrl);

            if (domain != null) {
                domains.add(domain);
            }

            savedUrls.add(cleanUrl);
        }

        while (savedUrls.size() > 1000) {
            String first =
                    savedUrls.iterator().next();
            savedUrls.remove(first);
        }

        preferences.edit()
                .putStringSet(
                        KEY,
                        domains)
                .putStringSet(
                        KEY_URLS,
                        savedUrls)
                .apply();
    }

    public List<String> getDomains() {

        Set<String> saved =
                preferences.getStringSet(
                        KEY,
                        Collections
                                .<String>emptySet());

        List<String> domains =
                new ArrayList<>(saved);

        Collections.sort(
                domains,
                String.CASE_INSENSITIVE_ORDER);

        return domains;
    }

    public void discoverFromHistory(
            BrowserHistory history) {

        if (history == null) {
            return;
        }

        List<String> urls =
                new ArrayList<>();

        int offset = 0;
        int pageSize = 250;

        while (offset < 5000) {

            List<BrowserHistory.Entry> entries =
                    history.getEntries(
                            "",
                            pageSize,
                            offset);

            if (entries == null ||
                    entries.isEmpty()) {
                break;
            }

            for (BrowserHistory.Entry entry :
                    entries) {

                urls.add(entry.url);
            }

            offset += entries.size();

            if (entries.size() < pageSize) {
                break;
            }
        }

        recordUrls(urls);
    }

    /*
     * Always read the cookie jar through HTTPS.
     *
     * A non-secure cookie is also visible to HTTPS, while a Secure cookie
     * is not visible to HTTP. Reading both schemes and concatenating the
     * results was the cause of the duplicate-looking cookies in the old UI.
     */
    public String getCookies(
            String domain) {

        if (domain == null ||
                domain.trim().isEmpty()) {
            return "";
        }

        CookieManager manager =
                CookieManager.getInstance();

        String url =
                "https://" +
                domain.trim() +
                "/";

        String cookies =
                manager.getCookie(url);

        if (cookies == null) {
            return "";
        }

        return cookies;
    }

    /*
     * Replace a visible root-path cookie without creating another
     * host/domain variant. We first expire both possible domain forms
     * and the host-only form, then create one canonical host-only cookie.
     *
     * This matters because Android WebView identifies a cookie by its
     * domain, path, and name. The old implementation wrote several
     * overlapping cookie variants for every edit.
     */
    public boolean setCookie(
            String domain,
            String name,
            String value) {

        return setCookie(
                domain,
                name,
                value,
                null,
                null);
    }

    public boolean setCookie(
            String domain,
            String name,
            String value,
            BrowserHistory history,
            List<String> openUrls) {

        if (domain == null ||
                name == null ||
                domain.trim().isEmpty() ||
                name.trim().isEmpty()) {
            return false;
        }

        String safeDomain =
                normalizeDomain(domain);

        String safeName =
                name.trim();

        if (safeDomain == null ||
                safeDomain.isEmpty()) {
            return false;
        }

        CookieManager manager =
                CookieManager.getInstance();

        List<String> scopes =
                getExistingCookieScopes(
                        safeDomain,
                        safeName);

        if (safeName.startsWith("__Host-")) {
            scopes.clear();
            scopes.add(safeDomain);
        }

        if (scopes.isEmpty()) {
            scopes.add(safeDomain);
        }

        boolean hostCookie =
                safeName.startsWith("__Host-");

        boolean secure =
                hostCookie ||
                isSecureCookie(
                        manager,
                        safeDomain,
                        safeName);

        Set<String> paths =
                getCookiePathCandidates(
                        safeDomain,
                        history,
                        openUrls);

        for (String scope : scopes) {
            for (String path : paths) {
                expireCookieVariants(
                        manager,
                        scope,
                        safeName,
                        path);
            }
        }

        String cookie =
                safeName +
                "=" +
                (value == null
                        ? ""
                        : value) +
                "; Path=/";

        if (secure ||
                safeName.startsWith("__Secure-") ||
                safeName.startsWith("__Host-")) {
            cookie += "; Secure";
        }

        /*
         * Keep a host-only cookie on the exact host. Parent-domain matches
         * are recreated as Domain cookies so editing does not accidentally
         * widen or narrow their scope.
         */
        for (String scope : scopes) {

            String scopedCookie =
                    cookie;

            if (!hostCookie &&
                    !scope.equals(
                            safeDomain)) {
                scopedCookie +=
                        "; Domain=." +
                        scope;
            }

            manager.setCookie(
                    httpsUrl(
                            safeDomain,
                            "/"),
                    scopedCookie);
        }

        syncCookies();

        boolean success =
                hasCookie(
                        safeDomain,
                        safeName,
                        value == null
                                ? ""
                                : value);

        return success;
    }

    public boolean deleteCookie(
            String domain,
            String name) {

        return deleteCookie(
                domain,
                name,
                null,
                null);
    }

    public boolean deleteCookie(
            String domain,
            String name,
            BrowserHistory history,
            List<String> openUrls) {

        if (domain == null ||
                name == null ||
                domain.trim().isEmpty() ||
                name.trim().isEmpty()) {
            return false;
        }

        String safeDomain =
                normalizeDomain(domain);

        String safeName =
                name.trim();

        if (safeDomain == null ||
                safeDomain.isEmpty()) {
            return false;
        }

        CookieManager manager =
                CookieManager.getInstance();

        List<String> scopes =
                getExistingCookieScopes(
                        safeDomain,
                        safeName);

        if (scopes.isEmpty()) {
            scopes.add(safeDomain);
        }

        Set<String> paths =
                getCookiePathCandidates(
                        safeDomain,
                        history,
                        openUrls);

        for (String scope : scopes) {

            for (String path : paths) {

                expireCookieVariants(
                        manager,
                        scope,
                        safeName,
                        path);
            }
        }

        syncCookies();

        return !hasCookieNameAtPaths(
                manager,
                scopes,
                safeName,
                paths);
    }

    /*
     * Delete cookies belonging to the site at all paths that Simple
     * Browser can actually observe from its recorded navigation URLs.
     *
     * Android 4.4's public CookieManager API exposes cookie lines, not
     * their domain/path metadata, so a path-specific cookie cannot be
     * deleted safely unless we address the path where it was observed.
     */
    public boolean deleteSiteCookies(
            String domain,
            BrowserHistory history,
            List<String> openUrls) {

        String safeDomain =
                normalizeDomain(domain);

        if (safeDomain == null ||
                safeDomain.isEmpty()) {
            return false;
        }

        LinkedHashSet<String> siteDomains =
                new LinkedHashSet<>();

        addDomainScopeParents(
                siteDomains,
                safeDomain);

        if (openUrls != null) {

            for (String url : openUrls) {

                String host =
                        getDomain(url);

                if (host == null) {
                    continue;
                }

                if (safeDomain.equals(host) ||
                        host.endsWith(
                                "." + safeDomain)) {

                    addDomainScopeParents(
                            siteDomains,
                            host);
                }
            }
        }

        if (history != null) {

            List<BrowserHistory.Entry> entries =
                    history.getEntries(
                            "",
                            1000,
                            0);

            if (entries != null) {

                for (BrowserHistory.Entry entry :
                        entries) {

                    String host =
                            getDomain(entry.url);

                    if (host == null) {
                        continue;
                    }

                    if (safeDomain.equals(host) ||
                            host.endsWith(
                                    "." + safeDomain)) {

                        addDomainScopeParents(
                                siteDomains,
                                host);
                    }
                }
            }
        }

        LinkedHashSet<String> urls =
                new LinkedHashSet<>();

        for (String siteDomain :
                siteDomains) {

            urls.add(
                    httpsUrl(
                            siteDomain,
                            "/"));
        }

        if (openUrls != null) {
            for (String url : openUrls) {
                addSiteUrl(
                        urls,
                        safeDomain,
                        url);
            }
        }

        if (history != null) {

            List<BrowserHistory.Entry> entries =
                    history.getEntries(
                            "",
                            1000,
                            0);

            if (entries != null) {

                for (BrowserHistory.Entry entry :
                        entries) {
                    addSiteUrl(
                            urls,
                            safeDomain,
                            entry.url);
                }
            }
        }

        LinkedHashSet<String> paths =
                new LinkedHashSet<>();

        for (String url : urls) {
            paths.addAll(
                    getPathCandidates(url));
        }

        CookieManager manager =
                CookieManager.getInstance();

        Set<String> names =
                new LinkedHashSet<>();

        for (String url : urls) {

            String cookies =
                    manager.getCookie(
                            toHttps(url));

            for (CookieValue cookie :
                    parseCookies(cookies)) {
                names.add(cookie.name);
            }
        }

        for (String siteDomain :
                siteDomains) {

            for (CookieValue cookie :
                    parseCookies(
                            manager.getCookie(
                                    httpsUrl(
                                            siteDomain,
                                            "/")))) {
                names.add(cookie.name);
            }
        }

        for (String name : names) {

            for (String scope :
                    siteDomains) {

                for (String path :
                        paths) {

                    expireCookieVariants(
                            manager,
                            scope,
                            name,
                            path);
                }
            }
        }

        syncCookies();

        boolean remaining = false;

        for (String siteDomain :
                siteDomains) {

            if (hasAnyCookies(
                    manager,
                    siteDomain)) {
                remaining = true;
                break;
            }
        }

        if (!remaining) {

            Set<String> domains =
                    new LinkedHashSet<>(
                            preferences.getStringSet(
                                    KEY,
                                    Collections
                                            .<String>emptySet()));

            domains.removeAll(
                    siteDomains);

            preferences.edit()
                    .putStringSet(
                            KEY,
                            domains)
                    .apply();
        }

        return !remaining;
    }

    public void removeDomain(
            String domain) {

        deleteSiteCookies(
                domain,
                null,
                null);
    }

    public void clearIndex() {

        preferences.edit()
                .remove(KEY)
                .remove(KEY_URLS)
                .remove(KEY_SNAPSHOT)
                .apply();
    }

    /** Saves this profile's view of the process-global WebView cookie jar. */
    public synchronized void snapshotCookies() {

        CookieManager manager =
                CookieManager.getInstance();

        JSONArray snapshot =
                new JSONArray();

        Set<String> urls =
                preferences.getStringSet(
                        KEY_URLS,
                        Collections
                                .<String>emptySet());

        for (String url : urls) {

            if (url == null ||
                    url.trim().isEmpty()) {
                continue;
            }

            try {
                String raw =
                        manager.getCookie(url);

                if (raw == null ||
                        raw.trim().isEmpty()) {
                    continue;
                }

                snapshot.put(
                        new JSONObject()
                                .put("url", url)
                                .put("cookies", raw));
            } catch (Throwable ignored) {
            }
        }

        preferences.edit()
                .putString(
                        KEY_SNAPSHOT,
                        snapshot.toString())
                .apply();
    }

    /** Restores this profile's cookie snapshot after clearing the process-global jar. */
    public synchronized void restoreCookies() {
        restoreCookies(null);
    }

    /**
     * Clears the process-global WebView cookie jar first, then restores only
     * this profile's saved cookie snapshot. Android 4.4 has no completion
     * callback for cookie deletion, so the fallback waits before restoring.
     */
    public synchronized void restoreCookies(
            final Runnable completion) {

        final String json =
                preferences.getString(
                        KEY_SNAPSHOT,
                        "");

        if (json == null ||
                json.trim().isEmpty()) {

            if (completion != null) {
                completion.run();
            }

            return;
        }

        clearRuntimeCookies(
                new Runnable() {
                    @Override
                    public void run() {
                        restoreSnapshot(
                                json,
                                completion);
                    }
                });
    }

    private void restoreSnapshot(
            String json,
            final Runnable completion) {

        if (json != null &&
                !json.trim().isEmpty()) {

            try {
                JSONArray snapshot =
                        new JSONArray(json);

                CookieManager manager =
                        CookieManager.getInstance();

                for (int i = 0;
                        i < snapshot.length();
                        i++) {

                    JSONObject item =
                            snapshot.getJSONObject(i);

                    String url =
                            item.optString("url", "");

                    String cookies =
                            item.optString("cookies", "");

                    if (url.isEmpty() ||
                            cookies.isEmpty()) {
                        continue;
                    }

                    for (String part :
                            cookies.split(";")) {

                        String cookie =
                                part.trim();

                        if (cookie.indexOf('=') <= 0) {
                            continue;
                        }

                        try {
                            manager.setCookie(
                                    url,
                                    cookie);
                        } catch (Throwable ignored) {
                        }
                    }
                }

                syncCookies();
            } catch (Throwable ignored) {
            }
        }

        if (completion != null) {
            new Handler(
                    Looper.getMainLooper())
                    .postDelayed(
                            completion,
                            Build.VERSION.SDK_INT < 21
                                    ? 500
                                    : 80);
        }
    }

    public static void clearRuntimeCookies() {
        clearRuntimeCookies(null);
    }

    /**
     * Clears Android's process-global WebView cookies. API 21+ exposes a
     * completion callback; Android 4.4 does not.
     */
    public static void clearRuntimeCookies(
            final Runnable completion) {

        try {

            final CookieManager manager =
                    CookieManager.getInstance();

            if (Build.VERSION.SDK_INT >= 21) {

                manager.removeAllCookies(
                        new ValueCallback<Boolean>() {
                            @Override
                            public void onReceiveValue(
                                    Boolean value) {

                                try {
                                    manager.flush();
                                } catch (Throwable ignored) {
                                }

                                if (completion != null) {
                                    completion.run();
                                }
                            }
                        });

                return;
            }

            manager.removeAllCookie();

            try {
                CookieSyncManager.getInstance()
                        .sync();
            } catch (Throwable ignored) {
            }

            if (completion != null) {
                new Handler(
                        Looper.getMainLooper())
                        .postDelayed(
                                completion,
                                500);
            }

        } catch (Throwable ignored) {

            if (completion != null) {
                new Handler(
                        Looper.getMainLooper())
                        .post(completion);
            }
        }
    }
    public List<CookieValue> parseCookies(
            String raw) {

        List<CookieValue> result =
                new ArrayList<>();

        if (raw == null ||
                raw.trim().isEmpty()) {
            return result;
        }

        Set<String> seen =
                new HashSet<>();

        String[] parts =
                raw.split(";");

        for (String part : parts) {

            String value =
                    part.trim();

            int equals =
                    value.indexOf('=');

            if (equals <= 0) {
                continue;
            }

            String name =
                    value.substring(
                            0,
                            equals)
                            .trim();

            String cookieValue =
                    value.substring(
                            equals + 1)
                            .trim();

            String key =
                    name +
                    "\u0000" +
                    cookieValue;

            if (!seen.add(key)) {
                continue;
            }

            result.add(
                    new CookieValue(
                            name,
                            cookieValue));
        }

        return result;
    }

    private void addSiteUrl(
            Set<String> urls,
            String domain,
            String url) {

        String candidateDomain =
                getDomain(url);

        if (!domain.equals(candidateDomain)) {
            return;
        }

        try {

            URI uri =
                    new URI(url);

            String path =
                    uri.getRawPath();

            if (path == null ||
                    path.isEmpty()) {
                path = "/";
            }

            urls.add(
                    "https://" +
                    domain +
                    path);

        } catch (Exception ignored) {
        }
    }

    private Set<String> getPathCandidates(
            String url) {

        LinkedHashSet<String> paths =
                new LinkedHashSet<>();

        paths.add("/");

        try {

            URI uri =
                    new URI(
                            toHttps(url));

            String path =
                    uri.getRawPath();

            if (path == null ||
                    path.isEmpty()) {
                return paths;
            }

            if (!path.startsWith("/")) {
                path =
                        "/" + path;
            }

            StringBuilder current =
                    new StringBuilder();

            String[] parts =
                    path.split(
                            "/");

            for (int i = 1;
                    i < parts.length;
                    i++) {

                if (parts[i].isEmpty()) {
                    continue;
                }

                current.append("/")
                        .append(parts[i]);

                paths.add(
                        current.toString());

                paths.add(
                        current.toString() +
                        "/");
            }

            paths.add(path);

        } catch (Exception ignored) {
        }

        return paths;
    }

    private Set<String> getCookiePathCandidates(
            String domain,
            BrowserHistory history,
            List<String> openUrls) {

        LinkedHashSet<String> paths =
                new LinkedHashSet<>();

        paths.add("/");

        if (openUrls != null) {

            for (String url : openUrls) {
                addCookiePaths(
                        paths,
                        domain,
                        url);
            }
        }

        if (history != null) {

            List<BrowserHistory.Entry> entries =
                    history.getEntries(
                            "",
                            2000,
                            0);

            if (entries != null) {

                for (BrowserHistory.Entry entry :
                        entries) {
                    addCookiePaths(
                            paths,
                            domain,
                            entry.url);
                }
            }
        }

        return paths;
    }

    private void addCookiePaths(
            Set<String> paths,
            String domain,
            String url) {

        String host =
                getDomain(url);

        if (host == null ||
                !(domain.equals(host) ||
                host.endsWith("." + domain))) {
            return;
        }

        paths.addAll(
                getPathCandidates(url));
    }

    private boolean hasCookieNameAtPaths(
            CookieManager manager,
            List<String> scopes,
            String name,
            Set<String> paths) {

        for (String scope : scopes) {

            for (String path : paths) {

                String raw =
                        manager.getCookie(
                                httpsUrl(
                                        scope,
                                        path));

                if (containsCookieName(
                        raw,
                        name)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void expireCookieVariants(
            CookieManager manager,
            String domain,
            String name,
            String path) {

        String expired =
                name +
                "=; Max-Age=0; " +
                "Expires=Thu, 01 Jan 1970 00:00:00 GMT; " +
                "Path=" +
                path;

        manager.setCookie(
                httpsUrl(
                        domain,
                        path),
                expired);

        manager.setCookie(
                httpsUrl(
                        domain,
                        path),
                expired +
                "; Secure");

        /*
         * Domain=domain and Domain=.domain are both issued intentionally.
         * They cover cookies created by different WebView/website code paths
         * while the host-only request above covers a host-only cookie.
         */
        manager.setCookie(
                httpsUrl(
                        domain,
                        path),
                expired +
                "; Domain=" +
                domain);

        manager.setCookie(
                httpsUrl(
                        domain,
                        path),
                expired +
                "; Domain=." +
                domain);

        manager.setCookie(
                httpsUrl(
                        domain,
                        path),
                expired +
                "; Domain=" +
                domain +
                "; Secure");

        manager.setCookie(
                httpsUrl(
                        domain,
                        path),
                expired +
                "; Domain=." +
                domain +
                "; Secure");
    }

    private List<String> getExistingCookieScopes(
            String domain,
            String name) {

        List<String> all =
                getCookieScopeDomains(domain);

        List<String> parentScopes =
                new ArrayList<>();

        for (int i = 1;
                i < all.size();
                i++) {

            String scope =
                    all.get(i);

            if (containsCookieName(
                    getCookies(scope),
                    name)) {
                parentScopes.add(scope);
            }
        }

        /*
         * A parent-domain cookie is also returned by getCookie() for the
         * child host. Therefore the child query cannot prove that a
         * host-only cookie exists. Prefer the matching parent scope when
         * one is observable; otherwise use the exact host.
         */
        if (!parentScopes.isEmpty()) {
            return parentScopes;
        }

        List<String> result =
                new ArrayList<>();

        if (!all.isEmpty() &&
                containsCookieName(
                        getCookies(
                                all.get(0)),
                        name)) {
            result.add(all.get(0));
        }

        return result;
    }

    private List<String> getCookieScopeDomains(
            String domain) {

        List<String> result =
                new ArrayList<>();

        String value =
                normalizeDomain(domain);

        while (value != null &&
                !value.isEmpty()) {

            result.add(value);

            int dot =
                    value.indexOf('.');

            if (dot < 0) {
                break;
            }

            String parent =
                    value.substring(
                            dot + 1);

            if (parent.indexOf('.') < 0) {
                break;
            }

            value = parent;
        }

        return result;
    }

    private void addDomainScopeParents(
            Set<String> domains,
            String domain) {

        for (String scope :
                getCookieScopeDomains(domain)) {
            domains.add(scope);
        }
    }

    private boolean hasAnyCookies(
            CookieManager manager,
            String domain) {

        String raw =
                manager.getCookie(
                        httpsUrl(
                                domain,
                                "/"));

        return raw != null &&
                !raw.trim().isEmpty();
    }

    private boolean isSecureCookie(
            CookieManager manager,
            String domain,
            String name) {

        String https =
                manager.getCookie(
                        httpsUrl(
                                domain,
                                "/"));

        String http =
                manager.getCookie(
                        "http://" +
                        domain +
                        "/");

        boolean inHttps =
                containsCookieName(
                        https,
                        name);

        boolean inHttp =
                containsCookieName(
                        http,
                        name);

        return inHttps &&
                !inHttp;
    }

    private boolean containsCookieName(
            String raw,
            String name) {

        for (CookieValue cookie :
                parseCookies(raw)) {

            if (name.equals(cookie.name)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasCookieName(
            String domain,
            String name) {

        for (CookieValue cookie :
                parseCookies(
                        getCookies(domain))) {

            if (name.equals(cookie.name)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasCookie(
            String domain,
            String name,
            String expectedValue) {

        for (CookieValue cookie :
                parseCookies(
                        getCookies(domain))) {

            if (name.equals(cookie.name) &&
                    expectedValue.equals(
                            cookie.value)) {
                return true;
            }
        }

        return false;
    }

    private String httpsUrl(
            String domain,
            String path) {

        if (path == null ||
                path.isEmpty()) {
            path = "/";
        }

        return "https://" +
                domain +
                (path.startsWith("/")
                        ? path
                        : "/" + path);
    }

    private String toHttps(
            String url) {

        if (url == null) {
            return "";
        }

        try {

            URI uri =
                    new URI(url);

            String host =
                    uri.getHost();

            if (host == null) {
                return url;
            }

            String path =
                    uri.getRawPath();

            if (path == null ||
                    path.isEmpty()) {
                path = "/";
            }

            return httpsUrl(
                    host.toLowerCase(
                            Locale.US),
                    path);

        } catch (Exception ignored) {
            return url;
        }
    }

    private String normalizeDomain(
            String domain) {

        String value =
                domain == null
                        ? ""
                        : domain.trim()
                                .toLowerCase(
                                        Locale.US);

        while (value.startsWith(".")) {
            value =
                    value.substring(1);
        }

        return value;
    }

    private String getDomain(
            String url) {

        try {

            URI uri =
                    new URI(url);

            String scheme =
                    uri.getScheme();

            String host =
                    uri.getHost();

            if (host == null ||
                    !("http".equalsIgnoreCase(scheme) ||
                      "https".equalsIgnoreCase(scheme))) {
                return null;
            }

            return host.toLowerCase(
                    Locale.US);

        } catch (Exception ignored) {
            return null;
        }
    }

    public void startSync() {

        if (Build.VERSION.SDK_INT >= 21) {
            return;
        }

        try {
            CookieSyncManager.getInstance()
                    .startSync();
        } catch (Throwable ignored) {
        }
    }

    public void stopSync() {

        if (Build.VERSION.SDK_INT >= 21) {
            return;
        }

        try {
            CookieSyncManager.getInstance()
                    .stopSync();
        } catch (Throwable ignored) {
        }
    }

    private void syncCookies() {

        CookieManager manager =
                CookieManager.getInstance();

        try {

            if (Build.VERSION.SDK_INT >= 21) {
                manager.flush();
            } else {
                CookieSyncManager.getInstance()
                        .sync();
            }

        } catch (Throwable ignored) {
        }

        try {
            manager.removeExpiredCookie();
        } catch (Throwable ignored) {
        }
    }

    public static final class CookieValue {

        public final String name;
        public final String value;

        public CookieValue(
                String name,
                String value) {
            this.name = name;
            this.value = value;
        }
    }
}
