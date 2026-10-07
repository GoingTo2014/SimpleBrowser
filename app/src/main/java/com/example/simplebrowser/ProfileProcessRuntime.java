package com.example.simplebrowser;

import android.content.Context;
import android.util.AtomicFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Tracks the one dedicated profile process used by Android 9+.
 *
 * The process is restarted when changing profiles so each profile can use
 * its own WebView data-directory suffix.
 */
public final class ProfileProcessRuntime {

    private static final String STATE_FILE =
            "profile_process_runtime.state";

    private ProfileProcessRuntime() {
    }

    public static void markStarted(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                profileId.trim().isEmpty()) {
            return;
        }

        write(
                context,
                profileId,
                android.os.Process.myPid());
    }

    public static void stopProfileProcess(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                profileId.trim().isEmpty()) {
            return;
        }

        RuntimeState state =
                read(context);

        if (state == null) {
            return;
        }

        if (!profileId.equals(
                state.profileId)) {

            return;
        }

        if (!isProcessAlive(state.pid)) {
            clear(context);
            return;
        }

        if (state.pid ==
                android.os.Process.myPid()) {
            return;
        }

        try {
            android.os.Process.killProcess(
                    state.pid);
        } catch (Throwable ignored) {
        }
    }

    public static String getRunningProfileId(
            Context context) {

        RuntimeState state =
                read(context);

        if (state == null ||
                !isProcessAlive(state.pid)) {
            if (state != null) {
                clear(context);
            }
            return null;
        }

        return state.profileId;
    }

    private static File stateFile(
            Context context) {

        return new File(
                context.getFilesDir(),
                STATE_FILE);
    }

    private static void write(
            Context context,
            String profileId,
            int pid) {

        AtomicFile file =
                new AtomicFile(
                        stateFile(context));

        FileOutputStream output = null;

        try {
            output =
                    file.startWrite();

            output.write(
                    (profileId +
                     "\n" +
                     pid)
                    .getBytes("UTF-8"));

            output.flush();
            output.getFD().sync();

            file.finishWrite(output);
            output = null;

        } catch (Throwable ignored) {

            if (output != null) {
                try {
                    file.failWrite(output);
                } catch (Throwable ignoredAgain) {
                }
            }
        }
    }

    private static RuntimeState read(
            Context context) {

        if (context == null) {
            return null;
        }

        FileInputStream input = null;

        try {
            input =
                    new AtomicFile(
                            stateFile(context))
                            .openRead();

            java.io.ByteArrayOutputStream output =
                    new java.io.ByteArrayOutputStream();

            byte[] buffer =
                    new byte[256];

            int count;

            while ((count =
                    input.read(buffer)) != -1) {
                output.write(
                        buffer,
                        0,
                        count);
            }

            String[] lines =
                    new String(
                            output.toByteArray(),
                            "UTF-8")
                    .split("\n");

            if (lines.length < 2) {
                return null;
            }

            String profileId =
                    lines[0].trim();

            int pid =
                    Integer.parseInt(
                            lines[1].trim());

            if (profileId.isEmpty() ||
                    pid <= 0) {
                return null;
            }

            RuntimeState state =
                    new RuntimeState();

            state.profileId =
                    profileId;

            state.pid =
                    pid;

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

    private static void clear(
            Context context) {

        try {
            new AtomicFile(
                    stateFile(context))
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
            return new File(
                    "/proc/" +
                    pid)
                    .exists();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class RuntimeState {
        String profileId;
        int pid;
    }
}
