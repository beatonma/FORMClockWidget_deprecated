package com.beatonma.formclockwidget;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.beatonma.colorpicker.ColorUtils;

import net.nurik.roman.formwatchface.common.FormClockView;

/**
 * Created by Michael on 25/05/2015.
 */
public class ConfigActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final static String TAG = "Config";
	public final static String PREFS = "widget_preferences";

	SharedPreferences preferences;
	Context context;

	boolean currentPreview = true;
	View headerView;

	FormClockView clockTrue;
	FormClockView clockFalse;

	// User preferences
	boolean useWallpaperPalette = false;
	int color1 = Color.WHITE;
	int color2 = Color.GRAY;
	int color3 = Color.BLACK;

	@Override
	public void onCreate(Bundle saved) {
		super.onCreate(saved);
		context = this;

		setContentView(R.layout.activity_config);
		replaceFragment(SettingsFragment.newInstance());

		loadSharedPreferences();
		preferences.registerOnSharedPreferenceChangeListener(this);

		headerView = findViewById(R.id.header_view);

		clockTrue = (FormClockView) findViewById(R.id.clock_view_1);
		clockFalse = (FormClockView) findViewById(R.id.clock_view_2);

		ViewTreeObserver vto = headerView.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				init();
				headerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});
	}

	private void init() {
		initBackground();

		initClockView(clockTrue);
		initClockView(clockFalse);

		if (Utils.isLollipop()) {
			View overlay = findViewById(R.id.header_overlay);
			if (overlay != null) {
				Utils.setBackground(
						overlay,
						getResources().getColor(R.color.Overlay),
						getResources().getColor(R.color.AccentLight));
			}
		}
	}

	private void initBackground() {
		ImageView wallpaper = (ImageView) findViewById(R.id.wallpaper);
		wallpaper.setImageBitmap(Utils.getWallpaperBitmap(context));
	}

	private void initClockView(FormClockView clock) {
		if (useWallpaperPalette) {
			loadColorsFromWallpaper();
		}

		clock.setColors(color1, color2, color3);
	}

	public void updatePreview() {
		View top = findViewById(R.id.preview2);

		if (useWallpaperPalette) {
			loadColorsFromWallpaper();
		}

		if (currentPreview) {
			FormClockView clock = (FormClockView) findViewById(R.id.clock_view_2);
			clock.setColors(color1, color2, color3);
			Utils.createCircularReveal(top, new int[] {Utils.CIRCULAR_BOTTOM});
		}
		else {
			FormClockView clock = (FormClockView) findViewById(R.id.clock_view_1);
			clock.setColors(color1, color2, color3);
			Utils.createCircularHide(top, new int[]{Utils.CIRCULAR_BOTTOM});
		}

		currentPreview = !currentPreview;
	}

	private void loadColorsFromWallpaper() {
		Palette palette = Utils.getWallpaperPalette(context);

		color1 = palette.getVibrantColor(color1);
		color2 = palette.getLightVibrantColor(color2);
		color3 = palette.getDarkVibrantColor(color3);
	}

	@Override
	public void onResume() {
		super.onResume();
		initBackground();
		loadColorsFromWallpaper();
		updatePreview();
		if (preferences != null) {
			preferences.registerOnSharedPreferenceChangeListener(this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (preferences != null) {
			preferences.unregisterOnSharedPreferenceChangeListener(this);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (preferences != null) {
			preferences.unregisterOnSharedPreferenceChangeListener(this);
		}
	}

	private void loadSharedPreferences() {
		preferences = getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
		useWallpaperPalette = preferences.getBoolean("pref_use_wallpaper_palette", false);

		color1 = ColorUtils.getColorFromPreference(preferences, "pref_color1", Color.WHITE);
		color2 = ColorUtils.getColorFromPreference(preferences, "pref_color2", Color.GRAY);
		color3 = ColorUtils.getColorFromPreference(preferences, "pref_color3", Color.BLACK);
	}

	public void replaceFragment(Fragment fragment) {
		FragmentManager fm = getFragmentManager();
		fm.beginTransaction()
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				.replace(R.id.fragment_container, fragment, "main")
				.commit();
	}

	public void updateFragments() {
		FragmentManager fm = getFragmentManager();
		SettingsFragment settingsFragment = (SettingsFragment) fm.findFragmentByTag("main");
		if (settingsFragment != null) {
			settingsFragment.updatePreferences(preferences);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(TAG, "SharedPreferenceChanged");
		loadSharedPreferences();
		updatePreview();
		updateFragments();
	}
}
