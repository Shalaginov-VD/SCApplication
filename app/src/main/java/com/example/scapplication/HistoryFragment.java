package com.example.scapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

public class HistoryFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String PREFS_NAME = "StepCounterPrefs";

    private String mParam1;
    private String mParam2;
    private TextView historyTextView;
    private DBHelper helper;
    private SharedPreferences prefs;

    public HistoryFragment() {
    }

    public static HistoryFragment newInstance(String param1, String param2) {
        HistoryFragment fragment = new HistoryFragment();
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
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        historyTextView = view.findViewById(R.id.historyTextView);
        helper = new DBHelper(getContext());
        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);

        loadHistoryData();
        return view;
    }

    private void loadHistoryData() {
        List<DBHelper.StepEntry> stepsList = helper.getLastWeekSteps();

        boolean hasStepsData = false;
        int totalSteps = 0;
        StringBuilder historyText = new StringBuilder();

        for (DBHelper.StepEntry entry : stepsList) {
            if (entry.steps > 0) {
                hasStepsData = true;
                totalSteps += entry.steps;
            }
        }

        long installDate = prefs.getLong("install_date", 0);
        if (installDate == 0) {
            installDate = System.currentTimeMillis();
            prefs.edit().putLong("install_date", installDate).apply();
        }

        Calendar installCal = Calendar.getInstance();
        installCal.setTimeInMillis(installDate);
        Calendar todayCal = Calendar.getInstance();
        boolean isInstallDay = installCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                installCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR);

        if (isInstallDay && !hasStepsData) {
            historyText.append("Начните ходить, чтобы увидеть свою статистику здесь.");
        } else if (hasStepsData) {
            historyText.append("История шагов за неделю:\n");

            for (DBHelper.StepEntry entry : stepsList) {
                if (entry.steps > 0) {
                    historyText.append(entry.dayName)
                            .append(" (")
                            .append(entry.date)
                            .append("): ")
                            .append(entry.steps)
                            .append(" шагов\n");
                }
            }
        } else {
            historyText.append("История шагов за неделю:\n");
                    historyText.append("Данных о шагах пока нет.");
        }

        historyTextView.setText(historyText.toString());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistoryData();
    }
}
