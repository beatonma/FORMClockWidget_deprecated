package com.beatonma.formclockwidget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Created by Michael on 28/05/2015.
 */
public class Utils {
	public final static int CIRCULAR_LEFT = 0;
	public final static int CIRCULAR_TOP = 1;
	public final static int CIRCULAR_RIGHT = 2;
	public final static int CIRCULAR_BOTTOM = 3;

	public static Palette getWallpaperPalette(Context context) {
		Bitmap bitmap = getWallpaperBitmap(context);
		return Palette.generate(bitmap);
	}

	public static Bitmap getWallpaperBitmap(Context context) {
		WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
		Drawable drawable = wallpaperManager.getDrawable();
		return drawableToBitmap(drawable);
	}

	public static Bitmap drawableToBitmap (Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable)drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	public static int dpToPx(Context context, int dp) {
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
		return px;
	}

	public static int getScreenWidthPx(Context context) {
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		int px = displayMetrics.widthPixels;
		return px;
	}

	public static void setBackground(View v, int color, int highlight) { // generate ripple drawable and set as view background
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			int[][] states = new int[][]{
					new int[]{android.R.attr.state_enabled},
					new int[]{android.R.attr.state_checked},
					new int[]{android.R.attr.state_pressed},
			};
			int [] colors = new int[] {
					highlight,
					highlight,
					highlight
			};
			ColorStateList stateList = new ColorStateList(states, colors);

			RippleDrawable ripple = new RippleDrawable(stateList, new ColorDrawable(color), null);
			v.setBackground(ripple);
		}
		else {
			StateListDrawable states = new StateListDrawable();
			states.addState(new int[] {android.R.attr.state_pressed}, new ColorDrawable(highlight));
			states.addState(new int[] {}, new ColorDrawable(color));
			v.setBackground(states);
		}
	}

	public static void setBackground(View v, int color) {
		float[] hsv = new float[3];

		Color.colorToHSV(color, hsv);
		hsv[1] += hsv[1] < 0.5f ? -0.1f : 0.1f;
		hsv[2] += hsv[2] < 0.5f ? -0.2f : 0.2f;

		setBackground(v, color, Color.HSVToColor(hsv));
	}

	public static void createCircularReveal(View v) {
		// get the center for the clipping circle
		int cx = (v.getLeft() + v.getRight()) / 2;
		int cy = (v.getTop() + v.getBottom()) / 2;
		createCircularReveal(v, cx, cy);
	}

	public static void createCircularReveal(View v, int cx, int cy) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			// get the final radius for the clipping circle
			int finalRadius = Math.max(v.getWidth(), v.getHeight());

			// create the animator for this view (the start radius is zero)
			Animator anim = ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, finalRadius);
			anim.setInterpolator(new AccelerateDecelerateInterpolator());

			// make the view visible and start the animation
			v.setVisibility(View.VISIBLE);
			anim.start();
		}
	}

	public static void createCircularReveal(View child, View parent) {
		if (parent == null) {
			createCircularReveal(child);
		}
		else {
			Point parentCenter = getCenter(parent);
			createCircularReveal(child, parentCenter.x, parentCenter.y);
		}
	}

	// Create reveal from a certain side
	public static void createCircularReveal(View v, int[] sides) {
		int cx = (v.getLeft() + v.getRight()) / 2;
		int cy = (v.getTop() + v.getBottom()) / 2;

		for (int i : sides) {
			switch (i) {
				case CIRCULAR_LEFT:
					cx = 0;
					break;
				case CIRCULAR_RIGHT:
					cx = v.getRight();
					break;
			}
			switch (i) {
				case CIRCULAR_TOP:
					cy = 0;
					break;
				case CIRCULAR_BOTTOM:
					cy = v.getBottom();
					break;
			}
		}

		createCircularReveal(v, cx, cy);
	}

	public static void createCircularHide(View v) {
		int cx = (v.getLeft() + v.getRight()) / 2;
		int cy = (v.getTop() + v.getBottom()) / 2;
		createCircularHide(v, cx, cy);
	}

	public static void createCircularHide(final View v, int cx, int cy) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			// get the initial radius for the clipping circle
			int initialRadius = v.getWidth();

			// create the animation (the final radius is zero)
			Animator anim = ViewAnimationUtils.createCircularReveal(v, cx, cy, initialRadius, 0);
			anim.setInterpolator(new AccelerateDecelerateInterpolator());

			// make the view invisible when the animation is done
			anim.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					v.setVisibility(View.INVISIBLE);
				}
			});

			// start the animation
			anim.start();
		}
	}

	public static void createCircularHide(View child, View parent) {
		if (parent == null) {
			createCircularHide(child);
		}
		else {
			Point parentCenter = getCenter(parent);
			createCircularHide(child, parentCenter.x, parentCenter.y);
		}
	}

	// Create reveal from a certain side
	public static void createCircularHide(View v, int[] sides) {
		int cx = (v.getLeft() + v.getRight()) / 2;
		int cy = (v.getTop() + v.getBottom()) / 2;

		for (int i : sides) {
			switch (i) {
				case CIRCULAR_LEFT:
					cx = 0;
					break;
				case CIRCULAR_RIGHT:
					cx = v.getRight();
					break;
			}
			switch (i) {
				case CIRCULAR_TOP:
					cy = 0;
					break;
				case CIRCULAR_BOTTOM:
					cy = v.getBottom();
					break;
			}
		}

		createCircularHide(v, cx, cy);
	}

	public static Point getCenter(View v) {
		Point point = new Point();
		point.x = (int) (v.getX() + (v.getMeasuredWidth() / 2));
		point.y = (int) (v.getY() + (v.getMeasuredHeight() / 2));
		return point;
	}
}
