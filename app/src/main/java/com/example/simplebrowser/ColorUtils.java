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

        int darkText =
                Color.rgb(32, 33, 36);

        double whiteContrast =
                getContrastRatio(
                        Color.WHITE,
                        background);

        double darkContrast =
                getContrastRatio(
                        darkText,
                        background);

        return whiteContrast >= darkContrast
                ? Color.WHITE
                : darkText;
    }

    public static double getContrastRatio(
            int first,
            int second) {

        double firstLuminance =
                getRelativeLuminance(first);

        double secondLuminance =
                getRelativeLuminance(second);

        double lighter =
                Math.max(
                        firstLuminance,
                        secondLuminance);

        double darker =
                Math.min(
                        firstLuminance,
                        secondLuminance);

        return (lighter + 0.05d) /
                (darker + 0.05d);
    }

    public static double getRelativeLuminance(
            int color) {

        double red =
                linearize(Color.red(color) / 255d);

        double green =
                linearize(Color.green(color) / 255d);

        double blue =
                linearize(Color.blue(color) / 255d);

        return (0.2126d * red) +
                (0.7152d * green) +
                (0.0722d * blue);
    }

    private static double linearize(
            double channel) {

        if (channel <= 0.03928d) {
            return channel / 12.92d;
        }

        return Math.pow(
                (channel + 0.055d) / 1.055d,
                2.4d);
    }

    public static int ensureContrast(
            int color,
            int background,
            double minimumRatio) {

        if (getContrastRatio(
                color,
                background) >= minimumRatio) {
            return color;
        }

        for (int i = 1; i <= 20; i++) {

            float amount =
                    i / 20f;

            int darker =
                    mix(
                            color,
                            Color.BLACK,
                            amount);

            if (getContrastRatio(
                    darker,
                    background) >= minimumRatio) {
                return darker;
            }
        }

        return getReadableTextColor(
                background);
    }

    public static int darken(
            int color,
            float amount) {

        return mix(
                color,
                Color.BLACK,
                amount);
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
