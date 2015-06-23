package com.beatonma.formclockwidget;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.beatonma.colorpicker.ColorPickerDialog;
import com.beatonma.colorpicker.ColorUtils;
import com.beatonma.colorpicker.OnColorPickedListener;
import com.google.samples.apps.iosched.ui.widget.SlidingTabLayout;

import net.nurik.roman.formwatchface.common.FormClockRenderer;
import net.nurik.roman.formwatchface.common.FormClockView;

/**
 * Created by Michael on 25/05/2015.
 */
public class ConfigActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final static String TAG = "Config";

	private SharedPreferences preferences;
	private Context context;
	private int themeId = 0;

	private Handler handler = new Handler();
	private Runnable toggleColorRunnable;

	private boolean currentPreview = true;
	private View headerView;
	private View colorContainer;

	private FormClockView clockTrue;
	private FormClockView clockFalse;

	private SlidingTabLayout tabLayout;
	private ViewPager viewPager;

	private ImageButton buttonColor1;
	private ImageButton buttonColor2;
	private ImageButton buttonColor3;

	private int accentColor = Color.parseColor("#FF1744"); // Color pulled from wallpaper

	private ProgressBar progressBar;

	// User preferences
	private boolean useWallpaperPalette = false;
	private int color1 = Color.WHITE;
	private int color2 = Color.GRAY;
	private int color3 = Color.BLACK;
	private Palette palette;

	private int orientation = FormClockRenderer.ORIENTATION_HORIZONTAL;

	private boolean wallpaperLoaded = false;
	private boolean wallpaperColorsLoaded = false;
	private int wallpaperColor1 = Color.WHITE;
	private int wallpaperColor2 = Color.GRAY;
	private int wallpaperColor3 = Color.BLACK;

	private boolean showShadow = false;
	private boolean showDate = false;
	private boolean showAlarm = false;

	private int debugTouchCounter = 0;

	@Override
	public void onCreate(Bundle saved) {
		super.onCreate(saved);
		context = this;

		loadSharedPreferences();
		preferences.registerOnSharedPreferenceChangeListener(this);

		themeId = WallpaperUtils.getThemeFromWallpaper(preferences);
		setTheme(themeId);
		setContentView(getLayoutId());

		headerView = findViewById(R.id.header_view);

		clockTrue = (FormClockView) findViewById(R.id.clock_view_1);
		clockFalse = (FormClockView) findViewById(R.id.clock_view_2);

		initTabs();
		initColorButtons();

		progressBar = (ProgressBar) findViewById(R.id.image_loading_progress_bar);
		progressBar.setIndeterminate(true);

		ViewTreeObserver vto = headerView.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				init();
				updateViewPagerHeight();
				headerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});
	}

	private void initColorButtons() {
		colorContainer = findViewById(R.id.colors_container);
		colorContainer.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return viewPager != null && viewPager.onTouchEvent(event);
			}
		});

		buttonColor1 = (ImageButton) findViewById(R.id.button_color1);
		buttonColor2 = (ImageButton) findViewById(R.id.button_color2);
		buttonColor3 = (ImageButton) findViewById(R.id.button_color3);

		Utils.setBackground(buttonColor1, color1);
		Utils.setBackground(buttonColor2, color2);
		Utils.setBackground(buttonColor3, color3);

		buttonColor1.setOnClickListener(new OnButtonClickedListener(1));
		buttonColor2.setOnClickListener(new OnButtonClickedListener(2));
		buttonColor3.setOnClickListener(new OnButtonClickedListener(3));
	}

	private void initTabs() {
		tabLayout = (SlidingTabLayout) findViewById(R.id.tablayout);
		viewPager = (ViewPager) findViewById(R.id.viewpager);

		View tabPadding = findViewById(R.id.tablayout_padding);

		ViewPagerAdapter adapter = new ViewPagerAdapter(getFragmentManager(), context);
		viewPager.setAdapter(adapter);

		tabLayout.setViewPager(viewPager);
		tabLayout.setDistributeEvenly(false);

		tabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				updateLayout();
			}

			@Override
			public void onPageSelected(int position) {
				updateLayout();
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});

		TypedArray a = getTheme().obtainStyledAttributes(themeId, new int[]{
				android.R.attr.colorPrimary,
				android.R.attr.colorAccent,
				R.attr.colorControlActivated
		});

		int bgColor = getResources().getColor(R.color.Primary);

		if (Utils.isLollipop()) {
			accentColor = a.getColor(1, 0);
		}
		else {
			accentColor = a.getColor(2, 0);
		}
		a.recycle();

		if (tabPadding != null) {
			tabPadding.setBackgroundColor(bgColor);
		}
		tabLayout.setBackgroundColor(bgColor);
		tabLayout.setSelectedIndicatorColors(accentColor);

		if (useLightText(bgColor)) {
			tabLayout.populateTabStrip(getResources().getColorStateList(R.color.text_selector_dark));
		}
		else {
			tabLayout.populateTabStrip(getResources().getColorStateList(R.color.text_selector_light));
		}
	}

	private void init() {
		loadColorsFromWallpaper();

		initClockView(clockTrue, true);
		initClockView(clockFalse, false);

		if (Utils.isLollipop()) {
			View overlay = findViewById(R.id.header_overlay);
			if (overlay != null) {
				Utils.setBackground(
						overlay,
						getResources().getColor(R.color.Overlay),
						accentColor);
//						getResources().getColor(R.color.AccentLight));

				overlay.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (debugTouchCounter > 10) {
							PrefUtils.DEBUG = !PrefUtils.DEBUG;
							Log.d(TAG, "DEBUG mode " + (PrefUtils.DEBUG ? "enabled" : "disabled"));
							Snackbar.make(viewPager, "Debug mode " + (PrefUtils.DEBUG ? "enabled" : "disabled"), Snackbar.LENGTH_SHORT).show();
							updatePreview();

							debugTouchCounter = 0;
						}
						else {
							debugTouchCounter++;
						}
					}
				});
			}
		}
	}

	private void initBackground(Bitmap bitmap) {
		if (!wallpaperLoaded) {
			final ImageView wallpaper = (ImageView) findViewById(R.id.wallpaper);
			wallpaper.setImageAlpha(0);
			wallpaper.setImageBitmap(bitmap);

			ValueAnimator animator = ValueAnimator.ofInt(0, 255);
			animator.setDuration(500);
			animator.setInterpolator(new AccelerateDecelerateInterpolator());
			animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					wallpaper.setImageAlpha((int) animation.getAnimatedValue());
				}
			});
			animator.start();

			wallpaperLoaded = true;
		}
	}

	private void initClockView(FormClockView clock, boolean visible) {
		switch (getLayoutId()) {
			case R.layout.activity_config_wide_landscape:
				clock.setTextSize(clock.getWidth() / 5);
				break;
			case R.layout.activity_config_wide_portrait:
				clock.setTextSize((int) (clock.getHeight() / 3.5));
				break;
			default:
				break;
		}

		if (useWallpaperPalette) {
			loadColorsFromWallpaper();
		}

		clock.setColors(color1, color2, color3);
		if (visible) {
			clock.setShowDate(showDate);
			clock.setShowAlarm(showAlarm);
			clock.setShowShadow(showShadow);
		}

		clock.setOrientation(orientation);
	}

	public void updatePreview() {
		View top = findViewById(R.id.preview2);

		if (useWallpaperPalette) {
			if (wallpaperColorsLoaded) {
				color1 = wallpaperColor1;
				color2 = wallpaperColor2;
				color3 = wallpaperColor3;
			}
			else {
				loadColorsFromWallpaper();
				return;
			}
		}

		if (currentPreview) {
			FormClockView clock = (FormClockView) findViewById(R.id.clock_view_2); // The one we want to show
			FormClockView otherClock = (FormClockView) findViewById(R.id.clock_view_1); // The one we want to hide

			otherClock.setShowDate(false);
			otherClock.setShowAlarm(false);
			otherClock.setShowShadow(false);
			otherClock.setOrientation(orientation);

			clock.setColors(color1, color2, color3);
			clock.setShowDate(showDate);
			clock.setShowAlarm(showAlarm);
			clock.setShowShadow(showShadow);
			clock.setOrientation(orientation);

			if (Utils.isLollipop()) {
				Utils.createCircularReveal(top, new int[]{Utils.CIRCULAR_BOTTOM});
			}
			else {
				top.bringToFront();
				top.setVisibility(View.VISIBLE);
			}
		}
		else {
			FormClockView clock = (FormClockView) findViewById(R.id.clock_view_1);
			FormClockView otherClock = (FormClockView) findViewById(R.id.clock_view_2);

			otherClock.setShowDate(false);
			otherClock.setShowAlarm(false);
			otherClock.setShowShadow(false);
			otherClock.setOrientation(orientation);

			clock.setColors(color1, color2, color3);
			clock.setShowDate(showDate);
			clock.setShowAlarm(showAlarm);
			clock.setShowShadow(showShadow);
			clock.setOrientation(orientation);

			if (Utils.isLollipop()) {
				Utils.createCircularHide(top, new int[]{Utils.CIRCULAR_BOTTOM});
			}
			else {
				top.bringToFront();
				top.setVisibility(View.INVISIBLE);
			}
		}

		currentPreview = !currentPreview;
	}

	private void loadColorsFromWallpaper() {

		new LoadWallpaperTask(context, new OnColorsLoadedListener() {
			@Override
			public void onColorsLoaded(int color1, int color2, int color3) {
				setWallpaperColors(color1, color2, color3);
			}
		}).execute();
	}

	public void setWallpaperColors(int color1, int color2, int color3) {
		wallpaperColor1 = color1;
		wallpaperColor2 = color2;
		wallpaperColor3 = color3;

		wallpaperColorsLoaded = true;

		if (useWallpaperPalette) {
			View top = findViewById(R.id.preview2);
			if (currentPreview) {
				FormClockView clock = (FormClockView) findViewById(R.id.clock_view_2);
				clock.setColors(color1, color2, color3);
				clock.setShowDate(showDate);
				Utils.createCircularReveal(top, new int[]{Utils.CIRCULAR_BOTTOM});
			}
			else {
				FormClockView clock = (FormClockView) findViewById(R.id.clock_view_1);
				clock.setColors(color1, color2, color3);
				clock.setShowDate(showDate);
				Utils.createCircularHide(top, new int[]{Utils.CIRCULAR_BOTTOM});
			}

			currentPreview = !currentPreview;
		}
	}


	@Override
	public void onResume() {
		super.onResume();

		wallpaperLoaded = false;
		wallpaperColorsLoaded = false;

		loadColorsFromWallpaper();
		if (preferences != null) {
			preferences.registerOnSharedPreferenceChangeListener(this);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onPause() {
		super.onPause();

		updateWidgets();

		handler.removeCallbacks(toggleColorRunnable);

		if (preferences != null) {
			preferences.unregisterOnSharedPreferenceChangeListener(this);
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		updateWidgets();

		if (preferences != null) {
			preferences.unregisterOnSharedPreferenceChangeListener(this);
		}
	}

	private void loadSharedPreferences() {
		preferences = getSharedPreferences(PrefUtils.PREFS, Activity.MODE_PRIVATE);
		useWallpaperPalette = preferences.getBoolean(PrefUtils.PREF_USE_WALLPAPER_PALETTE, false);
		showDate = preferences.getBoolean(PrefUtils.PREF_SHOW_DATE, false);
		showAlarm = preferences.getBoolean(PrefUtils.PREF_SHOW_ALARM, false);
		showShadow = preferences.getBoolean(PrefUtils.PREF_THEME_SHADOW, false);
		orientation = Integer.valueOf(preferences.getString(
				PrefUtils.PREF_THEME_ORIENTATION, "" + FormClockRenderer.ORIENTATION_HORIZONTAL));

		color1 = ColorUtils.getColorFromPreference(preferences, PrefUtils.PREF_COLOR1, Color.WHITE);
		color2 = ColorUtils.getColorFromPreference(preferences, PrefUtils.PREF_COLOR2, Color.GRAY);
		color3 = ColorUtils.getColorFromPreference(preferences, PrefUtils.PREF_COLOR3, Color.BLACK);
	}

	public void showColors() {
		if (colorContainer != null) {
			if (Utils.isLollipop()) {
				colorContainer.animate()
						.setDuration(300)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.translationY(colorContainer.getMeasuredHeight())
						.start();

				viewPager.animate()
						.setDuration(300)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.translationY(colorContainer.getMeasuredHeight())
						.start();
			}
			else {
				handler.removeCallbacks(toggleColorRunnable);
				toggleColorRunnable = new Runnable() {
					@Override
					public void run() {
						colorContainer.animate()
								.setDuration(300)
								.setInterpolator(new AccelerateDecelerateInterpolator())
								.alpha(1f)
								.start();
					}
				};

				colorContainer.animate()
						.setDuration(300)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.translationY(colorContainer.getMeasuredHeight())
						.start();

				viewPager.animate()
						.setDuration(300)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.translationY(colorContainer.getMeasuredHeight())
						.start();

				handler.postDelayed(toggleColorRunnable, 300);
			}
		}
	}

	public void hideColors() {
		if (colorContainer != null) {
			if (Utils.isLollipop()) {
				colorContainer.animate()
						.setDuration(300)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.translationY(0)
						.start();

				viewPager.animate()
						.setDuration(300)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.translationY(0)
						.start();
			}
			else {
				handler.removeCallbacks(toggleColorRunnable);
				toggleColorRunnable = new Runnable() {
					@Override
					public void run() {
						colorContainer.animate()
								.setDuration(300)
								.setInterpolator(new AccelerateDecelerateInterpolator())
								.translationY(0)
								.start();

						viewPager.animate()
								.setDuration(300)
								.setInterpolator(new AccelerateDecelerateInterpolator())
								.translationY(0)
								.start();
					}
				};

				colorContainer.animate()
						.setDuration(300)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.alpha(0f)
						.start();

				handler.postDelayed(toggleColorRunnable, 300);
			}
		}
	}

	public void updateLayout() {
		if (viewPager.getCurrentItem() == 0) {
			if (preferences.getBoolean(PrefUtils.PREF_USE_WALLPAPER_PALETTE, false)) {
				hideColors();
			}
			else {
				showColors();
			}
		}
		else {
			hideColors();
		}

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				updateViewPagerHeight();
			}
		}, 600);
	}

	public void updateViewPagerHeight() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int displayHeight = dm.heightPixels;

		int vpTop = (int) viewPager.getY();
		int newHeight = displayHeight - vpTop;

		ViewGroup.LayoutParams lp = viewPager.getLayoutParams();
		lp.height = newHeight;
		viewPager.requestLayout();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(TAG, "SharedPreferenceChanged");
		loadSharedPreferences();
		updatePreview();
		updateLayout();
	}

	public void setPalette(Palette palette) {
		this.palette = palette;
	}

	public Palette getPalette() {
		return this.palette;
	}

	public void showProgressBar() {
		if (progressBar != null) {
			progressBar.setVisibility(View.VISIBLE);
		}
	}

	public void hideProgressBar() {
		if (progressBar != null) {
			progressBar.setVisibility(View.INVISIBLE);
		}
	}

	private class LoadWallpaperTask extends AsyncTask<Void, Void, Void> {
		Context context;
		Bitmap bitmap;
		Palette palette;
		OnColorsLoadedListener listener;

		public LoadWallpaperTask(Context context, OnColorsLoadedListener listener) {
			this.context = context;
			this.listener = listener;
		}

		@Override
		protected void onPreExecute() {
			if (progressBar != null) {
				progressBar.setVisibility(View.VISIBLE);
			}
		}

		@Override
		protected Void doInBackground(Void... voids) {
			bitmap = WallpaperUtils.getWallpaperBitmap(context);
			palette = Palette.generate(bitmap, 16);
			return null;
		}

		@Override
		protected void onPostExecute(Void voids) {
			if (bitmap != null) {
				initBackground(bitmap);
			}

			if (palette != null) {
				int color1, color2, color3;
				if (WallpaperUtils.isMuzei(context)) {
					Log.d(TAG, "Colors picked from Muzei.");
					color1 = palette.getLightMutedColor(
							palette.getLightVibrantColor(
									palette.getMutedColor(Color.BLACK)));
					color2 = palette.getVibrantColor(
							palette.getDarkVibrantColor(
									palette.getMutedColor(
											palette.getDarkMutedColor(Color.GRAY))));
					color3 = Color.WHITE;

					if (color1 == Color.BLACK) {
						color1 = lighten(color2, 0.2f);
					}
				}
				else {
					int[] colors = WallpaperUtils.getLwpColors(context);

					if (colors != null) {
						Log.d(TAG, "Colors retrieved from registered LWP file.");
						color1 = colors[0];
						color2 = colors[1];
						color3 = colors[2];
					}
					else {
						Log.d(TAG, "Colors picked from standard bitmap.");
						color1 = palette.getVibrantColor(Color.WHITE);
						color2 = palette.getLightVibrantColor(Color.GRAY);
						color3 = palette.getDarkVibrantColor(Color.BLACK);
					}
				}

				if (listener != null) {
					listener.onColorsLoaded(color1, color2, color3);
				}

				setPalette(palette);
			}

			if (progressBar != null) {
				progressBar.setVisibility(View.INVISIBLE);
			}
		}

		private int lighten(int color, float amount) {
			float hsv[] = new float[3];
			Color.colorToHSV(color, hsv);
			hsv[2] = Math.max(0f, Math.min(1f, hsv[2] + amount));
			return Color.HSVToColor(hsv);
		}
	}

	private interface OnColorsLoadedListener {
		void onColorsLoaded(int color1, int color2, int color3);
	}

	private void updateWidgets() {
		Intent updateIntent = new Intent(this, WidgetProvider.class);
		updateIntent.setAction(WidgetProvider.ANIMATE);
		sendBroadcast(updateIntent);
	}

	private boolean useLightText(int color) {
		Resources res = getResources();

		return (color == res.getColor(R.color.Amber500)
				|| color == res.getColor(R.color.Grey500));
	}

	public int getAccentColor() {
		return accentColor;
	}

	public ViewPager getViewPager() {
		return viewPager;
	}

	public void updateButtons() {
		Utils.setBackground(buttonColor1, color1);
		Utils.setBackground(buttonColor2, color2);
		Utils.setBackground(buttonColor3, color3);

		updatePreview();
	}

	private int getLayoutId() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		int layoutId;

		int w = Utils.pxToDp(this, dm.widthPixels);
		int h = Utils.pxToDp(this, dm.heightPixels);

		if (w > h) { // Landscape
			if (w >= 650) {
				layoutId = R.layout.activity_config_wide_landscape;
				Log.d(TAG, "Using wide landscape layout");
			}
			else {
				layoutId = R.layout.activity_config_nopreview;
				Log.d(TAG, "Using nopreview layout");
			}
		}
		else { // Portrait
			if (w >= 600) {
				layoutId = R.layout.activity_config_wide_portrait;
				Log.d(TAG, "Using wide portrait layout");
			}
			else if (h >= 400) {
				layoutId = R.layout.activity_config;
				Log.d(TAG, "Using standard layout");
			}
			else {
				layoutId = R.layout.activity_config_nopreview;
				Log.d(TAG, "Using nopreview layout");
			}
		}

		return layoutId;
	}

	private class OnButtonClickedListener implements View.OnClickListener {
		int colorIndex = 0;

		public OnButtonClickedListener(int colorIndex) {
			this.colorIndex = colorIndex;
		}

		@Override
		public void onClick(View v) {
			FragmentManager fm = getFragmentManager();
			ColorPickerDialog colorDialog;
			OnColorPickedListener listener;

			String[] swatches = null;
//			if (colorsMode == COLORS_MODE_WALLPAPER) {
//				swatches = Utils.paletteToStringArray(context.getPalette());
//			}

			switch (colorIndex) {
				case 1:
					colorDialog = ColorPickerDialog.newInstance("pref_color1", swatches);
					listener = new OnColorPickedListener() {
						@Override
						public void onColorPicked(int swatch, int color) {
							color1 = ColorUtils.getColor(swatch, color + 1);
							updateButtons();
						}
					};
					break;
				case 2:
					colorDialog = ColorPickerDialog.newInstance("pref_color2", swatches);
					listener = new OnColorPickedListener() {
						@Override
						public void onColorPicked(int swatch, int color) {
							color2 = ColorUtils.getColor(swatch, color + 1);
							updateButtons();
						}
					};
					break;
				case 3:
					colorDialog = ColorPickerDialog.newInstance("pref_color3", swatches);
					listener = new OnColorPickedListener() {
						@Override
						public void onColorPicked(int swatch, int color) {
							color3 = ColorUtils.getColor(swatch, color + 1);
							updateButtons();
						}
					};
					break;
				default:
					colorDialog = ColorPickerDialog.newInstance("pref_color1", swatches);
					listener = new OnColorPickedListener() {
						@Override
						public void onColorPicked(int swatch, int color) {
							color1 = ColorUtils.getColor(swatch, color + 1);
							updateButtons();
						}
					};
			}

			colorDialog.setOnColorPickedListener(listener);
			colorDialog.show(fm, "ColorPickerDialog");
		}
	}
}
