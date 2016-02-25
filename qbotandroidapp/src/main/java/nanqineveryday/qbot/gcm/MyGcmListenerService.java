package nanqineveryday.qbot.gcm;

/**
 * Created by qiner on 2/16/16.
 */
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import nanqineveryday.qbot.QBotActivity;
import nanqineveryday.qbot.R;
import nanqineveryday.qbot.util.Constants;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";
    private SharedPreferences mSharedPreferences;
    private String robotname;
    private String username;

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);

        this.mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        this.robotname = this.mSharedPreferences.getString(Constants.ROBOT_NAME, "");
        this.username = this.mSharedPreferences.getString(Constants.USER_NAME, "");
        if (message.equals("GCMwakeUpCall"+username+robotname)) {
            // start main activity
            Log.i(TAG, "Activity waked up by remote"+username);
            Intent intent = new Intent(this, QBotActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            // normal downstream message.
            sendNotification(message);
        }
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        Log.i(TAG, "Sending Notification");
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, QBotActivity.class), 0);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("GCM Message")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setSound(defaultSoundUri)
                .setContentIntent(contentIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(2 /* ID of notification */, notificationBuilder.build());
    }
}
