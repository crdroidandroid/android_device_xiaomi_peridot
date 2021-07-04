/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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

package org.lineageos.settings;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Display.HdrCapabilities;

import org.lineageos.settings.display.ColorModeService;
import org.lineageos.settings.doze.DozeUtils;
import org.lineageos.settings.doze.PocketService;
import org.lineageos.settings.thermal.ThermalUtils;
import org.lineageos.settings.thermal.ThermalTileService;
import org.lineageos.settings.refreshrate.RefreshUtils;
import org.lineageos.settings.touchsampling.TouchSamplingUtils;
import org.lineageos.settings.touchsampling.TouchSamplingService;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = true;
    private static final String TAG = "XiaomiParts";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "Received broadcast: " + intent.getAction());
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Display
            context.startServiceAsUser(new Intent(context, ColorModeService.class), UserHandle.CURRENT);
            DozeUtils.onBootCompleted(context);
            ThermalUtils.startService(context);
            RefreshUtils.startService(context);
            overrideHdrTypes(context);

            // Restore touch sampling rate
            TouchSamplingUtils.restoreSamplingValue(context);

            // Pocket service
            PocketService.startService(context);

            // Thermal tile service
            Intent thermalServiceIntent = new Intent(context, ThermalTileService.class);
            context.startServiceAsUser(thermalServiceIntent, UserHandle.CURRENT);

            // Register unlock receiver for restoring HTSR
            IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
            context.registerReceiver(new UnlockReceiver(), filter);

            // Start TouchSamplingService to restore sampling rate
            Intent touchSamplingServiceIntent = new Intent(context, TouchSamplingService.class);
            context.startServiceAsUser(touchSamplingServiceIntent, UserHandle.CURRENT);
        }
    }

    private static void overrideHdrTypes(Context context) {
        // Override HDR types to enable Dolby Vision
        final DisplayManager dm = context.getSystemService(DisplayManager.class);
        dm.overrideHdrTypes(Display.DEFAULT_DISPLAY, new int[]{
                HdrCapabilities.HDR_TYPE_DOLBY_VISION, HdrCapabilities.HDR_TYPE_HDR10,
                HdrCapabilities.HDR_TYPE_HLG, HdrCapabilities.HDR_TYPE_HDR10_PLUS});
    }
}