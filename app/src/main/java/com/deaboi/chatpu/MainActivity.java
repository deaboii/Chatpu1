package com.deaboi.chatpu;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button start_btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        start_btn =findViewById(R.id.start_btn);
        start_btn.setOnClickListener(v ->
        {
            if (!Settings.canDrawOverlays(this)) {

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                );

                startActivity(intent);

            } else {

                Intent intent = new Intent(
                        MainActivity.this,
                        FloatingWidgetService.class
                );

                startService(intent);
            }
        });

    }



}
