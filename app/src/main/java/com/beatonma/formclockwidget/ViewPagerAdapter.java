package com.beatonma.formclockwidget;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

/**
 * Created by Michael on 04/06/2015.
 */
public class ViewPagerAdapter extends FragmentPagerAdapter {
	private final static String TAG = "PagerAdapter";
	private String[] titles;

	public ViewPagerAdapter(FragmentManager fm, Context context) {
		super(fm);
		titles = context.getResources().getStringArray(R.array.prefs_titles);
	}

	@Override
	public Fragment getItem(int position) {
		return PrefsFragment.newInstance(position);
	}

	@Override
	public int getCount() {
		return 3;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return titles[position];
	}
}
