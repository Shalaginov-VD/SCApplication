package com.example.scapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
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
    private BarChartView barChartView;

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
        barChartView = view.findViewById(R.id.barChartView);

        loadHistoryData();
        return view;
    }

    private void loadHistoryData() {
        List<DBHelper.StepEntry> stepsList = helper.getLastWeekSteps();

        boolean hasStepsData = false;
        int totalSteps = 0;
        StringBuilder historyText = new StringBuilder();

        List<BarChartView.BarData> barDataList = new ArrayList<>();

        for (DBHelper.StepEntry entry : stepsList) {
            if (entry.steps > 0) {
                hasStepsData = true;
                totalSteps += entry.steps;

                String shortDayName = getShortDayName(entry.dayName);
                barDataList.add(new BarChartView.BarData(shortDayName, entry.steps));
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
            historyText.append(getString(R.string.history_empty_start));
        } else if (hasStepsData) {
            historyText.append(getString(R.string.history_subtitle));

            barChartView.setData(barDataList);
            barChartView.setVisibility(View.VISIBLE);

            for (DBHelper.StepEntry entry : stepsList) {
                if (entry.steps > 0) {
                    historyText.append(getFullDayName(entry.dayName))
                            .append(" (")
                            .append(entry.date)
                            .append("): ")
                            .append(entry.steps)
                            .append(" ")
                            .append(getString(R.string.steps_unit));
                }
            }
        } else {
            historyText.append(getString(R.string.history_subtitle));
            historyText.append(getString(R.string.history_no_data));
        }

        historyTextView.setText(historyText.toString());
    }

    private String getShortDayName(String fullDayName) {
        switch (fullDayName) {
            case "Понедельник":
                return getString(R.string.day_mon_short);
            case "Вторник":
                return getString(R.string.day_tue_short);
            case "Среда":
                return getString(R.string.day_wed_short);
            case "Четверг":
                return getString(R.string.day_thu_short);
            case "Пятница":
                return getString(R.string.day_fri_short);
            case "Суббота":
                return getString(R.string.day_sat_short);
            case "Воскресенье":
                return getString(R.string.day_sun_short);
            default:
                return fullDayName.length() > 3 ? fullDayName.substring(0, 3) : fullDayName;
        }
    }

    private String getFullDayName(String dbDayName) {
        switch (dbDayName) {
            case "Понедельник":
                return getString(R.string.day_mon_full);
            case "Вторник":
                return getString(R.string.day_tue_full);
            case "Среда":
                return getString(R.string.day_wed_full);
            case "Четверг":
                return getString(R.string.day_thu_full);
            case "Пятница":
                return getString(R.string.day_fri_full);
            case "Суббота":
                return getString(R.string.day_sat_full);
            case "Воскресенье":
                return getString(R.string.day_sun_full);
            default:
                return dbDayName;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistoryData();
    }
}
