package com.example.simplebrowser;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Shows every tab as a large page preview that can be opened or
 * long-pressed to reorder.
 */
public class TabOverviewDialog {

    private static final long DRAG_HOLD_MS = 450L;

    private final MainActivity activity;
    private final PreviewStore previewStore;

    private View draggedView;
    private BrowserTab draggedTab;
    private ScrollView draggedScroll;
    private Runnable dragRunnable;
    private boolean dragging;
    private float dragStartY;

    public TabOverviewDialog(
            MainActivity activity) {

        this.activity = activity;
        this.previewStore =
                new PreviewStore(activity);
    }

    public void show() {

        final Dialog dialog =
                new Dialog(activity);

        LinearLayout root =
                new LinearLayout(activity);

        root.setOrientation(
                LinearLayout.VERTICAL);
        root.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8));

        int background =
                getBackgroundColor();

        root.setBackgroundColor(
                background);

        LinearLayout header =
                new LinearLayout(activity);

        header.setGravity(
                Gravity.CENTER_VERTICAL);

        TextView title =
                new TextView(activity);

        title.setText(
                activity.getTabManager()
                        .isGuestMode()
                        ? Localization.translate(
                                activity,
                                "tab.guest_tabs")
                        : activity.getTabManager()
                                .isIncognitoMode()
                        ? Localization.translate(
                                activity,
                                "tab.incognito_tabs")
                        : Localization.translate(
                                activity,
                                "Tabs"));
        title.setTextSize(20);
        title.setTypeface(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD);
        title.setTextColor(
                getTextColor());

        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1f));

        ImageButton newTab =
                new ImageButton(activity);

        newTab.setImageDrawable(
                new BrowserIconDrawable(
                        BrowserIconDrawable.ADD,
                        getTextColor()));
        newTab.setContentDescription(
                Localization.translate(
                        activity,
                        "New Tab"));
        newTab.setBackgroundColor(
                Color.TRANSPARENT);
        newTab.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8));

        newTab.setOnClickListener(
                v -> {
                    activity.getTabManager()
                            .addCurrentModeTab(
                                    activity
                                            .getBrowserSettings()
                                            .getHomePage());

                    refreshTabList(
                            listHolder,
                            dialog,
                            scrollHolder);
                });

        header.addView(
                newTab,
                new LinearLayout.LayoutParams(
                        dp(44),
                        dp(44)));

        ImageButton done =
                new ImageButton(activity);

        done.setImageDrawable(
                new BrowserIconDrawable(
                        BrowserIconDrawable.CLOSE,
                        getTextColor()));
        done.setContentDescription(
                Localization.translate(
                        activity,
                        "Close"));
        done.setBackgroundColor(
                Color.TRANSPARENT);
        done.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8));
        done.setOnClickListener(
                v -> dialog.dismiss());

        header.addView(
                done,
                new LinearLayout.LayoutParams(
                        dp(44),
                        dp(44)));

        root.addView(
                header);

        final GridLayout list =
                new GridLayout(activity);

        list.setColumnCount(
                getTabColumns());

        list.setUseDefaultMargins(false);

        final ScrollView scroll =
                new ScrollView(activity);

        scroll.addView(list);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1f));

        listHolder = list;
        scrollHolder = scroll;

        refreshTabList(
                list,
                dialog,
                scroll);

        dialog.setContentView(root);
        dialog.show();

        if (dialog.getWindow() != null) {

            dialog.getWindow().setLayout(
                    android.view.ViewGroup
                            .LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup
                            .LayoutParams.MATCH_PARENT);

            dialog.getWindow()
                    .setBackgroundDrawable(
                            new ColorDrawable(
                                    background));
        }
    }

    private GridLayout listHolder;
    private ScrollView scrollHolder;

    private void refreshTabList(
            GridLayout list,
            Dialog dialog,
            ScrollView scroll) {

        cancelDrag();

        list.removeAllViews();

        for (BrowserTab tab :
                activity.getTabManager()
                        .getTabs()) {

            list.addView(
                    createTabCard(
                            tab,
                            dialog,
                            list,
                            scroll));
        }
    }

    private View createTabCard(
            final BrowserTab tab,
            final Dialog dialog,
            final ViewGroup list,
            final ScrollView scroll) {

        final LinearLayout wrapper =
                new LinearLayout(activity);

        wrapper.setOrientation(
                LinearLayout.VERTICAL);

        final LinearLayout card =
                new LinearLayout(activity);

        card.setOrientation(
                LinearLayout.VERTICAL);

        int cardColor =
                tab == activity.getActiveTab()
                        ? ColorUtils.darken(
                                getAccentColor(),
                                0.08f)
                        : getSurfaceColor();

        GradientDrawable cardBackground =
                new GradientDrawable();

        cardBackground.setColor(
                cardColor);

        cardBackground.setCornerRadius(
                dp(12));

        card.setBackground(
                cardBackground);

        int cardWidth =
                getTabCardWidth();

        GridLayout.LayoutParams gridParams =
                new GridLayout.LayoutParams();

        gridParams.width = cardWidth;
        gridParams.height =
                ViewGroup.LayoutParams.WRAP_CONTENT;

        gridParams.setMargins(
                dp(4),
                dp(4),
                dp(4),
                dp(4));

        wrapper.setLayoutParams(
                gridParams);

        wrapper.setPadding(
                0,
                0,
                0,
                dp(4));

        LinearLayout top =
                new LinearLayout(activity);

        top.setGravity(
                Gravity.CENTER_VERTICAL);
        top.setPadding(
                dp(7),
                dp(3),
                dp(3),
                dp(3));

        ImageView favicon =
                new ImageView(activity);

        favicon.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE);

        if (tab.favicon != null &&
                !tab.favicon.isRecycled()) {

            favicon.setImageBitmap(
                    tab.favicon);

        } else {

            favicon.setImageDrawable(
                    new BrowserIconDrawable(
                            BrowserIconDrawable.TABS,
                            getTextColor()));
        }

        top.addView(
                favicon,
                new LinearLayout.LayoutParams(
                        dp(28),
                        dp(34)));

        TextView tabTitle =
                new TextView(activity);

        String title =
                tab.title == null ||
                tab.title.trim().isEmpty()
                        ? Localization.translate(
                                activity,
                                "New Tab")
                        : tab.title;

        tabTitle.setText(title);
        tabTitle.setTextSize(14);
        tabTitle.setSingleLine(true);
        tabTitle.setEllipsize(
                TextUtils.TruncateAt.END);
        tabTitle.setTextColor(
                getTextColor());

        top.addView(
                tabTitle,
                new LinearLayout.LayoutParams(
                        0,
                        dp(34),
                        1f));

        ImageButton close =
                new ImageButton(activity);

        close.setImageDrawable(
                new BrowserIconDrawable(
                        BrowserIconDrawable.CLOSE,
                        getTextColor()));
        close.setContentDescription(
                Localization.translate(
                        activity,
                        "Close tab"));
        close.setBackgroundColor(
                Color.TRANSPARENT);
        close.setPadding(
                dp(7),
                dp(7),
                dp(7),
                dp(7));

        close.setOnClickListener(
                v -> {

                    activity.getTabManager()
                            .closeTab(tab);

                    if (activity.getTabManager()
                            .getTabs()
                            .isEmpty()) {

                        dialog.dismiss();
                        return;
                    }

                    refreshTabList(
                            list,
                            dialog,
                            scroll);
                });

        top.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(40),
                        dp(40)));

        card.addView(
                top,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(40)));

        int previewWidth =
                Math.max(
                        dp(120),
                        cardWidth);

        int previewHeight =
                Math.max(
                        dp(76),
                        Math.round(
                                previewWidth *
                                9f /
                                16f));

        ImageView preview =
                new ImageView(activity);

        preview.setScaleType(
                ImageView.ScaleType.CENTER_CROP);

        Bitmap bitmap =
                createPreview(
                        tab,
                        previewWidth,
                        previewHeight);

        if (bitmap != null) {

            preview.setImageBitmap(
                    bitmap);

            previewStore.save(
                    tab.previewKey,
                    bitmap);

        } else {

            Bitmap saved =
                    previewStore.load(
                            tab.previewKey);

            if (saved != null) {

                preview.setImageBitmap(
                        saved);

            } else {

                preview.setBackgroundColor(
                        ColorUtils.darken(
                                getSurfaceColor(),
                                0.05f));
            }
        }

        card.addView(
                preview,
                new LinearLayout.LayoutParams(
                        -1,
                        previewHeight));

        View spacer =
                new View(activity);

        spacer.setBackgroundColor(
                getBackgroundColor());

        wrapper.addView(
                card,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(40) +
                        previewHeight));

        wrapper.addView(
                spacer,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(5)));

        card.setOnTouchListener(
                (view, event) ->
                        handleCardTouch(
                                tab,
                                wrapper,
                                list,
                                scroll,
                                dialog,
                                event));

        return wrapper;
    }

    private boolean handleCardTouch(
            BrowserTab tab,
            View wrapper,
            LinearLayout list,
            ScrollView scroll,
            Dialog dialog,
            MotionEvent event) {

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                cancelDrag();

                draggedView = wrapper;
                draggedTab = tab;
                draggedScroll = scroll;
                dragging = false;
                dragStartY =
                        event.getRawY();

                dragRunnable =
                        () -> {

                            if (draggedView != wrapper ||
                                    draggedTab != tab) {
                                return;
                            }

                            dragging = true;

                            wrapper.setAlpha(
                                    0.80f);
                            wrapper.setScaleX(
                                    1.02f);
                            wrapper.setScaleY(
                                    1.02f);

                            scroll.requestDisallowInterceptTouchEvent(
                                    true);
                        };

                wrapper.postDelayed(
                        dragRunnable,
                        DRAG_HOLD_MS);

                return true;

            case MotionEvent.ACTION_MOVE:

                if (!dragging) {

                    float dy =
                            event.getRawY() -
                            dragStartY;

                    if (Math.abs(dy) >
                            dp(10)) {
                        cancelDrag();
                    }

                    return true;
                }

                scroll.requestDisallowInterceptTouchEvent(
                        true);

                float rawY =
                        event.getRawY();

                wrapper.setTranslationY(
                        rawY -
                        dragStartY);

                int currentIndex =
                        list.indexOfChild(
                                wrapper);

                int targetIndex =
                        findDropIndex(
                                list,
                                wrapper,
                                rawY);

                if (targetIndex != currentIndex) {

                    activity.getTabManager()
                            .moveTab(
                                    tab,
                                    targetIndex);

                    list.removeView(
                            wrapper);

                    list.addView(
                            wrapper,
                            Math.min(
                                    targetIndex,
                                    list.getChildCount()));

                    wrapper.setTranslationY(0f);

                    dragStartY = rawY;
                }

                autoScroll(
                        scroll,
                        rawY);

                return true;

            case MotionEvent.ACTION_UP:

                if (dragging) {

                    finishDrag();

                } else {

                    cancelDrag();

                    activity.getTabManager()
                            .selectTab(tab);

                    dialog.dismiss();
                }

                return true;

            case MotionEvent.ACTION_CANCEL:

                finishDrag();
                return true;
        }

        return true;
    }

    private int findDropIndex(
            ViewGroup list,
            View dragged,
            float rawY) {

        int count =
                list.getChildCount();

        for (int i = 0;
                i < count;
                i++) {

            View child =
                    list.getChildAt(i);

            if (child == dragged) {
                continue;
            }

            int[] location =
                    new int[2];

            child.getLocationOnScreen(
                    location);

            float midpoint =
                    location[1] +
                    child.getHeight() /
                    2f;

            if (rawY < midpoint) {
                return i;
            }
        }

        return Math.max(
                0,
                count - 1);
    }

    private void autoScroll(
            ScrollView scroll,
            float rawY) {

        int[] location =
                new int[2];

        scroll.getLocationOnScreen(
                location);

        float top =
                location[1];

        float bottom =
                top +
                scroll.getHeight();

        if (rawY < top + dp(52)) {

            scroll.smoothScrollBy(
                    0,
                    -dp(18));

        } else if (rawY >
                bottom - dp(52)) {

            scroll.smoothScrollBy(
                    0,
                    dp(18));
        }
    }

    private void finishDrag() {

        if (draggedView != null) {

            draggedView.setAlpha(
                    1f);

            draggedView.setScaleX(
                    1f);
            draggedView.setScaleY(
                    1f);
            draggedView.setTranslationY(
                    0f);
        }

        if (draggedScroll != null) {
            draggedScroll
                    .requestDisallowInterceptTouchEvent(
                            false);
        }

        draggedView = null;
        draggedTab = null;
        draggedScroll = null;
        dragRunnable = null;
        dragging = false;
    }

    private void cancelDrag() {

        if (dragRunnable != null &&
                draggedView != null) {

            draggedView.removeCallbacks(
                    dragRunnable);
        }

        finishDrag();
    }

    private Bitmap createPreview(
            BrowserTab tab,
            int targetWidth,
            int targetHeight) {

        if (tab == null ||
                !tab.hasLoaded) {
            return null;
        }

        return TabPreview.capture(
                tab.webView,
                targetWidth * 2,
                targetHeight * 2);
    }

    private int getAccentColor() {

        return ColorUtils.parseColor(
                activity.getBrowserSettings()
                        .getAccentColor(),
                Color.WHITE);
    }

    private int getBackgroundColor() {

        BrowserTab active =
                activity.getActiveTab();

        if (active != null &&
                active.isGuest) {
            return Color.WHITE;
        }

        if (active != null &&
                active.isIncognito) {
            return Color.rgb(
                    32,
                    33,
                    36);
        }

        return ColorUtils.mix(
                getAccentColor(),
                Color.WHITE,
                0.94f);
    }

    private int getSurfaceColor() {

        BrowserTab active =
                activity.getActiveTab();

        if (active != null &&
                active.isGuest) {
            return Color.rgb(
                    245,
                    245,
                    245);
        }

        if (active != null &&
                active.isIncognito) {
            return Color.rgb(
                    48,
                    49,
                    52);
        }

        return ColorUtils.mix(
                getAccentColor(),
                Color.WHITE,
                0.90f);
    }

    private int getTabColumns() {

        int widthDp =
                (int) (
                        activity.getResources()
                                .getDisplayMetrics()
                                .widthPixels /
                        activity.getResources()
                                .getDisplayMetrics()
                                .density);

        if (widthDp >= 700) {
            return 3;
        }

        if (widthDp >= 360) {
            return 2;
        }

        return 1;
    }

    private int getTabCardWidth() {

        int columns =
                getTabColumns();

        int screenWidth =
                activity.getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        int rootPadding =
                dp(16);

        int childMargins =
                dp(8) * columns;

        int interColumnGaps =
                dp(8) * Math.max(
                        0,
                        columns - 1);

        return Math.max(
                dp(120),
                (screenWidth -
                        rootPadding -
                        childMargins -
                        interColumnGaps) /
                        columns);
    }

    private int getTextColor() {

        return ColorUtils.getReadableTextColor(
                getBackgroundColor());
    }

    private int dp(int value) {

        return (int) (
                value *
                activity.getResources()
                        .getDisplayMetrics()
                        .density +
                0.5f);
    }
}
