package me.eli.oldjitteralarm;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText descriptionInput;
    private EditText firstAlertInput;
    private EditText intervalInput;
    private EditText hoursInput;
    private EditText minutesInput;
    private EditText secondsInput;

    BroadcastReceiver onceOnlyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Nothing to see here!
        }
    };

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reset();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(onceOnlyReceiver, new IntentFilter("ONCE_ONLY"));
        registerReceiver(broadcastReceiver, new IntentFilter("RESET_ALARM"));
        setContentView(R.layout.activity_main);

        nameInput = findViewById(R.id.nameInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        firstAlertInput = findViewById(R.id.firstAlertInput);
        intervalInput = findViewById(R.id.intervalInput);
        hoursInput = findViewById(R.id.hoursInput);
        minutesInput = findViewById(R.id.minutesInput);
        secondsInput = findViewById(R.id.secondsInput);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String retrievedName = sp.getString("name", "");
        String retrievedDescription = sp.getString("description", "");
        String retrievedFirstAlert = sp.getString("firstAlertStoring", "");
        String retrievedInterval = sp.getString("intervalStoring", "");

        nameInput.setText(retrievedName);
        descriptionInput.setText(retrievedDescription);
        firstAlertInput.setText(retrievedFirstAlert);
        intervalInput.setText(retrievedInterval);

        hoursInput.setText(sp.getString("hoursOffset", ""));
        minutesInput.setText(sp.getString("minutesOffset", ""));
        secondsInput.setText(sp.getString("secondsOffset", ""));
        if(getIntent().getBooleanExtra("FROM_BOOTUP", false))
            reset();

    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();

        editor.putString("name", nameInput.getText().toString());
        editor.putString("description", descriptionInput.getText().toString());
        editor.putString("firstAlertStoring", firstAlertInput.getText().toString());
        editor.putString("intervalStoring", intervalInput.getText().toString());

        //Get offset times and store
        String hoursOffset = hoursInput.getText().toString();
        String minutesOffset = minutesInput.getText().toString();
        String SecondsOffset = secondsInput.getText().toString();
        editor.putString("hoursOffset", hoursOffset);
        editor.putString("minutesOffset", minutesOffset);
        editor.putString("secondsOffset", SecondsOffset);

        editor.apply();
    }

    public void startAlarm(View view) {

        //setup
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        //Finishes times
        int[] times = parseTime(firstAlertInput.getText().toString());
        times[0] = times[0] * 3600000;
        times[1] = times[1] * 60000;
        long millis = times[0] + times[1];
        int[] intervalTimes = parseTime(intervalInput.getText().toString());
        intervalTimes[0] = intervalTimes[0] * 3600000;
        intervalTimes[1] = intervalTimes[1] * 60000;
        long intervalMillis = intervalTimes[0] + intervalTimes[1];

        //Need to parse and format the offset now, before intent is created, then attach it to the intent.
        //HERE I NEED TO GET THE OFFSET AND CONVERT INTO MILLISECONDS

        int hours;
        int minutes;
        int seconds;
        try {
            hours = Integer.parseInt(hoursInput.getText().toString()) * 3600000;
        } catch(NumberFormatException e) {
            Toast.makeText(getApplicationContext(), "Failed: Check numbers!", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            minutes = Integer.parseInt(minutesInput.getText().toString()) * 60000;
        } catch(NumberFormatException e) {
            Toast.makeText(getApplicationContext(), "Failed: Check numbers!", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            seconds = Integer.parseInt(secondsInput.getText().toString()) * 1000;
        } catch(NumberFormatException e) {
            Toast.makeText(getApplicationContext(), "Failed: Check numbers!", Toast.LENGTH_LONG).show();
            return;
        }
        int offsetMillis = hours + minutes + seconds;
        //HERE: Randomly Generate the offset, add or subtract it when alarm is set. Make sure to attach the opposite to the PI so that the next alarm will trigger at te correct default time
        int offsetRandomMillis = 0;

        if(offsetMillis != 0){
            Random offsetRandom = new Random();
            int oneOrNegativeOne = (offsetRandom.nextBoolean()) ? 1 : -1;
            offsetRandomMillis = offsetRandom.nextInt(offsetMillis) * oneOrNegativeOne;
        }

        //Creates new offset alarm structure
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("name", nameInput.getText().toString());
        intent.putExtra("description", descriptionInput.getText().toString());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 234324243, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Creates new recursion alarm structure
        Intent intent2;
        boolean once = false;
        if(intervalMillis <= 0){
            intent2 = new Intent("ONCE_ONLY");
            once = true;
        } else {
            intent2 = new Intent("RESET_ALARM");
        }
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(this.getApplicationContext(), 000, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        //Stores old offset alarm details
        editor.putString("oldName", nameInput.getText().toString());
        editor.putString("oldDescription", descriptionInput.getText().toString());
        editor.apply();

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis + offsetRandomMillis, pendingIntent);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, pendingIntent2);

        //NEED TO STORE ALL CURRENT ALARM DATA
        editor.putLong("currentRandomOffset", offsetRandomMillis);
        editor.putLong("interval", intervalMillis);
        editor.putString("currentName", nameInput.getText().toString());
        editor.putString("currentDescription", descriptionInput.getText().toString());
        editor.putLong("offsetTriggerTime", System.currentTimeMillis() + millis + offsetRandomMillis);
        editor.putLong("exactTriggerTime", System.currentTimeMillis() + millis);
        editor.putString("intentNameExtra", nameInput.getText().toString());
        editor.putString("intentDescriptionExtra", descriptionInput.getText().toString());
        editor.putLong("totalOffset", offsetMillis);

        if(once){
            editor.putBoolean("runOnce", true);
        } else {
            editor.putBoolean("runOnce", false);
        }
        editor.putBoolean("firstRun", true);
        editor.putBoolean("alarmRunning", true);
        editor.apply();

        Toast.makeText(this, "Successfully set!", Toast.LENGTH_SHORT).show();
    }

    private int[] parseTime(String input){
        int[] times = new int[2];
        String[] split = input.split(":");
        for(int i = 0; i < times.length; i++){
            times[i] = Integer.parseInt(split[i]);
        }
        return times;
    }

    public void cancelAlarm(View view){

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("name", sp.getString("name", ""));
        intent.putExtra("description", sp.getString("description", ""));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 234324243, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        Intent intent2 = new Intent("RESET_ALARM");
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(this.getApplicationContext(), 000, intent2, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmService = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmService.cancel(pendingIntent);
        alarmService.cancel(pendingIntent2);

        editor.putBoolean("alarmRunning", false);
        editor.apply();

        Toast.makeText(this, "Cancelled Alarm", Toast.LENGTH_SHORT).show();
    }

    public void reset(){

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();

        String retrievedName = sp.getString("name", "");
        String retrievedDescription = sp.getString("description", "");
        long intervalMillis = sp.getLong("interval", 0);
        long offsetMillis = sp.getLong("totalOffset", 0);

        //setup
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        //Need to parse and format the offset now, before intent is created, then attach it to the intent.
        //HERE I NEED TO GET THE OFFSET AND CONVERT INTO MILLISECONDS
        //HERE: Randomly Generate the offset, add or subtract it when alarm is set. Make sure to attach the opposite to the PI so that the next alarm will trigger at te correct default time
        int offsetRandomMillis = 0;

        if(offsetMillis != 0){
            Random offsetRandom = new Random();
            int oneOrNegativeOne = (offsetRandom.nextBoolean()) ? 1 : -1;
            offsetRandomMillis = offsetRandom.nextInt((int)offsetMillis) * oneOrNegativeOne; //This is stupid and should be changed
        }

        //Creates new offset alarm structure
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("name", retrievedName);
        intent.putExtra("description", retrievedDescription);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 234324243, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Creates new recursion alarm structure
        Intent intent2 = new Intent("RESET_ALARM");
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(this.getApplicationContext(), 000, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        //Stores old offset alarm details
        editor.putString("oldName", retrievedName);
        editor.putString("oldDescription", retrievedName);
        editor.apply();

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + intervalMillis + offsetRandomMillis, pendingIntent);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + intervalMillis, pendingIntent2);

        //Need to store all the data
        editor.putLong("offsetTriggerTime", System.currentTimeMillis() + intervalMillis + offsetRandomMillis);
        editor.putLong("exactTriggerTime", System.currentTimeMillis() + intervalMillis);
        editor.putString("intentNameExtra", retrievedName);
        editor.putString("intentDescriptionExtra", retrievedDescription);
        editor.putLong("currentRandomOffset", offsetRandomMillis);
        editor.putLong("totalOffset", offsetMillis);
        editor.putBoolean("firstRun", false);
        editor.apply();
    }
}