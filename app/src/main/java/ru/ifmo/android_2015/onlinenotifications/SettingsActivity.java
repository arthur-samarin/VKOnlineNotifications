package ru.ifmo.android_2015.onlinenotifications;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import ru.ifmo.android_2015.onlinenotifications.service.MyService;
import ru.ifmo.android_2015.onlinenotifications.service.ServiceException;
import ru.ifmo.android_2015.onlinenotifications.service.Settings;
import ru.ifmo.android_2015.onlinenotifications.service.TaskListener;
import ru.ifmo.android_2015.onlinenotifications.util.SaveContainer;
import ru.ifmo.android_2015.onlinenotifications.util.Task;

public class SettingsActivity extends AppCompatActivity {
    private static long MIN_UPDATE_PERIOD_VALUE_MILLIS = 5000L /* 5s */;
    private static long MAX_UPDATE_PERIOD_VALUE_MILLIS = 15L * 60 * 1000 /* 15 min */;

    private SeekBar seekBar;
    private TextView updatePeriodTitle;
    private Button applyButton;

    private SaveContainer<SettingsActivity> saveContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        seekBar = (SeekBar) findViewById(R.id.updatePeriodSeekBar);
        updatePeriodTitle = (TextView) findViewById(R.id.updatePeriodTitle);

        if (savedInstanceState == null) {
            saveContainer = new SaveContainer<>();
            Helper.loadSettings(this);
        } else {
            saveContainer = (SaveContainer<SettingsActivity>) getLastCustomNonConfigurationInstance();
        }
        saveContainer.attachActivity(this);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateUpdatePeriodTitle();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        applyButton = (Button) findViewById(R.id.applyButton);
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings settings = new Settings();
                settings.setUpdatePeriod(progressToPeriod(seekBar.getProgress()), TimeUnit.MILLISECONDS);
                Helper.updateSettings(SettingsActivity.this, settings);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveContainer.detachActivity();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return saveContainer;
    }

    private static class Helper {
        private static void updateSettings(SettingsActivity activity, Settings settings) {
            final SaveContainer<SettingsActivity> saveContainer = activity.saveContainer;

            MyService.getInstance().updateSettings(settings, new TaskListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    saveContainer.addTask(new Task<SettingsActivity>() {
                        @Override
                        public void apply(SettingsActivity settingsActivity) {
                            Toast.makeText(settingsActivity, R.string.settings_applied_toast, Toast.LENGTH_SHORT).show();
                            settingsActivity.finish();
                        }
                    });
                }

                @Override
                public void onError(final ServiceException exception) {
                    saveContainer.addTask(new Task<SettingsActivity>() {
                        @Override
                        public void apply(SettingsActivity settingsActivity) {
                            Toast.makeText(settingsActivity, exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }

        private static void loadSettings(SettingsActivity activity) {
            final SaveContainer<SettingsActivity> saveContainer = activity.saveContainer;
            MyService.runWhenServiceAvailable(new Runnable() {
                @Override
                public void run() {
                    saveContainer.addTask(new Task<SettingsActivity>() {
                        @Override
                        public void apply(SettingsActivity settingsActivity) {
                            Settings settings = MyService.getInstance().getSettings();
                            settingsActivity.seekBar.setProgress(settingsActivity.periodToProgress(settings.getUpdatePeriod(TimeUnit.MILLISECONDS)));
                            settingsActivity.updateUpdatePeriodTitle();
                        }
                    });
                }
            });
        }
    }

    private void updateUpdatePeriodTitle() {
        int progress = seekBar.getProgress();
        long period = progressToPeriod(progress);
        long seconds = period / 1000L;

        String periodAsText;
        if (seconds < 60) {
            periodAsText = seconds + getString(R.string.time_unit_seconds);
        } else {
            long minutes = seconds / 60;
            periodAsText = minutes + (minutes >= 2 ? getString(R.string.time_unit_minutes) : getString(R.string.time_unit_minutes));
        }

        updatePeriodTitle.setText(getString(R.string.setting_update_period) + periodAsText);
    }

    private long progressToPeriod(int progress) {
        return (long) (MIN_UPDATE_PERIOD_VALUE_MILLIS + (MAX_UPDATE_PERIOD_VALUE_MILLIS - MIN_UPDATE_PERIOD_VALUE_MILLIS) * (progress / 100.0));
    }

    private int periodToProgress(long period) {
        return (int) ((period - MIN_UPDATE_PERIOD_VALUE_MILLIS) * 100 / (MAX_UPDATE_PERIOD_VALUE_MILLIS - MIN_UPDATE_PERIOD_VALUE_MILLIS));
    }
}
