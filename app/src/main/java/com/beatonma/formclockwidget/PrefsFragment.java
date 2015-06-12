package com.beatonma.formclockwidget;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Michael on 28/05/2015.
 */
public class PrefsFragment extends PreferenceFragment {
	private final static String TAG = "PrefsFragment";
	private final static int PREFS_THEME = 0;
	private final static int PREFS_COMPLICATIONS = 1;
//	private final static int PREFS_NOTIFICATIONS = 2;
	private final static int PREFS_OTHER = 2;

	ConfigActivity context;
	private HashMap<String, String> namePackageMap;
	private HashMap<String, String> packageLauncherMap;

	String[] installedApps = {""};
	boolean appsLoaded = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = (ConfigActivity) getActivity();

		Bundle args = getArguments();
		int tab = 0;

		if (args != null) {
			tab = args.getInt("page");
		}

		PreferenceManager pm = getPreferenceManager();
		pm.setSharedPreferencesName(PrefUtils.PREFS);

		switch (tab) {
			case PREFS_THEME:
				initTheme(pm);
				break;
			case PREFS_COMPLICATIONS:
				initComplications(pm);
				break;
			case PREFS_OTHER:
				initOther(pm);
				break;
			default:
				initTheme(pm);
				break;
		}
	}

	private void saveTouchActivity(String key) {
		String packageName = namePackageMap.get(key);
		String launcherName = packageLauncherMap.get(packageName);

		getPreferenceManager().getSharedPreferences().edit()
				.putString(PrefUtils.PREF_ON_TOUCH_PACKAGE, packageName)
				.putString(PrefUtils.PREF_ON_TOUCH_LAUNCHER, launcherName)
				.apply();
	}

	private void loadAppList() {
		new LoadAppListTask(context, null).execute();
	}

	public static PrefsFragment newInstance(int page) {
		PrefsFragment fragment = new PrefsFragment();
		Bundle args = new Bundle();
		args.putInt("page", page);
		fragment.setArguments(args);
		return fragment;
	}

	public void initTheme(PreferenceManager pm) {
		addPreferencesFromResource(R.xml.prefs_theme);
	}

	public void initComplications(PreferenceManager pm) {
		addPreferencesFromResource(R.xml.prefs_complications);
	}

	public void initNotifications(PreferenceManager pm) {
		addPreferencesFromResource(R.xml.prefs_notifications);
	}

	public void initOther(PreferenceManager pm) {
		addPreferencesFromResource(R.xml.prefs_other);

		// Disable animation on <Kitkat due to new api requirements
		// May be possible to re-enable after rendering is reworked
		if (!Utils.isKitkat()) {
			Preference animationPreference = pm.findPreference(PrefUtils.PREF_ENABLE_ANIMATION);
			if (animationPreference != null) {
				Log.d("", "Removing animation option due to missing APIs (Requires Android 4.4 Kitkat or higher)");
				getPreferenceScreen().removePreference(animationPreference);
			}
		}

		loadAppList();

		Preference onTouchPreference = pm.findPreference(PrefUtils.PREF_ON_TOUCH);
		if (onTouchPreference != null) {
			onTouchPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					if (appsLoaded) {
						showAppListDialog();
					}
					else {
						new LoadAppListTask(context, new OnAppsLoadedListener() {
							@Override
							public void onAppsLoaded() {
								showAppListDialog();
							}
						}).execute();
					}

					return false;
				}
			});
		}
	}

	private void showAppListDialog() {
		if (installedApps != null) {
			if (installedApps.length == 0) {
				showRetrySnackbar();
			}
			else {
				try {
					Log.d(TAG, "installedApps #: " + installedApps.length);
					new MaterialDialog.Builder(context)
							.title("Choose an app")
							.items(installedApps)
							.itemsCallback(new MaterialDialog.ListCallback() {
								@Override
								public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
									saveTouchActivity((String) text);
								}
							})
							.show();
				}
				catch (Exception e) {
					Log.e(TAG, "Error loading app list: " + e.toString());
					e.printStackTrace();
					showRetrySnackbar();
				}
			}
		}
		else {
			Log.e(TAG, "installedApps array is null!");
			showRetrySnackbar();
		}
	}

	private void showRetrySnackbar() {
		Snackbar snackbar = Snackbar.make(context.getViewPager(), R.string.error_applist, Snackbar.LENGTH_LONG)
				.setAction(R.string.error_retry, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						new AlternateLoadAppListTask(context, new OnAppsLoadedListener() {
							@Override
							public void onAppsLoaded() {
								showAppListDialog();
							}
						}).execute();
					}
				})
				.setActionTextColor(context.getAccentColor());
		snackbar.show();
	}

	private void showProgressBar() {
		context.showProgressBar();
	}

	private void hideProgressBar() {
		context.hideProgressBar();
	}

	private class LoadAppListTask extends AsyncTask<Void, Void, Void> {
		Context context;
		OnAppsLoadedListener listener;

		public LoadAppListTask(Context context, OnAppsLoadedListener listener) {
			this.context = context;
			this.listener = listener;
		}

		@Override
		protected void onPreExecute() {
			showProgressBar();
		}

		@Override
		protected Void doInBackground(Void... voids) {
			try {
				PackageManager pm = context.getPackageManager();
				Intent main = new Intent(Intent.ACTION_MAIN, null);
				main.addCategory(Intent.CATEGORY_LAUNCHER);

				List<ResolveInfo> launchables = pm.queryIntentActivities(main, 0);
				List<String> nameList = new ArrayList<String>();

				namePackageMap = new HashMap<String, String>();
				packageLauncherMap = new HashMap<String, String>();

				for (ResolveInfo info : launchables) {
					String niceName = (String) info.activityInfo.loadLabel(pm);
					String packageName = info.activityInfo.packageName;
					String launcherName = info.activityInfo.name;

					namePackageMap.put(niceName, packageName);
					packageLauncherMap.put(packageName, launcherName);
					nameList.add(niceName);
				}

				nameList.removeAll(Collections.singleton(null));
				Collections.sort(nameList);
				installedApps = new String[nameList.size()];

				int i = 0;
				for (String s : nameList) {
					installedApps[i++] = s;
				}
			}
			catch (Exception e) {
				Log.e(TAG, "Error loading packages: " + e.toString());
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void voids) {
			hideProgressBar();
			appsLoaded = true;
			if (listener != null) {
				listener.onAppsLoaded();
			}
		}
	}

	public class AlternateLoadAppListTask extends AsyncTask<String, Void, String> {
		Context context;
		OnAppsLoadedListener listener;
		PackageManager pm;
		List<ApplicationInfo> packages;

		public AlternateLoadAppListTask(Context context, OnAppsLoadedListener listener) {
			this.context = context;
			this.listener = listener;
		}

		@Override
		protected String doInBackground(String... image) {
			try {
				pm = context.getPackageManager();

				// Get a list of installed apps.
				packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

				List<String> nameList = new ArrayList<String>();
				namePackageMap = new HashMap<String, String>();
				packageLauncherMap = new HashMap<String, String>();

				for (ApplicationInfo p : packages) {
					// Get app name from package info
					String label = (String) pm.getApplicationLabel(p);
					String packageName = p.packageName;
					String launcherName = p.name;
					namePackageMap.put(label, packageName);
					packageLauncherMap.put(packageName, launcherName);

					nameList.add(label);
				}

				nameList.removeAll(Collections.singleton(null));
				Collections.sort(nameList);
				installedApps = new String[nameList.size()];

				int i = 0;
				for (String s : nameList) {
					Log.d(TAG, "app #" + i + " = " + s);
					installedApps[i++] = s;
				}

			}
			catch (Exception e) {
				Log.e(TAG, "Error loading packages: " + e.toString());
			}

			return null;
		}

		@Override
		protected void onPostExecute(String file) {
			appsLoaded = true;
			if (listener != null) {
				listener.onAppsLoaded();
			}
		}
	}

	private interface OnAppsLoadedListener {
		void onAppsLoaded();
	}
}
