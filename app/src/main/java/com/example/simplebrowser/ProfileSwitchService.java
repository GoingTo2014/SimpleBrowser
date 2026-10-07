package com.example.simplebrowser;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

    private static final String STATE_FILE =
            "profile_switch_state";

    private static volatile boolean running;

    private static final class SwitchState {
        String from;
        String to;
        int pid;
    }

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

        SwitchState state =
                new SwitchState();

        state.from = fromProfileId;
        state.to = toProfileId;
        state.pid =
                android.os.Process.myPid();

        if (!writeState(
                context,
                state)) {
            return false;
        }

        try {

            Intent intent =
                    new Intent(
                            context,
                            ProfileSwitchService.class);

            context.startService(intent);

            return true;

        } catch (Throwable error) {

            clearPending(context);
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

        SwitchState state =
                readState(context);

        if (state == null) {
            return;
        }

        /*
         * Never touch app_webview while another browser process is alive.
         * If the user launches the app while the helper is still finishing,
         * wait for that old process rather than starting WebView against the
         * wrong profile directory.
         */
        if (state.pid !=
                android.os.Process.myPid()) {

            long deadline =
                    System.currentTimeMillis() +
                    15000L;

            while (isProcessAlive(state.pid) &&
                    System.currentTimeMillis() < deadline) {

                try {
                    Thread.sleep(50L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            if (isProcessAlive(state.pid)) {
                return;
            }
        }

        try {

            completeSwap(
                    context,
                    state.from,
                    state.to);

            clearPending(context);

        } catch (Throwable ignored) {
            /*
             * Leave the state file intact. The next app launch or service
             * attempt can finish the swap.
             */
        }
    }

    public static void cancelPendingSwitch(
            Context context) {

        if (context != null) {
            clearPending(context);
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        if (running) {
            return START_REDELIVER_INTENT;
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

        return START_REDELIVER_INTENT;
    }

    private void performPendingSwitch() {

        SwitchState state =
                readState(this);

        if (state == null) {
            return;
        }

        /*
         * Wait for the WebView-using process to disappear.
         */
        long deadline =
                System.currentTimeMillis() +
                15000L;

        while (isProcessAlive(state.pid) &&
                System.currentTimeMillis() < deadline) {

            try {
                Thread.sleep(50L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        if (isProcessAlive(state.pid)) {
            return;
        }

        try {

            if (Build.VERSION.SDK_INT < 28) {

                completeSwap(
                        this,
                        state.from,
                        state.to);

                clearPending(this);

                launchBrowserWithRetry(
                        ProfileManager.MAIN_ID);

            } else {

                /*
                 * On Android 9+, the profile process must be freshly created
                 * so WebView can be configured with the target profile's
                 * data-directory suffix.
                 */
                clearPending(this);

                launchBrowserWithRetry(
                        state.to);
            }

        } catch (Throwable ignored) {
            /*
             * Keep the state file so a later launch can recover the legacy
             * filesystem swap.
             */
        }
    }

    private void launchBrowserWithRetry(
            String targetProfileId) {

        Intent intent;

        if (ProfileManager.MAIN_ID.equals(
                targetProfileId)) {

            intent =
                    new Intent(
                            this,
                            MainActivity.class);

        } else {

            intent =
                    new Intent(
                            this,
                            ProfileProcessActivity.class);

            intent.putExtra(
                    ProfileProcessActivity.EXTRA_PROFILE_ID,
                    targetProfileId);
        }

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NO_ANIMATION);

        for (int attempt = 0;
                attempt < 10;
                attempt++) {

            try {
                startActivity(intent);
                return;
            } catch (Throwable ignored) {

                if (attempt == 9) {
                    return;
                }

                try {
                    Thread.sleep(300L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static FileStateFile stateFile(
            Context context) {

        return new FileStateFile(
                new java.io.File(
                        context.getFilesDir(),
                        STATE_FILE));
    }

    private static final class FileStateFile {

        final android.util.AtomicFile file;

        FileStateFile(
                java.io.File base) {
            file =
                    new android.util.AtomicFile(
                            base);
        }
    }

    private static boolean writeState(
            Context context,
            SwitchState state) {

        FileStateFile holder =
                stateFile(context);

        java.io.FileOutputStream output =
                null;

        try {

            output =
                    holder.file.startWrite();

            String value =
                    state.from +
                    "\n" +
                    state.to +
                    "\n" +
                    state.pid +
                    "\n";

            output.write(
                    value.getBytes("UTF-8"));

            output.flush();
            output.getFD().sync();

            holder.file.finishWrite(output);

            return true;

        } catch (Throwable ignored) {

            if (output != null) {
                try {
                    holder.file.failWrite(output);
                } catch (Throwable ignoredAgain) {
                }
            }

            return false;
        }
    }

    private static SwitchState readState(
            Context context) {

        FileStateFile holder =
                stateFile(context);

        java.io.FileInputStream input =
                null;

        try {

            input =
                    holder.file.openRead();

            java.io.ByteArrayOutputStream output =
                    new java.io.ByteArrayOutputStream();

            byte[] buffer =
                    new byte[256];

            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(
                        buffer,
                        0,
                        count);
            }

            String[] lines =
                    new String(
                            output.toByteArray(),
                            "UTF-8")
                    .split("\\n");

            if (lines.length < 3) {
                return null;
            }

            String from =
                    lines[0].trim();

            String to =
                    lines[1].trim();

            int pid =
                    Integer.parseInt(
                            lines[2].trim());

            if (from.isEmpty() ||
                    to.isEmpty() ||
                    pid <= 0) {
                return null;
            }

            SwitchState state =
                    new SwitchState();

            state.from = from;
            state.to = to;
            state.pid = pid;

            return state;

        } catch (Throwable ignored) {

            return null;

        } finally {

            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void clearPending(
            Context context) {

        try {
            stateFile(context)
                    .file
                    .delete();
        } catch (Throwable ignored) {
        }
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
