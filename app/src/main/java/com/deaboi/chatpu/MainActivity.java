package com.deaboi.chatpu;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button buttonStart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        buttonStart =
                findViewById(R.id.buttonStart);


        buttonStart.setOnClickListener(
                v -> startChatpu()
        );
    }


    private void startChatpu() {

        // =================================================
        // CHECK OVERLAY PERMISSION
        // =================================================

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


        // =================================================
        // START FLOATING SERVICE
        // =================================================

        Intent floatingIntent =
                new Intent(
                        this,
                        FloatingWidgetService.class
                );

        startService(
                floatingIntent
        );


        Toast.makeText(
                this,
                "Chatpu started. Open WhatsApp.",
                Toast.LENGTH_SHORT
        ).show();
    }
}
