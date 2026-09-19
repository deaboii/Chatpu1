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

        // Only check window changes
        // This tells us which app is currently in front.

        if (event.getEventType()
                == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            checkCurrentApp();
        }
    }


    private void checkCurrentApp() {

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            hideFloatingWidget();
            return;
        }


        CharSequence packageName =
                root.getPackageName();

        if (packageName == null) {
            hideFloatingWidget();
            return;
        }


        String currentPackage =
                packageName.toString();


        // ==========================================
        // WHATSAPP
        // ==========================================

        if (currentPackage.equals("com.whatsapp")) {

            showFloatingWidget();

        }


        // ==========================================
        // ANY OTHER APP
        // ==========================================

        else {

            hideFloatingWidget();
        }
    }


    // ==========================================
    // SHOW WIDGET
    // ==========================================

    private void showFloatingWidget() {

        Intent intent =
                new Intent(
                        this,
                        FloatingWidgetService.class
                );

        intent.setAction(ACTION_SHOW);

        startService(intent);
    }


    // ==========================================
    // HIDE WIDGET
    // ==========================================

    private void hideFloatingWidget() {

        Intent intent =
                new Intent(
                        this,
                        FloatingWidgetService.class
                );

        intent.setAction(ACTION_HIDE);

        startService(intent);
    }


    // ==========================================
    // READ WHATSAPP NODES
    // ==========================================

    private void readNode(
            AccessibilityNodeInfo node) {

        if (node == null) {
            return;
        }


        if (node.getText() != null) {

            android.util.Log.d(
                    "WHATSAPP_NODE",
                    "TEXT=" + node.getText()
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

One more important change

Your "FloatingWidgetService" should not automatically show the widget when it starts.

At the end of "onCreate()", you already have:

selectionView.setVisibility(View.GONE);
resizeHandleView.setVisibility(View.GONE);

Change/add this immediately after it:

floatingView.setVisibility(View.GONE);

So initially all three are hidden.

Then:

WhatsApp opened → 🟢 appears

Long press → 🔵 rectangle appears

Long press again → 🔵 rectangle disappears

Leave WhatsApp → 🟢 + 🔵 disappear

This should also preserve your existing dragging and resizing behavior.