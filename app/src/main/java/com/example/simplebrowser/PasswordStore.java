package com.example.simplebrowser;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/**
 * Encrypted profile-scoped password storage.
 *
 * Password records are encrypted with AES-CBC and authenticated with
 * HMAC-SHA256. The vault keys are wrapped by an AndroidKeyStore RSA key.
 * Portable exports use a separate passphrase-derived AES/HMAC key, so an
 * exported file never contains the device's private vault key.
 */
public final class PasswordStore
        extends SQLiteOpenHelper {

    private static final String DATABASE_NAME =
            "passwords.db";

    private static final String KEY_PREFS =
            "password_keys";

    private static final String TABLE =
            "passwords";

    private static final int DATABASE_VERSION = 1;

    private static final Charset UTF8 =
            Charset.forName("UTF-8");

    private final Context context;
    private final String profileId;
    private final SecureRandom secureRandom =
            new SecureRandom();

    private static final class KeyMaterial {
        byte[] aes;
        byte[] hmac;
    }

    public static final class Entry {
        public long id;
        public String site;
        public String username;
        public String password;
        public String note;
        public String faviconBase64;
        public long created;
        public long updated;
    }

    public static final class SiteGroup {
        public String site;
        public String label;
        public String faviconBase64;
        public int count;
    }

    public PasswordStore(Context context) {
        this(
                context,
                ProfileManager.getActiveProfileId(
                        context));
    }

    public PasswordStore(
            Context context,
            String profileId) {

        super(
                context,
                ProfileManager.scopedDatabaseName(
                        DATABASE_NAME,
                        profileId),
                null,
                DATABASE_VERSION);

        this.context =
                context.getApplicationContext();

        this.profileId =
                profileId == null
                        ? ProfileManager.MAIN_ID
                        : profileId;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "encrypted TEXT NOT NULL," +
                "created INTEGER NOT NULL," +
                "updated INTEGER NOT NULL" +
                ")");

        db.execSQL(
                "CREATE INDEX idx_passwords_updated " +
                "ON " + TABLE + "(updated DESC)");
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {
    }

    public synchronized long save(
            long id,
            String site,
            String username,
            String password,
            String note)
            throws Exception {

        return save(
                id,
                site,
                username,
                password,
                note,
                "");
    }

    public synchronized long save(
            long id,
            String site,
            String username,
            String password,
            String note,
            String faviconBase64)
            throws Exception {

        String normalizedSite =
                normalizeSite(site);

        String payload =
                new JSONObject()
                        .put("site", safe(normalizedSite))
                        .put("username", safe(username))
                        .put("password", safe(password))
                        .put("note", safe(note))
                        .put("favicon", safe(faviconBase64))
                        .toString();

        String encrypted =
                encrypt(payload);

        long now =
                System.currentTimeMillis();

        ContentValues values =
                new ContentValues();

        values.put(
                "encrypted",
                encrypted);

        if (id <= 0) {

            values.put(
                    "created",
                    now);

            values.put(
                    "updated",
                    now);

            return getWritableDatabase()
                    .insert(
                            TABLE,
                            null,
                            values);

        } else {

            values.put(
                    "updated",
                    now);

            getWritableDatabase()
                    .update(
                            TABLE,
                            values,
                            "id = ?",
                            new String[] {
                                    String.valueOf(id)
                            });

            return id;
        }
    }

    public synchronized boolean containsCredentials(
            String site,
            String username,
            String password)
            throws Exception {

        String normalized =
                normalizeSite(site);

        List<Entry> entries =
                getEntries();

        for (Entry entry : entries) {
            if (normalized.equals(
                        normalizeSite(entry.site)) &&
                    safe(username).equals(
                        safe(entry.username)) &&
                    safe(password).equals(
                        safe(entry.password))) {
                return true;
            }
        }

        return false;
    }

    public synchronized List<Entry> getEntries()
            throws Exception {

        ArrayList<Entry> result =
                new ArrayList<>();

        Cursor cursor =
                getReadableDatabase()
                        .query(
                                TABLE,
                                new String[] {
                                        "id",
                                        "encrypted",
                                        "created",
                                        "updated"
                                },
                                null,
                                null,
                                null,
                                null,
                                "updated DESC");

        try {
            while (cursor.moveToNext()) {
                Entry entry =
                        decryptEntry(
                                cursor.getLong(0),
                                cursor.getString(1),
                                cursor.getLong(2),
                                cursor.getLong(3));

                if (entry != null) {
                    result.add(entry);
                }
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public synchronized List<SiteGroup> getSiteGroups()
            throws Exception {

        List<Entry> entries =
                getEntries();

        LinkedHashMap<String, SiteGroup> groups =
                new LinkedHashMap<>();

        for (Entry entry : entries) {

            String key =
                    normalizeSite(entry.site);

            SiteGroup group =
                    groups.get(key);

            if (group == null) {
                group = new SiteGroup();
                group.site = key;
                group.label = siteLabel(key);
                group.faviconBase64 =
                        safe(entry.faviconBase64);
                group.count = 0;
                groups.put(key, group);
            }

            group.count++;

            if ((group.faviconBase64 == null ||
                    group.faviconBase64.isEmpty()) &&
                    entry.faviconBase64 != null &&
                    !entry.faviconBase64.isEmpty()) {

                group.faviconBase64 =
                        entry.faviconBase64;
            }
        }

        return new ArrayList<>(
                groups.values());
    }

    public synchronized List<Entry> getEntriesForSite(
            String site)
            throws Exception {

        String normalized =
                normalizeSite(site);

        List<Entry> result =
                new ArrayList<>();

        for (Entry entry : getEntries()) {
            if (normalized.equals(
                    normalizeSite(entry.site))) {
                result.add(entry);
            }
        }

        return result;
    }

    public synchronized Entry get(long id)
            throws Exception {

        Cursor cursor =
                getReadableDatabase()
                        .query(
                                TABLE,
                                new String[] {
                                        "id",
                                        "encrypted",
                                        "created",
                                        "updated"
                                },
                                "id = ?",
                                new String[] {
                                        String.valueOf(id)
                                },
                                null,
                                null,
                                null);

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            return decryptEntry(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getLong(2),
                    cursor.getLong(3));

        } finally {
            cursor.close();
        }
    }

    public synchronized void delete(long id) {
        getWritableDatabase()
                .delete(
                        TABLE,
                        "id = ?",
                        new String[] {
                                String.valueOf(id)
                        });
    }

    public synchronized void clear() {
        getWritableDatabase()
                .delete(
                        TABLE,
                        null,
                        null);
    }

    public synchronized String exportEncrypted(
            String passphrase)
            throws Exception {

        validatePassphrase(passphrase);

        JSONArray array =
                new JSONArray();

        for (Entry entry : getEntries()) {

            array.put(
                    new JSONObject()
                            .put("site", safe(entry.site))
                            .put("username", safe(entry.username))
                            .put("password", safe(entry.password))
                            .put("note", safe(entry.note))
                            .put("favicon", safe(entry.faviconBase64)));
        }

        return encryptPortable(
                array.toString(),
                passphrase);
    }

    public synchronized int importEncrypted(
            String exported,
            String passphrase)
            throws Exception {

        validatePassphrase(passphrase);

        String json =
                decryptPortable(
                        exported,
                        passphrase);

        JSONArray array =
                new JSONArray(json);

        int imported = 0;

        for (int i = 0;
                i < array.length();
                i++) {

            JSONObject object =
                    array.getJSONObject(i);

            String site =
                    object.optString(
                            "site",
                            "");

            String username =
                    object.optString(
                            "username",
                            "");

            String password =
                    object.optString(
                            "password",
                            "");

            if (site.trim().isEmpty() ||
                    password.isEmpty()) {
                continue;
            }

            save(
                    -1,
                    site,
                    username,
                    password,
                    object.optString(
                            "note",
                            ""),
                    object.optString(
                            "favicon",
                            ""));

            imported++;
        }

        return imported;
    }

    public static void deleteCryptoKey(
            Context context,
            String profileId) {

        if (context == null ||
                profileId == null) {
            return;
        }

        String alias =
                getAlias(profileId);

        try {
            KeyStore keyStore =
                    KeyStore.getInstance(
                            "AndroidKeyStore");

            keyStore.load(null);

            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias);
            }
        } catch (Throwable ignored) {
        }

        try {
            context.getSharedPreferences(
                    ProfileManager.scopedPrefsName(
                            KEY_PREFS,
                            profileId),
                    Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
        } catch (Throwable ignored) {
        }
    }

    private Entry decryptEntry(
            long id,
            String encrypted,
            long created,
            long updated)
            throws Exception {

        JSONObject object =
                new JSONObject(
                        decrypt(encrypted));

        Entry entry =
                new Entry();

        entry.id = id;
        entry.site =
                object.optString("site", "");
        entry.username =
                object.optString("username", "");
        entry.password =
                object.optString("password", "");
        entry.note =
                object.optString("note", "");
        entry.faviconBase64 =
                object.optString("favicon", "");
        entry.created = created;
        entry.updated = updated;

        return entry;
    }

    private String encrypt(
            String plaintext)
            throws Exception {

        KeyMaterial keys =
                getKeyMaterial();

        byte[] iv =
                new byte[16];

        secureRandom.nextBytes(iv);

        Cipher cipher =
                Cipher.getInstance(
                        "AES/CBC/PKCS5Padding");

        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(
                        keys.aes,
                        "AES"),
                new IvParameterSpec(iv));

        byte[] ciphertext =
                cipher.doFinal(
                        plaintext.getBytes(
                                UTF8));

        byte[] authenticated =
                join(iv, ciphertext);

        byte[] tag =
                hmac(
                        keys.hmac,
                        authenticated);

        return "1:" +
                encode(iv) +
                ":" +
                encode(ciphertext) +
                ":" +
                encode(tag);
    }

    private String decrypt(
            String value)
            throws Exception {

        if (value == null) {
            throw new Exception(
                    "Encrypted password data is missing");
        }

        String[] parts =
                value.split(":");

        if (parts.length != 4 ||
                !"1".equals(parts[0])) {
            throw new Exception(
                    "Unsupported password encryption format");
        }

        KeyMaterial keys =
                getKeyMaterial();

        byte[] iv =
                Base64.decode(
                        parts[1],
                        Base64.DEFAULT);

        byte[] ciphertext =
                Base64.decode(
                        parts[2],
                        Base64.DEFAULT);

        byte[] tag =
                Base64.decode(
                        parts[3],
                        Base64.DEFAULT);

        byte[] expected =
                hmac(
                        keys.hmac,
                        join(iv, ciphertext));

        if (!MessageDigest.isEqual(
                expected,
                tag)) {

            throw new Exception(
                    "Password data integrity check failed");
        }

        Cipher cipher =
                Cipher.getInstance(
                        "AES/CBC/PKCS5Padding");

        cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(
                        keys.aes,
                        "AES"),
                new IvParameterSpec(iv));

        return new String(
                cipher.doFinal(ciphertext),
                UTF8);
    }

    private KeyMaterial getKeyMaterial()
            throws Exception {

        SharedPreferences preferences =
                context.getSharedPreferences(
                        ProfileManager.scopedPrefsName(
                                KEY_PREFS,
                                profileId),
                        Context.MODE_PRIVATE);

        ensureKeyPair();

        KeyStore keyStore =
                KeyStore.getInstance(
                        "AndroidKeyStore");

        keyStore.load(null);

        PrivateKey privateKey =
                (PrivateKey)
                        keyStore.getKey(
                                getAlias(profileId),
                                null);

        if (privateKey == null) {
            throw new Exception(
                    "Password encryption key is unavailable");
        }

        String wrappedAes =
                preferences.getString(
                        "wrapped_aes",
                        "");

        String wrappedHmac =
                preferences.getString(
                        "wrapped_hmac",
                        "");

        if (wrappedAes.isEmpty() ||
                wrappedHmac.isEmpty()) {

            KeyMaterial generated =
                    new KeyMaterial();

            generated.aes =
                    new byte[32];

            generated.hmac =
                    new byte[32];

            secureRandom.nextBytes(
                    generated.aes);

            secureRandom.nextBytes(
                    generated.hmac);

            preferences.edit()
                    .putString(
                            "wrapped_aes",
                            wrap(generated.aes))
                    .putString(
                            "wrapped_hmac",
                            wrap(generated.hmac))
                    .apply();

            return generated;
        }

        KeyMaterial keys =
                new KeyMaterial();

        keys.aes =
                unwrap(wrappedAes, privateKey);

        keys.hmac =
                unwrap(wrappedHmac, privateKey);

        return keys;
    }

    private String wrap(byte[] data)
            throws Exception {

        KeyStore keyStore =
                KeyStore.getInstance(
                        "AndroidKeyStore");

        keyStore.load(null);

        Certificate certificate =
                keyStore.getCertificate(
                        getAlias(profileId));

        PublicKey publicKey =
                certificate.getPublicKey();

        Cipher cipher =
                Cipher.getInstance(
                        "RSA/ECB/PKCS1Padding");

        cipher.init(
                Cipher.ENCRYPT_MODE,
                publicKey);

        return encode(
                cipher.doFinal(data));
    }

    private byte[] unwrap(
            String encoded,
            PrivateKey privateKey)
            throws Exception {

        Cipher cipher =
                Cipher.getInstance(
                        "RSA/ECB/PKCS1Padding");

        cipher.init(
                Cipher.DECRYPT_MODE,
                privateKey);

        return cipher.doFinal(
                Base64.decode(
                        encoded,
                        Base64.DEFAULT));
    }

    private void ensureKeyPair()
            throws Exception {

        String alias =
                getAlias(profileId);

        KeyStore keyStore =
                KeyStore.getInstance(
                        "AndroidKeyStore");

        keyStore.load(null);

        if (keyStore.containsAlias(alias)) {
            return;
        }

        Calendar start =
                Calendar.getInstance();

        Calendar end =
                Calendar.getInstance();

        end.add(
                Calendar.YEAR,
                30);

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance(
                        "RSA",
                        "AndroidKeyStore");

        generator.initialize(
                new KeyPairGeneratorSpec.Builder(
                        context)
                        .setAlias(alias)
                        .setSubject(
                                new X500Principal(
                                        "CN=" + alias))
                        .setSerialNumber(
                                BigInteger.ONE)
                        .setStartDate(
                                start.getTime())
                        .setEndDate(
                                end.getTime())
                        .setKeySize(2048)
                        .build());

        generator.generateKeyPair();
    }

    private String encryptPortable(
            String plaintext,
            String passphrase)
            throws Exception {

        byte[] salt =
                new byte[16];

        byte[] iv =
                new byte[16];

        secureRandom.nextBytes(salt);
        secureRandom.nextBytes(iv);

        byte[] derived =
                derivePortableKeys(
                        passphrase,
                        salt);

        byte[] aes =
                new byte[16];

        byte[] hmacKey =
                new byte[32];

        System.arraycopy(
                derived,
                0,
                aes,
                0,
                16);

        System.arraycopy(
                derived,
                16,
                hmacKey,
                0,
                32);

        Cipher cipher =
                Cipher.getInstance(
                        "AES/CBC/PKCS5Padding");

        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(
                        aes,
                        "AES"),
                new IvParameterSpec(iv));

        byte[] ciphertext =
                cipher.doFinal(
                        plaintext.getBytes(
                                UTF8));

        byte[] authenticated =
                join(
                        salt,
                        iv,
                        ciphertext);

        byte[] tag =
                hmac(
                        hmacKey,
                        authenticated);

        return "SBPM1|" +
                encode(salt) +
                "|" +
                encode(iv) +
                "|" +
                encode(ciphertext) +
                "|" +
                encode(tag);
    }

    private String decryptPortable(
            String exported,
            String passphrase)
            throws Exception {

        if (exported == null) {
            throw new Exception(
                    "Password export is empty");
        }

        String[] parts =
                exported.trim().split("\\|");

        if (parts.length != 5 ||
                !"SBPM1".equals(parts[0])) {
            throw new Exception(
                    "Unsupported password export format");
        }

        byte[] salt =
                Base64.decode(
                        parts[1],
                        Base64.DEFAULT);

        byte[] iv =
                Base64.decode(
                        parts[2],
                        Base64.DEFAULT);

        byte[] ciphertext =
                Base64.decode(
                        parts[3],
                        Base64.DEFAULT);

        byte[] tag =
                Base64.decode(
                        parts[4],
                        Base64.DEFAULT);

        byte[] derived =
                derivePortableKeys(
                        passphrase,
                        salt);

        byte[] aes =
                new byte[16];

        byte[] hmacKey =
                new byte[32];

        System.arraycopy(
                derived,
                0,
                aes,
                0,
                16);

        System.arraycopy(
                derived,
                16,
                hmacKey,
                0,
                32);

        byte[] expected =
                hmac(
                        hmacKey,
                        join(
                                salt,
                                iv,
                                ciphertext));

        if (!MessageDigest.isEqual(
                expected,
                tag)) {

            throw new Exception(
                    "Password export integrity check failed");
        }

        Cipher cipher =
                Cipher.getInstance(
                        "AES/CBC/PKCS5Padding");

        cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(
                        aes,
                        "AES"),
                new IvParameterSpec(iv));

        return new String(
                cipher.doFinal(ciphertext),
                UTF8);
    }

    private byte[] derivePortableKeys(
            String passphrase,
            byte[] salt)
            throws Exception {

        PBEKeySpec spec =
                new PBEKeySpec(
                        passphrase.toCharArray(),
                        salt,
                        60000,
                        384);

        SecretKeyFactory factory =
                SecretKeyFactory.getInstance(
                        "PBKDF2WithHmacSHA1");

        SecretKey key =
                factory.generateSecret(spec);

        return key.getEncoded();
    }

    private static byte[] hmac(
            byte[] key,
            byte[] data)
            throws Exception {

        Mac mac =
                Mac.getInstance(
                        "HmacSHA256");

        mac.init(
                new SecretKeySpec(
                        key,
                        "HmacSHA256"));

        return mac.doFinal(data);
    }

    private static byte[] join(
            byte[]... values) {

        int length = 0;

        for (byte[] value : values) {
            length += value.length;
        }

        byte[] result =
                new byte[length];

        int offset = 0;

        for (byte[] value : values) {
            System.arraycopy(
                    value,
                    0,
                    result,
                    offset,
                    value.length);
            offset += value.length;
        }

        return result;
    }

    private static String encode(byte[] data) {
        return Base64.encodeToString(
                data,
                Base64.NO_WRAP);
    }

    private static String normalizeSite(String site) {

        if (site == null ||
                site.trim().isEmpty()) {
            return "";
        }

        String value =
                site.trim();

        try {
            Uri uri =
                    Uri.parse(value);

            String scheme =
                    uri.getScheme();

            String authority =
                    uri.getAuthority();

            if (scheme != null &&
                    authority != null &&
                    ("http".equalsIgnoreCase(scheme) ||
                     "https".equalsIgnoreCase(scheme))) {

                return scheme.toLowerCase() +
                        "://" +
                        authority.toLowerCase();
            }

        } catch (Throwable ignored) {
        }

        return value;
    }

    private static String siteLabel(String site) {

        try {
            Uri uri =
                    Uri.parse(site);

            if (uri.getHost() != null &&
                    !uri.getHost().isEmpty()) {
                return uri.getHost();
            }
        } catch (Throwable ignored) {
        }

        return site;
    }

    private static String getAlias(
            String profileId) {

        String safe =
                profileId == null
                        ? ProfileManager.MAIN_ID
                        : profileId.replaceAll(
                                "[^A-Za-z0-9_]",
                                "_");

        return "SimpleBrowserPassword_" +
                safe;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void validatePassphrase(
            String passphrase)
            throws Exception {

        if (passphrase == null ||
                passphrase.length() < 6) {
            throw new Exception(
                    "The export password must be at least 6 characters long");
        }
    }
}
