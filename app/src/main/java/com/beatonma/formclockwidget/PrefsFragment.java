package com.beatonma.formclockwidget;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Michael on 28/05/2015.
 */
public class PrefsFragment extends PreferenceFragment {
	Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();

		PreferenceManager preferenceManager = getPreferenceManager();
		preferenceManager.setSharedPreferencesName(ConfigActivity.PREFS);

		addPreferencesFromResource(R.xml.preferences);

		// Disable animation option on 4.3 (or below) as schedule cannot be guaranteed
		// TODO re-enable when FormClockRenderer is sorted out properly
		if (!Utils.isKitkat()) {
			Preference animationPreference = preferenceManager.findPreference("pref_enable_animation");
			if (animationPreference != null) {
				Log.d("", "Removing animation option.");
				getPreferenceScreen().removePreference(animationPreference);
			}
		}
	}

	public static PrefsFragment newInstance() {
		PrefsFragment fragment = new PrefsFragment();
		return fragment;
	}
}
