/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.touchsampling;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.lineageos.settings.R;
import org.lineageos.settings.touchsampling.TouchSamplingUtils;
import org.lineageos.settings.utils.FileUtils;

public class TouchSamplingService extends Service {
    private static final String TAG = "TouchSamplingService";
    private static final long CHECK_INTERVAL_MS = 5000; // 5 seconds

    private Handler mHandler;
    private Runnable mPeriodicTask;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TouchSamplingService started");

        mHandler = new Handler();
        mPeriodicTask = new Runnable() {
            @Override
            public void run() {
                applyTouchSamplingRate();
                mHandler.postDelayed(this, CHECK_INTERVAL_MS); // Schedule the next check
            }
        };

        // Start the periodic task
        mHandler.post(mPeriodicTask);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Ensure the periodic task is running
        if (mHandler != null && mPeriodicTask != null) {
            mHandler.removeCallbacks(mPeriodicTask);
            mHandler.post(mPeriodicTask);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "TouchSamplingService stopped");

        // Stop the periodic task
        if (mHandler != null && mPeriodicTask != null) {
            mHandler.removeCallbacks(mPeriodicTask);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void applyTouchSamplingRate() {
        SharedPreferences sharedPref = getSharedPreferences(
                TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        boolean htsrEnabled = sharedPref.getBoolean(TouchSamplingSettingsFragment.HTSR_STATE, false);
        int state = htsrEnabled ? 1 : 0;

        Log.d(TAG, "Reapplying touch sampling rate: " + state);
        FileUtils.writeLine(TouchSamplingUtils.HTSR_FILE, Integer.toString(state));
    }
}
