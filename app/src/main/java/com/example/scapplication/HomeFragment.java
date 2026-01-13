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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean running = false;
    private float totalSteps = 0f;
    private float previousTotalSteps = 0f;
    private TextView stepsTakenTextView;
    private DBHelper helper;
    private String currentDate;
    private int savedStepsForToday = 0;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    public HomeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(getContext(), "Разрешение предоставлено", Toast.LENGTH_SHORT).show();
                        initSensor();
                    } else {
                        Toast.makeText(getContext(), "Разрешение запрещено", Toast.LENGTH_SHORT).show();
                        handlePermissionDenied();
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        stepsTakenTextView = view.findViewById(R.id.textView_stepsTaken);

        helper = new DBHelper(getContext());

        currentDate = DBHelper.getCurrentDate();

        savedStepsForToday = helper.getStepsForDate(currentDate);

        checkAndRequestPermission();
        loadData();
        resetSteps();
        checkDayChange();

        return view;
    }

    private void checkDayChange() {
        String today = DBHelper.getCurrentDate();

        if (!today.equals(currentDate)) {

            previousTotalSteps = totalSteps;
            saveData();

            currentDate = today;

            savedStepsForToday = 0;

            if (stepsTakenTextView != null) {
                stepsTakenTextView.setText("0");
            }

            saveStepsToDatabase(0);
        }
    }

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (getActivity() != null) {
                if (ContextCompat.checkSelfPermission(
                        getActivity(),
                        Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED) {
                    initSensor();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
                }
            }
        } else {
            initSensor();
        }
    }

    private void initSensor() {
        if (getActivity() != null) {
            sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            }
        }

        if (stepSensor == null) {
            Toast.makeText(getContext(), "Датчик счетчика шагов не найден!", Toast.LENGTH_LONG).show();
        } else {
            registerSensor();
        }
    }

    private void handlePermissionDenied() {
        if (stepsTakenTextView != null) {
            stepsTakenTextView.setText("0");
        }
        Toast.makeText(getContext(),
                "Для подсчета шагов необходимо разрешение на отслеживание активности",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        running = true;

        checkDayChange();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                (getActivity() != null &&
                        ContextCompat.checkSelfPermission(getActivity(),
                                Manifest.permission.ACTIVITY_RECOGNITION)
                                == PackageManager.PERMISSION_GRANTED)) {
            registerSensor();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        running = false;
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                (getActivity() != null &&
                        ContextCompat.checkSelfPermission(getActivity(),
                                Manifest.permission.ACTIVITY_RECOGNITION)
                                == PackageManager.PERMISSION_GRANTED)) {
            registerSensor();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void registerSensor() {
        if (stepSensor != null && sensorManager != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null) return;

        if (running) {
            totalSteps = event.values[0];

            if (previousTotalSteps == 0f) {
                previousTotalSteps = totalSteps;
                saveData();
            }

            int currentSteps = (int) (totalSteps - previousTotalSteps);

            int totalStepsToday = currentSteps + savedStepsForToday;

            if (stepsTakenTextView != null) {
                stepsTakenTextView.setText(String.valueOf(Math.max(0, totalStepsToday)));
            }

            saveStepsToDatabase(totalStepsToday);
        }
    }

    private void saveStepsToDatabase(int steps) {
        if (helper != null) {
            helper.addOrUpdateSteps(steps);
        }
    }

    private void resetSteps() {
        if (stepsTakenTextView != null) {
            stepsTakenTextView.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Удерживайте для сброса шагов", Toast.LENGTH_SHORT).show()
            );

            stepsTakenTextView.setOnLongClickListener(v -> {
                previousTotalSteps = totalSteps;
                stepsTakenTextView.setText("0");
                saveData();
                saveStepsToDatabase(0);
                return true;
            });
        }
    }

    private void saveData() {
        if (getContext() != null) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putFloat("key1", previousTotalSteps);
            editor.putString("lastDate", currentDate);
            editor.putInt("savedSteps", savedStepsForToday);
            editor.apply();
        }
    }

    private void loadData() {
        if (getContext() != null) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
            previousTotalSteps = sharedPreferences.getFloat("key1", 0f);
            currentDate = sharedPreferences.getString("lastDate", DBHelper.getCurrentDate());
            savedStepsForToday = sharedPreferences.getInt("savedSteps", 0);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
