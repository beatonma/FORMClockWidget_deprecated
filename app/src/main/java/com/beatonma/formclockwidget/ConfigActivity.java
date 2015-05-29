package com.beatonma.formclockwidget;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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
	ImageView backgroundTrue;
	ImageView backgroundFalse;

	FormClockView clockTrue;
	FormClockView clockFalse;

	Handler handler = new Handler();

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
		replaceFragment(PrefsFragment.newInstance());
//		replaceFragment(SettingsFragment.newInstance());	Nicer version but needs much more work

		loadSharedPreferences();
		preferences.registerOnSharedPreferenceChangeListener(this);

		headerView = findViewById(R.id.header_view);
		backgroundTrue = (ImageView) findViewById(R.id.background1);
		backgroundFalse = (ImageView) findViewById(R.id.background2);

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
		initBackground(backgroundTrue);
		initBackground(backgroundFalse);

		initClockView(clockTrue);
		initClockView(clockFalse);

		View overlay = findViewById(R.id.header_overlay);
		Utils.setBackground(
				overlay,
				getResources().getColor(R.color.Overlay),
				getResources().getColor(R.color.AccentLight));
	}

	private void initBackground(ImageView v) {
		v.setImageBitmap(Utils.getWallpaperBitmap(context));
	}

	private void initClockView(FormClockView clock) {
		clock.setColors(color1, color2, color3);
	}

	private void updatePreview() {
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
	public void onPause() {
		super.onPause();
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
				.replace(R.id.fragment_container, fragment)
				.commit();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(TAG, "SharedPreferenceChanged");
		loadSharedPreferences();
		updatePreview();
	}
}