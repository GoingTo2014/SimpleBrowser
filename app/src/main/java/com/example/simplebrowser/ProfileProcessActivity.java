package com.example.simplebrowser;

import android.content.Intent;
import android.os.Bundle;

/**
 * Base Activity for persistent profile processes.
 *
 * Android starts each profile in a dedicated process. The profile ID is
 * carried by the Activity intent so a reused Android process never has to
 * consult another process's cached SharedPreferences to discover its identity.
 */
public abstract class ProfileProcessActivity
        extends MainActivity {

    public static final String EXTRA_PROFILE_ID =
            "simplebrowser.profile_id";

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        Intent intent =
                getIntent();

        String profileId =
                intent == null
                        ? null
                        : intent.getStringExtra(
                                EXTRA_PROFILE_ID);

        /*
         * A dedicated profile process must never fall through to MainActivity
         * without an explicit persistent profile identity. Doing so could
         * make it initialize WebView against the default directory.
         */
        if (profileId == null ||
                profileId.trim().isEmpty() ||
                ProfileManager.MAIN_ID.equals(
                        profileId) ||
                ProfileManager.isGuest(profileId)) {
            finish();
            return;
        }

        profileId =
                profileId.trim();

        /*
         * Do not start a deleted/stale profile. Return to the normal launcher
         * path so the router can recover the app to Main.
         */
        if (!ProfileManager.isKnownPersistentProfile(
                this,
                profileId)) {

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

                fallback.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK |
                        Intent.FLAG_ACTIVITY_NO_ANIMATION);

                startActivity(fallback);
            } catch (Throwable ignored) {
            }

            finish();
            return;
        }

        ProfileManager.setProcessProfileId(
                profileId);

        ProfileProcessRuntime.markStarted(
                this,
                profileId);

        super.onCreate(
                savedInstanceState);
    }
}
