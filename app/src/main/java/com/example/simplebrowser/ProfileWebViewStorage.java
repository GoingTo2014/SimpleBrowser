package com.example.simplebrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.webkit.WebView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Owns the browser-engine storage boundary between persistent profiles.
 *
 * Android 9+ provides WebView.setDataDirectorySuffix(), which is the
 * supported way to give each process a separate WebView data directory.
 *
 * Android 4.4 through Android 8.1 do not have that API. Those versions use
 * one process-global Chromium WebView data directory, so profile switching
 * is implemented as a process restart. ProfileSwitchService waits for this
 * process to die, then swaps the complete WebView directory on disk before
 * launching the browser again.
 */
public final class ProfileWebViewStorage {

    private static final String BACKUP_ROOT =
            "webview_profiles";

    private static final String MIGRATION_PREFS =
            "webview_profile_migration";

    private static final String MIGRATION_PREFIX =
            "legacy_shared_";

    private static final String CACHE_DIR_NAME =
            "org.chromium.android_webview";

    private ProfileWebViewStorage() {
    }

    /**
     * Must be called before any WebView or other android.webkit API is used
     * in the main process.
     */
    public static void configureForProcess(
            Context context) {

        if (context == null ||
                Build.VERSION.SDK_INT < 28) {
            return;
        }

        String profileId =
                ProfileManager.getActiveProfileId(
                        context);

        if (ProfileManager.isGuest(profileId) ||
                ProfileManager.MAIN_ID.equals(profileId)) {
            /*
             * Main intentionally keeps the legacy default directory so
             * existing installations retain their original WebView data.
             */
            return;
        }

        /*
         * Before this feature existed, all profiles shared the default
         * WebView directory. On an upgrade where a non-main profile is the
         * active profile, copy that legacy state into its new directory once.
         *
         * Keep the old directory intact: it remains the Main profile's
         * legacy data and gives the upgrade a non-destructive migration path.
         */
        migrateLegacySharedDirectory(
                context,
                profileId);

        try {
            WebView.setDataDirectorySuffix(
                    getSuffix(profileId));
        } catch (Throwable ignored) {
            /*
             * Android 9+ should support this API. A defensive catch keeps
             * unusual vendor WebView implementations from crashing before
             * the browser UI can load.
             */
        }
    }

    private static void migrateLegacySharedDirectory(
            Context context,
            String profileId) {

        try {
            SharedPreferences preferences =
                    context.getSharedPreferences(
                            MIGRATION_PREFS,
                            Context.MODE_PRIVATE);

            String key =
                    MIGRATION_PREFIX +
                    safeProfileId(profileId);

            if (preferences.getBoolean(key, false)) {
                return;
            }

            File legacy =
                    getDefaultWebViewDirectory(context);

            File target =
                    getSuffixedWebViewDirectory(
                            context,
                            profileId);

            if (!target.exists() &&
                    legacy.exists()) {

                copyDirectory(
                        legacy,
                        target);
            }

            /*
             * Mark successful migration even when there was no legacy
             * directory. This prevents repeated filesystem scans.
             */
            preferences.edit()
                    .putBoolean(key, true)
                    .apply();

        } catch (Throwable ignored) {
            /*
             * Do not let migration break startup. If it failed, retry on
             * the next launch because the marker was not written.
             */
        }
    }

    public static void deleteProfileData(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                ProfileManager.MAIN_ID.equals(profileId) ||
                ProfileManager.isGuest(profileId)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 28) {
            deleteRecursively(
                    getSuffixedWebViewDirectory(
                            context,
                            profileId));
            return;
        }

        File profileRoot =
                getProfileBackupRoot(
                        context,
                        profileId);

        deleteRecursively(profileRoot);
    }

    /**
     * Swaps the shared WebView data used by Android 4.4-8.1.
     *
     * This method must only be called after the WebView-using process has
     * exited. It deliberately performs no android.webkit calls.
     */
    public static synchronized void swapLegacyProfileData(
            Context context,
            String fromProfileId,
            String toProfileId)
            throws IOException {

        if (context == null ||
                Build.VERSION.SDK_INT >= 28) {
            return;
        }

        if (fromProfileId == null ||
                fromProfileId.trim().isEmpty()) {
            fromProfileId = ProfileManager.MAIN_ID;
        }

        if (toProfileId == null ||
                toProfileId.trim().isEmpty()) {
            toProfileId = ProfileManager.MAIN_ID;
        }

        if (fromProfileId.equals(toProfileId)) {
            return;
        }

        File activeWebView =
                getDefaultWebViewDirectory(context);

        File fromWebView =
                getProfileWebViewDirectory(
                        context,
                        fromProfileId);

        File toWebView =
                getProfileWebViewDirectory(
                        context,
                        toProfileId);

        swapOneDirectory(
                activeWebView,
                fromWebView,
                toWebView);

        File activeCache =
                getWebViewCacheDirectory(context);

        File fromCache =
                getProfileCacheDirectory(
                        context,
                        fromProfileId);

        File toCache =
                getProfileCacheDirectory(
                        context,
                        toProfileId);

        swapOneDirectory(
                activeCache,
                fromCache,
                toCache);
    }

    private static void swapOneDirectory(
            File active,
            File fromBackup,
            File toBackup)
            throws IOException {

        File fromParent =
                fromBackup.getParentFile();

        if (fromParent != null &&
                !fromParent.exists() &&
                !fromParent.mkdirs() &&
                !fromParent.exists()) {
            throw new IOException(
                    "Could not create profile storage directory: " +
                    fromParent);
        }

        /*
         * A crashed swap can leave the source profile in its backup and the
         * target profile still in its backup. If both are present, the first
         * half of an older swap already happened; do not overwrite either.
         */
        if (fromBackup.exists() &&
                active.exists() &&
                toBackup.exists()) {
            return;
        }

        /*
         * First move the currently active profile out of Chromium's expected
         * location. This makes the next step safe to restore into that path.
         */
        if (!fromBackup.exists() &&
                active.exists()) {

            moveDirectory(
                    active,
                    fromBackup);
        }

        /*
         * Restore the selected profile. A profile that has never been used
         * simply gets a clean directory.
         */
        if (toBackup.exists()) {

            if (active.exists()) {
                deleteRecursively(active);
            }

            moveDirectory(
                    toBackup,
                    active);

        } else if (!active.exists()) {

            if (!active.mkdirs() &&
                    !active.exists()) {
                throw new IOException(
                        "Could not create active WebView directory: " +
                        active);
            }
        }
    }

    private static void moveDirectory(
            File source,
            File destination)
            throws IOException {

        if (!source.exists()) {
            return;
        }

        File parent =
                destination.getParentFile();

        if (parent != null &&
                !parent.exists() &&
                !parent.mkdirs() &&
                !parent.exists()) {
            throw new IOException(
                    "Could not create destination: " +
                    parent);
        }

        if (destination.exists()) {
            throw new IOException(
                    "Destination already exists: " +
                    destination);
        }

        if (source.renameTo(destination)) {
            return;
        }

        copyDirectory(
                source,
                destination);

        if (!deleteRecursively(source)) {
            throw new IOException(
                    "Could not remove old WebView directory: " +
                    source);
        }
    }

    private static void copyDirectory(
            File source,
            File destination)
            throws IOException {

        if (source == null ||
                destination == null) {
            throw new IOException(
                    "Invalid WebView directory");
        }

        if (source.isDirectory()) {

            if (!destination.exists() &&
                    !destination.mkdirs() &&
                    !destination.exists()) {
                throw new IOException(
                        "Could not create directory: " +
                        destination);
            }

            File[] children =
                    source.listFiles();

            if (children == null) {
                return;
            }

            for (File child : children) {
                copyDirectory(
                        child,
                        new File(
                                destination,
                                child.getName()));
            }

            return;
        }

        File parent =
                destination.getParentFile();

        if (parent != null &&
                !parent.exists() &&
                !parent.mkdirs() &&
                !parent.exists()) {
            throw new IOException(
                    "Could not create file directory: " +
                    parent);
        }

        FileInputStream input = null;
        FileOutputStream output = null;

        try {
            input =
                    new FileInputStream(source);

            output =
                    new FileOutputStream(
                            destination);

            byte[] buffer =
                    new byte[32768];

            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(
                        buffer,
                        0,
                        count);
            }

            output.getFD().sync();

        } finally {

            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                }
            }

            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static boolean deleteRecursively(
            File file) {

        if (file == null ||
                !file.exists()) {
            return true;
        }

        if (file.isDirectory()) {

            File[] children =
                    file.listFiles();

            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }

        return file.delete() ||
                !file.exists();
    }

    private static File getDefaultWebViewDirectory(
            Context context) {

        return new File(
                context.getApplicationInfo().dataDir,
                "app_webview");
    }

    private static File getSuffixedWebViewDirectory(
            Context context,
            String profileId) {

        return new File(
                context.getApplicationInfo().dataDir,
                "app_webview_" +
                getSuffix(profileId));
    }

    private static File getProfileBackupRoot(
            Context context,
            String profileId) {

        return new File(
                new File(
                        context.getFilesDir(),
                        BACKUP_ROOT),
                safeProfileId(profileId));
    }

    private static File getProfileWebViewDirectory(
            Context context,
            String profileId) {

        return new File(
                getProfileBackupRoot(
                        context,
                        profileId),
                "app_webview");
    }

    private static File getProfileCacheDirectory(
            Context context,
            String profileId) {

        return new File(
                getProfileBackupRoot(
                        context,
                        profileId),
                CACHE_DIR_NAME);
    }

    private static File getWebViewCacheDirectory(
            Context context) {

        return new File(
                context.getCacheDir(),
                CACHE_DIR_NAME);
    }

    private static String getSuffix(
            String profileId) {

        return "profile_" +
                safeProfileId(profileId);
    }

    private static String safeProfileId(
            String profileId) {

        if (profileId == null ||
                profileId.trim().isEmpty()) {
            return "main";
        }

        return profileId.replaceAll(
                "[^A-Za-z0-9_]",
                "_");
    }
}
