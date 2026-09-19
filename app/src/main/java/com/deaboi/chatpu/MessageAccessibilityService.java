package com.deaboi.chatpu;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

public class MessageAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if(event.getPackageName() == null ||
                !event.getPackageName().toString().equals("com.whatsapp"))
        {
            return;
        }

        android.view.accessibility.AccessibilityNodeInfo root = getRootInActiveWindow();
        if(root != null)
        {
            readNode(root);
        }

    }

    @Override
    public void onInterrupt() {

    }

    private void readNode(
            android.view.accessibility.AccessibilityNodeInfo node) {

        if (node.getText() != null) {

            AccessibilityNodeInfo p1 = node.getParent();
            AccessibilityNodeInfo p2 = p1 != null ? p1.getParent() : null;
            AccessibilityNodeInfo p3 = p2 != null ? p2.getParent() : null;

            Log.d("WHATSAPP_NODE",
                    "TEXT=" + node.getText()
                            + "\nP1=" + (p1 != null ? p1.toString() : "null")
                            + "\nP2=" + (p2 != null ? p2.toString() : "null")
                            + "\nP3=" + (p3 != null ? p3.toString() : "null"));
        }

        for (int i = 0; i < node.getChildCount(); i++) {

            android.view.accessibility.AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {
                readNode(child);
            }
        }
    }

}
