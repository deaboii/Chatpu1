package com.deaboi.chatpu;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MessageAccessibilityService
        extends AccessibilityService {


    // ==================================================
    // ACCESSIBILITY EVENT
    // ==================================================

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event) {

        if (event.getPackageName() == null) {
            return;
        }


        String packageName =
                event.getPackageName().toString();


        // ==================================================
        // WHATSAPP
        // ==================================================

        if (packageName.equals("com.whatsapp")) {

            showFloatingWidget();

            AccessibilityNodeInfo root =
                    getRootInActiveWindow();

            if (root != null) {

                readNode(root);
            }

        }


        // ==================================================
        // ANY OTHER APP
        // ==================================================

        else {

            hideFloatingWidget();
        }
    }


    // ==================================================
    // SHOW FLOATING WIDGET
    // ==================================================

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


    // ==================================================
    // HIDE FLOATING WIDGET
    // ==================================================

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


    // ==================================================
    // READ WHATSAPP NODES
    // ==================================================

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
                            + (
                            p1 != null
                                    ? p1.toString()
                                    : "null"
                    )
                            + "\nP2="
                            + (
                            p2 != null
                                    ? p2.toString()
                                    : "null"
                    )
                            + "\nP3="
                            + (
                            p3 != null
                                    ? p3.toString()
                                    : "null"
                    )
            );
        }


        // Read child nodes

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


    // ==================================================
    // ACCESSIBILITY INTERRUPTED
    // ==================================================

    @Override
    public void onInterrupt() {

    }
}
