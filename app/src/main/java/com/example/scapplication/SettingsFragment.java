package com.example.scapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    Switch switcher, soundSwitcher;
    boolean nightMode;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    private Spinner languageSpinner;
    private boolean isFirstLoad = true;

    private EditText goalEditText;
    private Button btnSaveGoal, btnReset;

    public SettingsFragment() {
    }

    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        switcher = view.findViewById(R.id.night_switch);
        languageSpinner = view.findViewById(R.id.language_spinner);
        goalEditText = view.findViewById(R.id.goal);
        btnSaveGoal = view.findViewById(R.id.save_goal);
        soundSwitcher = view.findViewById(R.id.sound_switch);
        btnReset = view.findViewById(R.id.btn_reset);

        sharedPreferences = getContext().getSharedPreferences("Mode", Context.MODE_PRIVATE);
        nightMode = sharedPreferences.getBoolean("night", false);

        if (nightMode) {
            switcher.setChecked(true);
        } else {
            switcher.setChecked(false);
        }

        switcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                nightMode = !nightMode;

                if (nightMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }

                editor = sharedPreferences.edit();
                editor.putBoolean("night", nightMode);
                editor.apply();

            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        String currentLang = getSavedLanguage();
        if (currentLang.equals("en")) {
            languageSpinner.setSelection(1);
        } else {
            languageSpinner.setSelection(0);
        }

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFirstLoad) {
                    isFirstLoad = false;
                    return;
                }

                String selectedLang = (position == 1) ? "en" : "ru";

                if (!selectedLang.equals(getSavedLanguage())) {
                    setLocale(selectedLang);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSaveGoal.setOnClickListener(v -> {
            String goalStr = goalEditText.getText().toString();
            if (!goalStr.isEmpty()) {
                SharedPreferences prefs = getActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor goalEditor = prefs.edit();
                goalEditor.putInt("step_goal", Integer.parseInt(goalStr));
                goalEditor.putBoolean("goal_reached_notified", false);
                goalEditor.apply();

                Toast.makeText(getContext(), "Цель сохранена!", Toast.LENGTH_SHORT).show();
            }
        });

        boolean soundEnabled = sharedPreferences.getBoolean("sound_enabled", true);
        soundSwitcher.setChecked(soundEnabled);

        soundSwitcher.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor = sharedPreferences.edit();
            editor.putBoolean("sound_enabled", isChecked);
            editor.apply();
        });

        DBHelper dbHelper = new DBHelper(getContext());

        btnReset.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle(getString(R.string.reset_dialog_title))
                    .setMessage(getString(R.string.reset_dialog_message))
                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                        dbHelper.clearAllData();

                        SharedPreferences stepPrefs = getActivity().getSharedPreferences("StepCounterPrefs", Context.MODE_PRIVATE);
                        stepPrefs.edit().putInt("steps", 0).apply();

                        Toast.makeText(getContext(), getString(R.string.stats_reset_toast), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        });

        return view;
    }

    private void setLocale(String langCode) {
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE).edit();
        editor.putString("My_Lang", langCode);
        editor.apply();

        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra("OPEN_SETTINGS", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private String getSavedLanguage() {
        SharedPreferences prefs = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return prefs.getString("My_Lang", "ru");
    }
}