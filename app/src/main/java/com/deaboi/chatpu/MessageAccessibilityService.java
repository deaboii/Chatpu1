package com.deaboi.chatpu;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MessageAccessibilityService extends AccessibilityService {

    private static final String ACTION_SHOW =
            "com.deaboi.chatpu.SHOW_WIDGET";

    private static final String ACTION_HIDE =
            "com.deaboi.chatpu.HIDE_WIDGET";


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event.getPackageName() == null) {
            return;
        }

        String packageName =
                event.getPackageName().toString();


        // WhatsApp is open
        if (packageName.equals("com.whatsapp")) {

            Intent intent =
                    new Intent(
                            this,
                            FloatingWidgetService.class
                    );

            intent.setAction(ACTION_SHOW);

            startService(intent);


            // Read WhatsApp screen
            AccessibilityNodeInfo root =
                    getRootInActiveWindow();

            if (root != null) {
                readNode(root);
            }

        }

        // Another app is open
        else {

            Intent intent =
                    new Intent(
                            this,
                            FloatingWidgetService.class
                    );

            intent.setAction(ACTION_HIDE);

            startService(intent);
        }
    }


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

            android.util.Log.d(
                    "WHATSAPP_NODE",
                    "TEXT=" + node.getText()
                            + "\nP1=" +
                            (p1 != null
                                    ? p1.toString()
                                    : "null")
                            + "\nP2=" +
                            (p2 != null
                                    ? p2.toString()
                                    : "null")
                            + "\nP3=" +
                            (p3 != null
                                    ? p3.toString()
                                    : "null")
            );
        }


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


    @Override
    public void onInterrupt() {

    }
}

Then add these two constants to "FloatingWidgetService.java"

Put them immediately below:

public class FloatingWidgetService extends Service {

Add:

private static final String ACTION_SHOW =
        "com.deaboi.chatpu.SHOW_WIDGET";

private static final String ACTION_HIDE =
        "com.deaboi.chatpu.HIDE_WIDGET";

And make sure your "onStartCommand()" contains:

@Override
public int onStartCommand(
        Intent intent,
        int flags,
        int startId) {

    if (intent != null) {

        String action = intent.getAction();

        if (ACTION_SHOW.equals(action)) {

            floatingView.setVisibility(
                    View.VISIBLE
            );

        } else if (ACTION_HIDE.equals(action)) {

            floatingView.setVisibility(
                    View.GONE
            );

            selectionView.setVisibility(
                    View.GONE
            );

            resizeHandleView.setVisibility(
                    View.GONE
            );

            selectionVisible = false;
        }
    }

    return START_STICKY;
}

One important thing

If Android Studio still shows red after this, don't click "Create field" or "Create method."

Send me a screenshot of the red "ACTION_SHOW"/"ACTION_HIDE" line, or paste your current "FloatingWidgetService.java", because then I can match the two files exactly.