package com.example.scapplication;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean running = false;
    private float totalSteps = 0f;
    private float previousTotalSteps = 0f;
    private TextView stepsTakenTextView;
    private ImageButton btnTutorial;
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
                        Toast.makeText(getContext(), getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
                        initSensor();
                    } else {
                        Toast.makeText(getContext(), getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
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

        btnTutorial = view.findViewById(R.id.btn_tutorial);

        helper = new DBHelper(getContext());

        currentDate = DBHelper.getCurrentDate();

        savedStepsForToday = helper.getStepsForDate(currentDate);

        checkAndRequestPermission();
        loadData();
        resetSteps();
        checkDayChange();

        btnTutorial.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.tutorial_title))
                    .setMessage(getString(R.string.tutorial_message))
                    .setPositiveButton(getString(R.string.ok_button), null)
                    .show();
        });

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
            Toast.makeText(getContext(), getString(R.string.sensor_not_found), Toast.LENGTH_LONG).show();
        } else {
            registerSensor();
        }
    }

    private void handlePermissionDenied() {
        if (stepsTakenTextView != null) {
            stepsTakenTextView.setText("0");
        }
        Toast.makeText(getContext(),
                getString(R.string.permission_required_message),
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
        if (event == null)
            return;

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

            checkStepGoal(totalStepsToday);

            saveStepsToDatabase(totalStepsToday);
        }
    }

    private void checkStepGoal(int currentSteps) {
        SharedPreferences prefs = getContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        int goal = prefs.getInt("step_goal", 0);
        boolean alreadyNotified = prefs.getBoolean("goal_reached_notified", false);

        if (goal > 0 && currentSteps >= goal && !alreadyNotified) {
            showGoalNotification(goal);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("goal_reached_notified", true);
            editor.apply();
        }
    }

    private void showGoalNotification(int goal) {
        String CHANNEL_ID = "goal_channel";
        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_goal_name);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        String title = getString(R.string.notification_goal_title);
        String content = getString(R.string.notification_goal_text, goal);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(2, builder.build());
    }

    private void saveStepsToDatabase(int steps) {
        if (helper != null) {
            helper.addOrUpdateSteps(steps);
        }
    }

    private void resetSteps() {
        if (stepsTakenTextView != null) {
            stepsTakenTextView.setOnClickListener(v ->
                    Toast.makeText(getContext(), getString(R.string.hold_to_reset), Toast.LENGTH_SHORT).show()
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
