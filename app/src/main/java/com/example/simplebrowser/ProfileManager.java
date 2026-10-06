package com.example.simplebrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages persistent browser profiles and the in-memory Guest session.
 *
 * The Main profile intentionally keeps the legacy storage names so
 * existing SimpleBrowser installations keep their data after updating.
 */
public final class ProfileManager {

    public static final String MAIN_ID = "main";
    public static final int MAX_PROFILES = 8;

    private static final String PREFS = "browser_profiles";
    private static final String KEY_PROFILES = "profiles";
    private static final String KEY_ACTIVE = "active_profile";

    private static String guestSessionId;

    public ProfileManager(Context context) {
        ensureInitialized(context);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE);
    }

    private static void ensureInitialized(Context context) {
        SharedPreferences preferences =
                preferences(context);

        if (!preferences.contains(KEY_PROFILES)) {
            preferences.edit()
                    .putString(KEY_PROFILES, "[]")
                    .putString(KEY_ACTIVE, MAIN_ID)
                    .apply();
        }
    }

    public static String getActiveProfileId(Context context) {
        ensureInitialized(context);

        if (guestSessionId != null) {
            return guestSessionId;
        }

        String id =
                preferences(context).getString(
                        KEY_ACTIVE,
                        MAIN_ID);

        return id == null ||
                id.trim().isEmpty()
                ? MAIN_ID
                : id.trim();
    }

    public static void setActiveProfileId(
            Context context,
            String profileId) {

        ensureInitialized(context);

        if (profileId == null ||
                profileId.trim().isEmpty()) {
            profileId = MAIN_ID;
        }

        if (profileId.startsWith("guest_")) {
            guestSessionId = profileId;
            return;
        }

        guestSessionId = null;

        preferences(context).edit()
                .putString(
                        KEY_ACTIVE,
                        profileId)
                .apply();
    }

    public static String createGuestSession() {
        guestSessionId =
                "guest_" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "");

        return guestSessionId;
    }

    public static boolean isGuest(
            String profileId) {

        return profileId != null &&
                profileId.startsWith("guest_");
    }

    public List<Profile> getProfiles() {
        return getProfiles(null);
    }

    public List<Profile> getProfiles(Context context) {
        List<Profile> result =
                new ArrayList<>();

        Profile main =
                new Profile(
                        MAIN_ID,
                        "Main",
                        "");

        result.add(main);

        if (context == null) {
            return result;
        }

        ensureInitialized(context);

        String json =
                preferences(context)
                        .getString(
                                KEY_PROFILES,
                                "[]");

        try {
            JSONArray array =
                    new JSONArray(json);

            for (int i = 0;
                    i < array.length();
                    i++) {

                JSONObject object =
                        array.getJSONObject(i);

                String id =
                        object.optString(
                                "id",
                                "");

                if (id.isEmpty() ||
                        MAIN_ID.equals(id) ||
                        isGuest(id)) {
                    continue;
                }

                String name =
                        object.optString(
                                "name",
                                "Profile");

                if (name.trim().isEmpty()) {
                    name = "Profile";
                }

                result.add(
                        new Profile(
                                id,
                                name,
                                object.optString(
                                        "pfp",
                                        "")));
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    public List<Profile> getProfilesForContext(
            Context context) {

        return getProfiles(context);
    }

    public Profile getProfile(
            Context context,
            String profileId) {

        if (profileId == null ||
                profileId.trim().isEmpty() ||
                MAIN_ID.equals(profileId)) {

            return new Profile(
                    MAIN_ID,
                    "Main",
                    "");
        }

        if (isGuest(profileId)) {
            return new Profile(
                    profileId,
                    "Guest Profile",
                    "");
        }

        List<Profile> profiles =
                getProfiles(context);

        for (Profile profile : profiles) {
            if (profile.id.equals(profileId)) {
                return profile;
            }
        }

        return null;
    }

    public Profile getActiveProfile(
            Context context) {

        return getProfile(
                context,
                getActiveProfileId(context));
    }

    public int getPersistentProfileCount(
            Context context) {

        return getProfiles(context).size();
    }

    public Profile createProfile(
            Context context,
            String name,
            String pfpBase64) {

        List<Profile> profiles =
                getProfiles(context);

        if (profiles.size() >= MAX_PROFILES) {
            return null;
        }

        String cleanName =
                name == null
                        ? ""
                        : name.trim();

        if (cleanName.isEmpty()) {
            cleanName = "Profile " +
                    (profiles.size());
        }

        if (cleanName.length() > 40) {
            cleanName =
                    cleanName.substring(0, 40);
        }

        String id =
                "profile_" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "");

        Profile profile =
                new Profile(
                        id,
                        cleanName,
                        pfpBase64 == null
                                ? ""
                                : pfpBase64);

        saveProfile(context, profile);

        return profile;
    }

    public boolean updateProfile(
            Context context,
            String profileId,
            String name,
            String pfpBase64) {

        if (profileId == null ||
                MAIN_ID.equals(profileId) ||
                isGuest(profileId)) {
            return false;
        }

        List<Profile> profiles =
                getProfiles(context);

        boolean found = false;

        JSONArray array =
                new JSONArray();

        for (Profile profile : profiles) {

            if (MAIN_ID.equals(profile.id)) {
                continue;
            }

            if (profile.id.equals(profileId)) {

                String cleanName =
                        name == null
                                ? profile.name
                                : name.trim();

                if (cleanName.isEmpty()) {
                    cleanName = "Profile";
                }

                if (cleanName.length() > 40) {
                    cleanName =
                            cleanName.substring(0, 40);
                }

                profile =
                        new Profile(
                                profile.id,
                                cleanName,
                                pfpBase64 == null
                                        ? profile.pfpBase64
                                        : pfpBase64);

                found = true;
            }

            try {
                array.put(
                        new JSONObject()
                                .put("id", profile.id)
                                .put("name", profile.name)
                                .put("pfp", profile.pfpBase64));
            } catch (Exception ignored) {
            }
        }

        if (!found) {
            return false;
        }

        preferences(context).edit()
                .putString(
                        KEY_PROFILES,
                        array.toString())
                .apply();

        return true;
    }

    private void saveProfile(
            Context context,
            Profile profile) {

        List<Profile> profiles =
                getProfiles(context);

        JSONArray array =
                new JSONArray();

        for (Profile current : profiles) {

            if (MAIN_ID.equals(current.id)) {
                continue;
            }

            try {
                array.put(
                        new JSONObject()
                                .put("id", current.id)
                                .put("name", current.name)
                                .put("pfp", current.pfpBase64));
            } catch (Exception ignored) {
            }
        }

        try {
            array.put(
                    new JSONObject()
                            .put("id", profile.id)
                            .put("name", profile.name)
                            .put("pfp", profile.pfpBase64));
        } catch (Exception ignored) {
        }

        preferences(context).edit()
                .putString(
                        KEY_PROFILES,
                        array.toString())
                .apply();
    }

    public boolean deleteProfile(
            Context context,
            String profileId) {

        if (profileId == null ||
                MAIN_ID.equals(profileId) ||
                isGuest(profileId) ||
                profileId.equals(
                        getActiveProfileId(context))) {
            return false;
        }

        List<Profile> profiles =
                getProfiles(context);

        JSONArray array =
                new JSONArray();

        boolean removed = false;

        for (Profile profile : profiles) {

            if (MAIN_ID.equals(profile.id)) {
                continue;
            }

            if (profile.id.equals(profileId)) {
                removed = true;
                continue;
            }

            try {
                array.put(
                        new JSONObject()
                                .put("id", profile.id)
                                .put("name", profile.name)
                                .put("pfp", profile.pfpBase64));
            } catch (Exception ignored) {
            }
        }

        if (!removed) {
            return false;
        }

        preferences(context).edit()
                .putString(
                        KEY_PROFILES,
                        array.toString())
                .apply();

        deleteProfileData(
                context,
                profileId);

        return true;
    }

    /**
     * Maps a legacy storage name to an isolated profile name.
     * Main intentionally uses the original name for upgrade compatibility.
     */
    public static String scopedName(
            String baseName,
            String profileId) {

        if (baseName == null ||
                profileId == null ||
                MAIN_ID.equals(profileId)) {
            return baseName;
        }

        String safe =
                profileId.replaceAll(
                        "[^A-Za-z0-9_]",
                        "_");

        int dot =
                baseName.lastIndexOf('.');

        if (dot > 0) {
            return baseName.substring(0, dot) +
                    "_" +
                    safe +
                    baseName.substring(dot);
        }

        return baseName +
                "_" +
                safe;
    }

    public static String scopedPrefsName(
            String baseName,
            String profileId) {

        return scopedName(
                baseName,
                profileId);
    }

    public static String scopedDatabaseName(
            String baseName,
            String profileId) {

        return scopedName(
                baseName,
                profileId);
    }

    /**
     * Deletes storage owned by one non-main profile.
     */
    public static void deleteProfileData(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                MAIN_ID.equals(profileId)) {
            return;
        }

        String[] databases = {
                "browser_history.db",
                "download_history.db",
                "bookmarks.db",
                "passwords.db"
        };

        for (String database :
                databases) {

            try {
                context.deleteDatabase(
                        scopedDatabaseName(
                                database,
                                profileId));
            } catch (Exception ignored) {
            }
        }

        String[] preferencesNames = {
                "browser_settings",
                "cookie_sites",
                "tab_previews",
                "password_keys"
        };

        for (String name :
                preferencesNames) {

            try {
                context.getSharedPreferences(
                        scopedPrefsName(
                                name,
                                profileId),
                        Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply();
            } catch (Exception ignored) {
            }
        }

        try {
            android.webkit.CookieManager
                    .getInstance()
                    .removeAllCookie();
        } catch (Throwable ignored) {
        }
    }

    public static String encodeBitmap(
            Bitmap bitmap) {

        if (bitmap == null) {
            return "";
        }

        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            float scale =
                    Math.min(
                            1f,
                            160f /
                                    Math.max(
                                            1f,
                                            Math.max(
                                                    width,
                                                    height)));

            int targetWidth =
                    Math.max(
                            1,
                            Math.round(
                                    width * scale));

            int targetHeight =
                    Math.max(
                            1,
                            Math.round(
                                    height * scale));

            Bitmap scaled =
                    Bitmap.createScaledBitmap(
                            bitmap,
                            targetWidth,
                            targetHeight,
                            true);

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            scaled.compress(
                    Bitmap.CompressFormat.JPEG,
                    82,
                    output);

            if (scaled != bitmap) {
                scaled.recycle();
            }

            return android.util.Base64.encodeToString(
                    output.toByteArray(),
                    android.util.Base64.NO_WRAP);

        } catch (Throwable ignored) {
            return "";
        }
    }

    public static Bitmap decodeBitmap(
            String encoded) {

        if (encoded == null ||
                encoded.isEmpty()) {
            return null;
        }

        try {
            byte[] bytes =
                    android.util.Base64.decode(
                            encoded,
                            android.util.Base64.DEFAULT);

            return BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.length);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static final class Profile {

        public final String id;
        public final String name;
        public final String pfpBase64;

        public Profile(
                String id,
                String name,
                String pfpBase64) {

            this.id = id;
            this.name = name;
            this.pfpBase64 =
                    pfpBase64 == null
                            ? ""
                            : pfpBase64;
        }

        public boolean isMain() {
            return MAIN_ID.equals(id);
        }

        public boolean isGuest() {
            return ProfileManager.isGuest(id);
        }
    }
}
