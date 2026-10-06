package com.example.simplebrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.security.KeyPairGeneratorSpec;

import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/**
 * Encrypted profile-scoped password storage.
 *
 * The database never stores plaintext password fields. Each record is
 * encrypted with AES-CBC and authenticated with HMAC-SHA256. The AES/HMAC
 * key material is itself wrapped by a per-profile RSA key in AndroidKeyStore.
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
        public long created;
        public long updated;
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
    public void onCreate(
            SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE " +
                TABLE +
                " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "encrypted TEXT NOT NULL," +
                "created INTEGER NOT NULL," +
                "updated INTEGER NOT NULL" +
                ")");

        db.execSQL(
                "CREATE INDEX idx_passwords_updated " +
                "ON " +
                TABLE +
                "(updated DESC)");
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

        String payload =
                new JSONObject()
                        .put("site", safe(site))
                        .put("username", safe(username))
                        .put("password", safe(password))
                        .put("note", safe(note))
                        .toString();

        String encrypted =
                encrypt(payload);

        long now =
                System.currentTimeMillis();

        if (id <= 0) {

            android.content.ContentValues values =
                    new android.content.ContentValues();

            values.put(
                    "encrypted",
                    encrypted);

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

            android.content.ContentValues values =
                    new android.content.ContentValues();

            values.put(
                    "encrypted",
                    encrypted);

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

    public synchronized Entry get(
            long id)
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

    public synchronized void delete(
            long id) {

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

        String payload =
                decrypt(encrypted);

        JSONObject object =
                new JSONObject(payload);

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
                new javax.crypto.spec.IvParameterSpec(
                        iv));

        byte[] ciphertext =
                cipher.doFinal(
                        plaintext.getBytes(
                                UTF8));

        byte[] authenticated =
                new byte[
                        iv.length +
                        ciphertext.length];

        System.arraycopy(
                iv,
                0,
                authenticated,
                0,
                iv.length);

        System.arraycopy(
                ciphertext,
                0,
                authenticated,
                iv.length,
                ciphertext.length);

        Mac mac =
                Mac.getInstance(
                        "HmacSHA256");

        mac.init(
                new SecretKeySpec(
                        keys.hmac,
                        "HmacSHA256"));

        byte[] tag =
                mac.doFinal(
                        authenticated);

        return "1:" +
                android.util.Base64.encodeToString(
                        iv,
                        android.util.Base64.NO_WRAP) +
                ":" +
                android.util.Base64.encodeToString(
                        ciphertext,
                        android.util.Base64.NO_WRAP) +
                ":" +
                android.util.Base64.encodeToString(
                        tag,
                        android.util.Base64.NO_WRAP);
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
                android.util.Base64.decode(
                        parts[1],
                        android.util.Base64.DEFAULT);

        byte[] ciphertext =
                android.util.Base64.decode(
                        parts[2],
                        android.util.Base64.DEFAULT);

        byte[] tag =
                android.util.Base64.decode(
                        parts[3],
                        android.util.Base64.DEFAULT);

        byte[] authenticated =
                new byte[
                        iv.length +
                        ciphertext.length];

        System.arraycopy(
                iv,
                0,
                authenticated,
                0,
                iv.length);

        System.arraycopy(
                ciphertext,
                0,
                authenticated,
                iv.length,
                ciphertext.length);

        Mac mac =
                Mac.getInstance(
                        "HmacSHA256");

        mac.init(
                new SecretKeySpec(
                        keys.hmac,
                        "HmacSHA256"));

        byte[] expected =
                mac.doFinal(
                        authenticated);

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
                new javax.crypto.spec.IvParameterSpec(
                        iv));

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

        String wrappedAes =
                preferences.getString(
                        "wrapped_aes",
                        "");

        String wrappedHmac =
                preferences.getString(
                        "wrapped_hmac",
                        "");

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
                            wrap(
                                    generated.aes,
                                    privateKey,
                                    true))
                    .putString(
                            "wrapped_hmac",
                            wrap(
                                    generated.hmac,
                                    privateKey,
                                    false))
                    .apply();

            return generated;
        }

        KeyMaterial result =
                new KeyMaterial();

        result.aes =
                unwrap(
                        wrappedAes,
                        privateKey);

        result.hmac =
                unwrap(
                        wrappedHmac,
                        privateKey);

        return result;
    }

    private String wrap(
            byte[] data,
            PrivateKey unusedPrivateKey,
            boolean usePublicKey)
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

        return android.util.Base64.encodeToString(
                cipher.doFinal(data),
                android.util.Base64.NO_WRAP);
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
                android.util.Base64.decode(
                        encoded,
                        android.util.Base64.DEFAULT));
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
                                        "CN=" +
                                        alias))
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

    private static String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }
}
