package com.deaboi.chatpu;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button buttonStart;
    private MediaProjectionManager mediaProjectionManager;

    private final ActivityResultLauncher<Intent> screenCaptureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {

                            Intent serviceIntent =
                                    new Intent(
                                            this,
                                            FloatingWidgetService.class
                                    );

                            serviceIntent.setAction(
                                    FloatingWidgetService.ACTION_CAPTURE_PERMISSION
                            );

                            serviceIntent.putExtra(
                                    "resultCode",
                                    result.getResultCode()
                            );

                            serviceIntent.putExtra(
                                    "resultData",
                                    result.getData()
                            );

                            startService(serviceIntent);

                        } else {

                            Toast.makeText(
                                    this,
                                    "Screen capture permission cancelled",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        finish();
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        buttonStart = findViewById(R.id.buttonStart);

        mediaProjectionManager =
                (MediaProjectionManager)
                        getSystemService(MEDIA_PROJECTION_SERVICE);

        buttonStart.setOnClickListener(
                v -> startChatpu()
        );

        if (FloatingWidgetService.ACTION_REQUEST_CAPTURE.equals(
                getIntent().getAction())) {

            requestScreenCapture();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

        // MainActivity gets reused (FLAG_ACTIVITY_SINGLE_TOP) instead of
        // recreated once it's already in memory, so onCreate() won't fire
        // again — without this override, tapping the bubble after the
        // first time just reopened the existing MainActivity screen and
        // never requested screen capture at all.
        setIntent(intent);

        if (FloatingWidgetService.ACTION_REQUEST_CAPTURE.equals(
                intent.getAction())) {

            requestScreenCapture();
        }
    }

    private void startChatpu() {

        if (!Settings.canDrawOverlays(this)) {

            Toast.makeText(
                    this,
                    "Please allow Display over other apps",
                    Toast.LENGTH_LONG
            ).show();

            Intent intent =
                    new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse(
                                    "package:" + getPackageName()
                            )
                    );

            startActivity(intent);

            return;
        }

        Intent floatingIntent =
                new Intent(
                        this,
                        FloatingWidgetService.class
                );

        startService(floatingIntent);

        Toast.makeText(
                this,
                "Chatpu started. Open WhatsApp.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void requestScreenCapture() {

        if (mediaProjectionManager == null) {

            Toast.makeText(
                    this,
                    "Screen capture is not available",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }

        Intent captureIntent =
                mediaProjectionManager
                        .createScreenCaptureIntent();

        screenCaptureLauncher.launch(captureIntent);
    }

}