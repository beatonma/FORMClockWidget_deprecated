package com.beatonma.formclockwidget;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Michael on 28/05/2015.
 */
public class PrefsFragment extends PreferenceFragment {
	Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();
		getPreferenceManager().setSharedPreferencesName(ConfigActivity.PREFS);

		addPreferencesFromResource(R.xml.preferences);
	}

	public static PrefsFragment newInstance() {
		PrefsFragment fragment = new PrefsFragment();
		return fragment;
	}
}
