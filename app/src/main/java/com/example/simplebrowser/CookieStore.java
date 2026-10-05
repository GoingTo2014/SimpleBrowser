package com.example.simplebrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CookieStore {

    private static final String PREFS =
            "cookie_sites";

    private static final String KEY =
            "domains";

    private final SharedPreferences preferences;

    public CookieStore(Context context) {
        preferences =
                context.getSharedPreferences(
                        PREFS,
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

        for (String url : urls) {

            String domain =
                    getDomain(url);

            if (domain != null) {
                domains.add(domain);
            }
        }

        preferences.edit()
                .putStringSet(
                        KEY,
                        domains)
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

    public String getCookies(
            String domain) {

        CookieManager manager =
                CookieManager.getInstance();

        String https =
                manager.getCookie(
                        "https://" + domain + "/");

        String http =
                manager.getCookie(
                        "http://" + domain + "/");

        if (https == null ||
                https.trim().isEmpty()) {
            return http == null ? "" : http;
        }

        if (http == null ||
                http.trim().isEmpty() ||
                https.equals(http)) {
            return https;
        }

        return https + "; " + http;
    }

    public void setCookie(
            String domain,
            String name,
            String value) {

        if (domain == null ||
                name == null) {
            return;
        }

        String cookie =
                name.trim() +
                "=" +
                (value == null ? "" : value) +
                "; Path=/";

        CookieManager manager =
                CookieManager.getInstance();

        manager.setCookie(
                "https://" + domain + "/",
                cookie);

        manager.setCookie(
                "http://" + domain + "/",
                cookie);

        sync();
    }

    public void deleteCookie(
            String domain,
            String name) {

        if (domain == null ||
                name == null) {
            return;
        }

        String expired =
                name.trim() +
                "=; Max-Age=0; " +
                "Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/";

        CookieManager manager =
                CookieManager.getInstance();

        manager.setCookie(
                "https://" + domain + "/",
                expired);

        manager.setCookie(
                "http://" + domain + "/",
                expired);

        sync();
    }

    public void removeDomain(
            String domain) {

        if (domain == null) {
            return;
        }

        List<CookieValue> cookies =
                parseCookies(
                        getCookies(domain));

        for (CookieValue cookie : cookies) {
            deleteCookie(
                    domain,
                    cookie.name);
        }

        Set<String> domains =
                new LinkedHashSet<>(
                        preferences.getStringSet(
                                KEY,
                                Collections
                                        .<String>emptySet()));

        domains.remove(domain);

        preferences.edit()
                .putStringSet(
                        KEY,
                        domains)
                .apply();
    }

    public void clearIndex() {

        preferences.edit()
                .remove(KEY)
                .apply();
    }

    public List<CookieValue> parseCookies(
            String raw) {

        List<CookieValue> result =
                new ArrayList<>();

        if (raw == null ||
                raw.trim().isEmpty()) {
            return result;
        }

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

            result.add(
                    new CookieValue(
                            name,
                            cookieValue));
        }

        return result;
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

            return host.toLowerCase();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void sync() {

        try {
            CookieSyncManager.getInstance()
                    .sync();
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
