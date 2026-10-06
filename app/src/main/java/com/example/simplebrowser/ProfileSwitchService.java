package com.example.simplebrowser;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import java.io.IOException;

/**
 * Performs the legacy WebView directory swap from a separate process.
 *
 * The main browser process is deliberately killed before this service touches
 * app_webview. This avoids changing Chromium files while WebView still owns
 * open files or database connections.
 */
public final class ProfileSwitchService extends Service {

    private static final String PREFS =
            "profile_switch_state";

    private static final String KEY_PENDING =
            "pending";

    private static final String KEY_FROM =
            "from_profile";

    private static final String KEY_TO =
            "to_profile";

    private static final String KEY_PID =
            "main_pid";

    private static volatile boolean running;

    public static boolean request(
            Context context,
            String fromProfileId,
            String toProfileId) {

        if (context == null ||
                fromProfileId == null ||
                toProfileId == null ||
                fromProfileId.equals(toProfileId)) {
            return false;
        }

        SharedPreferences preferences =
                preferences(context);

        preferences.edit()
                .putString(
                        KEY_FROM,
                        fromProfileId)
                .putString(
                        KEY_TO,
                        toProfileId)
                .putInt(
                        KEY_PID,
                        android.os.Process.myPid())
                .putBoolean(
                        KEY_PENDING,
                        true)
                .commit();

        try {

            Intent intent =
                    new Intent(
                            context,
                            ProfileSwitchService.class);

            context.startService(intent);

            return true;

        } catch (Throwable error) {

            preferences.edit()
                    .clear()
                    .apply();

            return false;
        }
    }

    /**
     * Recovery path for the rare case where Android kills the helper service
     * before it finishes. This runs before the first WebView is created.
     */
    public static void recoverIfNeeded(
            Context context) {

        if (context == null ||
                Build.VERSION.SDK_INT >= 28) {
            return;
        }

        SharedPreferences preferences =
                preferences(context);

        if (!preferences.getBoolean(
                KEY_PENDING,
                false)) {
            return;
        }

        String from =
                preferences.getString(
                        KEY_FROM,
                        ProfileManager.MAIN_ID);

        String to =
                preferences.getString(
                        KEY_TO,
                        ProfileManager.MAIN_ID);

        int pid =
                preferences.getInt(
                        KEY_PID,
                        -1);

        /*
         * Never touch app_webview while another browser process is alive.
         * If the user launches the app while the helper is still finishing,
         * wait for that old process rather than starting WebView against the
         * wrong profile directory.
         */
        if (pid != android.os.Process.myPid()) {

            long deadline =
                    System.currentTimeMillis() +
                    15000L;

            while (isProcessAlive(pid) &&
                    System.currentTimeMillis() < deadline) {

                try {
                    Thread.sleep(50L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            if (isProcessAlive(pid)) {
                return;
            }
        }

        try {

            completeSwap(
                    context,
                    from,
                    to);

            clearPending(
                    context);

        } catch (Throwable ignored) {
            /*
             * Leave the marker intact. The next app launch or service attempt
             * gets another opportunity to finish the recovery.
             */
        }
    }

    private static SharedPreferences preferences(
            Context context) {

        return context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        if (running) {
            return START_NOT_STICKY;
        }

        running = true;

        new Thread(
                () -> {

                    try {
                        performPendingSwitch();
                    } finally {
                        running = false;
                        stopSelf(startId);
                    }

                },
                "SimpleBrowser-ProfileSwitch")
                .start();

        return START_NOT_STICKY;
    }

    private void performPendingSwitch() {

        SharedPreferences preferences =
                preferences(this);

        if (!preferences.getBoolean(
                KEY_PENDING,
                false)) {
            return;
        }

        String from =
                preferences.getString(
                        KEY_FROM,
                        ProfileManager.MAIN_ID);

        String to =
                preferences.getString(
                        KEY_TO,
                        ProfileManager.MAIN_ID);

        int mainPid =
                preferences.getInt(
                        KEY_PID,
                        -1);

        /*
         * Wait for the WebView-using process to disappear. On a normal switch
         * Process.killProcess() makes this condition true almost immediately.
         */
        long deadline =
                System.currentTimeMillis() +
                15000L;

        while (isProcessAlive(mainPid) &&
                System.currentTimeMillis() < deadline) {

            try {
                Thread.sleep(50L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        if (isProcessAlive(mainPid)) {
            return;
        }

        try {

            completeSwap(
                    this,
                    from,
                    to);

            clearPending(
                    this);

            launchBrowser();

        } catch (Throwable ignored) {
            /*
             * Keep pending=true so MainActivity.recoverIfNeeded() can finish
             * the swap on the next launch.
             */
        }
    }

    private static void completeSwap(
            Context context,
            String from,
            String to)
            throws IOException {

        if (Build.VERSION.SDK_INT < 28) {
            ProfileWebViewStorage
                    .swapLegacyProfileData(
                            context,
                            from,
                            to);
        }
    }

    private void launchBrowser() {

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            startActivity(intent);
        } catch (Throwable ignored) {
        }
    }

    private static void clearPending(
            Context context) {

        preferences(context)
                .edit()
                .clear()
                .apply();
    }

    private static boolean isProcessAlive(
            int pid) {

        if (pid <= 0) {
            return false;
        }

        try {
            return new java.io.File(
                    "/proc/" +
                    pid)
                    .exists();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public IBinder onBind(
            Intent intent) {

        return null;
    }
}
