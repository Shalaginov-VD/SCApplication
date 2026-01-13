package com.example.scapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "steps.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_STEPS = "steps_history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_STEPS = "steps";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_STEPS + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_DATE + " TEXT UNIQUE," +
                    COLUMN_STEPS + " INTEGER," +
                    COLUMN_TIMESTAMP + " INTEGER" +
                    ")";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STEPS);
        onCreate(db);
    }

    public void addOrUpdateSteps(int steps) {
        String today = getCurrentDate();
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, today);
        values.put(COLUMN_STEPS, steps);
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());

        db.insertWithOnConflict(TABLE_STEPS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void addOrUpdateStepsForDate(String date, int steps) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_STEPS, steps);
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());

        db.insertWithOnConflict(TABLE_STEPS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int getStepsForDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STEPS,
                new String[]{COLUMN_STEPS},
                COLUMN_DATE + " = ?",
                new String[]{date},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int steps = cursor.getInt(0);
            cursor.close();
            return steps;
        }

        if (cursor != null) cursor.close();
        return 0;
    }

    public List<StepEntry> getLastWeekSteps() {
        List<StepEntry> stepsList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            if (i > 0) {
                calendar.add(Calendar.DAY_OF_YEAR, -1);
            }

            String date = dateFormat.format(calendar.getTime());

            Cursor cursor = db.query(TABLE_STEPS,
                    new String[]{COLUMN_STEPS},
                    COLUMN_DATE + " = ?",
                    new String[]{date},
                    null, null, null);

            int steps = 0;
            if (cursor != null && cursor.moveToFirst()) {
                steps = cursor.getInt(0);
                cursor.close();
            }

            stepsList.add(new StepEntry(date, steps, getDayName(calendar)));
        }
        return stepsList;
    }

    private String getDayName(Calendar calendar) {
        String[] days = new String[]{"Воскресенье", "Понедельник", "Вторник", "Среда",
                "Четверг", "Пятница", "Суббота"};
        return days[calendar.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    public static class StepEntry {
        public String date;
        public int steps;
        public String dayName;

        public StepEntry(String date, int steps, String dayName) {
            this.date = date;
            this.steps = steps;
            this.dayName = dayName;
        }
    }
}
