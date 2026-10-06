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

        if (tab == null) return;

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
        tab.loading = false;
        tab.title =
                Localization.translate(
                        activity,
                        "Bookmarks");

        tab.webView.removeJavascriptInterface(
                "BookmarksPage");
        tab.webView.getSettings()
                .setJavaScriptEnabled(true);
        tab.webView.addJavascriptInterface(
                new Bridge(tab),
                "BookmarksPage");

        BrowserPage.load(
                tab.webView,
                BrowserPage.BOOKMARKS,
                createHtml(query == null ? "" : query),
                BrowserPage.BOOKMARKS);

        activity.setBrowserPageIcon(
                tab,
                BrowserIconDrawable.BOOKMARK);
        activity.updateTabTitle(tab);
        activity.updateSecurity(tab);
        activity.updateNavigationButtonsForTabs();
    }

    public void remove(BrowserTab tab) {
        if (tab != null) {
            tab.webView.removeJavascriptInterface(
                    "BookmarksPage");
            tab.bookmarksPage = false;
        }
    }

    private String createHtml(String query) {

        int accent =
                ColorUtils.parseColor(
                        activity.getBrowserSettings()
                                .getAccentColor(),
                        android.graphics.Color.WHITE);

        int content =
                ColorUtils.mix(
                        accent,
                        android.graphics.Color.WHITE,
                        0.95f);

        int card =
                ColorUtils.mix(
                        accent,
                        android.graphics.Color.WHITE,
                        0.91f);

        int text =
                ColorUtils.getReadableTextColor(card);

        int secondary =
                ColorUtils.ensureContrast(
                        android.graphics.Color.rgb(
                                95, 95, 95),
                        card,
                        4.5d);

        int button = accent;

        StringBuilder html =
                new StringBuilder();

        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta name='viewport' content='width=device-width,initial-scale=1'>");
        html.append("<style>");
        html.append("*{box-sizing:border-box}");
        html.append("html,body{margin:0;padding:0;background:");
        html.append(ColorUtils.toHex(content));
        html.append(";color:");
        html.append(ColorUtils.toHex(text));
        html.append(";font-family:sans-serif}");
        html.append("body{padding:16px;max-width:920px;margin:auto}");
        html.append("h1{margin:0 0 4px;font-size:26px}");
        html.append(".desc{color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";font-size:13px;line-height:1.45;margin-bottom:14px}");
        html.append(".search{width:100%;padding:10px;border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append(";border-radius:6px;background:");
        html.append(ColorUtils.toHex(card));
        html.append(";color:");
        html.append(ColorUtils.toHex(text));
        html.append(";margin-bottom:10px}");
        html.append(".item{display:flex;align-items:center;gap:12px;background:");
        html.append(ColorUtils.toHex(card));
        html.append(";border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append(";border-radius:9px;padding:12px;margin-top:9px}");
        html.append(".favicon{width:42px;height:42px;border-radius:8px;object-fit:contain;flex:none}");
        html.append(".fallback{width:42px;height:42px;border-radius:8px;background:");
        html.append(ColorUtils.toHex(button));
        html.append(";color:");
        html.append(ColorUtils.toHex(ColorUtils.getReadableTextColor(button)));
        html.append(";display:flex;align-items:center;justify-content:center;font-weight:bold;font-size:18px;flex:none}");
        html.append(".main{min-width:0;flex:1}.title{font-size:15px;font-weight:bold;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}");
        html.append(".url{font-size:12px;color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";overflow:hidden;text-overflow:ellipsis;white-space:nowrap;margin-top:3px}");
        html.append(".actions{display:flex;flex-wrap:wrap;gap:7px;margin-top:8px}");
        html.append("button{border:1px solid ");
        html.append(ColorUtils.toHex(button));
        html.append(";background:");
        html.append(ColorUtils.toHex(button));
        html.append(";color:");
        html.append(ColorUtils.toHex(ColorUtils.getReadableTextColor(button)));
        html.append(";border-radius:5px;padding:8px 11px;font-weight:bold}");
        html.append("@media(max-width:480px){body{padding:10px}.item{align-items:flex-start}.actions{width:100%}.actions button{flex:1 1 100px}}");
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
            html.append("<div class='item'>");
            html.append(escape(t("bookmarks.empty")));
            html.append("</div>");
        }

        for (BookmarkStore.Entry entry :
                entries) {

            html.append("<div class='item'>");

            if (entry.favicon != null &&
                    entry.favicon.length > 0) {
                html.append("<img class='favicon' src='");
                html.append(escape(
                        BookmarkStore.faviconDataUri(
                                entry.favicon)));
                html.append("'>");
            } else {
                html.append("<div class='fallback'>");
                html.append(escape(initial(entry.title)));
                html.append("</div>");
            }

            html.append("<div class='main'>");
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

            html.append("</div></div></div>");
        }

        html.append("<script>");
        html.append("function search(v){BookmarksPage.search(v)}");
        html.append("function openBookmark(id){BookmarksPage.open(id)}");
        html.append("function deleteBookmark(id){");
        html.append("if(confirm(");
        html.append(js(t("bookmarks.confirm_delete")));
        html.append(")){BookmarksPage.deleteBookmark(id)}}");
        html.append("</script></body></html>");

        return Localization.translateHtml(
                activity,
                html.toString());
    }

    private String initial(String title) {
        if (title == null ||
                title.trim().isEmpty()) {
            return "?";
        }
        return String.valueOf(
                Character.toUpperCase(
                        title.trim().charAt(0)));
    }

    private String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String attribute(String value) {
        return escape(value)
                .replace("'", "&#39;");
    }

    private String js(String value) {
        if (value == null) return "''";
        return "'" +
                value.replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\r", "\\r")
                        .replace("\n", "\\n") +
                "'";
    }

    private String t(String key) {
        return Localization.translate(
                activity,
                key);
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

            if (entry != null) {
                activity.runOnUiThread(
                        () -> activity.openUrlOrSearchForTab(
                                tab,
                                entry.url));
            }
        }

        @JavascriptInterface
        public void deleteBookmark(long id) {
            store.delete(id);
            activity.runOnUiThread(
                    () -> show(tab, ""));
        }
    }
}
