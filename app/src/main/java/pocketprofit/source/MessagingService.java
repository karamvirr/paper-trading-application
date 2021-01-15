package com.pocketprofit.source;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pocketprofit.R;
import com.pocketprofit.source.activities.MainActivity;

/**
 * Handles displaying notifications for whenever cloud messages are received from PocketProfit
 * via Google Firebase.
 */
public class MessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        showNotification(remoteMessage.getNotification().getTitle(),  remoteMessage.getNotification().getBody());
    }

    /**
     * Displays a push notification received from Google Firebase.
     *
     * @param message to display in push notification.
     */
    public void showNotification(String title, String message) {
        PendingIntent pi
                = PendingIntent
                .getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "M_CH_ID");
        notificationBuilder
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pi)
                .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
    }
}