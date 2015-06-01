package com.beatonma.formclockwidget;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by Michael Beaton on 29/05/2015.
 *
 * Every minute at WidgetProvider.ANIMATION_START_SECOND, this service is started and continues
 * until WidgetProvider.ANIMATION_START_SECOND of the next minute. While active, the service
 * sends update requests to the widget provider every FRAME_DELAY milliseconds. This makes the
 * widget animate. When the animation period is complete, the service stops itself.
 */
public class WidgetAnimationService extends Service {
	private final static String TAG = "AnimationService";
	private final static int FRAME_DELAY = 33; // ~30fps
	private boolean enableAnimation = false;
	private static final IntentFilter timeIntentFilter;

	static {
		timeIntentFilter = new IntentFilter();
		timeIntentFilter.addAction(Intent.ACTION_TIME_TICK);
		timeIntentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		timeIntentFilter.addAction(Intent.ACTION_TIME_CHANGED);
	}

	public WidgetAnimationService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		registerReceiver(timeChangedReceiver, timeIntentFilter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(timeChangedReceiver);
	}

	Handler handler = new Handler();
	Runnable animationRunnable = new Runnable() {
		@Override
		public void run() {
			animatedWidgetUpdate();
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		doUpdate();
		return START_STICKY;
	}

	public void doUpdate() {
		SharedPreferences sp = getBaseContext().getSharedPreferences(ConfigActivity.PREFS, MODE_PRIVATE);
		enableAnimation = sp.getBoolean("pref_enable_animation", false);

		Log.d(TAG, "UPDATE SERVICE STARTED");
		if (enableAnimation) {
			handler.post(animationRunnable);
		}
		else {
			widgetUpdate();
		}
	}

	// Single update call, no animation
	public void widgetUpdate() {
		Intent updateIntent = new Intent(this, WidgetProvider.class);
		updateIntent.setAction(WidgetProvider.ANIMATE);
		sendBroadcast(updateIntent);
	}

	// Repeated update calls while 58 < [second] < 1
	public void animatedWidgetUpdate() {
		Intent intent = new Intent(this, WidgetProvider.class);
		intent.setAction(WidgetProvider.ANIMATE);
		sendBroadcast(intent);

		Calendar time = Calendar.getInstance();
		int second = time.get(Calendar.SECOND);

		if (second <= WidgetProvider.ANIMATION_STOP_SECOND || second >= WidgetProvider.ANIMATION_START_SECOND) {
			handler.postDelayed(animationRunnable, FRAME_DELAY);
		}
		else {
			finishWidgetAnimation();
			stop();
		}
	}

	public void finishWidgetAnimation() {
		Intent intent = new Intent(this, WidgetProvider.class);
		intent.setAction(WidgetProvider.FINISHED);
		sendBroadcast(intent);
	}

	public void stop() {
		handler.removeCallbacks(animationRunnable);
		stopSelf();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private final BroadcastReceiver timeChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			doUpdate();
		}
	};
}
