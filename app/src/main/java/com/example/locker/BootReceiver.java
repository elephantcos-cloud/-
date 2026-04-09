package com.example.locker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // অ্যাপ লঞ্চ করার দরকার নেই, Accessibility Service অটো স্টার্ট হবে
        }
    }
}
