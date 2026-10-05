package com.example.simplebrowser;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.regex.Pattern;

public class UpdateManager {

    public static final String REPOSITORY =
            "GoingTo2014/SimpleBrowser";

    private static final String RELEASES_API =
            "https://api.github.com/repos/" +
            REPOSITORY +
            "/releases?per_page=100";

    private static final long AUTO_CHECK_INTERVAL_MS =
            12L * 60L * 60L * 1000L;

    private static final String UPDATE_FILE =
            "simplebrowser-update.apk";

    private static final String PART_FILE =
            "simplebrowser-update.apk.part";

    private static final Pattern VERSION_PATTERN =
            Pattern.compile(
                    "\\d{4}\\.\\d{2}\\.\\d{2}\\.\\d{4}");

    private final MainActivity activity;
    private final BrowserSettings settings;
    private final Handler mainHandler =
            new Handler(
                    Looper.getMainLooper());

    private volatile boolean checking;
    private volatile boolean downloading;
    private volatile UpdateInfo availableUpdate;
    private volatile File pendingInstall;

    public UpdateManager(
            MainActivity activity,
            BrowserSettings settings) {

        this.activity = activity;
        this.settings = settings;
    }

    public boolean isAutomaticUpdatesEnabled() {
        return settings.isAutomaticUpdatesEnabled();
    }

    public UpdateInfo getAvailableUpdate() {
        return availableUpdate;
    }

    public void checkForUpdates(
            final boolean manual,
            final BrowserTab tab) {

        if (!manual &&
                !settings.isAutomaticUpdatesEnabled()) {
            return;
        }

        if (!manual &&
                !shouldAutoCheck()) {
            return;
        }

        synchronized (this) {
            if (checking) {
                return;
            }

            checking = true;
        }

        postStatus(
                tab,
                "Checking for updates...",
                false,
                false);

        new Thread(
                () -> {

                    UpdateInfo result = null;
                    String error = null;

                    try {

                        result =
                                fetchLatestUpdate();

                        settings.setLastUpdateCheck(
                                System.currentTimeMillis());

                    } catch (Exception exception) {

                        error =
                                exception.getMessage();

                        if (error == null ||
                                error.trim().isEmpty()) {
                            error =
                                    "Could not connect to GitHub.";
                        }
                    }

                    final UpdateInfo update =
                            result;
                    final String finalError =
                            error;

                    mainHandler.post(
                            () -> {

                                synchronized (
                                        UpdateManager.this) {
                                    checking = false;
                                }

                                availableUpdate =
                                        update;

                                activity.onUpdateCheckFinished(
                                        tab,
                                        update,
                                        finalError,
                                        manual);

                                if (!manual &&
                                        update != null &&
                                        finalError == null) {

                                    downloadAndInstall(
                                            update,
                                            null,
                                            true);
                                }
                            });
                },
                "SimpleBrowser-UpdateCheck")
                .start();
    }

    private boolean shouldAutoCheck() {

        long last =
                settings.getLastUpdateCheck();

        return last <= 0 ||
                System.currentTimeMillis() -
                        last >=
                        AUTO_CHECK_INTERVAL_MS;
    }

    private UpdateInfo fetchLatestUpdate()
            throws Exception {

        HttpURLConnection connection =
                null;

        try {

            URL url =
                    new URL(RELEASES_API);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(
                    10000);
            connection.setReadTimeout(
                    15000);
            connection.setInstanceFollowRedirects(
                    true);
            connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json");
            connection.setRequestProperty(
                    "X-GitHub-Api-Version",
                    "2026-03-10");
            connection.setRequestProperty(
                    "User-Agent",
                    "SimpleBrowser/" +
                    BuildConfig.VERSION_NAME);

            int responseCode =
                    connection.getResponseCode();

            if (responseCode !=
                    HttpURLConnection.HTTP_OK) {

                throw new Exception(
                        "GitHub returned HTTP " +
                        responseCode + ".");
            }

            String response =
                    readAll(
                            connection.getInputStream());

            JSONArray releases =
                    new JSONArray(response);

            UpdateInfo newest =
                    null;

            for (int i = 0;
                    i < releases.length();
                    i++) {

                JSONObject release =
                        releases.getJSONObject(i);

                if (release.optBoolean(
                        "draft",
                        false)) {
                    continue;
                }

                String tag =
                        release.optString(
                                "tag_name",
                                "");

                if (!isValidVersion(tag) ||
                        !isNewerVersion(
                                tag,
                                BuildConfig.VERSION_NAME)) {
                    continue;
                }

                JSONArray assets =
                        release.optJSONArray(
                                "assets");

                if (assets == null) {
                    continue;
                }

                JSONObject apkAsset =
                        null;

                for (int j = 0;
                        j < assets.length();
                        j++) {

                    JSONObject asset =
                            assets.getJSONObject(j);

                    if ("app-release.apk".equals(
                            asset.optString(
                                    "name",
                                    ""))) {

                        apkAsset = asset;
                        break;
                    }
                }

                if (apkAsset == null) {
                    continue;
                }

                String downloadUrl =
                        apkAsset.optString(
                                "browser_download_url",
                                "");

                String digest =
                        apkAsset.optString(
                                "digest",
                                "");

                if (downloadUrl.trim().isEmpty() ||
                        digest.trim().isEmpty()) {
                    continue;
                }

                UpdateInfo candidate =
                        new UpdateInfo(
                                tag,
                                downloadUrl,
                                digest,
                                release.optString(
                                        "body",
                                        ""));

                if (newest == null ||
                        isNewerVersion(
                                candidate.version,
                                newest.version)) {

                    newest = candidate;
                }
            }

            return newest;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void downloadAndInstall(
            final UpdateInfo update,
            final BrowserTab tab,
            final boolean automatic) {

        if (update == null ||
                downloading) {
            return;
        }

        downloading = true;

        postStatus(
                tab,
                "Downloading update " +
                update.version +
                "...",
                false,
                false);

        if (automatic) {
            Toast.makeText(
                    activity,
                    Localization.translate(
                            activity,
                            "Downloading update"),
                    Toast.LENGTH_SHORT)
                    .show();
        }

        new Thread(
                () -> {

                    File downloaded = null;
                    String error = null;

                    try {

                        downloaded =
                                downloadAndVerify(
                                        update);

                    } catch (Exception exception) {

                        error =
                                exception.getMessage();

                        if (error == null ||
                                error.trim().isEmpty()) {
                            error =
                                    "The update could not be verified.";
                        }
                    }

                    final File finalFile =
                            downloaded;
                    final String finalError =
                            error;

                    mainHandler.post(
                            () -> {

                                downloading = false;

                                if (finalFile == null) {

                                    postStatus(
                                            tab,
                                            finalError,
                                            false,
                                            false);

                                    if (automatic) {
                                        Toast.makeText(
                                                activity,
                                                finalError,
                                                Toast.LENGTH_LONG)
                                                .show();
                                    }

                                    return;
                                }

                                postStatus(
                                        tab,
                                        "Update downloaded. Android will ask you to install it.",
                                        false,
                                        true);

                                if (automatic) {
                                    Toast.makeText(
                                            activity,
                                            "Update downloaded. Opening installer...",
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }

                                launchInstaller(
                                        finalFile);
                            });
                },
                "SimpleBrowser-UpdateDownload")
                .start();
    }

    private File downloadAndVerify(
            UpdateInfo update)
            throws Exception {

        File cacheDir =
                activity.getCacheDir();

        File partial =
                new File(
                        cacheDir,
                        PART_FILE);

        File target =
                new File(
                        cacheDir,
                        UPDATE_FILE);

        if (partial.exists()) {
            partial.delete();
        }

        if (target.exists()) {
            target.delete();
        }

        HttpURLConnection connection =
                null;

        try {

            URL url =
                    new URL(update.downloadUrl);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(
                    10000);
            connection.setReadTimeout(
                    30000);
            connection.setInstanceFollowRedirects(
                    true);
            connection.setRequestProperty(
                    "User-Agent",
                    "SimpleBrowser/" +
                    BuildConfig.VERSION_NAME);
            connection.setRequestProperty(
                    "Accept",
                    "application/vnd.android.package-archive");

            int responseCode =
                    connection.getResponseCode();

            if (responseCode !=
                    HttpURLConnection.HTTP_OK) {

                throw new Exception(
                        "Update download failed with HTTP " +
                        responseCode + ".");
            }

            InputStream input =
                    new BufferedInputStream(
                            connection.getInputStream());

            FileOutputStream output =
                    new FileOutputStream(
                            partial);

            byte[] buffer =
                    new byte[8192];

            int count;

            try {

                while ((count =
                        input.read(buffer)) >= 0) {

                    output.write(
                            buffer,
                            0,
                            count);
                }

            } finally {

                try {
                    input.close();
                } catch (Exception ignored) {
                }

                try {
                    output.close();
                } catch (Exception ignored) {
                }
            }

            String actualDigest =
                    sha256(partial);

            String expectedDigest =
                    update.digest
                            .toLowerCase();

            if (expectedDigest.startsWith(
                    "sha256:")) {

                expectedDigest =
                        expectedDigest.substring(
                                7);
            }

            if (!expectedDigest.equals(
                    actualDigest)) {

                throw new Exception(
                        "Update verification failed: APK hash does not match the GitHub release.");
            }

            verifyApkIdentityAndSignature(
                    partial);

            if (target.exists()) {
                target.delete();
            }

            if (!partial.renameTo(target)) {
                throw new Exception(
                        "Could not prepare the downloaded update.");
            }

            return target;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }

            if (partial.exists()) {
                partial.delete();
            }
        }
    }

    private void verifyApkIdentityAndSignature(
            File apk)
            throws Exception {

        PackageManager packageManager =
                activity.getPackageManager();

        PackageInfo archiveInfo =
                packageManager
                        .getPackageArchiveInfo(
                                apk.getAbsolutePath(),
                                PackageManager.GET_SIGNATURES);

        if (archiveInfo == null ||
                !activity.getPackageName()
                        .equals(archiveInfo.packageName)) {

            throw new Exception(
                    "Update verification failed: package name does not match.");
        }

        PackageInfo installedInfo =
                packageManager.getPackageInfo(
                        activity.getPackageName(),
                        PackageManager.GET_SIGNATURES);

        if (installedInfo.signatures == null ||
                archiveInfo.signatures == null ||
                installedInfo.signatures.length == 0 ||
                archiveInfo.signatures.length == 0) {

            throw new Exception(
                    "Update verification failed: signing certificate is missing.");
        }

        byte[] installed =
                installedInfo.signatures[0].toByteArray();

        byte[] downloaded =
                archiveInfo.signatures[0].toByteArray();

        if (!MessageDigest.isEqual(
                installed,
                downloaded)) {

            throw new Exception(
                    "Update verification failed: signing certificate does not match the installed app.");
        }
    }

    private String sha256(
            File file)
            throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256");

        InputStream input =
                new BufferedInputStream(
                        new FileInputStream(file));

        byte[] buffer =
                new byte[8192];

        int count;

        try {

            while ((count =
                    input.read(buffer)) >= 0) {

                digest.update(
                        buffer,
                        0,
                        count);
            }

        } finally {

            try {
                input.close();
            } catch (Exception ignored) {
            }
        }

        byte[] bytes =
                digest.digest();

        StringBuilder result =
                new StringBuilder();

        for (byte value : bytes) {

            result.append(
                    String.format(
                            "%02x",
                            value & 0xFF));
        }

        return result.toString();
    }

    public void resumePendingInstall() {

        File file =
                pendingInstall;

        if (file == null ||
                !file.isFile()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 26 &&
                !activity.getPackageManager()
                        .canRequestPackageInstalls()) {
            return;
        }

        pendingInstall = null;

        launchInstaller(file);
    }

    private void launchInstaller(
            File file) {

        if (Build.VERSION.SDK_INT >= 26 &&
                !activity.getPackageManager()
                        .canRequestPackageInstalls()) {

            pendingInstall = file;

            try {

                Intent settingsIntent =
                        new Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse(
                                        "package:" +
                                        activity.getPackageName()));

                activity.startActivity(
                        settingsIntent);

                Toast.makeText(
                        activity,
                        Localization.translate(
                                activity,
                                "Allow Simple Browser to install updates, then return to Simple Browser."),
                        Toast.LENGTH_LONG)
                        .show();

            } catch (ActivityNotFoundException exception) {

                try {

                    activity.startActivity(
                            new Intent(
                                    Settings.ACTION_SECURITY_SETTINGS));

                } catch (Exception ignored) {
                }
            }

            return;
        }

        Uri uri;

        if (Build.VERSION.SDK_INT >= 24) {

            uri =
                    UpdateApkProvider.getUri();

        } else {

            uri =
                    Uri.fromFile(file);
        }

        Intent installer =
                new Intent(
                        Intent.ACTION_VIEW);

        installer.setDataAndType(
                uri,
                "application/vnd.android.package-archive");

        installer.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {

            activity.startActivity(
                    installer);

        } catch (Exception exception) {

            Toast.makeText(
                    activity,
                    "Could not open Android's installer.",
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void postStatus(
            BrowserTab tab,
            String message,
            boolean showInstall,
            boolean downloadingFinished) {

        final String finalMessage =
                message == null
                        ? ""
                        : message;

        mainHandler.post(
                () -> activity
                        .onUpdateStatus(
                                tab,
                                finalMessage,
                                showInstall));
    }

    private static boolean isValidVersion(
            String value) {

        return value != null &&
                VERSION_PATTERN.matcher(
                        value)
                        .matches();
    }

    private static boolean isNewerVersion(
            String first,
            String second) {

        return first.compareTo(second) > 0;
    }

    public static final class UpdateInfo {

        public final String version;
        public final String downloadUrl;
        public final String digest;
        public final String notes;

        public UpdateInfo(
                String version,
                String downloadUrl,
                String digest,
                String notes) {

            this.version = version;
            this.downloadUrl = downloadUrl;
            this.digest = digest;
            this.notes = notes;
        }
    }
}
