package com.example.locker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class LockService extends AccessibilityService {

    private static final String PREFS_NAME = "AppLockerPrefs";
    private static final String PASSWORD_KEY = "MasterPassword";
    private String correctPassword = "1234"; // ডিফল্ট পাসওয়ার্ড, পরে চেঞ্জ হবে

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        setServiceInfo(info);
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        correctPassword = prefs.getString(PASSWORD_KEY, "1234");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            
            // ব্লক লিস্ট: সেটিংস, প্যাকেজ ইনস্টলার, অ্যাপ ইনফো
            if (packageName.contains("com.android.settings") || 
                packageName.contains("com.google.android.packageinstaller") ||
                packageName.contains("com.android.packageinstaller")) {
                
                // যদি আমাদের অ্যাপের ভেতর পাসওয়ার্ড চাওয়া হয়, তখন ব্লক করবে না
                if (!isAskingPassword) {
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    
                    // কয়েকবার চেষ্টা করলে পাসওয়ার্ড চাওয়া
                    showPasswordDialog();
                }
            }
        }
    }

    private boolean isAskingPassword = false;

    private void showPasswordDialog() {
        isAskingPassword = true;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Master Password");
        
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (input.getText().toString().equals(correctPassword)) {
                    Toast.makeText(LockService.this, "Access Granted. You can now uninstall.", Toast.LENGTH_LONG).show();
                    // ইউজারকে অ্যাপে ঢুকতে দিন
                    Intent intent = new Intent(LockService.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    isAskingPassword = false;
                } else {
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    isAskingPassword = false;
                }
            }
        });
        
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                performGlobalAction(GLOBAL_ACTION_HOME);
                isAskingPassword = false;
            }
        });
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        dialog.show();
    }

    @Override
    public void onInterrupt() {
    }
}
