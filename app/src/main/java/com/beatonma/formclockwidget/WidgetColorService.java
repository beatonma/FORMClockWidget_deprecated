package com.beatonma.formclockwidget;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.graphics.Palette;
import android.util.Log;

/**
 * Created by Michael on 02/06/2015.
 *
 * Service to get colors from current wallpaper palette and broadcast them to the WidgetProvider.
 * Should run at most once per minute.
 */
public class WidgetColorService extends Service {
	private final static String TAG = "WidgetColorService";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "WidgetColorService started");
		new GetPaletteTask(this).execute();

		return START_STICKY;
	}

	private void broadcastColors(int color1, int color2, int color3) {
		Intent i = new Intent (this, WidgetProvider.class);
		i.setAction(WidgetProvider.COLOR_UPDATE);
		i.putExtra("color1", color1);
		i.putExtra("color2", color2);
		i.putExtra("color3", color3);
		sendBroadcast(i);

		stop();
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "ColorService destroyed");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void stop() {
		stopSelf();
	}

	private class GetPaletteTask extends AsyncTask<Void, Void, Void> {
		Context context;
		Palette palette;

		public GetPaletteTask(Context context) {
			this.context = context;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			palette = WallpaperUtils.getWallpaperPalette(context);
			return null;
		}

		@Override
		protected void onPostExecute(Void voids) {
			if (palette != null) {
				int color1, color2, color3;
				if (WallpaperUtils.isMuzei(context)) {
					Log.d(TAG, "Loading Muzei colours");

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

				broadcastColors(color1, color2, color3);
			}
		}

		private int lighten(int color, float amount) {
			float hsv[] = new float[3];
			Color.colorToHSV(color, hsv);
			hsv[2] = Math.max(0f, Math.min(1f, hsv[2] + amount));
			return Color.HSVToColor(hsv);
		}
	}
}
