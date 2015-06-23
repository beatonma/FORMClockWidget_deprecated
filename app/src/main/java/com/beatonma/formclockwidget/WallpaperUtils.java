package com.beatonma.formclockwidget;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.util.Log;

import com.google.android.apps.muzei.api.MuzeiContract;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Michael on 02/06/2015.
 */
public class WallpaperUtils {
	private final static String TAG = "WallpaperUtils";

	private final static String LWP_MUZEI = "net.nurik.roman.muzei";
	private final static String LWP_REGISTER = "registered_lwp";

	public static boolean isMuzei(Context context) {
		WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
		WallpaperInfo info = wallpaperManager.getWallpaperInfo();

		return (info != null && info.getPackageName().equals(LWP_MUZEI));
	}

	public static boolean isLwp(Context context) {
		WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
		WallpaperInfo info = wallpaperManager.getWallpaperInfo();

		return (info != null);
	}

	public static Palette getWallpaperPalette(Context context) {
		Bitmap bitmap = getWallpaperBitmap(context);
		Palette palette = Palette.generate(bitmap, 16);

		return palette;
	}

	public static Bitmap getWallpaperBitmap(Context context) {
		WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

		Drawable wallpaperDrawable;
		WallpaperInfo info = wallpaperManager.getWallpaperInfo();

		if (info != null) {
			// wallpaper is live
			String wallpaperPackage = info.getPackageName();

			if (wallpaperPackage.equals(LWP_MUZEI)) {
				Bitmap wallpaperBitmap = null;
				try {
					wallpaperBitmap = getMuzeiImage(context);
				}
				catch (Exception e) {
					Log.e(TAG, "Error getting Muzei bitmap.");
					e.printStackTrace();
				}

				if (wallpaperBitmap != null) {
					return wallpaperBitmap;
				}
			}

			// default to LWP thumbnail
			PackageManager pm = context.getPackageManager();
			wallpaperDrawable = info.loadThumbnail(pm);
		}
		else {
			// wallpaper is a static image
			wallpaperDrawable = wallpaperManager.getDrawable();
		}

		Bitmap bitmap = Utils.drawableToBitmap(wallpaperDrawable);

		return bitmap;
	}

	public static void registerPackage(Context context, String packageName, int color1, int color2, int color3) {
		List<String> inputFileContent = FileUtils.readFile(context, LWP_REGISTER);
		List<String> outputFileContent = new ArrayList<String>();

		for (String s : inputFileContent) {
			if (s.contains(packageName)) {
				Log.d(TAG, "Old entry for this package has been found and will be replaced.");
			}
			else {
				outputFileContent.add(s);
			}
		}

		String packageEntry = packageName + ";" + color1 + ";" + color2 + ";" + color3;
		Log.d(TAG, "Registering package: " + packageEntry);
		outputFileContent.add(packageEntry);
		FileUtils.writeFile(context, LWP_REGISTER, outputFileContent);
	}

	// Check if current LWP has registered colors and return them if so
	public static int[] getLwpColors(Context context) {
		WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
		WallpaperInfo info = wallpaperManager.getWallpaperInfo();

		if (info != null) {
			// wallpaper is live
			String wallpaperPackage = info.getPackageName();

			if (FileUtils.fileExists(context, LWP_REGISTER)) {
				String line = FileUtils.getLineContaining(context, LWP_REGISTER, wallpaperPackage);
				Log.d(TAG, "line = " + line);
				if (line == null) {
					Log.d(TAG, "LWP package not registered - get colors from preview icon.");
					return null;
				}
				else {
					String[] parts = line.split(";");
					Log.d(TAG, "parts #: " + parts.length);
					if (parts.length == 4) { // Should be 4 parts: packageName;color1;color2;color3
						try {
							int color1 = Integer.valueOf(parts[1]);
							int color2 = Integer.valueOf(parts[2]);
							int color3 = Integer.valueOf(parts[3]);

							return new int[] {color1, color2, color3};
						}
						catch (Exception e) { // incorrect number formatting?
							Log.e(TAG, "Error reading colors from file: " + e.toString());
						}
					}
				}
			}
			else {
				Log.d(TAG, "File does not exist.");
			}
		}

		return null;
	}

	public static int getThemeKeyFromWallpaper(Context context) {
		Palette p = getWallpaperPalette(context);
		int color = p.getVibrantColor(context.getResources().getColor(R.color.Accent));
		return whatColor(color);
	}

	public static int whatColor(int color) {
		float[] hsv = new float[3];
		int result;

		Color.colorToHSV(color, hsv);

		if (hsv[1] == 0) {
			// GREY
			result = 1;
		}
		else {
			float hue = hsv[0];

			if (hue <= 14f) {
				// RED
				result = 2;
			}
			else if (hue <= 33f) {
				// ORANGE
				result = 3;
			}
			else if (hue <= 70f) {
				// YELLOW
				result = 4;
			}
			else if (hue <= 180) {
				// GREEN
				result = 5;
			}
			else if (hue <= 260f) {
				// BLUE
				result = 6;
			}
			else if (hue <= 300f) {
				// PURPLE
				result = 7;
			}
			else if (hue <= 360f) {
				// PINK
				result = 8;
			}
			else {
				// who knows?
				result = 0;
			}
		}

		return result;
	}

	public static Bitmap getScaledBitmap(Bitmap bitmap) {
		int maxTextureSize = (int) (Utils.getMaxTextureSize() * 0.75);

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		int dstWidth = width;
		int dstHeight = height;

		int largestDimension = Math.max(width, height);

		if (largestDimension > maxTextureSize) {
			Log.d(TAG, "Large image");
			float scale = (float) maxTextureSize / (float)largestDimension;
			if (largestDimension == width) {
				Log.d(TAG, "Wide image");
				dstWidth = width;
				dstHeight = (width / height) * height;
			}
			else {
				Log.d(TAG, "Tall image");
				dstHeight = height;
				dstWidth = (height / width) * width;
			}

			dstWidth *= scale;
			dstHeight *= scale;

			Log.d(TAG, "scale: " + scale + "scaled width, height: " + dstWidth + ", " + dstHeight);
		}

		Bitmap output = Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, false);
		if (output != bitmap && bitmap != null) {
			bitmap.recycle();
		}

		return output;
	}

	public static Bitmap getMuzeiImage(Context context) {
		InputStream is = null;
		Bitmap output = null;
		ContentResolver resolver = context.getContentResolver();

		try {
			is = resolver.openInputStream(MuzeiContract.Artwork.CONTENT_URI);
		}
		catch (Exception e) {
			Log.e(TAG, "Error opening input stream from Muzei: " + e.toString());
			e.printStackTrace();
		}

		if (is != null) {
			try {
				BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);
				int maxTextureSize = Utils.getMaxTextureSize();
				int n = (int) (maxTextureSize * 0.30);
				int imageWidth = decoder.getWidth();
				int imageHeight = decoder.getHeight();

				Rect rect;
				if (imageHeight > maxTextureSize * 0.8 || imageWidth > maxTextureSize * 0.8) {
					rect = new Rect((imageWidth / 2) - n, (imageHeight / 2) - n, (imageWidth / 2) + n, (imageHeight / 2) + n);
				}
				else {
					rect = new Rect(0, 0, imageWidth, imageHeight);
				}

				BitmapFactory.Options options = new BitmapFactory.Options();
				output = decoder.decodeRegion(rect, options);
			}
			catch (OutOfMemoryError e) {
				Log.e(TAG, "Out of memory - image too large: " + e.toString());
				e.printStackTrace();
				output = null;
			}
			catch (Exception e) {
				Log.e(TAG, "Error getting bitmap region decoder: " + e.toString());
				output = null;
			}
		}
		else {
			return null;
		}
		return output;
	}

	public static int getThemeFromWallpaper(SharedPreferences preferences) {
		int[] themes = { R.style.AppTheme,
				R.style.Grey, R.style.Red, R.style.DeepOrange, R.style.Amber,
				R.style.Green, R.style.Blue, R.style.DeepPurple, R.style.Pink};
		return themes[WallpaperUtils.whatColor(preferences.getInt(PrefUtils.WALLPAPER_COLOR1, Color.GRAY))];
	}
}
