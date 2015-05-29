package com.beatonma.formclockwidget;

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
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.widget.RemoteViews;

import com.beatonma.colorpicker.ColorUtils;

import net.nurik.roman.formwatchface.common.FormClockView;

import java.util.Calendar;

/**
 * Created by Michael Beaton on 25/05/2015.
 */
public class WidgetProvider extends AppWidgetProvider implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final static String TAG = "WidgetProvider";
	final static String UPDATE = "com.beatonma.formclockwidget.UPDATE";
	final static long UPDATE_INTERVAL = 60000l;

	private final static int WIDTH = 1000;
	private final static int HEIGHT = 300;

	Context context;
	SharedPreferences preferences;
	private FormClockView clockView;

	// User preferences
	boolean useWallpaperPalette = false;
	int color1 = Color.WHITE;
	int color2 = Color.GRAY;
	int color3 = Color.BLACK;

	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		this.context = context;

		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			appWidgetManager.updateAppWidget(appWidgetId, views);
			updateAppWidget(context, appWidgetManager, appWidgetId);
		}
	}

	public void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
		if (clockView == null) {
			//initClockView(context);
			loadSharedPreferences();
		}

		Bitmap bitmap = clockView.getDrawingCache();

		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
		views.setImageViewBitmap(R.id.clock_view, bitmap);
		views.setOnClickPendingIntent(R.id.container, getUpdateIntent(context));
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	private void initClockView(Context context) {
		clockView = new FormClockView(context);
		clockView.setTextSize((int) context.getResources().getDimension(R.dimen.preview_text_size));
		clockView.setColors(Color.WHITE, Color.GRAY, Color.BLACK);
		clockView.setDrawingCacheEnabled(true);
		clockView.measure(WIDTH, HEIGHT);
		clockView.layout(0, 0, WIDTH, HEIGHT);

		if (useWallpaperPalette) {
			setClockColorsToWallpaper(context);
		}
	}

	private void initClockView(Context context, int color1, int color2, int color3) {
		clockView = new FormClockView(context);
		clockView.setTextSize((int) context.getResources().getDimension(R.dimen.preview_text_size));
		clockView.setColors(color1, color2, color3);
		clockView.setDrawingCacheEnabled(true);
		clockView.measure(WIDTH, HEIGHT);
		clockView.layout(0, 0, WIDTH, HEIGHT);
	}

	private void setClockColorsToWallpaper(final Context context) {
		Palette palette = Utils.getWallpaperPalette(context);

		int color1, color2, color3;
		color1 = palette.getVibrantColor(Color.WHITE);
		color2 = palette.getMutedColor(Color.GRAY);
		color3 = palette.getDarkVibrantColor(Color.BLACK);

		initClockView(context, color1, color2, color3);
	}

	private PendingIntent getUpdateIntent(Context context) {
		Intent intent = new Intent(UPDATE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		return pendingIntent;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		this.context = context;

		Log.d(TAG, "received action: " + intent.getAction());

		if (intent.getAction().equals(UPDATE)) {
			Log.d(TAG, "Update intent received");
			ComponentName appWidget = new ComponentName(context.getPackageName(), getClass().getName());
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			int ids[] = appWidgetManager.getAppWidgetIds(appWidget);
			for (int id : ids) {
				updateAppWidget(context, appWidgetManager, id);
			}
		}
	}

	@Override
	public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
		super.onRestored(context, oldWidgetIds, newWidgetIds);
		this.context = context;
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		this.context = context;

		preferences = context.getSharedPreferences(ConfigActivity.PREFS, Activity.MODE_PRIVATE);
		preferences.registerOnSharedPreferenceChangeListener(this);

		Calendar time = Calendar.getInstance();
		time.set(Calendar.SECOND, 0);
		time.set(Calendar.MILLISECOND, 0);

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.setRepeating(AlarmManager.RTC, time.getTime().getTime(), UPDATE_INTERVAL, getUpdateIntent(context));
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		this.context = context;

		preferences = context.getSharedPreferences(ConfigActivity.PREFS, Activity.MODE_PRIVATE);
		preferences.unregisterOnSharedPreferenceChangeListener(this);

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(getUpdateIntent(context));
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		this.context = context;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		loadSharedPreferences();
	}

	public void loadSharedPreferences() {
		if (context != null) {
			preferences = context.getSharedPreferences(ConfigActivity.PREFS, Activity.MODE_PRIVATE);
			useWallpaperPalette = preferences.getBoolean("pref_use_wallpaper_palette", false);
			color1 = ColorUtils.getColorFromPreference(preferences, "pref_color1", Color.WHITE);
			color2 = ColorUtils.getColorFromPreference(preferences, "pref_color2", Color.GRAY);
			color3 = ColorUtils.getColorFromPreference(preferences, "pref_color3", Color.BLACK);

			if (useWallpaperPalette) {
				initClockView(context);
			}
			else {
				initClockView(context, color1, color2, color3);
			}
		}
		else {
			Log.d(TAG, "Loading prefs failed: context is null");
			initClockView(context);
		}
	}
}
