package com.example.simplebrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Synchronizes Simple Browser profile metadata and browser settings through
 * Cloud Firestore using the Firebase Auth ID token already owned by the app.
 *
 * This deliberately uses REST instead of the Firebase Android SDK so Simple
 * Browser keeps its no-external-dependency and Android 4.4 compatibility.
 */
public final class SimpleSyncManager {

    public interface Callback {
        void onComplete(boolean success, String message);
    }

    private static final String PROJECT_ID = "simple-54d66";

    private static final String FIRESTORE_BASE =
            "https://firestore.googleapis.com/v1/projects/" +
            PROJECT_ID +
            "/databases/(default)/documents";

    private static final String PREFS = "simple_sync";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_LAST_SYNC = "last_sync";
    private static final String KEY_LAST_ATTEMPT = "last_attempt";
    private static final String KEY_PROFILE_PREFIX = "profile_modified_";
    private static final String KEY_DELETED = "deleted_profiles";

    private static final long AUTO_SYNC_INTERVAL_MS =
            5L * 60L * 1000L;

    private SimpleSyncManager() {
    }

    public static void syncAsync(
            final Context context,
            final Callback callback) {

        if (context == null) {
            notifyCallback(
                    callback,
                    false,
                    "No context");
            return;
        }

        final Context appContext =
                context.getApplicationContext();

        SharedPreferences preferences =
                appContext.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE);

        long now =
                System.currentTimeMillis();

        long lastAttempt =
                preferences.getLong(
                        KEY_LAST_ATTEMPT,
                        0L);

        if (callback == null &&
                now - lastAttempt <
                        AUTO_SYNC_INTERVAL_MS) {
            return;
        }

        preferences.edit()
                .putLong(
                        KEY_LAST_ATTEMPT,
                        now)
                .apply();

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {

                        boolean success = false;
                        String message;

                        try {
                            message =
                                    sync(appContext);
                            success = true;

                        } catch (Exception exception) {
                            message =
                                    exception.getMessage();

                            if (message == null ||
                                    message.trim().isEmpty()) {
                                message = "Sync failed";
                            }
                        }

                        notifyCallback(
                                callback,
                                success,
                                message);
                    }
                },
                "SimpleBrowserSync")
                .start();
    }

    public static String getDeviceId(
            Context context) {

        if (context == null) {
            return "";
        }

        SharedPreferences preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE);

        String value =
                preferences.getString(
                        KEY_DEVICE_ID,
                        "");

        if (value == null ||
                value.trim().isEmpty()) {

            value =
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "");

            preferences.edit()
                    .putString(
                            KEY_DEVICE_ID,
                            value)
                    .apply();
        }

        return value;
    }

    public static long getLastSync(
            Context context) {

        if (context == null) {
            return 0L;
        }

        return context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE)
                .getLong(
                        KEY_LAST_SYNC,
                        0L);
    }

    public static void markProfileChanged(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                profileId.trim().isEmpty() ||
                ProfileManager.isGuest(profileId)) {
            return;
        }

        SharedPreferences preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE);

        String key =
                KEY_PROFILE_PREFIX +
                profileId;

        long previous =
                preferences.getLong(
                        key,
                        0L);

        long now =
                System.currentTimeMillis();

        if (now <= previous) {
            now = previous + 1L;
        }

        preferences.edit()
                .putLong(
                        key,
                        now)
                .apply();
    }

    public static void markProfileDeleted(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                profileId.trim().isEmpty() ||
                ProfileManager.MAIN_ID.equals(
                        profileId) ||
                ProfileManager.isGuest(profileId)) {
            return;
        }

        SharedPreferences preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE);

        JSONObject deleted =
                readDeletedProfiles(
                        preferences);

        long previous =
                deleted.optLong(
                        profileId,
                        0L);

        long now =
                System.currentTimeMillis();

        if (now <= previous) {
            now = previous + 1L;
        }

        try {
            deleted.put(
                    profileId,
                    now);
        } catch (Exception ignored) {
        }

        preferences.edit()
                .putString(
                        KEY_DELETED,
                        deleted.toString())
                .putLong(
                        KEY_PROFILE_PREFIX +
                                profileId,
                        now)
                .apply();
    }

    private static String sync(
            Context context) throws Exception {

        SimpleAccountManager account =
                new SimpleAccountManager(context);

        if (!account.isSignedIn()) {
            throw new Exception(
                    "Sign in to Simple Account first.");
        }

        String token =
                account.getValidIdToken();

        if (token == null ||
                token.trim().isEmpty()) {
            throw new Exception(
                    "Simple Account session expired.");
        }

        try {
            return performSync(
                    context,
                    token,
                    account.getUid());
        } catch (HttpFailure failure) {
            if (failure.statusCode != 401) {
                throw failure;
            }

            try {
                token =
                        account.refreshIdToken();
            } catch (Exception ignored) {
                throw new Exception(
                        "Simple Account session expired.");
            }

            return performSync(
                    context,
                    token,
                    account.getUid());
        }
    }

    private static String performSync(
            Context context,
            String token,
            String uid) throws Exception {

        if (uid == null ||
                uid.trim().isEmpty()) {
            throw new Exception(
                    "Simple Account user is unavailable.");
        }

        Map<String, CloudProfile> cloudProfiles =
                listCloudProfiles(
                        token,
                        uid);

        ProfileManager profileManager =
                new ProfileManager(context);

        List<ProfileManager.Profile> localProfiles =
                profileManager.getProfiles(
                        context);

        Map<String, Boolean> localIds =
                new HashMap<>();

        int uploaded = 0;
        int downloaded = 0;

        /*
         * First reconcile every local persistent profile with its cloud copy.
         * This uploads profiles that exist only locally and imports settings
         * for profiles that already exist in the cloud.
         */
        for (ProfileManager.Profile profile :
                localProfiles) {

            if (profile == null ||
                    profile.id == null ||
                    ProfileManager.isGuest(
                            profile.id)) {
                continue;
            }

            localIds.put(
                    profile.id,
                    true);

            long localModified =
                    getLocalModified(
                            context,
                            profile.id);

            CloudProfile cloud =
                    cloudProfiles.get(
                            profile.id);

            if (cloud == null) {

                long uploadTime =
                        localModified <= 0L
                                ? System.currentTimeMillis()
                                : localModified;

                putCloudProfile(
                        token,
                        uid,
                        context,
                        profile,
                        uploadTime);

                markLocalSynced(
                        context,
                        profile.id,
                        uploadTime);

                uploaded++;
                continue;
            }

            if (cloud.deleted) {

                if (canDeleteLocally(
                        context,
                        profile.id) &&
                        cloud.updatedAt >= localModified) {

                    if (profileManager
                            .deleteSyncedProfile(
                                    context,
                                    profile.id)) {

                        setLocalModified(
                                context,
                                profile.id,
                                cloud.updatedAt);

                        downloaded++;
                    }
                }

                continue;
            }

            if (localModified > cloud.updatedAt) {

                putCloudProfile(
                        token,
                        uid,
                        context,
                        profile,
                        localModified);

                markLocalSynced(
                        context,
                        profile.id,
                        localModified);

                uploaded++;
                continue;
            }

            if (applyCloudProfile(
                    context,
                    profileManager,
                    cloud)) {
                downloaded++;
            }
        }

        /*
         * Now create every cloud profile that does not exist locally.
         * Settings are imported independently of whether the profile was
         * newly created or already existed, so they cannot be skipped.
         */
        SharedPreferences syncPreferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE);

        JSONObject localDeletedProfiles =
                readDeletedProfiles(
                        syncPreferences);

        for (CloudProfile cloud :
                cloudProfiles.values()) {

            if (cloud == null ||
                    cloud.id == null ||
                    cloud.deleted ||
                    localIds.containsKey(
                            cloud.id)) {
                continue;
            }

            long localDeletedAt =
                    localDeletedProfiles.optLong(
                            cloud.id,
                            0L);

            if (localDeletedAt >=
                    cloud.updatedAt) {
                continue;
            }

            if (profileManager
                    .upsertSyncedProfile(
                            context,
                            cloud.id,
                            cloud.name)) {

                downloaded++;
            }

            /*
             * Always import the cloud settings after the profile reconciliation
             * attempt. This also handles an existing profile whose name did
             * not need changing.
             */
            importSettings(
                    context,
                    cloud.id,
                    cloud.settings);

            setLocalModified(
                    context,
                    cloud.id,
                    cloud.updatedAt);
        }

        syncDeletionTombstones(
                context,
                token,
                uid,
                cloudProfiles);

        context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE)
                .edit()
                .putLong(
                        KEY_LAST_SYNC,
                        System.currentTimeMillis())
                .apply();

        return "Profiles synced: " +
                uploaded +
                " uploaded, " +
                downloaded +
                " downloaded.";
    }

    private static void syncDeletionTombstones(
            Context context,
            String token,
            String uid,
            Map<String, CloudProfile> cloudProfiles)
            throws Exception {

        SharedPreferences preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE);

        JSONObject deleted =
                readDeletedProfiles(
                        preferences);

        JSONArray ids =
                deleted.names();

        if (ids == null) {
            return;
        }

        boolean changed = false;

        for (int i = 0;
                i < ids.length();
                i++) {

            String id =
                    ids.optString(
                            i,
                            "");

            if (id.trim().isEmpty()) {
                continue;
            }

            long deletedAt =
                    deleted.optLong(
                            id,
                            0L);

            CloudProfile cloud =
                    cloudProfiles.get(id);

            if (cloud == null ||
                    deletedAt > cloud.updatedAt) {

                putDeletedCloudProfile(
                        token,
                        uid,
                        context,
                        id,
                        deletedAt);

            } else if (cloud.updatedAt >= deletedAt) {

                deleted.remove(id);
                changed = true;
            }
        }

        if (changed) {
            preferences.edit()
                    .putString(
                            KEY_DELETED,
                            deleted.toString())
                    .apply();
        }
    }

    private static boolean applyCloudProfile(
            Context context,
            ProfileManager profileManager,
            CloudProfile cloud) {

        if (cloud == null ||
                cloud.id == null) {
            return false;
        }

        boolean changed =
                profileManager.upsertSyncedProfile(
                        context,
                        cloud.id,
                        cloud.name);

        boolean hadSettings =
                cloud.settings != null &&
                !cloud.settings.trim().isEmpty();

        importSettings(
                context,
                cloud.id,
                cloud.settings);

        setLocalModified(
                context,
                cloud.id,
                cloud.updatedAt);

        return changed || hadSettings;
    }

    private static void applyCloudMainProfile(
            Context context,
            ProfileManager profileManager,
            CloudProfile cloud) {

        if (cloud == null) {
            return;
        }

        profileManager.upsertSyncedProfile(
                context,
                ProfileManager.MAIN_ID,
                cloud.name);

        importSettings(
                context,
                ProfileManager.MAIN_ID,
                cloud.settings);

        setLocalModified(
                context,
                ProfileManager.MAIN_ID,
                cloud.updatedAt);
    }

    private static boolean canDeleteLocally(
            Context context,
            String profileId) {

        return !ProfileManager.MAIN_ID.equals(
                profileId) &&
                !profileId.equals(
                        ProfileManager
                                .getActiveProfileId(
                                        context));
    }

    private static long getLocalModified(
            Context context,
            String profileId) {

        return context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE)
                .getLong(
                        KEY_PROFILE_PREFIX +
                                profileId,
                        0L);
    }

    private static void setLocalModified(
            Context context,
            String profileId,
            long time) {

        if (profileId == null ||
                profileId.trim().isEmpty()) {
            return;
        }

        context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE)
                .edit()
                .putLong(
                        KEY_PROFILE_PREFIX +
                                profileId,
                        Math.max(
                                0L,
                                time))
                .apply();
    }

    private static void markLocalSynced(
            Context context,
            String profileId,
            long time) {

        setLocalModified(
                context,
                profileId,
                time);
    }

    private static void putCloudProfile(
            String token,
            String uid,
            Context context,
            ProfileManager.Profile profile,
            long updatedAt) throws Exception {

        BrowserSettings settings =
                new BrowserSettings(
                        context,
                        profile.id);

        JSONObject fields =
                new JSONObject();

        fields.put(
                "profileId",
                stringValue(profile.id));

        fields.put(
                "name",
                stringValue(profile.name));

        fields.put(
                "settings",
                stringValue(
                        settings.exportSyncJson()));

        fields.put(
                "updatedAt",
                integerValue(updatedAt));

        fields.put(
                "deleted",
                booleanValue(false));

        fields.put(
                "deviceId",
                stringValue(
                        getDeviceId(context)));

        fields.put(
                "schemaVersion",
                integerValue(1L));

        commitDocument(
                token,
                documentPath(
                        uid,
                        profile.id),
                fields);
    }

    private static void putDeletedCloudProfile(
            String token,
            String uid,
            Context context,
            String profileId,
            long updatedAt) throws Exception {

        JSONObject fields =
                new JSONObject();

        fields.put(
                "profileId",
                stringValue(profileId));

        fields.put(
                "name",
                stringValue(""));

        fields.put(
                "settings",
                stringValue(""));

        fields.put(
                "updatedAt",
                integerValue(updatedAt));

        fields.put(
                "deleted",
                booleanValue(true));

        fields.put(
                "deviceId",
                stringValue(
                        getDeviceId(context)));

        fields.put(
                "schemaVersion",
                integerValue(1L));

        commitDocument(
                token,
                documentPath(
                        uid,
                        profileId),
                fields);
    }

    private static Map<String, CloudProfile>
            listCloudProfiles(
                    String token,
                    String uid) throws Exception {

        String url =
                FIRESTORE_BASE +
                "/users/" +
                encode(uid) +
                "/services/simpleBrowser/profiles" +
                "?pageSize=20";

        JSONObject root =
                requestJson(
                        "GET",
                        url,
                        token,
                        null);

        Map<String, CloudProfile> result =
                new HashMap<>();

        JSONArray documents =
                root.optJSONArray(
                        "documents");

        if (documents == null) {
            return result;
        }

        for (int i = 0;
                i < documents.length();
                i++) {

            JSONObject document =
                    documents.optJSONObject(i);

            if (document == null) {
                continue;
            }

            JSONObject fields =
                    document.optJSONObject(
                            "fields");

            if (fields == null) {
                continue;
            }

            String documentName =
                    document.optString(
                            "name",
                            "");

            String fallbackId =
                    documentName.substring(
                            documentName.lastIndexOf(
                                    '/') + 1);

            CloudProfile cloud =
                    new CloudProfile();

            cloud.id =
                    fieldString(
                            fields,
                            "profileId",
                            fallbackId);

            cloud.name =
                    fieldString(
                            fields,
                            "name",
                            "Profile");

            cloud.settings =
                    fieldString(
                            fields,
                            "settings",
                            "");

            cloud.updatedAt =
                    fieldLong(
                            fields,
                            "updatedAt",
                            0L);

            cloud.deleted =
                    fieldBoolean(
                            fields,
                            "deleted",
                            false);

            if (cloud.id != null &&
                    !cloud.id.trim().isEmpty()) {

                result.put(
                        cloud.id,
                        cloud);
            }
        }

        return result;
    }

    private static void commitDocument(
            String token,
            String documentPath,
            JSONObject fields) throws Exception {

        JSONObject update =
                new JSONObject();

        update.put(
                "name",
                documentPath);

        update.put(
                "fields",
                fields);

        JSONObject write =
                new JSONObject();

        write.put(
                "update",
                update);

        JSONArray writes =
                new JSONArray();

        writes.put(write);

        JSONObject body =
                new JSONObject();

        body.put(
                "writes",
                writes);

        requestJson(
                "POST",
                FIRESTORE_BASE +
                        ":commit",
                token,
                body);
    }

    private static JSONObject requestJson(
            String method,
            String url,
            String token,
            JSONObject body) throws Exception {

        HttpURLConnection connection = null;
        OutputStream output = null;

        try {
            connection =
                    (HttpURLConnection)
                            new URL(url)
                                    .openConnection();

            connection.setRequestMethod(
                    method);

            connection.setConnectTimeout(
                    15000);

            connection.setReadTimeout(
                    20000);

            connection.setUseCaches(
                    false);

            connection.setRequestProperty(
                    "Accept",
                    "application/json");

            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " +
                    token);

            if (body != null) {
                connection.setDoOutput(
                        true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8");

                output =
                        connection.getOutputStream();

                output.write(
                        body.toString()
                                .getBytes(
                                        "UTF-8"));

                output.flush();
            }

            int status =
                    connection.getResponseCode();

            InputStream input =
                    status >= 200 &&
                    status < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            String response =
                    readFully(input);

            if (status < 200 ||
                    status >= 300) {

                throw new HttpFailure(
                        status,
                        response);
            }

            if (response == null ||
                    response.trim().isEmpty()) {
                return new JSONObject();
            }

            return new JSONObject(
                    response);

        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Exception ignored) {
                }
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readFully(
            InputStream input) throws Exception {

        if (input == null) {
            return "";
        }

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                input,
                                "UTF-8"));

        StringBuilder result =
                new StringBuilder();

        String line;

        while ((line =
                reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();

        return result.toString();
    }

    private static String documentPath(
            String uid,
            String profileId) throws Exception {

        return "projects/" +
                PROJECT_ID +
                "/databases/(default)/documents/users/" +
                encode(uid) +
                "/services/simpleBrowser/profiles/" +
                encode(profileId);
    }

    private static String encode(
            String value) throws Exception {

        return URLEncoder.encode(
                value == null ? "" : value,
                "UTF-8")
                .replace(
                        "+",
                        "%20");
    }

    private static JSONObject stringValue(
            String value) throws Exception {

        JSONObject result =
                new JSONObject();

        result.put(
                "stringValue",
                value == null
                        ? ""
                        : value);

        return result;
    }

    private static JSONObject integerValue(
            long value) throws Exception {

        JSONObject result =
                new JSONObject();

        result.put(
                "integerValue",
                String.valueOf(value));

        return result;
    }

    private static JSONObject booleanValue(
            boolean value) throws Exception {

        JSONObject result =
                new JSONObject();

        result.put(
                "booleanValue",
                value);

        return result;
    }

    private static String fieldString(
            JSONObject fields,
            String key,
            String fallback) {

        JSONObject field =
                fields.optJSONObject(key);

        if (field == null) {
            return fallback;
        }

        String value =
                field.optString(
                        "stringValue",
                        fallback);

        return value == null
                ? fallback
                : value;
    }

    private static long fieldLong(
            JSONObject fields,
            String key,
            long fallback) {

        JSONObject field =
                fields.optJSONObject(key);

        if (field == null) {
            return fallback;
        }

        String value =
                field.optString(
                        "integerValue",
                        "");

        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean fieldBoolean(
            JSONObject fields,
            String key,
            boolean fallback) {

        JSONObject field =
                fields.optJSONObject(key);

        if (field == null) {
            return fallback;
        }

        return field.optBoolean(
                "booleanValue",
                fallback);
    }

    private static void importSettings(
            Context context,
            String profileId,
            String settingsJson) {

        if (settingsJson == null ||
                settingsJson.trim().isEmpty()) {
            return;
        }

        try {
            BrowserSettings settings =
                    new BrowserSettings(
                            context,
                            profileId);

            settings.importSyncJson(
                    new JSONObject(
                            settingsJson));
        } catch (Exception ignored) {
        }
    }

    private static JSONObject readDeletedProfiles(
            SharedPreferences preferences) {

        String json =
                preferences.getString(
                        KEY_DELETED,
                        "{}");

        try {
            return new JSONObject(
                    json == null ||
                    json.trim().isEmpty()
                            ? "{}"
                            : json);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private static void notifyCallback(
            final Callback callback,
            final boolean success,
            final String message) {

        if (callback == null) {
            return;
        }

        new Handler(
                Looper.getMainLooper())
                .post(
                        new Runnable() {
                            @Override
                            public void run() {
                                callback.onComplete(
                                        success,
                                        message);
                            }
                        });
    }

    private static final class CloudProfile {
        String id;
        String name;
        String settings;
        long updatedAt;
        boolean deleted;
    }

    private static final class HttpFailure
            extends Exception {

        final int statusCode;

        HttpFailure(
                int statusCode,
                String body) {

            super(
                    "HTTP " +
                    statusCode +
                    (body == null ||
                     body.trim().isEmpty()
                            ? ""
                            : ": " + body));

            this.statusCode =
                    statusCode;
        }
    }
}
