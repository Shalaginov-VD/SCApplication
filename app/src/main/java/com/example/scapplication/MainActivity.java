package com.example.scapplication;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment homeFragment, historyFragment, settingsFragment;

    private void playNavSound() {
        SharedPreferences prefs = getSharedPreferences("Mode", MODE_PRIVATE);
        if (prefs.getBoolean("sound_enabled", true)) {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.navigation);
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
            });
            mediaPlayer.start();
        }
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, fragment)
                .commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottom_nav);
        homeFragment = new HomeFragment();
        historyFragment = new HistoryFragment();
        settingsFragment = new SettingsFragment();

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra("OPEN_SETTINGS", false)) {
                setCurrentFragment(settingsFragment);
                bottomNavigationView.setSelectedItemId(R.id.settings);
            } else {
                setCurrentFragment(homeFragment);
            }
        }

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Fragment selectedFragment = null;

                switch (menuItem.getItemId()) {
                    case R.id.home:
                        selectedFragment = homeFragment;
                        break;
                    case R.id.history:
                        selectedFragment = historyFragment;
                        break;
                    case R.id.settings:
                        selectedFragment = settingsFragment;
                        break;
                }

                if (selectedFragment != null) {
                    playNavSound();
                    setCurrentFragment(selectedFragment);
                }
                return true;
            }
        });
    }

    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "ru");

        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}
