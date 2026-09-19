package com.deaboi.chatpu;

import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

public class FloatingWidgetService extends Service {

    private WindowManager windowManager;

    private View floatingView;
    private View selectionView;

    private WindowManager.LayoutParams floatingParams;
    private WindowManager.LayoutParams selectionParams;

    // Blue selection rectangle size
    private final int SELECTION_HEIGHT = 180;

    @Override
    public void onCreate() {

        super.onCreate();

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;

        // -----------------------------
        // GREEN FLOATING BUTTON
        // -----------------------------

        floatingView = LayoutInflater.from(this)
                .inflate(R.layout.floating_widget, null);

        floatingParams = new WindowManager.LayoutParams(
                65,
                65,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        floatingParams.gravity = Gravity.TOP | Gravity.END;

        // Initial position of green button
        floatingParams.x = 20;
        floatingParams.y = 300;

        windowManager.addView(floatingView, floatingParams);


        // -----------------------------
        // BLUE SELECTION RECTANGLE
        // -----------------------------

        selectionView = new SelectionView();

        selectionParams = new WindowManager.LayoutParams(
                screenWidth -floatingParams.x-65-10,
                SELECTION_HEIGHT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        selectionParams.gravity = Gravity.TOP | Gravity.END;

        // Put blue rectangle immediately to LEFT of green button
        selectionParams.x =
                floatingParams.x + 10;

        selectionParams.y =
                floatingParams.y + (65 - SELECTION_HEIGHT) / 2;

        windowManager.addView(selectionView, selectionParams);


        // -----------------------------
        // DRAG GREEN BUTTON
        // -----------------------------

        floatingView.setOnTouchListener(new View.OnTouchListener() {

            private int initialX;
            private int initialY;

            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        initialX = floatingParams.x;
                        initialY = floatingParams.y;

                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        return true;


                    case MotionEvent.ACTION_MOVE:

                        floatingParams.x =
                                initialX +
                                (int) (initialTouchX - event.getRawX());

                        floatingParams.y =
                                initialY +
                                (int) (event.getRawY() - initialTouchY);
                        selectionParams.width = screenWidth -floatingParams.x-65-10;
                        selectionParams.x =floatingParams.x+10;
                        selectionParams.y = floatingParams.y + (65-SELECTION_HEIGHT)/2;

                        // Move GREEN button
                        windowManager.updateViewLayout(
                                selectionView,
                                selectionParams
                        );


                        // Keep BLUE rectangle attached
                        selectionParams.x =
                                floatingParams.x + 65 + 10;

                        selectionParams.y =
                                floatingParams.y +
                                (65 - SELECTION_HEIGHT) / 2;


                        windowManager.updateViewLayout(
                                selectionView,
                                selectionParams
                        );

                        return true;


                    case MotionEvent.ACTION_UP:

                        return true;
                }

                return false;
            }
        });
    }


    // -----------------------------
    // BLUE RECTANGLE VIEW
    // -----------------------------

    private class SelectionView extends View {

        private Paint paint;

        public SelectionView() {

            super(FloatingWidgetService.this);

            paint = new Paint();

            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setAntiAlias(true);

            // Transparent inside
            setBackgroundColor(Color.TRANSPARENT);
        }


        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            float padding = 3;

            canvas.drawRect(
                    padding,
                    padding,
                    getWidth() - padding,
                    getHeight() - padding,
                    paint
            );
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }


    @Override
    public void onDestroy() {

        super.onDestroy();

        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }

        if (selectionView != null) {
            windowManager.removeView(selectionView);
        }
    }
}