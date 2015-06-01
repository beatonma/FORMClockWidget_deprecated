package com.beatonma.formclockwidget;

import android.app.Service;
import android.content.Intent;
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

	public WidgetAnimationService() {
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
		SharedPreferences sp = getBaseContext().getSharedPreferences(ConfigActivity.PREFS, MODE_PRIVATE);
		enableAnimation = sp.getBoolean("pref_enable_animation", false);

		Log.d(TAG, "UPDATE SERVICE STARTED");
		if (enableAnimation) {
			handler.post(animationRunnable);
		}
		else {
			widgetUpdate();
		}

		return super.onStartCommand(intent, flags, startId);
	}

	// Single update call, no animation
	public void widgetUpdate() {
		Log.d(TAG, "Non-animated update initiated.");
		Intent intent = new Intent(this, WidgetProvider.class);
		intent.setAction(WidgetProvider.ANIMATE);
		sendBroadcast(intent);

		stop();
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
			stop();
		}
	}

	public void stop() {
		Log.d(TAG, "Stopping update service.");
		handler.removeCallbacks(animationRunnable);
		stopSelf();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
