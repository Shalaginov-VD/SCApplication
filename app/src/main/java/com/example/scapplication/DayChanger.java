package com.example.scapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DayChanger extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_DATE_CHANGED.equals(intent.getAction()) ||
                Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) ||
                Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {

            savePreviousDaySteps(context);

            resetCounterForNewDay(context);
        }
    }

    private void savePreviousDaySteps(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        int lastSteps = sharedPreferences.getInt("lastSteps", 0);
        String lastDate = sharedPreferences.getString("lastDate", "");

        if (!lastDate.isEmpty() && lastSteps > 0) {
            DBHelper dbHelper = new DBHelper(context);
            dbHelper.addOrUpdateStepsForDate(lastDate, lastSteps);
        }
    }

    private void resetCounterForNewDay(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String today = new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date());

        editor.putString("lastDate", today);
        editor.putInt("lastSteps", 0);
        editor.putFloat("key1", 0f);
        editor.apply();
    }
}
