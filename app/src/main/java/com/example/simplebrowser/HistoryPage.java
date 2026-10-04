package com.example.simplebrowser;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class HistoryPage {

    private final MainActivity activity;
    private final BrowserHistory history;

    public HistoryPage(
            MainActivity activity,
            BrowserHistory history) {

        this.activity = activity;
        this.history = history;
    }

    public void show(
            BrowserTab tab,
            String query) {

        tab.settingsPage = false;
        tab.errorPage = false;
        tab.loading = false;
        tab.url = "browser://history";
        tab.title = "History";

        WebView webView = tab.webView;

        webView.removeJavascriptInterface(
                "HistoryPage");

        webView.getSettings()
                .setJavaScriptEnabled(true);

        webView.addJavascriptInterface(
                new Bridge(tab),
                "HistoryPage");

        webView.loadDataWithBaseURL(
                "https://browser.local/history",
                createHtml(query),
                "text/html",
                "UTF-8",
                null);

        activity.updateTabTitle(tab);
        activity.setUrlText("browser://history");
        activity.updateSecurity(tab);
    }

    private String createHtml(
            String query) {

        List<BrowserHistory.Entry> entries =
                history.getEntries(query);

        StringBuilder html =
                new StringBuilder();

        html.append(
                "<!DOCTYPE html><html><head>");

        html.append(
                "<meta name='viewport' " +
                "content='width=device-width,initial-scale=1'>");

        html.append("<style>");
        html.append(
                "html,body{margin:0;background:#FFFFFF;" +
                "color:#202124;font-family:sans-serif;}");
        html.append(
                ".page{max-width:900px;margin:0 auto;padding:24px;}");
        html.append(
                "h1{margin:0 0 6px;font-size:28px;}");
        html.append(
                ".top{display:flex;gap:10px;margin:18px 0;}");
        html.append(
                "input{flex:1;padding:11px;font-size:15px;" +
                "border:1px solid #BDBDBD;border-radius:5px;}");
        html.append(
                "button{padding:10px 13px;border:0;" +
                "border-radius:5px;background:#202124;" +
                "color:#FFFFFF;}");
        html.append(
                ".entry{padding:13px 0;border-top:1px solid #E5E5E5;}");
        html.append(
                ".title{font-weight:bold;font-size:15px;}");
        html.append(
                ".url{color:#555;font-size:13px;" +
                "word-break:break-all;margin-top:3px;}");
        html.append(
                ".time{color:#777;font-size:12px;margin-top:4px;}");
        html.append(
                ".row{display:flex;gap:8px;align-items:flex-start;}");
        html.append(
                ".grow{flex:1;}");
        html.append(
                ".delete{background:#B3261E;}");
        html.append("</style></head><body>");

        html.append("<div class='page'>");
        html.append("<h1>History</h1>");

        html.append("<div class='top'>");
        html.append(
                "<input id='search' placeholder='Search history' " +
                "value='" +
                escape(query) +
                "' onkeydown=\"search(event)\">");
        html.append(
                "<button onclick='searchNow()'>Search</button>");
        html.append(
                "<button onclick='clearAll()'>Clear</button>");
        html.append("</div>");

        if (entries.isEmpty()) {

            html.append(
                    "<p>No history entries found.</p>");

        } else {

            DateFormat format =
                    DateFormat.getDateTimeInstance(
                            DateFormat.MEDIUM,
                            DateFormat.SHORT);

            for (BrowserHistory.Entry entry :
                    entries) {

                html.append("<div class='entry'><div class='row'>");
                html.append("<div class='grow'>");

                String title =
                        entry.title == null ||
                        entry.title.trim().isEmpty()
                                ? entry.url
                                : entry.title;

                html.append(
                        "<div class='title'>" +
                        escape(title) +
                        "</div>");

                html.append(
                        "<div class='url'>" +
                        escape(entry.url) +
                        "</div>");

                html.append(
                        "<div class='time'>" +
                        escape(
                                format.format(
                                        new Date(entry.time))) +
                        "</div>");

                html.append("</div>");

                html.append(
                        "<button class='delete' " +
                        "onclick='removeEntry(" +
                        entry.id +
                        ")'>Delete</button>");

                html.append("</div></div>");
            }
        }

        html.append("</div>");

        html.append("<script>");
        html.append(
                "function searchNow(){" +
                "HistoryPage.search(document.getElementById('search').value);}");
        html.append(
                "function search(e){" +
                "if(e.keyCode===13)searchNow();}");
        html.append(
                "function clearAll(){HistoryPage.clear();}");
        html.append(
                "function removeEntry(id){HistoryPage.remove(id);}");
        html.append("</script>");

        html.append("</body></html>");

        return html.toString();
    }

    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace(""", "&quot;")
                .replace("'", "&#39;");
    }

    private class Bridge {

        private final BrowserTab tab;

        Bridge(BrowserTab tab) {
            this.tab = tab;
        }

        @JavascriptInterface
        public void search(String value) {

            activity.runOnUiThread(
                    () -> show(
                            tab,
                            value));
        }

        @JavascriptInterface
        public void remove(long id) {

            activity.runOnUiThread(
                    () -> {

                        history.delete(id);
                        show(tab, "");
                    });
        }

        @JavascriptInterface
        public void clear() {

            activity.runOnUiThread(
                    () -> {

                        history.clear();

                        Toast.makeText(
                                activity,
                                "History cleared",
                                Toast.LENGTH_SHORT)
                                .show();

                        show(tab, "");
                    });
        }
    }
}
