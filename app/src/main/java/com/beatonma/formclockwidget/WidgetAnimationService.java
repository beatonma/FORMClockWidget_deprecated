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
 *
 * If animation is not enabled, this service stays alive in the background and updates when
 * it receives any of the intents detailed in INTENT_FILTER.
 */
public class WidgetAnimationService extends Service {
	private final static String TAG = "AnimationService";
	private final static int FRAME_DELAY = 33; // ~30fps
	private static final IntentFilter INTENT_FILTER;

	static {
		INTENT_FILTER = new IntentFilter();
		INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
		INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
		INTENT_FILTER.addAction("com.google.android.apps.muzei.ACTION_ARTWORK_CHANGED");
		INTENT_FILTER.addAction("com.beatonma.formclockwidget.EXTERNAL_LWP");
	}

	private Handler handler = new Handler();
	private Runnable animationRunnable = new Runnable() {
		@Override
		public void run() {
			animatedWidgetUpdate();
		}
	};

	public WidgetAnimationService() {

	}

	@Override
	public void onCreate() {
		super.onCreate();
		registerReceiver(timeChangedReceiver, INTENT_FILTER);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(timeChangedReceiver);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		doUpdate();
		return START_STICKY;
	}

	private void doUpdate() {
		SharedPreferences sp = getBaseContext().getSharedPreferences(PrefUtils.PREFS, MODE_PRIVATE);
		boolean enableAnimation = sp.getBoolean("pref_enable_animation", false);

		Log.d(TAG, "UPDATE SERVICE STARTED");
		if (enableAnimation) {
			handler.post(animationRunnable);
		}
		else {
			staticWidgetUpdate();
		}
	}

	// Single update call, no animation
	private void staticWidgetUpdate() {
		Intent updateIntent = new Intent(this, WidgetProvider.class);
		updateIntent.setAction(WidgetProvider.ANIMATE);
		sendBroadcast(updateIntent);
	}

	// Repeated update calls while 58 < [second] < 1
	private void animatedWidgetUpdate() {
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

	private void finishWidgetAnimation() {
		Intent intent = new Intent(this, WidgetProvider.class);
		intent.setAction(WidgetProvider.FINISHED);
		sendBroadcast(intent);
	}

	private void stop() {
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
