package com.example.simplebrowser;

import android.content.Context;
import android.util.AtomicFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Tracks the lifetime of dedicated profile processes.
 *
 * A profile process can outlive its Activity because Android may cache it.
 * This small on-disk lease prevents a deleted profile's process slot from
 * being reused for another profile while that old process is still alive.
 */
public final class ProfileProcessRuntime {

    private static final String FILE_PREFIX =
            "profile_process_runtime_";

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

        int slot =
                ProfileManager.getProcessSlot(
                        context,
                        profileId);

        if (slot <= 0) {
            return;
        }

        write(
                context,
                slot,
                profileId,
                android.os.Process.myPid());
    }

    public static boolean isSlotOccupied(
            Context context,
            int slot) {

        RuntimeState state =
                read(context, slot);

        if (state == null) {
            return false;
        }

        if (!isProcessAlive(state.pid)) {
            delete(
                    context,
                    slot);
            return false;
        }

        return processMatchesSlot(
                context,
                slot,
                state.pid);
    }

    public static void stopProfileProcess(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                profileId.trim().isEmpty()) {
            return;
        }

        int slot =
                findMappedSlot(
                        context,
                        profileId);

        if (slot <= 0) {
            return;
        }

        RuntimeState state =
                read(context, slot);

        if (state == null) {
            return;
        }

        if (!isProcessAlive(state.pid) ||
                !profileId.equals(state.profileId) ||
                !processMatchesSlot(
                        context,
                        slot,
                        state.pid)) {

            delete(
                    context,
                    slot);
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

        /*
         * Leave the lease file until the old process is actually gone.
         * isSlotOccupied() will retire it on the next allocation attempt.
         */
    }

    private static int findMappedSlot(
            Context context,
            String profileId) {

        String json =
                readProcessSlots(context);

        if (json == null ||
                json.trim().isEmpty()) {
            return 0;
        }

        try {
            org.json.JSONObject slots =
                    new org.json.JSONObject(json);

            int slot =
                    slots.optInt(
                            profileId,
                            0);

            return slot >= 1 &&
                    slot <= ProfileManager.MAX_PROFILES
                    ? slot
                    : 0;

        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static String readProcessSlots(
            Context context) {

        if (context == null) {
            return null;
        }

        FileInputStream input = null;

        try {
            AtomicFile file =
                    new AtomicFile(
                            new File(
                                    context.getFilesDir(),
                                    "profile_process_slots.state"));

            input =
                    file.openRead();

            java.io.ByteArrayOutputStream output =
                    new java.io.ByteArrayOutputStream();

            byte[] buffer =
                    new byte[4096];

            int count;

            while ((count =
                    input.read(buffer)) != -1) {
                output.write(
                        buffer,
                        0,
                        count);
            }

            return new String(
                    output.toByteArray(),
                    "UTF-8");

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

    private static File stateFile(
            Context context,
            int slot) {

        return new File(
                context.getFilesDir(),
                FILE_PREFIX +
                slot +
                ".state");
    }

    private static void write(
            Context context,
            int slot,
            String profileId,
            int pid) {

        AtomicFile file =
                new AtomicFile(
                        stateFile(
                                context,
                                slot));

        FileOutputStream output = null;

        try {
            output =
                    file.startWrite();

            String value =
                    profileId +
                    "\n" +
                    pid;

            output.write(
                    value.getBytes("UTF-8"));

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
            Context context,
            int slot) {

        FileInputStream input = null;

        try {
            input =
                    new AtomicFile(
                            stateFile(
                                    context,
                                    slot))
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

            state.profileId = profileId;
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

    private static void delete(
            Context context,
            int slot) {

        try {
            new AtomicFile(
                    stateFile(
                            context,
                            slot))
                    .delete();
        } catch (Throwable ignored) {
        }
    }

    private static boolean isProcessAlive(
            int pid) {

        if (pid <= 0) {
            return false;
        }

        return new File(
                "/proc/" +
                pid)
                .exists();
    }

    private static boolean processMatchesSlot(
            Context context,
            int slot,
            int pid) {

        File cmdline =
                new File(
                        "/proc/" +
                        pid +
                        "/cmdline");

        FileInputStream input = null;

        try {
            input =
                    new FileInputStream(
                            cmdline);

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

            String command =
                    new String(
                            output.toByteArray(),
                            "UTF-8")
                    .replace(
                            " ",
                            "")
                    .trim();

            String expected =
                    context.getPackageName() +
                    ":profile" +
                    slot;

            return expected.equals(command);

        } catch (Throwable ignored) {
            return false;

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static final class RuntimeState {
        String profileId;
        int pid;
    }
}
