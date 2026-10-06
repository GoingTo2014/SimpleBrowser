package com.example.simplebrowser;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.webkit.ConsoleMessage;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Main navigation/error client. Built-in pages are identified through
 * BrowserPage so their private WebView base URL never reaches the UI.
 */
public class BrowserWebViewClient
        extends WebViewClient {

    private final MainActivity activity;
    private final BrowserTab tab;

    public BrowserWebViewClient(
            MainActivity activity,
            BrowserTab tab) {

        this.activity = activity;
        this.tab = tab;
    }

    @Override
    public boolean shouldOverrideUrlLoading(
            WebView view,
            String url) {

        if (activity.handleProfileActionUrl(
                tab,
                url)) {
            return true;
        }

        if (url != null &&
                url.startsWith(
                        "simplebrowser://password-site/")) {

            String encoded =
                    url.substring(
                            "simplebrowser://password-site/"
                                    .length());

            activity.showPasswordSite(
                    tab,
                    android.net.Uri.decode(
                            encoded));

            return true;
        }

        String settingsSection =
                BrowserPage.getSettingsSection(url);

        if (settingsSection != null) {

            activity.showSettingsSection(
                    tab,
                    settingsSection);

            return true;
        }

        if (BrowserPage.DEMO.equals(
                BrowserPage.toPublicRoute(url))) {
            activity.showDemoPage(tab);
            return true;
        }

        if (BrowserPage.isInternalUrl(url)) {
            return true;
        }

        if (tab.settingsPage) {
            activity.removeSettingsBridge(tab);
        }

        BrowserLogger.log(
                "NAV",
                "shouldOverrideUrlLoading: " +
                url);

        return false;
    }

    @Override
    public void doUpdateVisitedHistory(
            WebView view,
            String url,
            boolean isReload) {

        if (url == null ||
                BrowserPage.isInternalUrl(url)) {
            return;
        }

        tab.url = url;

        if (tab == activity.getActiveTab()) {
            activity.setUrlText(url);
            activity.updateSecurity(tab);
            activity.updateNavigationButtonsForTabs();
        }

        BrowserLogger.log(
                "HISTORY",
                (isReload ? "reload: " : "url: ") +
                url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(
            WebView view,
            String url) {

        if (url != null &&
                !BrowserPage.isInternalUrl(url)) {
            BrowserLogger.log(
                    "NETWORK",
                    "GET " + url);
        }

        return null;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(
            WebView view,
            WebResourceRequest request) {

        if (Build.VERSION.SDK_INT >= 21 &&
                request != null &&
                request.getUrl() != null) {

            String url =
                    request.getUrl().toString();

            if (!BrowserPage.isInternalUrl(url)) {
                BrowserLogger.log(
                        "NETWORK",
                        request.getMethod() +
                        " " +
                        url);
            }
        }

        return null;
    }

    @Override
    public void onPageStarted(
            WebView view,
            String url,
            Bitmap favicon) {

        String route =
                BrowserPage.toPublicRoute(url);

        String settingsSection =
                BrowserPage.getSettingsSection(url);

        if (settingsSection != null) {

            activity.setBrowserPageIcon(
                    tab,
                    BrowserIconDrawable.SETTINGS_PAGE);

            if (!tab.settingsPage) {
                activity.restoreSettingsPage(
                        tab,
                        settingsSection);
            } else {
                tab.settingsSection =
                        settingsSection;
                tab.url =
                        activity.getSettingsUrl(
                                settingsSection);
            }

            tab.loading = false;

        } else if (BrowserPage.DEFAULT.equals(route)) {

            tab.settingsPage = false;
            tab.errorPage = false;
            tab.historyPage = false;
            tab.downloadsPage = false;
            tab.defaultPage = true;
            tab.loading = false;
            tab.url = "";
            tab.title =
                    Localization.translate(
                            activity,
                            "New Tab");
            activity.setBrowserPageIcon(
                    tab,
                    BrowserIconDrawable.HOME);

        } else if (BrowserPage.HISTORY.equals(route)) {

            tab.settingsPage = false;
            tab.errorPage = false;
            tab.defaultPage = false;
            tab.downloadsPage = false;
            tab.historyPage = true;
            tab.loading = false;
            tab.url = BrowserPage.HISTORY;
            tab.title =
                    Localization.translate(
                            activity,
                            "History");
            activity.setBrowserPageIcon(
                    tab,
                    BrowserIconDrawable.TABS);

        } else if (BrowserPage.DOWNLOADS.equals(route)) {

            tab.settingsPage = false;
            tab.errorPage = false;
            tab.defaultPage = false;
            tab.historyPage = false;
            tab.cookiesPage = false;
            tab.bookmarksPage = false;
            tab.passwordsPage = false;
            tab.profilesPage = false;
            tab.downloadsPage = true;
            tab.loading = false;
            tab.url = BrowserPage.DOWNLOADS;
            tab.title =
                    Localization.translate(
                            activity,
                            "Downloads");
            activity.setBrowserPageIcon(
                    tab,
                    BrowserIconDrawable.FILE);

        } else if (BrowserPage.COOKIES.equals(route)) {

            tab.settingsPage = false;
            tab.errorPage = false;
            tab.defaultPage = false;
            tab.historyPage = false;
            tab.downloadsPage = false;
            tab.bookmarksPage = false;
            tab.passwordsPage = false;
            tab.profilesPage = false;
            tab.cookiesPage = true;
            tab.loading = false;
            tab.url = BrowserPage.COOKIES;
            tab.title =
                    Localization.translate(
                            activity,
                            "Cookies");
            activity.setBrowserPageIcon(
                    tab,
                    BrowserIconDrawable.SECURE);

        } else if (BrowserPage.BOOKMARKS.equals(route)) {

            tab.settingsPage = false;
            tab.defaultPage = false;
            tab.historyPage = false;
            tab.downloadsPage = false;
            tab.cookiesPage = false;
            tab.passwordsPage = false;
            tab.profilesPage = false;
            tab.bookmarksPage = true;
            tab.errorPage = false;
            tab.loading = false;
            tab.url = BrowserPage.BOOKMARKS;
            tab.title =
                    Localization.translate(
                            activity,
                            "Bookmarks");
            activity.setBrowserPageIcon(
                    tab,
                    BrowserIconDrawable.HOME);

        } else if (BrowserPage.PASSWORDS.equals(route)) {

            tab.settingsPage = false;
            tab.defaultPage = false;
            tab.historyPage = false;
            tab.downloadsPage = false;
            tab.cookiesPage = false;
            tab.bookmarksPage = false;
            tab.profilesPage = false;
            tab.passwordsPage = true;
            tab.errorPage = false;
            tab.loading = false;
            tab.url = BrowserPage.PASSWORDS;
            tab.title =
                    Localization.translate(
                            activity,
                            "Password manager");
            activity.setBrowserPageIcon(
                    tab,
                    BrowserIconDrawable.SECURE);

        } else if (BrowserPage.PROFILES.equals(route)) {

            tab.settingsPage = false;
            tab.defaultPage = false;
            tab.historyPage = false;
            tab.downloadsPage = false;
            tab.cookiesPage = false;
            tab.bookmarksPage = false;
            tab.passwordsPage = false;
            tab.profilesPage = true;
            tab.errorPage = false;
            tab.loading = false;
            tab.url = BrowserPage.PROFILES;
            tab.title =
                    Localization.translate(
                            activity,
                            "Profiles");
            activity.setBrowserPageIcon(
                    tab,
                    BrowserIconDrawable.HOME);

        } else if (BrowserPage.DEMO.equals(route)) {

            tab.settingsPage = false;
            tab.errorPage = false;
            tab.defaultPage = false;
            tab.historyPage = false;
            tab.downloadsPage = false;
            tab.cookiesPage = false;
            tab.loading = false;
            tab.url = BrowserPage.DEMO;
            tab.title =
                    Localization.translate(
                            activity,
                            "HTML Demo");
            activity.setBrowserPageIcon(
                    tab,
                    BrowserIconDrawable.LOCAL_FILE);

        } else if (BrowserPage.ERROR.equals(route)) {

            /*
             * The custom error document reloads from its own
             * internal route. Preserve the original failing URL
             * in tab.url so the address bar remains useful.
             */
            tab.settingsPage = false;
            tab.defaultPage = false;
            tab.historyPage = false;
            tab.downloadsPage = false;
            tab.errorPage = true;
            tab.loading = false;
            tab.title =
                    Localization.translate(
                            activity,
                            "Page unavailable");
            activity.setBrowserPageIcon(
                    tab,
                    BrowserIconDrawable.DANGER);

        } else {

            String nextUrl =
                    url == null
                            ? ""
                            : url;

            boolean urlChanged =
                    tab.url != null &&
                    !tab.url.equals(nextUrl);

            if (urlChanged) {
                tab.favicon = null;
                activity.updateTabIcon(tab, null);
            }

            if (tab.settingsPage ||
                    tab.defaultPage ||
                    tab.historyPage ||
                    tab.downloadsPage ||
                    tab.cookiesPage ||
                    tab.bookmarksPage ||
                    tab.passwordsPage ||
                    tab.profilesPage ||
                    tab.errorPage) {

                activity.removeInternalPageState(tab);
            }

            tab.url = nextUrl;
            tab.loading = true;
            tab.sslError = false;
        }

        if (url != null &&
                !BrowserPage.isInternalUrl(url) &&
                !tab.errorPage &&
                !tab.isIncognito &&
                !tab.isGuest) {

            activity.restoreWebStorage(
                    tab,
                    url);
        }

        activity.pageStarted(
                tab,
                tab.errorPage
                        ? tab.url
                        : route != null
                        ? route
                        : url);
    }

    @Override
    public void onPageFinished(
            WebView view,
            String url) {

        if (!BrowserPage.isInternalUrl(url) &&
                !tab.errorPage) {

            if (activity.getBrowserSettings()
                    .isJavaScriptEnabled()) {

                injectPasswordSubmitWatcher(view);

                activity.restoreWebStorage(
                        tab,
                        url,
                        () -> injectWebStorageWatcher(view));
            }

            tab.url = url;
            tab.loading = false;

            if (!tab.isIncognito &&
                    !tab.isGuest) {
                activity.recordVisit(
                        tab,
                        url);
            }

            if (tab.favicon != null &&
                    !tab.favicon.isRecycled() &&
                    activity.getBrowserHistory() != null) {
                activity.getBrowserHistory()
                        .updateLatestFavicon(
                                url,
                                tab.title,
                                tab.favicon);
            }

            if (!tab.isIncognito &&
                    !tab.isGuest &&
                    activity.getCookieStore() != null) {
                activity.getCookieStore()
                        .recordUrl(url);
            }

            if (!tab.isIncognito &&
                    !tab.isGuest &&
                    activity.getTabManager() != null) {
                activity.getTabManager()
                        .savePreview(tab);
            }
        }

        activity.pageFinished(
                tab,
                tab.settingsPage
                        ? activity.getSettingsUrl(
                                tab.settingsSection)
                        : tab.errorPage
                        ? tab.url
                        : BrowserPage.toPublicRoute(url) != null
                        ? BrowserPage.toPublicRoute(url)
                        : url);
    }

    private void injectPasswordSubmitWatcher(
            WebView view) {

        String script =
                "(function(){" +
                "try{" +
                "if(window.__simpleBrowserPasswordWatcher)return;" +
                "window.__simpleBrowserPasswordWatcher=true;" +

                "function capture(form){" +
                "try{" +
                "var root=form||document;" +
                "var p=root.querySelector?root.querySelector('input[type=password]'):null;" +
                "if(!p||!p.value)return;" +
                "var u=root.querySelector?root.querySelector('input[name=username],input[name=user],input[type=email],input[autocomplete=username]'):null;" +
                "PasswordCapture.submitted(location.href,u?u.value:'',p.value);" +
                "}catch(e){}" +
                "}" +

                "document.addEventListener('submit',function(event){" +
                "capture(event.target);" +
                "},true);" +

                "try{" +
                "var originalSubmit=HTMLFormElement.prototype.submit;" +
                "HTMLFormElement.prototype.submit=function(){" +
                "capture(this);" +
                "return originalSubmit.apply(this,arguments);" +
                "};" +
                "}catch(e){}" +

                "document.addEventListener('touchend',function(event){" +
                "try{" +
                "var target=event.target;" +
                "var button=null;" +
                "for(var n=0;target&&n<8;n++,target=target.parentNode){" +
                "var tag=String(target.tagName||'').toLowerCase();" +
                "var type=String(target.type||'').toLowerCase();" +
                "var role=target.getAttribute?String(target.getAttribute('role')||'').toLowerCase():'';" +
                "if(tag==='button'||role==='button'||(tag==='input'&&(type==='submit'||type==='button'))){button=target;break;}" +
                "}" +
                "if(button)capture(button.form||null);" +
                "}catch(e){}" +
                "},true);" +

                "document.addEventListener('click',function(event){" +
                "try{" +
                "var target=event.target;" +
                "var button=null;" +
                "for(var n=0;target&&n<8;n++,target=target.parentNode){" +
                "var tag=String(target.tagName||'').toLowerCase();" +
                "var type=String(target.type||'').toLowerCase();" +
                "var role=target.getAttribute?String(target.getAttribute('role')||'').toLowerCase():'';" +
                "if(tag==='button'||role==='button'||(tag==='input'&&(type==='submit'||type==='button'))){button=target;break;}" +
                "}" +
                "if(!button)return;" +
                "capture(button.form||document);" +
                "}catch(e){}" +
                "},true);" +

                "document.addEventListener('keydown',function(event){" +
                "try{" +
                "if(event.keyCode!==13)return;" +
                "var target=event.target;" +
                "capture(target&&target.form?target.form:null);" +
                "}catch(e){}" +
                "},true);" +

                "}catch(e){}" +
                "})();";

        try {
            view.evaluateJavascript(
                    script,
                    null);
        } catch (Throwable ignored) {
            try {
                view.loadUrl(
                        "javascript:" +
                        script);
            } catch (Throwable ignoredAgain) {
            }
        }
    }

    private void injectWebStorageWatcher(
            WebView view) {

        String script =
                "(function(){" +
                "try{" +
                "if(window.__simpleBrowserStorageWatcher)return;" +
                "window.__simpleBrowserStorageWatcher=true;" +
                "function capture(){" +
                "try{" +
                "var o={};" +
                "for(var i=0;i<localStorage.length;i++){" +
                "var k=localStorage.key(i);" +
                "o[k]=localStorage.getItem(k);" +
                "}" +
                "var origin=location.protocol+'//'+location.host;" +
                "StorageCapture.save(origin,JSON.stringify(o));" +
                "}catch(e){}" +
                "}" +
                "capture();" +
                "setInterval(capture,2000);" +
                "}catch(e){}" +
                "})();";

        try {
            view.evaluateJavascript(
                    script,
                    null);
        } catch (Throwable ignored) {
            try {
                view.loadUrl(
                        "javascript:" +
                        script);
            } catch (Throwable ignoredAgain) {
            }
        }
    }

    @Override
    public void onReceivedError(
            WebView view,
            int errorCode,
            String description,
            String failingUrl) {

        if (Build.VERSION.SDK_INT >= 23) {
            return;
        }

        handleMainFrameError(
                view,
                errorCode,
                description,
                failingUrl);
    }

    @Override
    public void onReceivedError(
            WebView view,
            WebResourceRequest request,
            WebResourceError error) {

        if (Build.VERSION.SDK_INT < 23 ||
                request == null ||
                !request.isForMainFrame()) {
            return;
        }

        String url =
                request.getUrl() == null
                        ? ""
                        : request.getUrl().toString();

        String description =
                error == null
                        ? ""
                        : error.getDescription() == null
                        ? ""
                        : error.getDescription().toString();

        int code =
                error == null
                        ? 0
                        : error.getErrorCode();

        handleMainFrameError(
                view,
                code,
                description,
                url);
    }

    private void handleMainFrameError(
            WebView view,
            int errorCode,
            String description,
            String failingUrl) {

        if (tab.errorPage ||
                failingUrl == null ||
                failingUrl.trim().isEmpty() ||
                BrowserPage.isInternalUrl(failingUrl)) {
            return;
        }

        String currentUrl =
                view == null
                        ? null
                        : view.getUrl();

        if (currentUrl == null ||
                currentUrl.trim().isEmpty()) {
            currentUrl = tab.url;
        }

        boolean matchesCurrent =
                failingUrl.equals(currentUrl);

        boolean matchesTracked =
                failingUrl.equals(
                        tab.url);

        if (!matchesCurrent &&
                !matchesTracked) {
            return;
        }

        BrowserLogger.log(
                "ERROR",
                "Load error " +
                errorCode +
                " for " +
                failingUrl +
                ": " +
                description);

        tab.sslError = false;

        activity.showErrorPage(
                tab,
                failingUrl,
                description);
    }

    @Override
    public void onReceivedSslError(
            WebView view,
            SslErrorHandler handler,
            SslError error) {

        tab.sslError = true;

        activity.updateSecurity(tab);

        handler.cancel();

        String url =
                error == null ||
                error.getUrl() == null
                        ? tab.url
                        : error.getUrl();

        if (url != null &&
                !url.trim().isEmpty() &&
                !BrowserPage.isInternalUrl(url) &&
                !tab.errorPage) {

            BrowserLogger.log(
                    "ERROR",
                    "SSL error for " + url);

            activity.showErrorPage(
                    tab,
                    url,
                    Localization.translate(
                            activity,
                            "The site's security certificate could not be verified."));
        }
    }
}
