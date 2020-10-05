package me.eli.oldjitteralarm;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Random;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction() != null && !intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
            return;

        //NOTE THAT IT WORKS, JUST NEED TO DO THE RIGHT THINGS WITH THE DATA
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if(sp.getBoolean("alarmRunning", false)) {
            String name = sp.getString("currentName", "");
            String description = sp.getString("currentDescription", "");
            long exactTriggerTime = sp.getLong("exactTriggerTime", 0);
            long offsetTriggerTime = sp.getLong("offsetTriggerTime", 0);
            boolean runOnce = sp.getBoolean("runOnce", true);
            long interval = sp.getLong("interval", 0);
            long currentRandomOffset = sp.getLong("currentRandomOffset", 0);

            if(System.currentTimeMillis() >= offsetTriggerTime){
                //Send notification and do nothing else.
                playNotification(context, name, description);
            }

            if(runOnce){
                if(System.currentTimeMillis() >= exactTriggerTime){
                    //send an intent to call the broadcast receiver in the MainActivity class so that the alarm will reset automatically.
                    //If needed, set a new alarm here that will run off that one when it triggers (put it back of track).

                    //Create a new alarm with the remaining time left, if multiple triggers have passed then do the math and start at the appropriate time.
                    //Need to open the MainActivity with an intent with the boolean extra FROM_BOOTUP when th alarm is done.

                    long exactTimeAtNextTrigger = exactTriggerTime;
                    while(exactTimeAtNextTrigger < System.currentTimeMillis()){
                        exactTimeAtNextTrigger += interval;
                    }
                    long offsetTimeAtNextTrigger = exactTimeAtNextTrigger + currentRandomOffset;

                    Intent i = new Intent(context, StartMainReceiver.class);

                    //Creates offset alarm structure
                    Intent i2 = new Intent(context, AlarmReceiver.class);
                    intent.putExtra("name", name);
                    intent.putExtra("description", description);

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 234324243, i2, PendingIntent.FLAG_UPDATE_CURRENT);

                    //Creates new recursion alarm structure
                    PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

                    AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    manager.setExact(AlarmManager.RTC_WAKEUP, exactTimeAtNextTrigger, pendingIntent2);
                    manager.setExact(AlarmManager.RTC_WAKEUP, offsetTimeAtNextTrigger, pendingIntent);
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void playNotification(Context context, String name, String description){
        long[] vibrate = {250, 350, 250, 350, 750, 750};
        SoundPool pool = getSoundPool();
        Random r = new Random();
        int randomNumber = 0;
        while(randomNumber == 0){
            randomNumber = r.nextInt(122);
        }
        final int rawResourceID = context.getResources().getIdentifier("a" + randomNumber, "raw", context.getPackageName());
        pool.load(context, rawResourceID, 0);
        Uri randomSound = Uri.parse("android.resource://" + context.getPackageName() + "/" + rawResourceID);
        String CHANNEL_ID = "JITTERALARM1";

        String channelName = "JitterAlarm Notification Channel";
        String channelDescription = "Notification channel for JitterAlarm";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID + randomNumber, channelName, importance);
            channel.setSound(randomSound, audioAttributes);
            channel.setDescription(channelDescription);
            channel.shouldVibrate();
            channel.setVibrationPattern(vibrate);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            if(notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }

        Intent startMain = new Intent(context, MainActivity.class);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent startMainPI = PendingIntent.getActivity(context, 0, startMain, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID + randomNumber)
                .setContentText(description)
                .setSmallIcon(R.drawable.ic_alarm)
                .setLights(Color.GREEN, 500, 2000)
                .setAutoCancel(true)
                .setContentTitle(name)
                .setContentIntent(startMainPI)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if(notificationManager != null)
            notificationManager.notify(1, builder.build());
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
}