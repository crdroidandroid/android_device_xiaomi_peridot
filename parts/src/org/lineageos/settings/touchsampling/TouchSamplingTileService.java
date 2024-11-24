/*
 * Copyright (C) 2025 The LineageOS Project
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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import org.lineageos.settings.R;
import org.lineageos.settings.touchsampling.TouchSamplingUtils;
import org.lineageos.settings.utils.FileUtils;

public class TouchSamplingTileService extends TileService {

    private static final String TAG = "TouchSamplingTileService";
    private static final String NOTIFICATION_CHANNEL_ID = "touch_sampling_tile_service_channel";
    private static final int NOTIFICATION_ID = 3;

    private NotificationManager mNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        setupNotificationChannel();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Log.d(TAG, "Tile added");
        updateTileState();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        Log.d(TAG, "Tile removed");
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        Log.d(TAG, "Tile started listening");
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        Log.d(TAG, "Tile clicked");
        toggleTouchSampling();
        updateTileState();
    }

    private void updateTileState() {
        boolean htsrEnabled = isTouchSamplingEnabled();

        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(htsrEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    private void toggleTouchSampling() {
        boolean currentState = isTouchSamplingEnabled();
        boolean newState = !currentState;

        // Update SharedPreferences
        saveTouchSamplingState(newState);

        // Start or stop the service
        Intent serviceIntent = new Intent(this, TouchSamplingService.class);
        if (newState) {
            startService(serviceIntent);
            showTouchSamplingNotification();
        } else {
            stopService(serviceIntent);
            cancelTouchSamplingNotification();
        }

        // Update the state in the file
        writeTouchSamplingStateToFile(newState ? 1 : 0);
    }

    private boolean isTouchSamplingEnabled() {
        SharedPreferences sharedPref = getSharedPreferences(
                TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(TouchSamplingSettingsFragment.HTSR_STATE, false);
    }

    private void saveTouchSamplingState(boolean state) {
        SharedPreferences sharedPref = getSharedPreferences(
                TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean(TouchSamplingSettingsFragment.HTSR_STATE, state).apply();
    }

    private void writeTouchSamplingStateToFile(int state) {
        if (!FileUtils.writeLine(TouchSamplingUtils.HTSR_FILE, Integer.toString(state))) {
            Log.e(TAG, "Failed to write touch sampling state to file");
        }
    }

    private void setupNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.touch_sampling_mode_title),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setBlockable(true);
        mNotificationManager.createNotificationChannel(channel);
    }

    private void showTouchSamplingNotification() {
        Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.touch_sampling_mode_title))
                .setContentText(getString(R.string.touch_sampling_mode_notification))
                .setSmallIcon(R.drawable.ic_touch_sampling_tile)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setFlag(Notification.FLAG_NO_CLEAR, true)
                .build();

        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void cancelTouchSamplingNotification() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    /**
     * Receiver to handle boot completion and reinitialize the tile state.
     */
    public static class BootCompletedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Log.d(TAG, "Boot completed - reinitializing tile state");

                SharedPreferences sharedPref = context.getSharedPreferences(
                        TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
                boolean htsrEnabled = sharedPref.getBoolean(TouchSamplingSettingsFragment.HTSR_STATE, false);

                int state = htsrEnabled ? 1 : 0;
                if (!FileUtils.writeLine(TouchSamplingUtils.HTSR_FILE, Integer.toString(state))) {
                    Log.e(TAG, "Failed to write touch sampling state to file during boot");
                }
            }
        }
    }
}
