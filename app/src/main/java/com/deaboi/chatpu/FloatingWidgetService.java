package com.deaboi.chatpu;

import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Handler;
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
    private View resizeHandleView;

    private WindowManager.LayoutParams floatingParams;
    private WindowManager.LayoutParams selectionParams;
    private WindowManager.LayoutParams resizeHandleParams;

    private int selectionHeight = 180;

    private final int RESIZE_HANDLE_HEIGHT = 40;

    // Long press settings
    private final Handler handler = new Handler();
    private boolean selectionVisible = false;
    private boolean longPressTriggered = false;

    private float downX;
    private float downY;

    private static FloatingWidgetService instance;

    @Override
    public void onCreate() {

        super.onCreate();

        instance = this;

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        android.util.DisplayMetrics metrics =
                new android.util.DisplayMetrics();

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

        floatingParams.gravity =
                Gravity.TOP | Gravity.END;

        floatingParams.x = 20;
        floatingParams.y = 300;

        windowManager.addView(
                floatingView,
                floatingParams
        );


        // -----------------------------
        // BLUE SELECTION RECTANGLE
        // -----------------------------

        selectionView = new SelectionView();

        selectionParams = new WindowManager.LayoutParams(
                screenWidth - floatingParams.x - 65 - 10,
                selectionHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );

        selectionParams.gravity =
                Gravity.TOP | Gravity.END;

        selectionParams.x =
                floatingParams.x + 10;

        selectionParams.y =
                floatingParams.y
                        + (65 - selectionHeight) / 2;

        windowManager.addView(
                selectionView,
                selectionParams
        );


        // -----------------------------
        // RESIZE HANDLE
        // -----------------------------

        resizeHandleView = new ResizeHandleView();

        resizeHandleParams = new WindowManager.LayoutParams(
                screenWidth - floatingParams.x - 65 - 10,
                RESIZE_HANDLE_HEIGHT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        resizeHandleParams.gravity =
                Gravity.TOP | Gravity.END;

        resizeHandleParams.x =
                selectionParams.x;

        resizeHandleParams.y =
                selectionParams.y
                        + selectionParams.height
                        - RESIZE_HANDLE_HEIGHT / 2;

        windowManager.addView(
                resizeHandleView,
                resizeHandleParams
        );


        // Hide selection initially
        selectionView.setVisibility(View.GONE);
        resizeHandleView.setVisibility(View.GONE);


        // -----------------------------
        // GREEN BUTTON TOUCH
        // -----------------------------

        floatingView.setOnTouchListener(
                new View.OnTouchListener() {

                    private int initialX;
                    private int initialY;

                    private float initialTouchX;
                    private float initialTouchY;

                    private boolean moved = false;

                    private final Runnable longPressRunnable =
                            new Runnable() {

                                @Override
                                public void run() {

                                    if (!moved) {

                                        longPressTriggered = true;

                                        toggleSelection();
                                    }
                                }
                            };


                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event) {

                        switch (event.getAction()) {

                            case MotionEvent.ACTION_DOWN:

                                initialX =
                                        floatingParams.x;

                                initialY =
                                        floatingParams.y;

                                initialTouchX =
                                        event.getRawX();

                                initialTouchY =
                                        event.getRawY();

                                downX =
                                        event.getRawX();

                                downY =
                                        event.getRawY();

                                moved = false;

                                longPressTriggered = false;

                                handler.postDelayed(
                                        longPressRunnable,
                                        600
                                );

                                return true;


                            case MotionEvent.ACTION_MOVE:

                                float dx =
                                        Math.abs(
                                                event.getRawX()
                                                        - downX
                                        );

                                float dy =
                                        Math.abs(
                                                event.getRawY()
                                                        - downY
                                        );

                                if (dx > 10 || dy > 10) {

                                    moved = true;

                                    handler.removeCallbacks(
                                            longPressRunnable
                                    );
                                }


                                // Move green button
                                floatingParams.x =
                                        initialX
                                                + (int) (
                                                initialTouchX
                                                        - event.getRawX()
                                        );

                                floatingParams.y =
                                        initialY
                                                + (int) (
                                                event.getRawY()
                                                        - initialTouchY
                                        );


                                windowManager.updateViewLayout(
                                        floatingView,
                                        floatingParams
                                );


                                // Update blue rectangle position
                                selectionParams.width =
                                        screenWidth
                                                - floatingParams.x
                                                - 65
                                                - 10;

                                selectionParams.x =
                                        floatingParams.x + 10;

                                selectionParams.y =
                                        floatingParams.y
                                                + (65 - selectionHeight) / 2;


                                if (selectionVisible) {

                                    windowManager.updateViewLayout(
                                            selectionView,
                                            selectionParams
                                    );
                                }


                                // Update resize handle
                                resizeHandleParams.width =
                                        selectionParams.width;

                                resizeHandleParams.x =
                                        selectionParams.x;

                                resizeHandleParams.y =
                                        selectionParams.y
                                                + selectionParams.height
                                                - RESIZE_HANDLE_HEIGHT / 2;


                                if (selectionVisible) {

                                    windowManager.updateViewLayout(
                                            resizeHandleView,
                                            resizeHandleParams
                                    );
                                }

                                return true;


                            case MotionEvent.ACTION_UP:

                                handler.removeCallbacks(
                                        longPressRunnable
                                );

                                return true;


                            case MotionEvent.ACTION_CANCEL:

                                handler.removeCallbacks(
                                        longPressRunnable
                                );

                                return true;
                        }

                        return false;
                    }
                }
        );


        // -----------------------------
        // RESIZE HANDLE TOUCH
        // -----------------------------

        resizeHandleView.setOnTouchListener(
                new View.OnTouchListener() {

                    private float initialTouchY;
                    private int initialHeight;

                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event) {

                        switch (event.getAction()) {

                            case MotionEvent.ACTION_DOWN:

                                initialTouchY =
                                        event.getRawY();

                                initialHeight =
                                        selectionParams.height;

                                return true;


                            case MotionEvent.ACTION_MOVE:

                                int newHeight =
                                        initialHeight
                                                + (int) (
                                                event.getRawY()
                                                        - initialTouchY
                                        );


                                if (newHeight < 80) {
                                    newHeight = 80;
                                }

                                if (newHeight > 1000) {
                                    newHeight = 1000;
                                }


                                selectionHeight =
                                        newHeight;

                                selectionParams.height =
                                        newHeight;


                                windowManager.updateViewLayout(
                                        selectionView,
                                        selectionParams
                                );


                                resizeHandleParams.width =
                                        selectionParams.width;

                                resizeHandleParams.x =
                                        selectionParams.x;

                                resizeHandleParams.y =
                                        selectionParams.y
                                                + selectionParams.height
                                                - RESIZE_HANDLE_HEIGHT / 2;


                                windowManager.updateViewLayout(
                                        resizeHandleView,
                                        resizeHandleParams
                                );

                                return true;


                            case MotionEvent.ACTION_UP:

                                return true;
                        }

                        return false;
                    }
                }
        );
    }


    // -----------------------------
    // SHOW / HIDE SELECTION
    // -----------------------------

    private void toggleSelection() {

        selectionVisible =
                !selectionVisible;

        if (selectionVisible) {

            selectionView.setVisibility(
                    View.VISIBLE
            );

            resizeHandleView.setVisibility(
                    View.VISIBLE
            );

        } else {

            selectionView.setVisibility(
                    View.GONE
            );

            resizeHandleView.setVisibility(
                    View.GONE
            );
        }
    }


    // -----------------------------
    // WHATSAPP VISIBILITY
    // -----------------------------

    public static void setWhatsAppVisible(
            boolean visible) {

        if (instance == null ||
                instance.floatingView == null) {
            return;
        }

        instance.floatingView.setVisibility(
                visible
                        ? View.VISIBLE
                        : View.GONE
        );

        // Always hide selection when leaving WhatsApp
        if (!visible) {

            instance.selectionVisible = false;

            instance.selectionView.setVisibility(
                    View.GONE
            );

            instance.resizeHandleView.setVisibility(
                    View.GONE
            );
        }
    }


    // -----------------------------
    // BLUE RECTANGLE
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

            setBackgroundColor(
                    Color.TRANSPARENT
            );
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


    // -----------------------------
    // RESIZE HANDLE
    // -----------------------------

    private class ResizeHandleView extends View {

        private Paint paint;

        public ResizeHandleView() {

            super(FloatingWidgetService.this);

            paint = new Paint();

            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);

            setBackgroundColor(
                    Color.TRANSPARENT
            );
        }


        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            canvas.drawRect(
                    0,
                    getHeight() - 5,
                    getWidth(),
                    getHeight(),
                    paint
            );
        }
    }


    // -----------------------------
    // BIND
    // -----------------------------

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }


    // -----------------------------
    // DESTROY
    // -----------------------------

    @Override
    public void onDestroy() {

        super.onDestroy();

        instance = null;

        handler.removeCallbacksAndMessages(null);

        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }

        if (selectionView != null) {
            windowManager.removeView(selectionView);
        }

        if (resizeHandleView != null) {
            windowManager.removeView(resizeHandleView);
        }
    }
}