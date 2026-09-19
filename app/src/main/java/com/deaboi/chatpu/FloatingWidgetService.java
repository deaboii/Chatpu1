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

    public static final String ACTION_SHOW =
            "com.deaboi.chatpu.ACTION_SHOW";

    public static final String ACTION_HIDE =
            "com.deaboi.chatpu.ACTION_HIDE";

    private WindowManager windowManager;

    private View floatingView;
    private View selectionView;
    private View resizeHandleView;

    private WindowManager.LayoutParams floatingParams;
    private WindowManager.LayoutParams selectionParams;
    private WindowManager.LayoutParams resizeHandleParams;

    private int screenWidth;

    private int selectionHeight = 180;

    private static final int FLOATING_SIZE = 65;
    private static final int RESIZE_HANDLE_HEIGHT = 40;

    private final Handler handler = new Handler();

    private boolean selectionVisible = false;

    private float downX;
    private float downY;


    // --------------------------------------------------
    // CREATE SERVICE
    // --------------------------------------------------

    @Override
    public void onCreate() {

        super.onCreate();

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        android.util.DisplayMetrics metrics =
                new android.util.DisplayMetrics();

        windowManager.getDefaultDisplay().getMetrics(metrics);

        screenWidth = metrics.widthPixels;


        // ==================================================
        // GREEN FLOATING BUTTON
        // ==================================================

        floatingView =
                LayoutInflater.from(this)
                        .inflate(
                                R.layout.floating_widget,
                                null
                        );

        floatingParams =
                new WindowManager.LayoutParams(
                        FLOATING_SIZE,
                        FLOATING_SIZE,
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


        // ==================================================
        // BLUE SELECTION RECTANGLE
        // ==================================================

        selectionView =
                new SelectionView();

        selectionParams =
                new WindowManager.LayoutParams(
                        screenWidth
                                - floatingParams.x
                                - FLOATING_SIZE
                                - 10,

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
                        + (FLOATING_SIZE - selectionHeight) / 2;

        windowManager.addView(
                selectionView,
                selectionParams
        );


        // ==================================================
        // RESIZE HANDLE
        // ==================================================

        resizeHandleView =
                new ResizeHandleView();

        resizeHandleParams =
                new WindowManager.LayoutParams(
                        selectionParams.width,
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

        selectionView.setVisibility(
                View.GONE
        );

        resizeHandleView.setVisibility(
                View.GONE
        );


        // ==================================================
        // GREEN BUTTON TOUCH
        // ==================================================

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

                                        toggleSelection();
                                    }
                                }
                            };


                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event) {

                        switch (event.getAction()) {


                            // ----------------------------------
                            // TOUCH DOWN
                            // ----------------------------------

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

                                handler.postDelayed(
                                        longPressRunnable,
                                        600
                                );

                                return true;


                            // ----------------------------------
                            // MOVE
                            // ----------------------------------

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


                                // ----------------------------------
                                // UPDATE BLUE RECTANGLE
                                // ----------------------------------

                                selectionParams.width =
                                        screenWidth
                                                - floatingParams.x
                                                - FLOATING_SIZE
                                                - 10;

                                selectionParams.x =
                                        floatingParams.x + 10;

                                selectionParams.y =
                                        floatingParams.y
                                                + (
                                                FLOATING_SIZE
                                                        - selectionHeight
                                        ) / 2;


                                if (selectionVisible) {

                                    windowManager.updateViewLayout(
                                            selectionView,
                                            selectionParams
                                    );
                                }


                                // ----------------------------------
                                // UPDATE RESIZE HANDLE
                                // ----------------------------------

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


                            // ----------------------------------
                            // TOUCH UP
                            // ----------------------------------

                            case MotionEvent.ACTION_UP:

                                handler.removeCallbacks(
                                        longPressRunnable
                                );

                                return true;


                            // ----------------------------------
                            // CANCEL
                            // ----------------------------------

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


        // ==================================================
        // RESIZE HANDLE TOUCH
        // ==================================================

        resizeHandleView.setOnTouchListener(
                new View.OnTouchListener() {

                    private float initialTouchY;

                    private int initialHeight;


                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event) {

                        switch (event.getAction()) {


                            // ----------------------------------
                            // RESIZE DOWN
                            // ----------------------------------

                            case MotionEvent.ACTION_DOWN:

                                initialTouchY =
                                        event.getRawY();

                                initialHeight =
                                        selectionParams.height;

                                return true;


                            // ----------------------------------
                            // RESIZE MOVE
                            // ----------------------------------

                            case MotionEvent.ACTION_MOVE:

                                int newHeight =
                                        initialHeight
                                                + (int) (
                                                event.getRawY()
                                                        - initialTouchY
                                        );


                                // Minimum height

                                if (newHeight < 80) {

                                    newHeight = 80;
                                }


                                // Maximum height

                                if (newHeight > 1000) {

                                    newHeight = 1000;
                                }


                                selectionHeight =
                                        newHeight;

                                selectionParams.height =
                                        newHeight;


                                // Update rectangle

                                windowManager.updateViewLayout(
                                        selectionView,
                                        selectionParams
                                );


                                // Update handle

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


                            // ----------------------------------
                            // RESIZE UP
                            // ----------------------------------

                            case MotionEvent.ACTION_UP:

                                return true;
                        }

                        return false;
                    }
                }
        );
    }


    // ==================================================
    // SHOW / HIDE SELECTION
    // ==================================================

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


    // ==================================================
    // SERVICE COMMANDS
    // ==================================================

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        if (intent != null) {

            String action =
                    intent.getAction();


            // ----------------------------------
            // SHOW GREEN BUTTON
            // ----------------------------------

            if (ACTION_SHOW.equals(action)) {

                if (floatingView != null) {

                    floatingView.setVisibility(
                            View.VISIBLE
                    );
                }
            }


            // ----------------------------------
            // HIDE EVERYTHING
            // ----------------------------------

            else if (ACTION_HIDE.equals(action)) {

                if (floatingView != null) {

                    floatingView.setVisibility(
                            View.GONE
                    );
                }


                if (selectionView != null) {

                    selectionView.setVisibility(
                            View.GONE
                    );
                }


                if (resizeHandleView != null) {

                    resizeHandleView.setVisibility(
                            View.GONE
                    );
                }


                selectionVisible =
                        false;
            }
        }


        return START_STICKY;
    }


    // ==================================================
    // BLUE SELECTION RECTANGLE
    // ==================================================

    private class SelectionView
            extends View {

        private final Paint paint;


        public SelectionView() {

            super(
                    FloatingWidgetService.this
            );

            paint =
                    new Paint();

            paint.setColor(
                    Color.BLUE
            );

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(
                    5
            );

            paint.setAntiAlias(
                    true
            );

            setBackgroundColor(
                    Color.TRANSPARENT
            );
        }


        @Override
        protected void onDraw(
                Canvas canvas) {

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


    // ==================================================
    // RESIZE HANDLE
    // ==================================================

    private class ResizeHandleView
            extends View {

        private final Paint paint;


        public ResizeHandleView() {

            super(
                    FloatingWidgetService.this
            );

            paint =
                    new Paint();

            paint.setColor(
                    Color.BLUE
            );

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setAntiAlias(
                    true
            );

            setBackgroundColor(
                    Color.TRANSPARENT
            );
        }


        @Override
        protected void onDraw(
                Canvas canvas) {

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


    // ==================================================
    // BIND
    // ==================================================

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent) {

        return null;
    }


    // ==================================================
    // DESTROY
    // ==================================================

    @Override
    public void onDestroy() {

        handler.removeCallbacksAndMessages(
                null
        );


        if (floatingView != null) {

            windowManager.removeView(
                    floatingView
            );

            floatingView = null;
        }


        if (selectionView != null) {

            windowManager.removeView(
                    selectionView
            );

            selectionView = null;
        }


        if (resizeHandleView != null) {

            windowManager.removeView(
                    resizeHandleView
            );

            resizeHandleView = null;
        }


        super.onDestroy();
    }
}

"MessageAccessibilityService.java"

This version detects whether the current accessibility window is WhatsApp and tells the floating service to show/hide the green button.

:::writing{variant="standard" id="74106" title="MessageAccessibilityService.java"}

package com.deaboi.chatpu;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MessageAccessibilityService
        extends AccessibilityService {


    // ==================================================
    // ACCESSIBILITY EVENT
    // ==================================================

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event) {

        if (event.getPackageName() == null) {
            return;
        }


        String packageName =
                event.getPackageName().toString();


        // ==================================================
        // WHATSAPP
        // ==================================================

        if (packageName.equals("com.whatsapp")) {

            showFloatingWidget();

            AccessibilityNodeInfo root =
                    getRootInActiveWindow();

            if (root != null) {

                readNode(root);
            }

        }


        // ==================================================
        // ANY OTHER APP
        // ==================================================

        else {

            hideFloatingWidget();
        }
    }


    // ==================================================
    // SHOW FLOATING WIDGET
    // ==================================================

    private void showFloatingWidget() {

        Intent intent =
                new Intent(
                        this,
                        FloatingWidgetService.class
                );

        intent.setAction(
                FloatingWidgetService.ACTION_SHOW
        );

        startService(intent);
    }


    // ==================================================
    // HIDE FLOATING WIDGET
    // ==================================================

    private void hideFloatingWidget() {

        Intent intent =
                new Intent(
                        this,
                        FloatingWidgetService.class
                );

        intent.setAction(
                FloatingWidgetService.ACTION_HIDE
        );

        startService(intent);
    }


    // ==================================================
    // READ WHATSAPP NODES
    // ==================================================

    private void readNode(
            AccessibilityNodeInfo node) {


        if (node.getText() != null) {

            AccessibilityNodeInfo p1 =
                    node.getParent();

            AccessibilityNodeInfo p2 =
                    p1 != null
                            ? p1.getParent()
                            : null;

            AccessibilityNodeInfo p3 =
                    p2 != null
                            ? p2.getParent()
                            : null;


            Log.d(
                    "WHATSAPP_NODE",

                    "TEXT=" + node.getText()
                            + "\nP1="
                            + (
                            p1 != null
                                    ? p1.toString()
                                    : "null"
                    )
                            + "\nP2="
                            + (
                            p2 != null
                                    ? p2.toString()
                                    : "null"
                    )
                            + "\nP3="
                            + (
                            p3 != null
                                    ? p3.toString()
                                    : "null"
                    )
            );
        }


        // Read child nodes

        for (
                int i = 0;
                i < node.getChildCount();
                i++
        ) {

            AccessibilityNodeInfo child =
                    node.getChild(i);


            if (child != null) {

                readNode(child);
            }
        }
    }


    // ==================================================
    // ACCESSIBILITY INTERRUPTED
    // ==================================================

    @Override
    public void onInterrupt() {

    }
}

Important: if you already have "FloatingWidgetService" running, rebuild the app and restart the services after replacing these files. Don't use Android Studio's “Create method "setWhatsAppVisible"” anymore—the new code doesn't need that method.