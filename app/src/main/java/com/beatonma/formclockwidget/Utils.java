package com.beatonma.formclockwidget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES10;
import android.os.Build;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.List;

/**
 * Created by Michael on 28/05/2015.
 */
public class Utils {
	private final static String TAG = "Utils";


	// Used for circular reveal/hide methods to easily start animation from a certain side
	// or corner if two of these are used
	public final static int CIRCULAR_LEFT = 0;
	public final static int CIRCULAR_TOP = 1;
	public final static int CIRCULAR_RIGHT = 2;
	public final static int CIRCULAR_BOTTOM = 3;

	public static boolean isKitkat() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
	}

	public static boolean isLollipop() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	}

	public static int getMaxTextureSize() {
		int maxTextureSize = 0;

		EGLDisplay dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
		int[] vers = new int[2];
		EGL14.eglInitialize(dpy, vers, 0, vers, 1);

		int[] configAttr = {
				EGL14.EGL_COLOR_BUFFER_TYPE, EGL14.EGL_RGB_BUFFER,
				EGL14.EGL_LEVEL, 0,
				EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
				EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
				EGL14.EGL_NONE
		};
		EGLConfig[] configs = new EGLConfig[1];
		int[] numConfig = new int[1];
		EGL14.eglChooseConfig(dpy, configAttr, 0,
				configs, 0, 1, numConfig, 0);
		if (numConfig[0] == 0) {
			// TROUBLE! No config found.
		}
		EGLConfig config = configs[0];

		int[] surfAttr = {
				EGL14.EGL_WIDTH, 64,
				EGL14.EGL_HEIGHT, 64,
				EGL14.EGL_NONE
		};
		EGLSurface surf = EGL14.eglCreatePbufferSurface(dpy, config, surfAttr, 0);

		int[] ctxAttrib = {
				EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
				EGL14.EGL_NONE
		};
		EGLContext ctx = EGL14.eglCreateContext(dpy, config, EGL14.EGL_NO_CONTEXT, ctxAttrib, 0);

		EGL14.eglMakeCurrent(dpy, surf, surf, ctx);

		int[] maxSize = new int[1];
		GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0);
		maxTextureSize = maxSize[0];

		EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
				EGL14.EGL_NO_CONTEXT);
		EGL14.eglDestroySurface(dpy, surf);
		EGL14.eglDestroyContext(dpy, ctx);
		EGL14.eglTerminate(dpy);

		return maxTextureSize;
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

	public static int pxToDp(Context context, int px) {
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
		return dp;
	}

	public static int getScreenWidth(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return dm.widthPixels;
	}

	public static int getScreenHeight(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return dm.heightPixels;
	}

	// generate ripple drawable and set as view background
	public static void setBackground(View v, int color, int highlight) {
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
		hsv[0] = (hsv[0] + 50) % 360; // hue
		hsv[1] += hsv[1] < 0.5f ? 0.2f : -0.2f; // saturation
		hsv[2] += hsv[2] < 0.5f ? 0.3f : -0.3f; // value/brightness

		setBackground(v, color, Color.HSVToColor(hsv));
	}

	public static void createCircularReveal(View v) {
		// get the center for the clipping circle
		int cx = (v.getLeft() + v.getRight()) / 2;
		int cy = (v.getTop() + v.getBottom()) / 2;
		createCircularReveal(v, cx, cy);
	}

	public static void createCircularReveal(View v, int cx, int cy) {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				int finalRadius = Math.max(v.getWidth(), v.getHeight());

				Animator anim = ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, finalRadius);
				anim.setInterpolator(new AccelerateDecelerateInterpolator());

				v.setVisibility(View.VISIBLE);
				anim.start();
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Error creating circular reveal: " + e.toString());
			v.setVisibility(View.VISIBLE);
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
		try {
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
		catch (Exception e) {
			Log.e(TAG, "Error creating circular hide: " + e.toString());
			v.setVisibility(View.INVISIBLE);
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

	public static String[] paletteToStringArray(Palette palette) {
		List<Palette.Swatch> swatches = palette.getSwatches();
		String[] output = new String[swatches.size()];

		int i = 0;
		for (Palette.Swatch swatch : swatches) {
			output[i] = String.format("#%06X", 0xFFFFFF & swatch.getRgb());
		}

		return output;
	}
}
