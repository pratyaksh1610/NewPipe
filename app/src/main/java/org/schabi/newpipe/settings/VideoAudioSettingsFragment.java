package org.schabi.newpipe.settings;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;

import com.google.android.material.snackbar.Snackbar;

import java.util.LinkedList;
import java.util.List;
import org.schabi.newpipe.R;
import org.schabi.newpipe.util.PermissionHelper;

public class VideoAudioSettingsFragment extends BasePreferenceFragment {

    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateSeekOptions();

        listener = (sharedPreferences, s) -> {

            // on M and above, if user chooses to minimise to popup player on exit and the app doesn't have
            // display over other apps permission, show a snackbar to let the user give permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    s.equals(getString(R.string.minimize_on_exit_key))) {

                String newSetting = sharedPreferences.getString(s, null);
                if (newSetting != null
                        && newSetting.equals(getString(R.string.minimize_on_exit_popup_key))
                        && !Settings.canDrawOverlays(getContext())) {

                    Snackbar.make(getListView(), R.string.permission_display_over_apps, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.settings,
                                    view -> PermissionHelper.checkSystemAlertWindowPermission(getContext()))
                            .show();

                }
            } else if (s.equals(getString(R.string.use_inexact_seek_key))) {
                updateSeekOptions();
            }
        };
    }

    /**
     * Update fast-forward/-rewind seek duration options according to language and inexact seek setting.
     * Exoplayer can't seek 5 seconds in audio when using inexact seek.
     */
    private void updateSeekOptions() {
        //initializing R.array.seek_duration_description to display the translation of seconds
        final Resources res = getResources();
        final String[] durationsValues = res.getStringArray(R.array.seek_duration_value);
        final List<String> displayedDurationValues = new LinkedList<>();
        final List<String> displayedDescriptionValues = new LinkedList<>();
        int currentDurationValue;
        final boolean inexactSeek = getPreferenceManager().getSharedPreferences()
            .getBoolean(res.getString(R.string.use_inexact_seek_key), false);

        for (String durationsValue : durationsValues) {
            currentDurationValue = Integer.parseInt(durationsValue) / 1000;
            if (inexactSeek && currentDurationValue % 10 == 5) {
                continue;
            }
            try {
                displayedDurationValues.add(durationsValue);
                displayedDescriptionValues.add(String.format(
                    res.getQuantityString(R.plurals.dynamic_seek_duration_description,
                        currentDurationValue),
                    currentDurationValue));
            } catch (Resources.NotFoundException ignored) {
                //if this happens, the translation is missing, and the english string will be displayed instead
            }
        }
        final ListPreference durations = (ListPreference) findPreference(getString(R.string.seek_duration_key));
        durations.setEntryValues(displayedDurationValues.toArray(new CharSequence[0]));
        durations.setEntries(displayedDescriptionValues.toArray(new CharSequence[0]));
        final int selectedDuration = Integer.parseInt(durations.getValue());
        if (selectedDuration / 1000 % 10 == 5) {
            durations.setValue(Integer.toString(selectedDuration + 5 * 1000));
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.video_audio_settings);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);

    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
    }
}
