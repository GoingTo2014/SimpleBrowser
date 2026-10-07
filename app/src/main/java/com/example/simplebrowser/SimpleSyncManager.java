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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final String KEY_PROFILE_DIRTY_PREFIX = "profile_dirty_";
    private static final String KEY_PROFILE_CLOUD_VERSION_PREFIX =
            "profile_cloud_version_";
    private static final String KEY_PROFILE_SNAPSHOT_PREFIX =
            "profile_sync_snapshot_";
    private static final String KEY_DELETED = "deleted_profiles";

    private static final long AUTO_SYNC_INTERVAL_MS =
            5L * 60L * 1000L;

    private static final AtomicBoolean SYNC_RUNNING =
            new AtomicBoolean(false);

    private SimpleSyncManager() {
    }

    public static void syncAsync(
            final Context context,
            final Callback callback) {

        syncAsync(
                context,
                callback,
                false);
    }

    public static void syncAsync(
            final Context context,
            final Callback callback,
            final boolean force) {

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

        if (!force &&
                callback == null &&
                now - lastAttempt <
                        AUTO_SYNC_INTERVAL_MS) {
            return;
        }

        if (!SYNC_RUNNING.compareAndSet(
                false,
                true)) {
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
                        } finally {
                            SYNC_RUNNING.set(false);
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
                .putBoolean(
                        KEY_PROFILE_DIRTY_PREFIX +
                                profileId,
                        true)
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
         * Reconcile every local persistent profile first. Local content is
         * considered dirty by comparing it with the exact snapshot that was
         * last accepted from Firestore. This catches changes even when a
         * setter forgot to call markProfileChanged().
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

            CloudProfile cloud =
                    cloudProfiles.get(
                            profile.id);

            int result =
                    reconcileProfile(
                            context,
                            token,
                            uid,
                            profileManager,
                            profile,
                            cloud);

            if (result == 1) {
                uploaded++;
            } else if (result == 2) {
                downloaded++;
            } else if (result == 3) {
                uploaded++;
                downloaded++;
            }
        }

        /*
         * Create cloud-only persistent profiles locally and import their
         * settings independently of the profile-name reconciliation.
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

            importSettings(
                    context,
                    cloud.id,
                    cloud.settings);

            markLocalSynced(
                    context,
                    cloud.id,
                    0L,
                    cloud.serverVersion);

            saveLocalSnapshot(
                    context,
                    profileManager,
                    cloud.id);
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

    /*
     * Synchronizes only the currently active profile. This is intentionally a
     * single-document read so the active profile can be checked frequently
     * without reading the entire profile collection every few seconds.
     */
    public static void syncActiveProfileAsync(
            final Context context,
            final Callback callback,
            final boolean force) {

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

        if (!force &&
                callback == null &&
                now - lastAttempt <
                        AUTO_SYNC_INTERVAL_MS) {
            return;
        }

        if (!SYNC_RUNNING.compareAndSet(
                false,
                true)) {
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
                                    syncActiveProfile(
                                            appContext);
                            success = true;

                        } catch (Exception exception) {
                            message =
                                    exception.getMessage();

                            if (message == null ||
                                    message.trim().isEmpty()) {
                                message = "Sync failed";
                            }
                        } finally {
                            SYNC_RUNNING.set(false);
                        }

                        notifyCallback(
                                callback,
                                success,
                                message);
                    }
                },
                "SimpleBrowserActiveSync")
                .start();
    }

    private static String syncActiveProfile(
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
            return performActiveProfileSync(
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

            return performActiveProfileSync(
                    context,
                    token,
                    account.getUid());
        }
    }

    private static String performActiveProfileSync(
            Context context,
            String token,
            String uid) throws Exception {

        String activeProfileId =
                ProfileManager.getActiveProfileId(
                        context);

        if (activeProfileId == null ||
                activeProfileId.trim().isEmpty() ||
                ProfileManager.isGuest(
                        activeProfileId)) {
            return "Active profile unchanged.";
        }

        ProfileManager profileManager =
                new ProfileManager(context);

        ProfileManager.Profile profile =
                profileManager.getProfile(
                        context,
                        activeProfileId);

        if (profile == null) {
            return "Active profile unchanged.";
        }

        CloudProfile cloud =
                getCloudProfile(
                        token,
                        uid,
                        activeProfileId);

        int result =
                reconcileProfile(
                        context,
                        token,
                        uid,
                        profileManager,
                        profile,
                        cloud);

        if (result == 0) {
            return "Active profile unchanged.";
        }

        return "Active profile synced.";
    }

    private static int reconcileProfile(
            Context context,
            String token,
            String uid,
            ProfileManager profileManager,
            ProfileManager.Profile profile,
            CloudProfile cloud) throws Exception {

        if (profile == null ||
                profile.id == null ||
                ProfileManager.isGuest(
                        profile.id)) {
            return 0;
        }

        ProfileManager.Profile freshProfile =
                profileManager.getProfile(
                        context,
                        profile.id);

        if (freshProfile != null) {
            profile = freshProfile;
        }

        long localModified =
                getLocalModified(
                        context,
                        profile.id);

        String localSnapshot =
                buildLocalSnapshot(
                        context,
                        profile);

        boolean localDirty =
                isProfileDirty(
                        context,
                        profile.id,
                        localSnapshot);

        if (cloud == null) {

            long uploadTime =
                    localModified <= 0L
                            ? System.currentTimeMillis()
                            : localModified;

            String cloudVersion =
                    putCloudProfile(
                            token,
                            uid,
                            context,
                            profile,
                            uploadTime);

            markLocalSynced(
                    context,
                    profile.id,
                    uploadTime,
                    cloudVersion);

            saveLocalSnapshot(
                    context,
                    profileManager,
                    profile.id);

            return 1;
        }

        if (cloud.deleted) {

            /*
             * An active profile must never disappear underneath the user.
             * When it is deleted remotely, its local copy is uploaded again.
             * Non-active profiles still follow the normal tombstone path.
             */
            if (profile.id.equals(
                    ProfileManager.getActiveProfileId(
                            context))) {

                long uploadTime =
                        localModified <= 0L
                                ? System.currentTimeMillis()
                                : localModified;

                String cloudVersion =
                        putCloudProfile(
                                token,
                                uid,
                                context,
                                profile,
                                uploadTime);

                markLocalSynced(
                        context,
                        profile.id,
                        uploadTime,
                        cloudVersion);

                saveLocalSnapshot(
                        context,
                        profileManager,
                        profile.id);

                return 1;
            }

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

                    clearLocalSnapshot(
                            context,
                            profile.id);

                    return 2;
                }
            }

            return 0;
        }

        String lastCloudVersion =
                getLastCloudVersion(
                        context,
                        profile.id);

        if (localDirty) {

            String baseSnapshot =
                    getLastSnapshot(
                            context,
                            profile.id);

            CloudProfile candidateCloud =
                    cloud;

            /*
             * Use Firestore's current updateTime as a compare-and-swap
             * precondition. This prevents two devices that both read version
             * N from silently overwriting each other.
             */
            for (int attempt = 0;
                    attempt < 3;
                    attempt++) {

                if (candidateCloud == null) {
                    break;
                }

                if (!baseSnapshot.trim().isEmpty() &&
                        !lastCloudVersion.isEmpty() &&
                        !candidateCloud.serverVersion.equals(
                                lastCloudVersion)) {

                    JSONObject merged =
                            mergeProfileSnapshots(
                                    baseSnapshot,
                                    buildLocalSnapshot(
                                            context,
                                            profile),
                                    candidateCloud);

                    String mergedName =
                            merged.optString(
                                    "name",
                                    profile.name);

                    JSONObject mergedSettings =
                            merged.optJSONObject(
                                    "settings");

                    if (mergedSettings == null) {
                        mergedSettings =
                                new JSONObject();
                    }

                    profileManager.upsertSyncedProfile(
                            context,
                            profile.id,
                            mergedName);

                    new BrowserSettings(
                            context,
                            profile.id)
                            .replaceSyncJson(
                                    mergedSettings);

                    profile =
                            profileManager.getProfile(
                                    context,
                                    profile.id);

                    if (profile == null) {
                        return 0;
                    }

                    localSnapshot =
                            buildLocalSnapshot(
                                    context,
                                    profile);
                }

                /*
                 * Refresh one final time before the HTTP write so a local
                 * profile rename is not written from a stale Profile object.
                 */
                ProfileManager.Profile profileBeforeUpload =
                        profileManager.getProfile(
                                context,
                                profile.id);

                if (profileBeforeUpload != null) {
                    profile = profileBeforeUpload;
                    localSnapshot =
                            buildLocalSnapshot(
                                    context,
                                    profile);
                }

                long uploadTime =
                        localModified <= 0L
                                ? System.currentTimeMillis()
                                : localModified;

                try {

                    String cloudVersion =
                            putCloudProfile(
                                    token,
                                    uid,
                                    context,
                                    profile,
                                    uploadTime,
                                    candidateCloud.serverVersion);

                    ProfileManager.Profile afterUploadProfile =
                            profileManager.getProfile(
                                    context,
                                    profile.id);

                    String afterUploadSnapshot =
                            buildLocalSnapshot(
                                    context,
                                    afterUploadProfile);

                    if (!localSnapshot.equals(
                            afterUploadSnapshot)) {

                        /*
                         * A local edit happened while the HTTP write was in
                         * flight. Do not acknowledge it as synced; its
                         * snapshot difference will make the next poll upload
                         * it again.
                         */
                        markProfileChanged(
                                context,
                                profile.id);

                        return 1;
                    }

                    markLocalSynced(
                            context,
                            profile.id,
                            uploadTime,
                            cloudVersion);

                    saveLocalSnapshot(
                            context,
                            profileManager,
                            profile.id);

                    return 1;

                } catch (HttpFailure failure) {

                    if (!isFirestorePreconditionFailure(
                            failure)) {
                        throw failure;
                    }

                    /*
                     * Someone changed the document after our GET. Read the
                     * latest copy and repeat the three-way merge against the
                     * snapshot from which this local edit was made.
                     */
                    if (attempt >= 2) {
                        throw failure;
                    }

                    candidateCloud =
                            getCloudProfile(
                                    token,
                                    uid,
                                    profile.id);

                    if (candidateCloud == null) {
                        break;
                    }
                }
            }

            /*
             * A profile document can disappear between the read and write.
             * In that case, recreate it from the local copy.
             */
            long uploadTime =
                    localModified <= 0L
                            ? System.currentTimeMillis()
                            : localModified;

            String cloudVersion =
                    putCloudProfile(
                            token,
                            uid,
                            context,
                            profile,
                            uploadTime);

            markLocalSynced(
                    context,
                    profile.id,
                    uploadTime,
                    cloudVersion);

            saveLocalSnapshot(
                    context,
                    profileManager,
                    profile.id);

            return 1;
        }

        /*
         * No local edits: accept the cloud copy whenever its Firestore
         * document version differs from our last accepted version.
         */
        if (!cloud.serverVersion.equals(
                lastCloudVersion)) {

            if (applyCloudProfile(
                    context,
                    profileManager,
                    cloud)) {

                markLocalSynced(
                        context,
                        profile.id,
                        localModified,
                        cloud.serverVersion);

                saveLocalSnapshot(
                        context,
                        profileManager,
                        profile.id);

                return 2;
            }

            markLocalSynced(
                    context,
                    profile.id,
                    localModified,
                    cloud.serverVersion);

            saveLocalSnapshot(
                    context,
                    profileManager,
                    profile.id);
        }

        /*
         * Profiles created by older versions can have a cloud version but no
         * local snapshot yet. Treat the cloud copy as the initial baseline
         * rather than uploading an unverified local copy over it.
         */
        else if (getLastSnapshot(
                context,
                profile.id).trim().isEmpty()) {

            applyCloudProfile(
                    context,
                    profileManager,
                    cloud);

            markLocalSynced(
                    context,
                    profile.id,
                    localModified,
                    cloud.serverVersion);

            saveLocalSnapshot(
                    context,
                    profileManager,
                    profile.id);

            return 2;
        }

        return 0;
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

        markLocalSynced(
                context,
                ProfileManager.MAIN_ID,
                0L,
                cloud.serverVersion);
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

    private static boolean isProfileDirty(
            Context context,
            String profileId,
            String localSnapshot) {

        String savedSnapshot =
                getLastSnapshot(
                        context,
                        profileId);

        /*
         * The snapshot is the authoritative local baseline. Do not rely on
         * SharedPreferences dirty flags here because profile processes can
         * have independently cached copies of those preferences.
         *
         * A missing snapshot is not itself a local edit. Existing installs
         * first establish a cloud baseline before local content is treated as
         * divergent.
         */
        if (savedSnapshot == null ||
                savedSnapshot.trim().isEmpty()) {
            return false;
        }

        return !savedSnapshot.equals(
                localSnapshot == null
                        ? ""
                        : localSnapshot);
    }

    private static String buildLocalSnapshot(
            Context context,
            ProfileManager.Profile profile) {

        if (profile == null) {
            return "";
        }

        try {
            JSONObject snapshot =
                    new JSONObject();

            snapshot.put(
                    "name",
                    profile.name == null
                            ? ""
                            : profile.name);

            snapshot.put(
                    "settings",
                    new JSONObject(
                            new BrowserSettings(
                                    context,
                                    profile.id)
                                    .exportSyncJson()));

            return snapshot.toString();

        } catch (Exception ignored) {
            return "";
        }
    }

    private static String getLastSnapshot(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                profileId.trim().isEmpty()) {
            return "";
        }

        return context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE)
                .getString(
                        KEY_PROFILE_SNAPSHOT_PREFIX +
                                profileId,
                        "");
    }

    private static void saveLocalSnapshot(
            Context context,
            ProfileManager profileManager,
            String profileId) {

        if (context == null ||
                profileManager == null ||
                profileId == null ||
                profileId.trim().isEmpty()) {
            return;
        }

        ProfileManager.Profile profile =
                profileManager.getProfile(
                        context,
                        profileId);

        if (profile == null) {
            return;
        }

        String snapshot =
                buildLocalSnapshot(
                        context,
                        profile);

        if (snapshot.trim().isEmpty()) {
            return;
        }

        context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE)
                .edit()
                .putString(
                        KEY_PROFILE_SNAPSHOT_PREFIX +
                                profileId,
                        snapshot)
                .apply();
    }

    private static void clearLocalSnapshot(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                profileId.trim().isEmpty()) {
            return;
        }

        context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE)
                .edit()
                .remove(
                        KEY_PROFILE_SNAPSHOT_PREFIX +
                                profileId)
                .apply();
    }

    private static JSONObject mergeProfileSnapshots(
            String baseSnapshot,
            String localSnapshot,
            CloudProfile cloud) {

        JSONObject base =
                parseSnapshot(baseSnapshot);

        JSONObject local =
                parseSnapshot(localSnapshot);

        JSONObject cloudSettings =
                parseJsonObject(
                        cloud == null
                                ? ""
                                : cloud.settings);

        JSONObject baseSettings =
                base.optJSONObject(
                        "settings");

        if (baseSettings == null) {
            baseSettings =
                    new JSONObject();
        }

        JSONObject localSettings =
                local.optJSONObject(
                        "settings");

        if (localSettings == null) {
            localSettings =
                    new JSONObject();
        }

        String baseName =
                base.optString(
                        "name",
                        "");

        String localName =
                local.optString(
                        "name",
                        "");

        String cloudName =
                cloud == null
                        ? ""
                        : cloud.name;

        boolean localNameChanged =
                !localName.equals(
                        baseName);

        boolean cloudNameChanged =
                !cloudName.equals(
                        baseName);

        String mergedName;

        if (localNameChanged) {
            mergedName = localName;
        } else if (cloudNameChanged) {
            mergedName = cloudName;
        } else {
            mergedName = localName;
        }

        JSONObject mergedSettings =
                new JSONObject();

        Set<String> keys =
                new HashSet<>();

        addJsonKeys(
                keys,
                baseSettings);
        addJsonKeys(
                keys,
                localSettings);
        addJsonKeys(
                keys,
                cloudSettings);

        for (String key : keys) {

            boolean localChanged =
                    !jsonValuesEqual(
                            localSettings,
                            baseSettings,
                            key);

            boolean cloudChanged =
                    !jsonValuesEqual(
                            cloudSettings,
                            baseSettings,
                            key);

            JSONObject source;

            if (localChanged) {
                source = localSettings;
            } else if (cloudChanged) {
                source = cloudSettings;
            } else {
                source = baseSettings;
            }

            if (source.has(key)) {
                try {
                    mergedSettings.put(
                            key,
                            source.get(key));
                } catch (Exception ignored) {
                }
            }
        }

        JSONObject result =
                new JSONObject();

        try {
            result.put(
                    "name",
                    mergedName);

            result.put(
                    "settings",
                    mergedSettings);
        } catch (Exception ignored) {
        }

        return result;
    }

    private static JSONObject parseSnapshot(
            String snapshot) {

        return parseJsonObject(
                snapshot);
    }

    private static JSONObject parseJsonObject(
            String json) {

        if (json == null ||
                json.trim().isEmpty()) {
            return new JSONObject();
        }

        try {
            return new JSONObject(json);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private static void addJsonKeys(
            Set<String> keys,
            JSONObject object) {

        if (keys == null ||
                object == null) {
            return;
        }

        JSONArray names =
                object.names();

        if (names == null) {
            return;
        }

        for (int i = 0;
                i < names.length();
                i++) {

            String name =
                    names.optString(
                            i,
                            "");

            if (!name.trim().isEmpty()) {
                keys.add(name);
            }
        }
    }

    private static boolean jsonValuesEqual(
            JSONObject first,
            JSONObject second,
            String key) {

        boolean firstHas =
                first != null &&
                first.has(key);

        boolean secondHas =
                second != null &&
                second.has(key);

        if (firstHas != secondHas) {
            return false;
        }

        if (!firstHas) {
            return true;
        }

        Object firstValue =
                first.opt(key);

        Object secondValue =
                second.opt(key);

        if (firstValue == null ||
                JSONObject.NULL.equals(
                        firstValue)) {
            return secondValue == null ||
                    JSONObject.NULL.equals(
                            secondValue);
        }

        return firstValue.equals(
                secondValue);
    }

    private static boolean isProfileDirty(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                profileId.trim().isEmpty()) {
            return false;
        }

        return context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE)
                .getBoolean(
                        KEY_PROFILE_DIRTY_PREFIX +
                                profileId,
                        false);
    }

    private static String getLastCloudVersion(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null ||
                profileId.trim().isEmpty()) {
            return "";
        }

        return context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE)
                .getString(
                        KEY_PROFILE_CLOUD_VERSION_PREFIX +
                                profileId,
                        "");
    }

    private static void markLocalSynced(
            Context context,
            String profileId,
            long time,
            String cloudVersion) {

        if (profileId == null ||
                profileId.trim().isEmpty()) {
            return;
        }

        SharedPreferences.Editor editor =
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
                        .putBoolean(
                                KEY_PROFILE_DIRTY_PREFIX +
                                        profileId,
                                false);

        if (cloudVersion != null &&
                !cloudVersion.trim().isEmpty()) {
            editor.putString(
                    KEY_PROFILE_CLOUD_VERSION_PREFIX +
                            profileId,
                    cloudVersion);
        }

        editor.apply();
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

    private static String putCloudProfile(
            String token,
            String uid,
            Context context,
            ProfileManager.Profile profile,
            long updatedAt) throws Exception {

        return putCloudProfile(
                token,
                uid,
                context,
                profile,
                updatedAt,
                "");
    }

    private static String putCloudProfile(
            String token,
            String uid,
            Context context,
            ProfileManager.Profile profile,
            long updatedAt,
            String expectedCloudVersion) throws Exception {

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

        return commitDocument(
                token,
                documentPath(
                        uid,
                        profile.id),
                fields,
                expectedCloudVersion);
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

            CloudProfile cloud =
                    parseCloudDocument(
                            document);

            if (cloud != null) {
                result.put(
                        cloud.id,
                        cloud);
            }
        }

        return result;
    }

    private static CloudProfile getCloudProfile(
            String token,
            String uid,
            String profileId) throws Exception {

        String url =
                FIRESTORE_BASE +
                "/users/" +
                encode(uid) +
                "/services/simpleBrowser/profiles/" +
                encode(profileId);

        try {

            JSONObject document =
                    requestJson(
                            "GET",
                            url,
                            token,
                            null);

            return parseCloudDocument(
                    document);

        } catch (HttpFailure failure) {

            if (failure.statusCode == 404) {
                return null;
            }

            throw failure;
        }
    }

    private static CloudProfile parseCloudDocument(
            JSONObject document) {

        if (document == null) {
            return null;
        }

        JSONObject fields =
                document.optJSONObject(
                        "fields");

        if (fields == null) {
            return null;
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

        cloud.serverVersion =
                document.optString(
                        "updateTime",
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

        if (cloud.id == null ||
                cloud.id.trim().isEmpty()) {
            return null;
        }

        return cloud;
    }

    private static String commitDocument(
            String token,
            String documentPath,
            JSONObject fields) throws Exception {

        return commitDocument(
                token,
                documentPath,
                fields,
                "");
    }

    private static String commitDocument(
            String token,
            String documentPath,
            JSONObject fields,
            String expectedCloudVersion) throws Exception {

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

        if (expectedCloudVersion != null &&
                !expectedCloudVersion.trim().isEmpty()) {

            JSONObject precondition =
                    new JSONObject();

            precondition.put(
                    "updateTime",
                    expectedCloudVersion);

            write.put(
                    "currentDocument",
                    precondition);
        }

        JSONArray writes =
                new JSONArray();

        writes.put(write);

        JSONObject body =
                new JSONObject();

        body.put(
                "writes",
                writes);

        JSONObject response =
                requestJson(
                        "POST",
                        FIRESTORE_BASE +
                                ":commit",
                        token,
                        body);

        JSONArray writeResults =
                response.optJSONArray(
                        "writeResults");

        if (writeResults != null &&
                writeResults.length() > 0) {

            JSONObject first =
                    writeResults.optJSONObject(0);

            if (first != null) {
                String updateTime =
                        first.optString(
                                "updateTime",
                                "");

                if (!updateTime.isEmpty()) {
                    return updateTime;
                }
            }
        }

        return "";
    }

    private static boolean isFirestorePreconditionFailure(
            HttpFailure failure) {

        if (failure == null) {
            return false;
        }

        String message =
                failure.getMessage();

        return failure.statusCode == 400 &&
                message != null &&
                message.contains("FAILED_PRECONDITION");
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

            settings.replaceSyncJson(
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
        String serverVersion = "";
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
