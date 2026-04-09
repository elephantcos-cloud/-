package com.example.locker;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final int REQUEST_CODE_OVERLAY = 2;
    private static final int REQUEST_CODE_USAGE = 3;
    private static final int REQUEST_CODE_WRITE_SETTINGS = 4;

    private DevicePolicyManager mDPM;
    private ComponentName mAdminName;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("AppLockerPrefs", MODE_PRIVATE);
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminName = new ComponentName(this, AdminReceiver.class);

        // প্রথমবার চেক: যদি সব পারমিশন না থাকে, চাও
        if (!checkAllPermissions()) {
            requestAllPermissions();
        } else {
            // পারমিশন আছে, পাসওয়ার্ড সেট বা চেক করার UI দেখাও
            setupPasswordUI();
        }
    }

    private boolean checkAllPermissions() {
        // Accessibility চেক করা যায় না প্রোগ্রামেটিক্যালি, সেটিংসে পাঠাতে হবে
        boolean overlay = Settings.canDrawOverlays(this);
        boolean usage = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            usage = (getSystemService(Context.USAGE_STATS_SERVICE) != null);
        }
        boolean writeSettings = Settings.System.canWrite(this);
        boolean admin = mDPM.isAdminActive(mAdminName);
        
        return overlay && usage && writeSettings && admin;
    }

    private void requestAllPermissions() {
        // Device Admin
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);

        // Overlay
        Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(overlayIntent, REQUEST_CODE_OVERLAY);

        // Usage Access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent usageIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(usageIntent, REQUEST_CODE_USAGE);
        }

        // Write Settings
        Intent writeIntent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(writeIntent, REQUEST_CODE_WRITE_SETTINGS);

        // Accessibility (এটা ইন্টেন্ট দিয়ে সরাসরি চালু করা যায় না, ইউজারকে সেটিংসে পাঠাতে হয়)
        Toast.makeText(this, "Please enable Accessibility for this app from Settings -> Accessibility", Toast.LENGTH_LONG).show();
        Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(accessibilityIntent);
        
        // সব চাওয়া শেষে অ্যাপ বন্ধ করে দিবে
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finishAffinity();
            }
        }, 5000);
    }

    private void setupPasswordUI() {
        setContentView(R.layout.activity_main);
        EditText passInput = findViewById(R.id.password_input);
        Button saveBtn = findViewById(R.id.save_btn);
        
        String savedPass = prefs.getString("MasterPassword", null);
        if (savedPass == null) {
            // প্রথমবার পাসওয়ার্ড সেট
            saveBtn.setOnClickListener(v -> {
                String pass = passInput.getText().toString();
                if (!pass.isEmpty()) {
                    prefs.edit().putString("MasterPassword", pass).apply();
                    finishAffinity();
                }
            });
        } else {
            // পাসওয়ার্ড চেক করে ভেতরে ঢোকা
            saveBtn.setOnClickListener(v -> {
                if (passInput.getText().toString().equals(savedPass)) {
                    Toast.makeText(this, "Welcome", Toast.LENGTH_SHORT).show();
                    // এখানে অ্যাপের ভেতরের কন্ট্রোল প্যানেল দেখাবে
                } else {
                    finishAffinity();
                }
            });
        }
    }
}
