package com.deaboi.chatpu;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MessageAccessibilityService
        extends AccessibilityService {

    private static MessageAccessibilityService instance;

    public static MessageAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {

        super.onServiceConnected();

        instance = this;

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

    // POC method — finds WhatsApp's message compose box and sets its
// text directly via the accessibility API, instead of relying on
// clipboard + manual paste. Returns false if no editable field was
// found in the current window (e.g. you're not on a chat screen).
    public boolean insertTextIntoComposeBox(String text) {

        AccessibilityNodeInfo root = getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        AccessibilityNodeInfo editable = findEditableNode(root);

        if (editable == null) {
            return false;
        }

        Bundle arguments = new Bundle();

        arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
        );

        return editable.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                arguments
        );
    }

    private AccessibilityNodeInfo findEditableNode(
            AccessibilityNodeInfo node) {

        if (node == null) {
            return null;
        }

        if (node.isEditable()) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {

            AccessibilityNodeInfo child = node.getChild(i);

            AccessibilityNodeInfo result = findEditableNode(child);

            if (result != null) {
                return result;
            }

            if (child != null) {
                child.recycle();
            }
        }

        return null;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        if (instance == this) {
            instance = null;
        }
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