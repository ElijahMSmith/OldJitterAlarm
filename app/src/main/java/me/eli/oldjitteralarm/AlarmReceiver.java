package me.eli.oldjitteralarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import java.util.Random;

public class AlarmReceiver extends BroadcastReceiver {

    private String name = "";
    private String description = "";
    private Context context;
    private Uri randomSound;
    private final String CHANNEL_ID = "JITTERALARM";
    private int channelAdendum = 1;

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;
        name = "" + intent.getStringExtra("name");
        description = "" + intent.getStringExtra("description");

        //SoundPool pool = MainActivity.pool;
        Random r = new Random();
        int randomNumber = 0;
        while (randomNumber == 0) {
            randomNumber = r.nextInt(122);
        }

        channelAdendum = randomNumber;
        final int rawResourceID = context.getResources().getIdentifier("a" + randomNumber, "raw", context.getPackageName());
        SoundPool pool = getSoundPool();
        pool.load(context, rawResourceID, 0);

        randomSound = Uri.parse("android.resource://"
                + context.getPackageName() + "/"
                + rawResourceID);
        pool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId,
                                       int status) {
                soundPool.play(rawResourceID, 1f, 1f, 0, 0, 1f);
                playNoto();
            }
        });
    }

    private SoundPool getSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .build();
        return new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
    }

    private void playNoto() {
        String channelName = "JitterAlarm Notification Channel";
        String channelDescription = "Notification channel for JitterAlarm";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        long[] vibrate = {250, 350, 250, 350, 750, 750};

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID + channelAdendum, channelName, importance);
            channel.setSound(randomSound, audioAttributes);
            channel.setDescription(channelDescription);
            channel.shouldVibrate();
            channel.setVibrationPattern(vibrate);

            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }

        Intent startMain = new Intent(context, MainActivity.class);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent startMainPI = PendingIntent.getActivity(context, 0, startMain, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID + channelAdendum)
                .setContentText(description)
                .setSmallIcon(R.drawable.ic_alarm)
                .setLights(Color.GREEN, 500, 2000)
                .setAutoCancel(true)
                .setContentTitle(name)
                .setContentIntent(startMainPI)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if (notificationManager != null) //Compiler demands we account for it possibly being null
            notificationManager.notify(1, builder.build());
    }
}
