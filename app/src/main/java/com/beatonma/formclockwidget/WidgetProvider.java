package com.beatonma.formclockwidget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.beatonma.colorpicker.ColorUtils;

import net.nurik.roman.formwatchface.common.FormClockRenderer;
import net.nurik.roman.formwatchface.common.FormClockView;

import java.util.Calendar;

/**
 * Created by Michael Beaton on 25/05/2015.
 */
public class WidgetProvider extends AppWidgetProvider implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final static String TAG = "WidgetProvider";
	final static String UPDATE = "com.beatonma.formclockwidget.UPDATE";
	final static String ANIMATE = "com.beatonma.formclockwidget.ANIMATE";
	final static String FINISHED = "com.beatonma.formclockwidget.FINISHED";
	final static String COLOR_UPDATE = "com.beatonma.formclockwidget.COLOR_UPDATE";
	final static String EXTERNAL_LWP = "com.beatonma.formclockwidget.EXTERNAL_LWP";
	final static int INVALID_COLOR = 1234567890;

	private final static int WIDGET_LONG = 1000;
	private final static int WIDGET_SHORT = 500;

	private int maxWidth = 1000;
	private int maxHeight = 500;

	public final static int ANIMATION_START_SECOND = 58;
	public final static int ANIMATION_STOP_SECOND = 1;

	private int textSize = 210;

	//	Context context;
	private SharedPreferences preferences;

	private Intent animationService;

	// User preferences
	private boolean useWallpaperPalette = false;
	private boolean enableAnimation = false;
	private boolean showShadow = false;
	private boolean showDate = false;
	private boolean showAlarm = false;
	private int color1 = Color.WHITE;
	private int color2 = Color.GRAY;
	private int color3 = Color.BLACK;
	private int orientation = FormClockRenderer.ORIENTATION_HORIZONTAL;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		updateWidgets(context);
		scheduleUpdate(context);
	}

	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
		loadSharedPreferences(context);
		scheduleUpdate(context);

		int width = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
		int height = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

		preferences.edit()
				.putInt(PrefUtils.MAX_WIDGET_WIDTH, width)
				.putInt(PrefUtils.MAX_WIDGET_HEIGHT, height)
				.commit();
	}

	private void updateWidgets(Context context) {
		loadSharedPreferences(context);

		if (useWallpaperPalette) {
			long timeSinceColorUpdate = System.currentTimeMillis() - preferences.getLong(PrefUtils.WALLPAPER_COLORS_UPDATED, 10000l);

			// Don't update colors if we have already done so in last 5 seconds
			if (timeSinceColorUpdate < 5000l) {
				color1 = preferences.getInt(PrefUtils.WALLPAPER_COLOR1, Color.BLACK);
				color2 = preferences.getInt(PrefUtils.WALLPAPER_COLOR2, Color.GRAY);
				color3 = preferences.getInt(PrefUtils.WALLPAPER_COLOR3, Color.WHITE);

				updateWidgets(context, color1, color2, color3);
			}
			else {
				Log.d(TAG, "refreshing wallpaper colors");
				preferences.edit()
						.putLong(PrefUtils.WALLPAPER_COLORS_UPDATED, System.currentTimeMillis())
						.commit();
				setClockColorsToWallpaper(context);
			}
		}
		else {
			color1 = ColorUtils.getColorFromPreference(preferences, PrefUtils.PREF_COLOR1, Color.WHITE);
			color2 = ColorUtils.getColorFromPreference(preferences, PrefUtils.PREF_COLOR2, Color.GRAY);
			color3 = ColorUtils.getColorFromPreference(preferences, PrefUtils.PREF_COLOR3, Color.BLACK);

			updateWidgets(context, color1, color2, color3);
		}
	}

	private void updateWidgets(Context context, int color1, int color2, int color3) {
		ComponentName appWidget = new ComponentName(context.getPackageName(), getClass().getName());
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int ids[] = appWidgetManager.getAppWidgetIds(appWidget);

		FormClockView formClockView = initClockView(context, color1, color2, color3);
		Bitmap bitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		formClockView.draw(canvas);
		updateWidgets(context, appWidgetManager, ids, bitmap);
	}

	private void updateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, Bitmap bitmap) {
		for (int appWidgetId : appWidgetIds) {
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			views.setImageViewBitmap(R.id.clock_view, bitmap);
			views.setOnClickPendingIntent(R.id.clock_view, getOnClickIntent(context));
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}

		if (bitmap != null) {
			bitmap.recycle();
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);

		loadSharedPreferences(context);

		switch (intent.getAction()) {
			case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
				scheduleUpdate(context);
				animationService = new Intent(context, WidgetAnimationService.class);
				context.startService(animationService);
				break;

			case UPDATE:
				scheduleUpdate(context);
				animationService = new Intent(context, WidgetAnimationService.class);
				context.startService(animationService);
				break;

			case ANIMATE:
				updateWidgets(context);
				break;

			case FINISHED:
				Log.d(TAG, "Update finished - scheduling next update");
				scheduleUpdate(context);
				break;

			case COLOR_UPDATE:
				updateClockColorsFromIntent(context, intent);
				break;

			case EXTERNAL_LWP:
				Log.d(TAG, "Intent received from external LWP");
				Bundle extras = intent.getExtras();
				if (extras != null) {
					String packageName = extras.getString("lwp_package", "");
					int color1 = extras.getInt("lwp_color1", INVALID_COLOR);
					int color2 = extras.getInt("lwp_color2", INVALID_COLOR);
					int color3 = extras.getInt("lwp_color3", INVALID_COLOR);

					if (packageName.equals("") || color1 == INVALID_COLOR
							|| color2 == INVALID_COLOR || color3 == INVALID_COLOR) {
						return;
					}
					else {
						Log.d(TAG, "Registering package: " + packageName + ";" + color1 + ";" + color2 + ";" + color3);
						WallpaperUtils.registerPackage(context, packageName, color1, color2, color3);

						updateWidgets(context);
					}
				}
				break;
		}
	}

	@Override
	public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
		super.onRestored(context, oldWidgetIds, newWidgetIds);

		scheduleUpdate(context);
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);

		preferences = context.getSharedPreferences(PrefUtils.PREFS, Activity.MODE_PRIVATE);
		preferences.registerOnSharedPreferenceChangeListener(this);

		scheduleUpdate(context);
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);

		preferences = context.getSharedPreferences(PrefUtils.PREFS, Activity.MODE_PRIVATE);
		preferences.unregisterOnSharedPreferenceChangeListener(this);

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(getUpdateIntent(context));

		animationService = new Intent(context, WidgetAnimationService.class);
		context.stopService(animationService);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);

		animationService = new Intent(context, WidgetAnimationService.class);
		context.stopService(animationService);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		loadSharedPreferences(sharedPreferences);
	}

	private void loadSharedPreferences(SharedPreferences preferences) {
		this.preferences = preferences;
		useWallpaperPalette = preferences.getBoolean(PrefUtils.PREF_USE_WALLPAPER_PALETTE, false);
		orientation = Integer.valueOf(preferences.getString(PrefUtils.PREF_THEME_ORIENTATION, "" + FormClockRenderer.ORIENTATION_HORIZONTAL));
		enableAnimation = preferences.getBoolean(PrefUtils.PREF_ENABLE_ANIMATION, false);
		showDate = preferences.getBoolean(PrefUtils.PREF_SHOW_DATE, false);
		showAlarm = preferences.getBoolean(PrefUtils.PREF_SHOW_ALARM, false);
		showShadow = preferences.getBoolean(PrefUtils.PREF_THEME_SHADOW, false);

		color1 = ColorUtils.getColorFromPreference(preferences, PrefUtils.PREF_COLOR1, Color.WHITE);
		color2 = ColorUtils.getColorFromPreference(preferences, PrefUtils.PREF_COLOR2, Color.GRAY);
		color3 = ColorUtils.getColorFromPreference(preferences, PrefUtils.PREF_COLOR3, Color.BLACK);
	}

	private void loadSharedPreferences(Context context) {
		if (context != null) {
			preferences = context.getSharedPreferences(PrefUtils.PREFS, Activity.MODE_PRIVATE);
			loadSharedPreferences(preferences);
		}
		else {
			Log.d(TAG, "Loading prefs failed: context is null");
		}
	}

	@SuppressLint("NewApi")
	public void scheduleUpdate(Context context) {
		Calendar time = Calendar.getInstance();
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		if (enableAnimation) {
			Log.d(TAG, "Scheduling next animated update");
			time.set(Calendar.SECOND, ANIMATION_START_SECOND);
			time.set(Calendar.MILLISECOND, 0);

			if (Utils.isKitkat()) {
				alarmManager.setExact(AlarmManager.RTC, time.getTime().getTime(), getUpdateIntent(context));
			}
			else {
				alarmManager.set(AlarmManager.RTC, time.getTime().getTime(), getUpdateIntent(context));
			}
		}
		else { // No scheduling necessary - just start the service
			animationService = new Intent(context, WidgetAnimationService.class);
			context.startService(animationService);
		}
	}

	private FormClockView initClockView(Context context, int color1, int color2, int color3) {
		loadSharedPreferences(context);

		updateRenderParams();

		FormClockView clockView = new FormClockView(context);
		clockView.setTextSize(textSize);
		clockView.setShowDate(showDate);
		clockView.setShowAlarm(showAlarm);
		clockView.setShowShadow(showShadow);
		clockView.setOrientation(orientation);
		clockView.setColors(color1, color2, color3);

		clockView.measure(maxWidth, maxHeight);
		clockView.layout(0, 0, maxWidth, maxHeight);
		return clockView;
	}

	// Multipliers here derived through trial and error
	private void updateRenderParams() {
		maxWidth = (int) (preferences.getInt(PrefUtils.MAX_WIDGET_WIDTH, WIDGET_LONG) * 1.4);
		maxHeight = (int) (preferences.getInt(PrefUtils.MAX_WIDGET_HEIGHT, WIDGET_SHORT) * 1.4);

		if (orientation == FormClockRenderer.ORIENTATION_HORIZONTAL) {
			maxHeight = maxWidth / 2;
			textSize = (int) (maxWidth / 4.8);
		}
		else if (orientation == FormClockRenderer.ORIENTATION_VERTICAL) {
			maxWidth = (int) (maxHeight * 0.9);
			textSize = (int) (maxHeight / 2.8);
		}
	}

	private void setClockColorsToWallpaper(Context context) {
		Intent colorService = new Intent(context, WidgetColorService.class);
		context.startService(colorService);
	}

	private void updateClockColorsFromIntent(Context context, Intent intent) {
		int color1 = intent.getIntExtra("color1", Color.WHITE);
		int color2 = intent.getIntExtra("color2", Color.GRAY);
		int color3 = intent.getIntExtra("color3", Color.BLACK);

		loadSharedPreferences(context);
		preferences.edit()
				.putLong(PrefUtils.WALLPAPER_COLORS_UPDATED, System.currentTimeMillis())
				.putInt(PrefUtils.WALLPAPER_COLOR1, color1)
				.putInt(PrefUtils.WALLPAPER_COLOR2, color2)
				.putInt(PrefUtils.WALLPAPER_COLOR3, color3)
				.commit();


		updateWidgets(context, color1, color2, color3);
	}

	private PendingIntent getUpdateIntent(Context context) {
		Intent intent = new Intent(UPDATE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		return pendingIntent;
	}

	private PendingIntent getOnClickIntent(Context context) {
		String pkg = preferences.getString(PrefUtils.PREF_ON_TOUCH_PACKAGE, PrefUtils.PREF_ON_TOUCH_DEFAULT_PACKAGE);
		String activity = preferences.getString(PrefUtils.PREF_ON_TOUCH_LAUNCHER, PrefUtils.PREF_ON_TOUCH_DEFAULT_LAUNCHER);

		ComponentName name = new ComponentName(pkg, activity);
		Intent intent;

		if (pkg == null) {
			return null;
		}

		if (activity == null || activity.equals("null") || activity.equals("")
				|| !(pkg.equals(PrefUtils.PREF_ON_TOUCH_DEFAULT_PACKAGE) && activity.equals(PrefUtils.PREF_ON_TOUCH_DEFAULT_LAUNCHER))) {
			intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
		}
		else {
			intent = new Intent(Intent.ACTION_MAIN);

			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
					Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			intent.setComponent(name);
		}

		if (intent == null) {
			Log.e(TAG, "Couldn't find activity for package: " + pkg);
			name = new ComponentName(PrefUtils.PREF_ON_TOUCH_DEFAULT_PACKAGE, PrefUtils.PREF_ON_TOUCH_DEFAULT_LAUNCHER);
			intent = new Intent(Intent.ACTION_MAIN);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			intent.setComponent(name);
		}

		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		return pendingIntent;
	}
}
