package com.example.simplebrowser;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class TabStripLayout extends LinearLayout {

    private View draggedChild;

    public TabStripLayout(Context context) {
        super(context);
        setChildrenDrawingOrderEnabled(true);
    }

    public TabStripLayout(
            Context context,
            AttributeSet attrs) {
        super(context, attrs);
        setChildrenDrawingOrderEnabled(true);
    }

    public TabStripLayout(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(
                context,
                attrs,
                defStyleAttr);
        setChildrenDrawingOrderEnabled(true);
    }

    public void setDraggedChild(
            View view) {

        draggedChild = view;
        invalidate();
    }

    public void clearDraggedChild() {

        draggedChild = null;
        invalidate();
    }

    @Override
    protected int getChildDrawingOrder(
            int childCount,
            int drawingPosition) {

        if (draggedChild == null ||
                childCount <= 1) {

            return drawingPosition;
        }

        int draggedIndex =
                indexOfChild(
                        draggedChild);

        if (draggedIndex < 0 ||
                draggedIndex >= childCount) {

            return drawingPosition;
        }

        /*
         * Draw every normal tab in its existing order, but
         * always draw the dragged tab last so it stays above
         * neighboring tabs while crossing them.
         */
        if (drawingPosition ==
                childCount - 1) {

            return draggedIndex;
        }

        if (drawingPosition >=
                draggedIndex) {

            return drawingPosition + 1;
        }

        return drawingPosition;
    }
}
