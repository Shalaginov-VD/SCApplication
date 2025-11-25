package com.example.scapplication;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;

    private boolean running = false;
    private float totalSteps = 0f;
    private float previousTotalSteps = 0f;

    private TextView stepsTakenTextView;

    private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
            }
        }

        stepsTakenTextView = findViewById(R.id.textView_stepsTaken);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        if (stepSensor == null) {
            Toast.makeText(this, "Датчик счетчика шагов не найден!", Toast.LENGTH_LONG).show();
        }

        loadData();
        resetSteps();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerSensor();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    private void registerSensor() {
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
            Log.d("Step Counter", "Step Sensor Registered");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null) return;

        Log.d("Step Counter", "Sensor event received: " + event.values[0]);

        if (running) {
            totalSteps = event.values[0];

            if (previousTotalSteps == 0f) {
                previousTotalSteps = totalSteps;
                saveData();
            }

            int currentSteps = (int) (totalSteps - previousTotalSteps);
            stepsTakenTextView.setText(String.valueOf(Math.max(0, currentSteps)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        registerSensor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
    }

    private void resetSteps() {
        stepsTakenTextView.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Удерживайте для сброса шагов", Toast.LENGTH_SHORT).show()
        );

        stepsTakenTextView.setOnLongClickListener(v -> {
            previousTotalSteps = totalSteps;
            stepsTakenTextView.setText("0");
            saveData();
            return true;
        });
    }

    private void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("key1", previousTotalSteps);
        editor.apply();
        Log.d("Step Counter", "Steps saved: " + previousTotalSteps);
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        previousTotalSteps = sharedPreferences.getFloat("key1", 0f);
        Log.d("Step Counter", "Loaded steps: " + previousTotalSteps);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение предоставлено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Разрешение запрещено", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}