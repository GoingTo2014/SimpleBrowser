package com.example.simplebrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.RSAKeyGenParameterSpec;

import javax.crypto.Cipher;

/**
 * Native Simple Account authentication/session manager.
 *
 * Uses Firebase Authentication's REST API so the browser keeps its
 * Android 4.4/API 19 compatibility and does not require the modern
 * Firebase Android SDK.
 */
public final class SimpleAccountManager {

    public static final String ACCOUNT_SITE =
            "https://goingto2014.github.io/SimpleAccount/";

    private static final String API_KEY =
            "AIzaSyCvzguZnR0UzwuYSTiNEtrzAPvU1eYOZQ8";

    private static final String AUTH_BASE =
            "https://identitytoolkit.googleapis.com/v1/accounts:";

    private static final String TOKEN_BASE =
            "https://securetoken.googleapis.com/v1/token";

    private static final String PREFS =
            "simple_account";

    private static final String KEY_UID =
            "uid";

    private static final String KEY_EMAIL =
            "email";

    private static final String KEY_ID_TOKEN =
            "id_token";

    private static final String KEY_EXPIRES_AT =
            "expires_at";

    private static final String KEY_REFRESH_TOKEN =
            "refresh_token";

    private static final String KEY_ENCRYPTED_REFRESH =
            "encrypted_refresh_token";

    private static final String KEYSTORE =
            "AndroidKeyStore";

    private static final String KEY_ALIAS =
            "SimpleAccountRefreshKey";

    private static final long REFRESH_SKEW_MS =
            60L * 1000L;

    private final Context context;
    private final SharedPreferences preferences;

    public SimpleAccountManager(Context context) {
        this.context =
                context.getApplicationContext();

        preferences =
                this.context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE);
    }

    public synchronized boolean isSignedIn() {
        return getUid() != null &&
                getUid().trim().length() > 0 &&
                getRefreshToken() != null;
    }

    public synchronized String getUid() {
        return preferences.getString(
                KEY_UID,
                null);
    }

    public synchronized String getEmail() {
        return preferences.getString(
                KEY_EMAIL,
                null);
    }

    public synchronized String getIdToken() {
        return preferences.getString(
                KEY_ID_TOKEN,
                null);
    }

    private synchronized String getRefreshToken() {
        String encrypted =
                preferences.getString(
                        KEY_ENCRYPTED_REFRESH,
                        null);

        if (encrypted != null &&
                encrypted.length() > 0) {
            return decryptRefreshToken(
                    encrypted);
        }

        /*
         * Compatibility fallback for a session written by an early build.
         * New sessions always use the encrypted value.
         */
        return preferences.getString(
                KEY_REFRESH_TOKEN,
                null);
    }

    private synchronized long getExpiresAt() {
        return preferences.getLong(
                KEY_EXPIRES_AT,
                0L);
    }

    public synchronized void saveSession(
            String uid,
            String email,
            String idToken,
            String refreshToken,
            long expiresInSeconds) {

        if (uid == null ||
                idToken == null ||
                refreshToken == null) {
            return;
        }

        long expiresAt =
                System.currentTimeMillis() +
                Math.max(
                        60L,
                        expiresInSeconds) *
                        1000L;

        SharedPreferences.Editor editor =
                preferences.edit()
                        .putString(
                                KEY_UID,
                                uid)
                        .putString(
                                KEY_EMAIL,
                                email == null
                                        ? ""
                                        : email)
                        .putString(
                                KEY_ID_TOKEN,
                                idToken)
                        .putLong(
                                KEY_EXPIRES_AT,
                                expiresAt);

        String encrypted =
                encryptRefreshToken(
                        refreshToken);

        if (encrypted != null) {
            editor.remove(
                        KEY_REFRESH_TOKEN)
                    .putString(
                        KEY_ENCRYPTED_REFRESH,
                        encrypted);
        } else {
            /*
             * The refresh token remains app-private if the pre-23 Android
             * keystore implementation is unavailable.
             */
            editor.putString(
                    KEY_REFRESH_TOKEN,
                    refreshToken)
                .remove(
                    KEY_ENCRYPTED_REFRESH);
        }

        editor.commit();
    }

    public synchronized void signOut() {
        preferences.edit()
                .clear()
                .commit();
    }

    /**
     * Returns a still-valid ID token, refreshing it synchronously when
     * necessary. Call this from a background thread.
     */
    public String getValidIdToken()
            throws Exception {

        String token =
                getIdToken();

        long expiresAt =
                getExpiresAt();

        if (token != null &&
                token.length() > 0 &&
                expiresAt >
                        System.currentTimeMillis() +
                        REFRESH_SKEW_MS) {
            return token;
        }

        return refreshIdToken();
    }

    /**
     * Firebase refresh-token exchange.
     */
    public synchronized String refreshIdToken()
            throws Exception {

        String refreshToken =
                getRefreshToken();

        if (refreshToken == null ||
                refreshToken.length() == 0) {
            throw new Exception(
                    "No Simple Account session is available.");
        }

        byte[] body =
                ("grant_type=refresh_token" +
                 "&refresh_token=" +
                 urlEncode(refreshToken))
                        .getBytes("UTF-8");

        HttpURLConnection connection =
                (HttpURLConnection)
                        new URL(
                                TOKEN_BASE +
                                "?key=" +
                                urlEncode(API_KEY))
                        .openConnection();

        connection.setRequestMethod(
                "POST");

        connection.setConnectTimeout(
                15000);

        connection.setReadTimeout(
                15000);

        connection.setDoOutput(true);

        connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded");

        OutputStream output =
                connection.getOutputStream();

        output.write(body);
        output.flush();
        output.close();

        String response =
                readResponse(
                        connection);

        int code =
                connection.getResponseCode();

        connection.disconnect();

        if (code < 200 ||
                code >= 300) {

            throw new Exception(
                    firebaseError(
                            response));
        }

        JSONObject json =
                new JSONObject(response);

        String newToken =
                json.optString(
                        "id_token",
                        "");

        String newRefresh =
                json.optString(
                        "refresh_token",
                        "");

        String uid =
                json.optString(
                        "user_id",
                        getUid());

        String email =
                getEmail();

        long expiresIn =
                parseLong(
                        json.optString(
                                "expires_in",
                                "3600"),
                        3600L);

        if (newRefresh.length() == 0) {
            newRefresh =
                    refreshToken;
        }

        saveSession(
                uid,
                email,
                newToken,
                newRefresh,
                expiresIn);

        return newToken;
    }

    private String encryptRefreshToken(
            String refreshToken) {

        if (refreshToken == null ||
                refreshToken.length() == 0) {
            return null;
        }

        try {
            KeyStore store =
                    KeyStore.getInstance(
                            KEYSTORE);

            store.load(
                    null);

            ensureRsaKey();

            PrivateKey privateKey =
                    ((KeyStore.PrivateKeyEntry)
                            store.getEntry(
                                    KEY_ALIAS,
                                    null))
                            .getPrivateKey();

            java.security.Certificate certificate =
                    store.getCertificate(
                            KEY_ALIAS);

            if (certificate == null) {
                return null;
            }

            Cipher cipher =
                    Cipher.getInstance(
                            "RSA/ECB/PKCS1Padding");

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    certificate.getPublicKey());

            byte[] encrypted =
                    cipher.doFinal(
                            refreshToken.getBytes(
                                    "UTF-8"));

            return Base64.encodeToString(
                    encrypted,
                    Base64.NO_WRAP);

        } catch (Throwable ignored) {
            return null;
        }
    }

    private String decryptRefreshToken(
            String encryptedText) {

        try {
            byte[] encrypted =
                    Base64.decode(
                            encryptedText,
                            Base64.NO_WRAP);

            KeyStore store =
                    KeyStore.getInstance(
                            KEYSTORE);

            store.load(
                    null);

            if (!store.containsAlias(
                    KEY_ALIAS)) {
                return null;
            }

            KeyStore.PrivateKeyEntry entry =
                    (KeyStore.PrivateKeyEntry)
                            store.getEntry(
                                    KEY_ALIAS,
                                    null);

            if (entry == null) {
                return null;
            }

            Cipher cipher =
                    Cipher.getInstance(
                            "RSA/ECB/PKCS1Padding");

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    entry.getPrivateKey());

            return new String(
                    cipher.doFinal(encrypted),
                    "UTF-8");

        } catch (Throwable ignored) {
            return null;
        }
    }

    private void ensureRsaKey()
            throws Exception {

        KeyStore store =
                KeyStore.getInstance(
                        KEYSTORE);

        store.load(null);

        if (store.containsAlias(
                KEY_ALIAS)) {
            return;
        }

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance(
                        "RSA",
                        KEYSTORE);

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M) {

            /*
             * The legacy KeyStore-compatible path below is retained for
             * API 19-22. Modern devices can still use it because the
             * KeyPairGeneratorSpec path is accepted by AndroidKeyStore.
             */
        }

        android.security.KeyPairGeneratorSpec.Builder builder =
                new android.security.KeyPairGeneratorSpec.Builder(
                        context)
                        .setAlias(KEY_ALIAS)
                        .setSubject(
                                new javax.security.auth.x500.X500Principal(
                                        "CN=Simple Account"))
                        .setSerialNumber(
                                java.math.BigInteger.ONE)
                        .setStartDate(
                                new java.util.Date(
                                        System.currentTimeMillis() -
                                        1000L))
                        .setEndDate(
                                new java.util.Date(
                                        System.currentTimeMillis() +
                                        10L * 365L * 24L * 60L * 60L * 1000L));

        generator.initialize(
                builder.build());

        generator.generateKeyPair();
    }

    private static long parseLong(
            String value,
            long fallback) {

        try {
            return Long.parseLong(value);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static String urlEncode(
            String value)
            throws Exception {

        return java.net.URLEncoder.encode(
                value,
                "UTF-8");
    }

    private static String readResponse(
            HttpURLConnection connection)
            throws Exception {

        InputStream input = null;

        try {
            if (connection.getResponseCode()
                    >= 400) {
                input =
                        connection.getErrorStream();
            } else {
                input =
                        connection.getInputStream();
            }

            if (input == null) {
                return "";
            }

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

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

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static String firebaseError(
            String response) {

        try {
            JSONObject json =
                    new JSONObject(
                            response);

            JSONObject error =
                    json.optJSONObject(
                            "error");

            String message =
                    error == null
                            ? ""
                            : error.optString(
                                    "message",
                                    "");

            if ("INVALID_REFRESH_TOKEN".equals(
                    message) ||
                    "TOKEN_EXPIRED".equals(
                    message) ||
                    "USER_DISABLED".equals(
                    message)) {
                return
                        "Your Simple Account session has expired. Please sign in again.";
            }

        } catch (Throwable ignored) {
        }

        return
                "Simple Account authentication failed.";
    }
}
