package com.example.simplebrowser;

import android.graphics.Color;

/**
 * Shared color helpers for the browser chrome and settings UI.
 *
 * No theme or external color library is required, keeping this usable
 * on Android 4.4 (API 19).
 */
public final class ColorUtils {

    private ColorUtils() {
    }

    public static int parseColor(
            String value,
            int fallback) {

        try {
            return Color.parseColor(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static int getReadableTextColor(
            int background) {

        double luminance =
                (0.299d * Color.red(background)) +
                (0.587d * Color.green(background)) +
                (0.114d * Color.blue(background));

        return luminance >= 160d
                ? Color.rgb(32, 33, 36)
                : Color.WHITE;
    }

    public static int mix(
            int first,
            int second,
            float amount) {

        if (amount < 0f) {
            amount = 0f;
        }

        if (amount > 1f) {
            amount = 1f;
        }

        int red =
                Math.round(
                        Color.red(first) +
                        (Color.red(second) -
                                Color.red(first)) *
                                amount);

        int green =
                Math.round(
                        Color.green(first) +
                        (Color.green(second) -
                                Color.green(first)) *
                                amount);

        int blue =
                Math.round(
                        Color.blue(first) +
                        (Color.blue(second) -
                                Color.blue(first)) *
                                amount);

        return Color.rgb(red, green, blue);
    }

    public static String toHex(
            int color) {

        return String.format(
                "#%02X%02X%02X",
                Color.red(color),
                Color.green(color),
                Color.blue(color));
    }
}
