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

        ProfileManager.setProcessProfileId(
                profileId);

        super.onCreate(
                savedInstanceState);
    }
}
