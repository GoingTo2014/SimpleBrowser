package com.example.simplebrowser;

import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DownloadsPage {

    private static final int PAGE_SIZE = 25;

    private final MainActivity activity;
    private final DownloadHistory history;

    public DownloadsPage(
            MainActivity activity,
            DownloadHistory history) {

        this.activity = activity;
        this.history = history;
    }

    public void show(
            BrowserTab tab,
            String query) {

        tab.settingsPage = false;
        tab.errorPage = false;
        tab.defaultPage = false;
        tab.historyPage = false;
        tab.downloadsPage = true;
        tab.loading = false;
        tab.url = "browser://downloads";
        tab.title = Localization.translate(activity, "Downloads");

        WebView webView = tab.webView;

        webView.removeJavascriptInterface(
                "DownloadsPage");

        webView.getSettings()
                .setJavaScriptEnabled(true);

        webView.addJavascriptInterface(
                new Bridge(tab),
                "DownloadsPage");

        BrowserPage.load(
                webView,
                BrowserPage.DOWNLOADS,
                createHtml(
                        query,
                        tab.isIncognito),
                null);

        activity.updateTabTitle(tab);
        activity.setUrlText(
                "browser://downloads");
        activity.updateSecurity(tab);
    }

    public void remove(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        tab.webView.removeJavascriptInterface(
                "DownloadsPage");

        tab.downloadsPage = false;
    }

    private String createHtml(
            String query,
            boolean incognito) {

        List<DownloadHistory.Entry> entries =
                incognito
                        ? Collections
                                .<DownloadHistory.Entry>
                                emptyList()
                        : history.getEntries(
                                query,
                                PAGE_SIZE,
                                0);

        String background =
                incognito
                        ? "#202124"
                        : "#F6F7F9";

        String surface =
                incognito
                        ? "#303134"
                        : "#FFFFFF";

        String text =
                incognito
                        ? "#FFFFFF"
                        : "#202124";

        String secondary =
                incognito
                        ? "#B9B9B9"
                        : "#5F6368";

        String border =
                incognito
                        ? "#4A4B4F"
                        : "#E1E4E8";

        StringBuilder html =
                new StringBuilder();

        html.append(
                "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' " +
                "content='width=device-width,initial-scale=1'>");

        html.append("<style>");

        html.append(
                "html,body{margin:0;padding:0;overflow-x:hidden;" +
                "background:" + background + ";" +
                "color:" + text + ";" +
                "font-family:sans-serif;}");

        html.append(
                ".page{max-width:920px;margin:0 auto;" +
                "padding:22px 18px 30px;}");

        html.append(
                "h1{margin:0;font-size:27px;}");

        html.append(
                ".sub{margin:5px 0 18px;color:" +
                secondary + ";font-size:13px;}");

        html.append(
                ".top{display:flex;gap:8px;" +
                "margin-bottom:18px;align-items:center;}");

        html.append(
                "input{flex:1;min-width:0;height:42px;" +
                "box-sizing:border-box;padding:0 12px;" +
                "font-size:15px;border:1px solid " +
                border + ";border-radius:9px;" +
                "background:" + surface + ";color:" + text + ";}");

        html.append(
                "button{height:42px;flex:0 0 auto;" +
                "border:0;border-radius:9px;" +
                "padding:0 13px;background:#202124;color:#FFF;}");

        html.append(
                ".entry{background:" + surface + ";" +
                "border:1px solid " + border + ";" +
                "border-radius:12px;padding:14px;" +
                "margin-bottom:10px;}");

        html.append(
                ".row{display:flex;gap:12px;" +
                "align-items:flex-start;}");

        html.append(
                ".grow{flex:1;min-width:0;width:0;}");

        html.append(
                ".name{font-size:15px;font-weight:600;" +
                "line-height:20px;word-break:break-word;}");

        html.append(
                ".name a{color:" + text + ";" +
                "text-decoration:none;}");

        html.append(
                ".url{margin-top:5px;font-size:12px;" +
                "line-height:17px;color:" + secondary + ";" +
                "white-space:normal;word-break:break-all;" +
                "overflow-wrap:break-word;max-width:100%;}");

        html.append(
                ".meta{margin-top:7px;font-size:12px;" +
                "color:" + secondary + ";}");

        html.append(
                ".delete{flex:0 0 auto;background:#B3261E;" +
                "padding:0 11px;}");

        html.append(
                ".loading{text-align:center;padding:12px;" +
                "color:" + secondary + ";display:none;}");

        html.append("</style></head><body>");
        html.append("<div class='page'>");
        html.append("<h1>Downloads</h1>");

        if (incognito) {

            html.append(
                    "<div class='sub'>" +
                    "Download history is not saved in Incognito Mode." +
                    "</div>");

        } else {

            html.append(
                    "<div class='sub'>" +
                    "Files you've downloaded from Simple Browser." +
                    "</div>");

            html.append("<div class='top'>");

            html.append(
                    "<input id='search' " +
                    "placeholder='Search downloads' " +
                    "value='" +
                    escape(query) +
                    "' onkeydown='searchKey(event)'>");

            html.append(
                    "<button onclick='searchNow()'>Search</button>");

            html.append(
                    "<button onclick='clearAll()'>Clear</button>");

            html.append("</div>");
        }

        html.append("<div id='list'>");

        for (DownloadHistory.Entry entry :
                entries) {

            html.append(
                    entryHtml(
                            entry,
                            text,
                            secondary));
        }

        if (entries.isEmpty()) {

            html.append(
                    "<div id='empty' class='loading' " +
                    "style='display:block;'>");

            html.append(
                    incognito
                            ? "Nothing is shown here while browsing privately."
                            : "No downloads found.");

            html.append("</div>");
        }

        html.append("</div>");

        if (!incognito &&
                !entries.isEmpty()) {

            html.append(
                    "<div id='loading' class='loading'>" +
                    "Loading more..." +
                    "</div>");
        }

        html.append("</div>");

        if (!incognito) {

            html.append("<script>");

            html.append(
                    "var offset=" +
                    entries.size() +
                    ",loading=false,done=false;");

            html.append(
                    "function searchNow(){" +
                    "DownloadsPage.search(" +
                    "document.getElementById('search').value);}");

            html.append(
                    "function searchKey(e){" +
                    "if(e.keyCode===13)searchNow();}");

            html.append(
                    "function clearAll(){DownloadsPage.clear();}");

            html.append(
                    "function removeEntry(id){" +
                    "DownloadsPage.remove(id);}");

            html.append(
                    "function openEntry(id){" +
                    "DownloadsPage.open(id);}");

            html.append(
                    "function esc(v){" +
                    "return String(v==null?'':v)" +
                    ".replace(/&/g,'&amp;')" +
                    ".replace(/</g,'&lt;')" +
                    ".replace(/>/g,'&gt;')" +
                    ".replace(/\"/g,'&quot;')" +
                    ".replace(/'/g,'&#39;');}");

            html.append(
                    "function appendItems(json){" +
                    "var data=JSON.parse(json);" +
                    "var list=document.getElementById('list');" +
                    "var empty=document.getElementById('empty');" +
                    "if(empty)empty.style.display='none';" +
                    "for(var i=0;i<data.length;i++){" +
                    "var e=data[i];" +
                    "var title=e.filename&&e.filename.length?e.filename:'Download';" +
                    "var s='<div class=\\'entry\\'><div class=\\'row\\'>" +
                    "<div class=\\'grow\\'>" +
                    "<div class=\\'name\\'><a href=\\'javascript:openEntry(" +
                    "'+e.id+')\\'>'+esc(title)+'</a></div>'+" +
                    "'<div class=\\'url\\'>'+esc(e.url)+'</div>'+" +
                    "'<div class=\\'meta\\'>'+esc(e.status)+' &bull; '+" +
                    "esc(e.mimeType)+' &bull; '+esc(e.timeText)+'</div>'+" +
                    "'</div><button class=\\'delete\\' onclick=\\'removeEntry(" +
                    "'+e.id+')\\'>Delete</button></div></div>';" +
                    "list.insertAdjacentHTML('beforeend',s);" +
                    "}" +
                    "offset+=data.length;" +
                    "if(data.length===0)done=true;}");

            html.append(
                    "function loadMore(){" +
                    "if(loading||done)return;" +
                    "loading=true;" +
                    "document.getElementById('loading').style.display='block';" +
                    "var json=DownloadsPage.loadMore(" +
                    "document.getElementById('search').value,offset);" +
                    "appendItems(json);" +
                    "loading=false;" +
                    "document.getElementById('loading').style.display='none';}");

            html.append(
                    "window.onscroll=function(){" +
                    "if(window.innerHeight+window.pageYOffset >= " +
                    "document.body.offsetHeight-500)loadMore();" +
                    "};");

            html.append("</script>");
        }

        html.append("</body></html>");

        return Localization.translateHtml(activity, html.toString());
    }

    private String entryHtml(
            DownloadHistory.Entry entry,
            String text,
            String secondary) {

        DateFormat format =
                DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM,
                        DateFormat.SHORT);

        DownloadManager manager =
                (DownloadManager)
                        activity.getSystemService(
                                activity.DOWNLOAD_SERVICE);

        String filename =
                entry.filename == null ||
                entry.filename.trim().isEmpty()
                        ? "Download"
                        : entry.filename;

        return
                "<div class='entry'><div class='row'>" +
                "<div class='grow'>" +
                "<div class='name'>" +
                "<a href='javascript:openEntry(" +
                entry.id +
                ")'>" +
                escape(filename) +
                "</a></div>" +
                "<div class='url'>" +
                escape(entry.url) +
                "</div>" +
                "<div class='meta'>" +
                escape(
                        getStatus(
                                manager,
                                entry.downloadId)) +
                " &bull; " +
                escape(
                        entry.mimeType == null ||
                        entry.mimeType.trim().isEmpty()
                                ? "Unknown type"
                                : entry.mimeType) +
                " &bull; " +
                escape(
                        format.format(
                                new Date(entry.time))) +
                "</div>" +
                "</div>" +
                "<button class='delete' " +
                "onclick='removeEntry(" +
                entry.id +
                ")'>Delete</button>" +
                "</div></div>";
    }

    private String getStatus(
            DownloadManager manager,
            long downloadId) {

        if (manager == null) {
            return "Status unavailable";
        }

        try {

            DownloadManager.Query query =
                    new DownloadManager.Query();

            query.setFilterById(
                    downloadId);

            android.database.Cursor cursor =
                    manager.query(query);

            try {

                if (!cursor.moveToFirst()) {
                    return "Download record unavailable";
                }

                int status =
                        cursor.getInt(
                                cursor.getColumnIndex(
                                        DownloadManager
                                                .COLUMN_STATUS));

                if (status ==
                        DownloadManager
                                .STATUS_SUCCESSFUL) {
                    return "Completed";
                }

                if (status ==
                        DownloadManager
                                .STATUS_FAILED) {
                    return "Failed";
                }

                if (status ==
                        DownloadManager
                                .STATUS_RUNNING) {
                    return "Downloading";
                }

                if (status ==
                        DownloadManager
                                .STATUS_PENDING) {
                    return "Queued";
                }

                if (status ==
                        DownloadManager
                                .STATUS_PAUSED) {
                    return "Paused";
                }

                return "Waiting";

            } finally {

                cursor.close();
            }

        } catch (Exception e) {

            return "Status unavailable";
        }
    }

    private String escape(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private class Bridge {

        private final BrowserTab tab;

        Bridge(BrowserTab tab) {
            this.tab = tab;
        }

        @JavascriptInterface
        public void search(
                String value) {

            activity.runOnUiThread(
                    () -> show(
                            tab,
                            value));
        }

        @JavascriptInterface
        public String loadMore(
                String query,
                int offset) {

            try {

                List<DownloadHistory.Entry> entries =
                        history.getEntries(
                                query,
                                PAGE_SIZE,
                                Math.max(
                                        0,
                                        offset));

                JSONArray array =
                        new JSONArray();

                DateFormat format =
                        DateFormat.getDateTimeInstance(
                                DateFormat.MEDIUM,
                                DateFormat.SHORT);

                DownloadManager manager =
                        (DownloadManager)
                                activity.getSystemService(
                                        activity.DOWNLOAD_SERVICE);

                for (DownloadHistory.Entry entry :
                        entries) {

                    JSONObject object =
                            new JSONObject();

                    object.put(
                            "id",
                            entry.id);

                    object.put(
                            "url",
                            entry.url);

                    object.put(
                            "filename",
                            entry.filename == null
                                    ? ""
                                    : entry.filename);

                    object.put(
                            "mimeType",
                            entry.mimeType == null ||
                            entry.mimeType.trim().isEmpty()
                                    ? "Unknown type"
                                    : entry.mimeType);

                    object.put(
                            "status",
                            getStatus(
                                    manager,
                                    entry.downloadId));

                    object.put(
                            "timeText",
                            format.format(
                                    new Date(
                                            entry.time)));

                    array.put(object);
                }

                return array.toString();

            } catch (Exception e) {

                return "[]";
            }
        }

        @JavascriptInterface
        public void remove(
                long id) {

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
                                "Downloads history cleared",
                                Toast.LENGTH_SHORT)
                                .show();

                        show(tab, "");
                    });
        }

        @JavascriptInterface
        public void open(
                long id) {

            activity.runOnUiThread(
                    () -> {

                        DownloadHistory.Entry entry =
                                history.get(id);

                        if (entry == null) {
                            return;
                        }

                        try {

                            DownloadManager manager =
                                    (DownloadManager)
                                            activity.getSystemService(
                                                    activity.DOWNLOAD_SERVICE);

                            Uri uri =
                                    manager == null
                                            ? null
                                            : manager
                                                    .getUriForDownloadedFile(
                                                            entry.downloadId);

                            if (uri == null) {

                                Toast.makeText(
                                        activity,
                                        "Download is not ready",
                                        Toast.LENGTH_SHORT)
                                        .show();

                                return;
                            }

                            Intent intent =
                                    new Intent(
                                            Intent.ACTION_VIEW);

                            intent.setDataAndType(
                                    uri,
                                    entry.mimeType == null ||
                                    entry.mimeType.trim().isEmpty()
                                            ? "application/octet-stream"
                                            : entry.mimeType);

                            intent.addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            activity.startActivity(intent);

                        } catch (Exception e) {

                            Toast.makeText(
                                    activity,
                                    "No app can open this download",
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        }
    }
}
