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
        "com.deaboi.chatpu.SHOW_WIDGET";

public static final String ACTION_HIDE =
        "com.deaboi.chatpu.HIDE_WIDGET";

public static final String ACTION_REQUEST_CAPTURE =
        "com.deaboi.chatpu.REQUEST_CAPTURE";

public static final String ACTION_CAPTURE_PERMISSION =
        "com.deaboi.chatpu.CAPTURE_PERMISSION";

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

@Override
public void onCreate() {

    super.onCreate();

    windowManager =
            (WindowManager) getSystemService(WINDOW_SERVICE);

    android.util.DisplayMetrics metrics =
            new android.util.DisplayMetrics();

    windowManager.getDefaultDisplay().getMetrics(metrics);

    screenWidth = metrics.widthPixels;

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

    floatingView.setVisibility(View.GONE);
    selectionView.setVisibility(View.GONE);
    resizeHandleView.setVisibility(View.GONE);

    floatingView.setOnTouchListener(
            new View.OnTouchListener() {

                private int initialX;
                private int initialY;

                private float initialTouchX;
                private float initialTouchY;

                private boolean moved = false;
                private boolean longPressed = false;

                private final Runnable longPressRunnable =
                        new Runnable() {

                            @Override
                            public void run() {

                                if (!moved) {

                                    longPressed = true;

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
                            longPressed = false;

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

                            if (!moved && !longPressed) {

                                Intent captureIntent =
                                        new Intent(
                                                FloatingWidgetService.this,
                                                MainActivity.class
                                        );

                                captureIntent.setAction(
                                        ACTION_REQUEST_CAPTURE
                                );

                                captureIntent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                                );

                                startActivity(
                                        captureIntent
                                );
                            }

                            longPressed = false;

                            return true;

                        case MotionEvent.ACTION_CANCEL:

                            handler.removeCallbacks(
                                    longPressRunnable
                            );

                            longPressed = false;

                            return true;
                    }

                    return false;
                }
            }
    );

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

@Override
public int onStartCommand(
        Intent intent,
        int flags,
        int startId) {

    if (intent != null) {

        String action =
                intent.getAction();

        if (ACTION_SHOW.equals(action)) {

            if (floatingView != null) {

                floatingView.setVisibility(
                        View.VISIBLE
                );
            }

        } else if (ACTION_HIDE.equals(action)) {

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

            selectionVisible = false;

        } else if (ACTION_CAPTURE_PERMISSION.equals(action)) {

            int resultCode =
                    intent.getIntExtra(
                            "resultCode",
                            -1
                    );

            Intent resultData =
                    intent.getParcelableExtra(
                            "resultData"
                    );

            if (resultCode == -1
                    || resultData == null) {

                android.util.Log.e(
                        "CHATPU_CAPTURE",
                        "Invalid screen capture permission result"
                );

                return START_STICKY;
            }

            android.util.Log.d(
                    "CHATPU_CAPTURE",
                    "Screen capture permission received"
            );

            android.util.Log.d(
                    "CHATPU_CAPTURE",
                    "Ready for actual screen capture"
            );
        }
    }

    return START_STICKY;
}

private class SelectionView extends View {

    private final Paint paint;

    public SelectionView() {

        super(
                FloatingWidgetService.this
        );

        paint = new Paint();

        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setAntiAlias(true);

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

private class ResizeHandleView extends View {

    private final Paint paint;

    public ResizeHandleView() {

        super(
                FloatingWidgetService.this
        );

        paint = new Paint();

        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        setBackgroundColor(Color.TRANSPARENT);
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

@Nullable
@Override
public IBinder onBind(Intent intent) {

    return null;
}

@Override
public void onDestroy() {

    handler.removeCallbacksAndMessages(null);

    if (floatingView != null) {

        windowManager.removeView(floatingView);
        floatingView = null;
    }

    if (selectionView != null) {

        windowManager.removeView(selectionView);
        selectionView = null;
    }

    if (resizeHandleView != null) {

        windowManager.removeView(resizeHandleView);
        resizeHandleView = null;
    }

    super.onDestroy();
}

}