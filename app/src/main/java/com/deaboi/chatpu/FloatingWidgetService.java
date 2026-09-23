package com.deaboi.chatpu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteBuffer;

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

    private int screenHeightFull;
    private int screenDensity;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;

    @Override
    public void onCreate() {

        super.onCreate();

        android.util.Log.d(
                "CHATPU_CAPTURE",
                "FloatingWidgetService.onCreate() — new instance, "
                        + "mediaProjection is null at this point by definition"
        );

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        // NOTE: do not use windowManager.getDefaultDisplay() here — a Service
        // is not a "visual context" and that call throws
        // UnsupportedOperationException on Android 30+. getResources() works
        // from any context type.
        android.util.DisplayMetrics metrics =
                getResources().getDisplayMetrics();

        screenWidth = metrics.widthPixels;
        screenHeightFull = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        mediaProjectionManager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

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

        floatingView.setVisibility(View.VISIBLE);
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

                                    // --- POC: WhatsApp pre-filled text test ---
                                    // Purely additive — fires alongside whatever
                                    // the tap already does below. Safe to remove
                                    // this one call to revert with zero side
                                    // effects on the capture/permission flow.
                                    openWhatsAppWithPrefilledText();

                                    if (mediaProjection != null) {

                                        // Already have permission from a
                                        // previous tap — capture directly,
                                        // no dialog, no other activity.
                                        performCapture();

                                    } else {

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

    // --- POC constants: WhatsApp pre-filled text test ---
    // Phone number in international format, digits only, no "+".
    private static final String POC_TEST_PHONE = "919777535210";
    private static final String POC_TEST_MESSAGE = "hello world";

    /**
     * POC: opens the WhatsApp chat for POC_TEST_PHONE with POC_TEST_MESSAGE
     * already typed into the compose box (not sent — the user still has to
     * tap send). Uses the standard wa.me "click to chat" deep link, so it
     * does not depend on WhatsApp's internal view hierarchy and will not
     * conflict with anything the accessibility service or floating widget
     * already does.
     */
    private void openWhatsAppWithPrefilledText() {

        try {

            String url =
                    "https://wa.me/"
                            + POC_TEST_PHONE
                            + "?text="
                            + Uri.encode(POC_TEST_MESSAGE);

            Intent whatsappIntent =
                    new Intent(Intent.ACTION_VIEW);

            whatsappIntent.setData(
                    Uri.parse(url)
            );

            whatsappIntent.setPackage("com.whatsapp");

            whatsappIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(whatsappIntent);

            android.util.Log.d(
                    "CHATPU_POC",
                    "Opened WhatsApp chat with pre-filled text for "
                            + POC_TEST_PHONE
            );

        } catch (Exception e) {

            android.util.Log.e(
                    "CHATPU_POC",
                    "Failed to open WhatsApp with pre-filled text",
                    e
            );

            Toast.makeText(
                    this,
                    "Couldn't open WhatsApp: " + e.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        }
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

    // Called ONCE, right after mediaProjection is granted. Creates a
// VirtualDisplay that stays alive for the rest of the service's life —
// do NOT release this between captures. On many Android 14+ devices,
// releasing the VirtualDisplay silently invalidates the whole
// MediaProjection (fires onStop()), which is exactly what was causing
// the permission dialog to reappear on every tap.
    private void setupCaptureSurface() {

        if (mediaProjection == null || imageReader != null) {
            return;
        }

        imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeightFull,
                PixelFormat.RGBA_8888,
                2
        );

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ChatpuCapture",
                screenWidth,
                screenHeightFull,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                handler
        );
    }

    // Called on every tap once the surface is live. Grabs whatever frame
// is currently queued — the virtual display has been continuously
// producing frames in the background since setupCaptureSurface() ran,
// so a recent one is normally ready immediately. A short delay covers
// the rare case where nothing has been queued yet.
    private void performCapture() {

        if (mediaProjection == null || imageReader == null) {
            return;
        }

        handler.postDelayed(this::grabQueuedFrame, 150);
    }

    private void grabQueuedFrame() {

        Image image = imageReader.acquireLatestImage();

        if (image == null) {

            Toast.makeText(
                    this,
                    "No frame ready yet — try tapping again",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Bitmap fullBitmap = imageToBitmap(image);

        image.close();

        if (fullBitmap == null) {
            return;
        }

        Bitmap cropped = cropToSelection(fullBitmap);

        saveCroppedBitmap(cropped);
    }

    private Bitmap imageToBitmap(Image image) {

        Image.Plane plane = image.getPlanes()[0];

        ByteBuffer buffer = plane.getBuffer();

        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * screenWidth;

        Bitmap raw = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeightFull,
                Bitmap.Config.ARGB_8888
        );

        raw.copyPixelsFromBuffer(buffer);

        return Bitmap.createBitmap(raw, 0, 0, screenWidth, screenHeightFull);
    }

    private Bitmap cropToSelection(Bitmap fullBitmap) {

        // Read the selection box's real on-screen position instead of
        // recomputing it from LayoutParams gravity math — that manual
        // math didn't account for the status bar inset, which is why the
        // crop was landing a bit above the actual blue box.
        int[] location = new int[2];
        selectionView.getLocationOnScreen(location);

        int left = location[0];
        int top = location[1];
        int width = selectionView.getWidth();
        int height = selectionView.getHeight();

        left = Math.max(0, left);
        top = Math.max(0, top);
        width = Math.min(width, fullBitmap.getWidth() - left);
        height = Math.min(height, fullBitmap.getHeight() - top);

        if (width <= 0 || height <= 0) {
            return fullBitmap;
        }

        return Bitmap.createBitmap(fullBitmap, left, top, width, height);
    }

    private void releaseCaptureResources() {

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private void saveCroppedBitmap(Bitmap bitmap) {

        // Insert into MediaStore (not app-private storage) so this
        // actually shows up in the Gallery/Photos app. No WRITE_EXTERNAL_
        // STORAGE permission needed on Android 10+ for an app's own
        // MediaStore inserts under scoped storage.
        String fileName = "chatpu_" + System.currentTimeMillis() + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_PICTURES + "/Chatpu"
        );

        ContentResolver resolver = getContentResolver();

        Uri itemUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );

        if (itemUri == null) {

            handler.post(() -> Toast.makeText(
                    FloatingWidgetService.this,
                    "Could not create gallery entry",
                    Toast.LENGTH_SHORT
            ).show());

            return;
        }

        try (OutputStream out = resolver.openOutputStream(itemUri)) {

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

            handler.post(() -> Toast.makeText(
                    FloatingWidgetService.this,
                    "Saved to gallery: " + fileName,
                    Toast.LENGTH_SHORT
            ).show());

        } catch (IOException e) {

            handler.post(() -> Toast.makeText(
                    FloatingWidgetService.this,
                    "Save failed: " + e.getMessage(),
                    Toast.LENGTH_SHORT
            ).show());
        }
    }

    private void startForegroundWithNotification() {

        String channelId = "chatpu_capture_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Chatpu overlay",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);

            notificationManager.createNotificationChannel(channel);
        }

        Notification notification =
                new Notification.Builder(this, channelId)
                        .setContentTitle("Chatpu running")
                        .setContentText("Floating translator is active")
                        .setSmallIcon(android.R.drawable.ic_menu_view)
                        .build();

        startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        );
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

                // NOTE: Activity.RESULT_OK is literally -1 in Android — using
                // -1 as the "extra missing" default here collided with a
                // genuinely successful grant and made every real permission
                // grant look invalid. Integer.MIN_VALUE can't collide with
                // any real result code.
                int resultCode =
                        intent.getIntExtra(
                                "resultCode",
                                Integer.MIN_VALUE
                        );

                Intent resultData =
                        intent.getParcelableExtra(
                                "resultData"
                        );

                if (resultCode == Integer.MIN_VALUE
                        || resultData == null) {

                    android.util.Log.e(
                            "CHATPU_CAPTURE",
                            "Invalid screen capture permission result"
                    );

                    return START_STICKY;
                }

                // Must start the mediaProjection-type foreground service
                // AFTER the user has granted the capture permission (we
                // have a valid resultCode/resultData now) and BEFORE
                // calling getMediaProjection() — this exact order is what
                // Android 14+ enforces.
                startForegroundWithNotification();

                mediaProjection =
                        mediaProjectionManager.getMediaProjection(
                                resultCode,
                                resultData
                        );

                mediaProjection.registerCallback(
                        new MediaProjection.Callback() {

                            @Override
                            public void onStop() {

                                android.util.Log.e(
                                        "CHATPU_CAPTURE",
                                        "MediaProjection.onStop() fired — "
                                                + "system revoked the cached "
                                                + "projection, clearing cache"
                                );

                                // System revoked it (e.g. user stopped
                                // sharing from the notification) — clear
                                // the cache and drop the now-dead surface so
                                // the next tap re-requests permission fresh.
                                releaseCaptureResources();
                                mediaProjection = null;
                            }
                        },
                        handler
                );

                android.util.Log.d(
                        "CHATPU_CAPTURE",
                        "Screen capture permission granted — cached for reuse"
                );

                setupCaptureSurface();

                Toast.makeText(
                        this,
                        "Ready — tap the bubble again to capture",
                        Toast.LENGTH_SHORT
                ).show();
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

        releaseCaptureResources();

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

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