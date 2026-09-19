package com.deaboi.chatpu;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MessageAccessibilityService extends AccessibilityService {

    private boolean whatsappActive = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event.getPackageName() == null) {
            return;
        }

        String packageName =
                event.getPackageName().toString();

        boolean isWhatsApp =
                packageName.equals("com.whatsapp");


        // -----------------------------
        // CONTROL FLOATING WIDGET
        // -----------------------------

        if (isWhatsApp && !whatsappActive) {

            whatsappActive = true;

            FloatingWidgetService.setWhatsAppVisible(
                    true
            );

        } else if (!isWhatsApp && whatsappActive) {

            whatsappActive = false;

            FloatingWidgetService.setWhatsAppVisible(
                    false
            );
        }


        // Only process WhatsApp nodes
        if (!isWhatsApp) {
            return;
        }


        // -----------------------------
        // EXISTING WHATSAPP NODE READING
        // -----------------------------

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root != null) {

            readNode(root);
        }
    }


    @Override
    public void onInterrupt() {

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


            Log.d(
                    "WHATSAPP_NODE",
                    "TEXT=" + node.getText()
                            + "\nP1="
                            + (p1 != null
                            ? p1.toString()
                            : "null")
                            + "\nP2="
                            + (p2 != null
                            ? p2.toString()
                            : "null")
                            + "\nP3="
                            + (p3 != null
                            ? p3.toString()
                            : "null")
            );
        }


        for (int i = 0;
             i < node.getChildCount();
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {

                readNode(child);
            }
        }
    }
}