package com.example.simplebrowser;

import android.webkit.JavascriptInterface;

import java.util.List;

/**
 * Built-in bookmark manager page.
 */
public final class BookmarksPage {

    private final MainActivity activity;
    private final BookmarkStore store;

    public BookmarksPage(
            MainActivity activity,
            BookmarkStore store) {

        this.activity = activity;
        this.store = store;
    }

    public void show(
            BrowserTab tab,
            String query) {

        if (tab == null) {
            return;
        }

        tab.settingsPage = false;
        tab.defaultPage = false;
        tab.historyPage = false;
        tab.downloadsPage = false;
        tab.cookiesPage = false;
        tab.passwordsPage = false;
        tab.profilesPage = false;
        tab.errorPage = false;
        tab.bookmarksPage = true;
        tab.url = BrowserPage.BOOKMARKS;
        tab.title =
                Localization.translate(
                        activity,
                        "Bookmarks");

        String html =
                createHtml(
                        query == null
                                ? ""
                                : query);

        BrowserPage.load(
                tab.webView,
                BrowserPage.BOOKMARKS,
                html,
                BrowserPage.BOOKMARKS);

        activity.setBrowserPageIcon(
                tab,
                BrowserIconDrawable.HOME);

        activity.updateTabTitle(tab);
        activity.updateSecurity(tab);
        activity.settingsLoaded(tab);
    }

    public void remove(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        tab.bookmarksPage = false;
    }

    private String createHtml(
            String query) {

        int accent =
                ColorUtils.parseColor(
                        activity.getBrowserSettings()
                                .getAccentColor(),
                        android.graphics.Color.WHITE);

        int content =
                ColorUtils.mix(
                        accent,
                        android.graphics.Color.WHITE,
                        0.94f);

        int card =
                ColorUtils.mix(
                        accent,
                        android.graphics.Color.WHITE,
                        0.90f);

        int text =
                ColorUtils.getReadableTextColor(card);

        int secondary =
                ColorUtils.ensureContrast(
                        android.graphics.Color.rgb(
                                90, 90, 90),
                        card,
                        4.5d);

        int button =
                ColorUtils.ensureContrast(
                        ColorUtils.darken(accent, 0.20f),
                        card,
                        3.0d);

        StringBuilder html =
                new StringBuilder();

        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta name='viewport' content='width=device-width,initial-scale=1'>");
        html.append("<style>*{box-sizing:border-box}html,body{margin:0;padding:0;background:");
        html.append(ColorUtils.toHex(content));
        html.append(";color:");
        html.append(ColorUtils.toHex(text));
        html.append(";font-family:sans-serif}body{padding:16px;max-width:900px;margin:auto}");
        html.append("h1{margin:0 0 4px;font-size:26px}.desc{color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";font-size:13px;margin-bottom:14px}.search{width:100%;padding:9px;border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append(";background:");
        html.append(ColorUtils.toHex(card));
        html.append(";color:");
        html.append(ColorUtils.toHex(text));
        html.append("}.card{background:");
        html.append(ColorUtils.toHex(card));
        html.append(";border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append(";border-radius:8px;padding:12px;margin-top:10px}.title{font-weight:bold;font-size:15px}");
        html.append(".url{font-size:12px;overflow-wrap:anywhere;color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";margin-top:3px}.actions{display:flex;flex-wrap:wrap;gap:7px;margin-top:9px}");
        html.append("button{border:1px solid ");
        html.append(ColorUtils.toHex(button));
        html.append(";background:");
        html.append(ColorUtils.toHex(button));
        html.append(";color:");
        html.append(ColorUtils.toHex(ColorUtils.getReadableTextColor(button)));
        html.append(";border-radius:5px;padding:9px 12px;font-weight:bold}");
        html.append("@media(max-width:420px){body{padding:10px}.actions button{flex:1 1 120px}}");
        html.append("</style></head><body>");
        html.append("<h1>");
        html.append(escape(t("bookmarks.title")));
        html.append("</h1>");
        html.append("<div class='desc'>");
        html.append(escape(t("bookmarks.desc")));
        html.append("</div>");
        html.append("<input class='search' type='search' placeholder='");
        html.append(escape(t("bookmarks.search")));
        html.append("' value='");
        html.append(attribute(query));
        html.append("' oninput='search(this.value)'>");

        List<BookmarkStore.Entry> entries =
                store.getEntries(query);

        if (entries.isEmpty()) {
            html.append("<div class='card'>");
            html.append(escape(t("bookmarks.empty")));
            html.append("</div>");
        }

        for (BookmarkStore.Entry entry :
                entries) {

            html.append("<div class='card'>");
            html.append("<div class='title'>");
            html.append(escape(entry.title));
            html.append("</div>");
            html.append("<div class='url'>");
            html.append(escape(entry.url));
            html.append("</div>");
            html.append("<div class='actions'>");
            html.append("<button onclick='openBookmark(");
            html.append(entry.id);
            html.append(")'>");
            html.append(escape(t("bookmarks.open")));
            html.append("</button>");
            html.append("<button onclick='deleteBookmark(");
            html.append(entry.id);
            html.append(")'>");
            html.append(escape(t("common.delete")));
            html.append("</button>");
            html.append("</div></div>");
        }

        html.append("<script>");
        html.append("function search(v){Android.search(v)}");
        html.append("function openBookmark(id){Android.open(id)}");
        html.append("function deleteBookmark(id){if(confirm(");
        html.append(js(t("bookmarks.confirm_delete")));
        html.append(")){Android.deleteBookmark(id)}}");
        html.append("</script></body></html>");

        return Localization.translateHtml(
                activity,
                html.toString());
    }

    private String t(String key) {
        return Localization.translate(
                activity,
                key);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace(""", "&quot;");
    }

    private String attribute(String value) {
        return escape(value)
                .replace("'", "&#39;");
    }

    private String js(String value) {
        if (value == null) {
            return "''";
        }
        return "'" +
                value.replace("\", "\\")
                        .replace("'", "\'")
                        .replace("", "\r")
                        .replace("
", "\n") +
                "'";
    }

    public final class Bridge {

        private final BrowserTab tab;

        public Bridge(BrowserTab tab) {
            this.tab = tab;
        }

        @JavascriptInterface
        public void search(String query) {
            activity.runOnUiThread(
                    () -> show(tab, query));
        }

        @JavascriptInterface
        public void open(long id) {
            BookmarkStore.Entry entry =
                    store.get(id);

            if (entry == null) {
                return;
            }

            activity.runOnUiThread(
                    () -> activity.openUrlOrSearchForTab(
                            tab,
                            entry.url));
        }

        @JavascriptInterface
        public void deleteBookmark(long id) {
            store.delete(id);
            activity.runOnUiThread(
                    () -> show(tab, ""));
        }
    }
}
