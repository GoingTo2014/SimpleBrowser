package com.example.simplebrowser;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

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
    private static final String KEY_MAIN_NAME = "main_name";
    private static final String KEY_PROCESS_SLOTS =
            "profile_process_slots";

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
                    .putString(KEY_MAIN_NAME, "Profile 1")
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
                .commit();
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

    /**
     * Gives each persistent profile a stable Android process slot.
     * Main uses the application's default process (slot 0). User-created
     * profiles occupy slots 1..MAX_PROFILES and retain their slot even when
     * other profiles are deleted.
     */
    public static synchronized int getProcessSlot(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                profileId.trim().isEmpty() ||
                MAIN_ID.equals(profileId) ||
                isGuest(profileId)) {
            return 0;
        }

        ensureInitialized(context);

        SharedPreferences preferences =
                preferences(context);

        String json =
                preferences.getString(
                        KEY_PROCESS_SLOTS,
                        "{}");

        try {
            JSONObject slots =
                    new JSONObject(json);

            int existing =
                    slots.optInt(
                            profileId,
                            0);

            if (existing >= 1 &&
                    existing <= MAX_PROFILES) {
                return existing;
            }

            boolean[] used =
                    new boolean[MAX_PROFILES + 1];

            java.util.Iterator<String> keys =
                    slots.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                int slot =
                        slots.optInt(key, 0);

                if (slot >= 1 &&
                        slot <= MAX_PROFILES) {
                    used[slot] = true;
                }
            }

            for (int slot = 1;
                    slot <= MAX_PROFILES;
                    slot++) {

                if (!used[slot]) {
                    slots.put(
                            profileId,
                            slot);

                    preferences.edit()
                            .putString(
                                    KEY_PROCESS_SLOTS,
                                    slots.toString())
                            .commit();

                    return slot;
                }
            }

        } catch (Throwable ignored) {
            /*
             * Fall through to a deterministic emergency slot. The normal
             * allocation path above is used for all valid profile sets.
             */
        }

        return 1;
    }

    public static synchronized void releaseProcessSlot(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                MAIN_ID.equals(profileId) ||
                isGuest(profileId)) {
            return;
        }

        try {
            SharedPreferences preferences =
                    preferences(context);

            JSONObject slots =
                    new JSONObject(
                            preferences.getString(
                                    KEY_PROCESS_SLOTS,
                                    "{}"));

            slots.remove(profileId);

            preferences.edit()
                    .putString(
                            KEY_PROCESS_SLOTS,
                            slots.toString())
                    .commit();

        } catch (Throwable ignored) {
        }
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
                        getMainProfileName(context));

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
                                name));
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
                    getMainProfileName(context));
        }

        if (isGuest(profileId)) {
            return new Profile(
                    profileId,
                    "Guest Profile");
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
            String name) {

        List<Profile> profiles =
                getProfiles(context);

        // MAX_PROFILES counts user-created profiles; Main is separate.
        if (profiles.size() - 1 >= MAX_PROFILES) {
            return null;
        }

        String cleanName =
                name == null
                        ? ""
                        : name.trim();

        if (cleanName.isEmpty()) {
            cleanName = "Profile " +
                    (profiles.size() + 1);
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
                        cleanName);

        saveProfile(context, profile);

        return profile;
    }

    public boolean updateProfile(
            Context context,
            String profileId,
            String name) {

        if (profileId == null ||
                isGuest(profileId)) {
            return false;
        }

        String cleanName =
                name == null
                        ? ""
                        : name.trim();

        if (cleanName.isEmpty()) {
            cleanName = MAIN_ID.equals(profileId)
                    ? "Profile 1"
                    : "Profile";
        }

        if (cleanName.length() > 40) {
            cleanName =
                    cleanName.substring(0, 40);
        }

        if (MAIN_ID.equals(profileId)) {
            preferences(context).edit()
                    .putString(
                            KEY_MAIN_NAME,
                            cleanName)
                    .apply();
            return true;
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

                String updatedName =
                        name == null
                                ? profile.name
                                : name.trim();

                if (updatedName.isEmpty()) {
                    updatedName = "Profile";
                }

                if (updatedName.length() > 40) {
                    updatedName =
                            updatedName.substring(0, 40);
                }

                profile =
                        new Profile(
                                profile.id,
                                updatedName);

                found = true;
            }

            try {
                array.put(
                        new JSONObject()
                                .put("id", profile.id)
                                .put("name", profile.name));
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

    private String getMainProfileName(
            Context context) {

        String value =
                preferences(context).getString(
                        KEY_MAIN_NAME,
                        "Profile 1");

        if (value == null ||
                value.trim().isEmpty()) {
            return "Profile 1";
        }

        return value.trim();
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
                                .put("name", current.name));
            } catch (Exception ignored) {
            }
        }

        try {
            array.put(
                    new JSONObject()
                            .put("id", profile.id)
                            .put("name", profile.name));
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
                                .put("name", profile.name));
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

        releaseProcessSlot(
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
                "passwords.db",
                "web_storage.db"
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

        PasswordStore.deleteCryptoKey(
                context,
                profileId);

        /*
         * WebView has its own Chromium storage outside the browser's SQLite
         * stores. Remove that profile's isolated WebView data as well.
         */
        ProfileWebViewStorage.deleteProfileData(
                context,
                profileId);
    }

    public static final class Profile {

        public final String id;
        public final String name;

        public Profile(
                String id,
                String name) {

            this.id = id;
            this.name = name;
        }

        public boolean isMain() {
            return MAIN_ID.equals(id);
        }

        public boolean isGuest() {
            return ProfileManager.isGuest(id);
        }
    }
}
