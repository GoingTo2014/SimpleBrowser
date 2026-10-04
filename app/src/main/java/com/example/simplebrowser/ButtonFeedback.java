package com.example.simplebrowser;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

/**
 * Transparent button feedback for browser chrome.
 *
 * Android 4.4 has no RippleDrawable, so this uses the same
 * state-based idea without changing the size of the control.
 */
public final class ButtonFeedback {

    private ButtonFeedback() {
    }

    public static StateListDrawable create(
            int iconColor) {

        StateListDrawable states =
                new StateListDrawable();

        states.addState(
                new int[] {
                        -android.R.attr.state_enabled
                },
                transparent());

        states.addState(
                new int[] {
                        android.R.attr.state_pressed
                },
                highlight(iconColor, 38));

        states.addState(
                new int[] {
                        android.R.attr.state_hovered
                },
                highlight(iconColor, 24));

        states.addState(
                new int[] {
                        android.R.attr.state_focused
                },
                highlight(iconColor, 20));

        states.addState(
                new int[] { },
                transparent());

        return states;
    }

    private static GradientDrawable transparent() {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                Color.TRANSPARENT);

        return drawable;
    }

    private static GradientDrawable highlight(
            int color,
            int alpha) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                Color.argb(
                        alpha,
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)));

        drawable.setCornerRadius(6f);

        return drawable;
    }
}
