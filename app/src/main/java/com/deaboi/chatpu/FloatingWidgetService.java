package com.deaboi.chatpu;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

public class FloatingWidgetService extends Service {

    @Override
    public void onCreate() {

        super.onCreate();


        WindowManager windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        View floatingView = LayoutInflater.from(this)
                .inflate(R.layout.floating_widget, null);

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        65,
                        65,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 20;
        params.y = 300;

        windowManager.addView(floatingView, params);

        floatingView.setOnTouchListener(new View.OnTouchListener() {

            private int initialX;
            private int initialY;

            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        initialX = params.x;
                        initialY = params.y;

                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        return true;

                    case MotionEvent.ACTION_MOVE:

                        params.x = initialX +
                                (int) (initialTouchX - event.getRawX());

                        params.y = initialY +
                                (int) (event.getRawY() - initialTouchY);

                        windowManager.updateViewLayout(
                                floatingView,
                                params
                        );

                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                }

                return false;
            }
        });


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
