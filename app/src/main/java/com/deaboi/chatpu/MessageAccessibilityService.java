package com.deaboi.chatpu;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MessageAccessibilityService
extends AccessibilityService {

@Override
protected void onServiceConnected() {

    super.onServiceConnected();

    checkCurrentApp();
}

@Override
public void onAccessibilityEvent(
        AccessibilityEvent event) {

    if (event.getEventType()
            == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            || event.getEventType()
            == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

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

    if (currentPackage.equals("com.whatsapp")) {

        showFloatingWidget();

    } else {

        hideFloatingWidget();
    }
}

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