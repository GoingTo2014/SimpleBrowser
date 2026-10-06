package com.example.simplebrowser;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Keeps the browser task visually present while the legacy WebView process
 * is being replaced. It intentionally creates no WebView.
 */
public final class ProfileSwitchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL);

        root.setGravity(
                Gravity.CENTER);

        root.setBackgroundColor(
                Color.WHITE);

        int padding =
                (int) (24 *
                        getResources()
                                .getDisplayMetrics()
                                .density +
                        0.5f);

        root.setPadding(
                padding,
                padding,
                padding,
                padding);

        TextView title =
                new TextView(this);

        title.setText(
                "Simple Browser");

        title.setTextColor(
                Color.DKGRAY);

        title.setTextSize(20);

        title.setGravity(
                Gravity.CENTER);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        -2));

        ProgressBar progress =
                new ProgressBar(this);

        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(
                        -2,
                        -2);

        progressParams.topMargin =
                (int) (18 *
                        getResources()
                                .getDisplayMetrics()
                                .density +
                        0.5f);

        root.addView(
                progress,
                progressParams);

        setContentView(root);
    }
}
