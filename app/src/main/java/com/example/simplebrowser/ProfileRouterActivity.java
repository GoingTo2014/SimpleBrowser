package com.example.simplebrowser;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

/**
 * WebView-free entry point used for launcher and external web intents.
 *
 * The router deliberately does not touch any android.webkit API. It selects
 * the active profile first, then starts the process that owns that profile's
 * WebView data directory.
 */
public final class ProfileRouterActivity extends Activity {

    private static final String PROFILE_ID_EXTRA =
            ProfileProcessActivity.EXTRA_PROFILE_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        route();
    }

    private void route() {
        String activeProfileId =
                ProfileManager.getActiveProfileId(this);

        /*
         * Guest sessions are process-local and cannot survive a cold launch.
         * Treat a stale guest marker as Main.
         */
        if (ProfileManager.isGuest(activeProfileId)) {
            ProfileManager.setActiveProfileId(
                    this,
                    ProfileManager.MAIN_ID);
            activeProfileId =
                    ProfileManager.MAIN_ID;
        }

        Class<? extends MainActivity> target =
                ProfileManager.MAIN_ID.equals(activeProfileId)
                        ? MainActivity.class
                        : ProfileProcessActivity.class;

        Intent forward =
                getIntent() == null
                        ? new Intent()
                        : new Intent(getIntent());

        forward.setClass(
                this,
                target);

        if (!ProfileManager.MAIN_ID.equals(
                activeProfileId)) {
            forward.putExtra(
                    PROFILE_ID_EXTRA,
                    activeProfileId);
        }

        forward.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NO_ANIMATION);

        try {
            startActivity(forward);
            overridePendingTransition(0, 0);
        } catch (Throwable error) {
            /*
             * Never leave the launcher trapped on a failed profile handoff.
             * Reset the active profile and retry Main exactly once.
             */
            ProfileManager.setActiveProfileId(
                    this,
                    ProfileManager.MAIN_ID);

            try {
                Intent fallback =
                        new Intent(
                                this,
                                MainActivity.class);

                fallback.setAction(
                        Intent.ACTION_MAIN);

                fallback.addCategory(
                        Intent.CATEGORY_LAUNCHER);

                fallback.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK |
                        Intent.FLAG_ACTIVITY_NO_ANIMATION);

                startActivity(fallback);
                overridePendingTransition(0, 0);
            } catch (Throwable ignored) {
            }
        }

        finish();
    }

}
