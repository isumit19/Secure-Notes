package com.android.sumit.securenotes.utils;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by ravi on 21/02/18.
 */

public class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

    public interface ClickListener {
        void onClick(View view, int position);
        void onLongClick(View view, int position);
    }
    private ClickListener clicklistener;
    private GestureDetector gestureDetector;

    public RecyclerTouchListener(Context context, final RecyclerView recycleView, final ClickListener clicklistener) {

        this.clicklistener = clicklistener;

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {

                View childView = recycleView.findChildViewUnder(e.getX(), e.getY());

                if (childView != null && clicklistener != null) {
                    clicklistener.onClick(childView, recycleView.getChildAdapterPosition(childView));
                    return true;
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

                View childView = recycleView.findChildViewUnder(e.getX(), e.getY());

                if (childView != null && clicklistener != null) {
                    clicklistener.onLongClick(childView, recycleView.getChildAdapterPosition(childView));
                }
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        View child = rv.findChildViewUnder(e.getX(), e.getY());
        if (child != null && clicklistener != null && gestureDetector.onTouchEvent(e)) {
            return true;
        }

        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }


}
