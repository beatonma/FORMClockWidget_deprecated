package com.beatonma.formclockwidget;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;

import com.beatonma.colorpicker.ColorPickerDialog;
import com.beatonma.colorpicker.ColorUtils;
import com.beatonma.colorpicker.OnColorPickedListener;

/**
 * Created by Michael Beaton on 29/05/2015.
 */
public class SettingsFragment extends Fragment {
	private final static String TAG = "SettingsFragment";

	ConfigActivity context;
	SharedPreferences preferences;

	ImageButton buttonColor1;
	ImageButton buttonColor2;
	ImageButton buttonColor3;

	View container;
	View colorContainer;

	@Override
	public void onCreate(Bundle saved) {
		super.onCreate(saved);
		context = (ConfigActivity) getActivity();
		context.setTheme(getThemeFromWallpaper());
		preferences = context.getSharedPreferences(ConfigActivity.PREFS, Context.MODE_PRIVATE);
	}

	public static SettingsFragment newInstance() {
		SettingsFragment fragment = new SettingsFragment();
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle saved) {
		final View v = inflater.inflate(R.layout.fragment_settings, parent, false);

		colorContainer = v.findViewById(R.id.colors_container);
		container = v.findViewById(R.id.container);

		buttonColor1 = (ImageButton) v.findViewById(R.id.button_color1);
		buttonColor2 = (ImageButton) v.findViewById(R.id.button_color2);
		buttonColor3 = (ImageButton) v.findViewById(R.id.button_color3);

		Utils.setBackground(buttonColor1, context.color1);
		Utils.setBackground(buttonColor2, context.color2);
		Utils.setBackground(buttonColor3, context.color3);

		buttonColor1.setOnClickListener(new OnButtonClickedListener(1));
		buttonColor2.setOnClickListener(new OnButtonClickedListener(2));
		buttonColor3.setOnClickListener(new OnButtonClickedListener(3));

		if (preferences.getBoolean("pref_use_wallpaper_palette", true)) {
			ViewTreeObserver vto = v.findViewById(R.id.colors_container).getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					hideColors(false);
					v.findViewById(R.id.colors_container).getViewTreeObserver().removeOnGlobalLayoutListener(this);
				}
			});
		}

		return v;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public void updateButtons() {
			Utils.setBackground(buttonColor1, context.color1);
			Utils.setBackground(buttonColor2, context.color2);
			Utils.setBackground(buttonColor3, context.color3);

			context.updatePreview();
//		}
	}

	private class OnButtonClickedListener implements View.OnClickListener {
		int colorIndex = 0;

		public OnButtonClickedListener(int colorIndex) {
			this.colorIndex = colorIndex;
		}

		@Override
		public void onClick(View v) {
			FragmentManager fm = context.getFragmentManager();
			ColorPickerDialog colorDialog;
			OnColorPickedListener listener;

			switch (colorIndex) {
				case 1:
					colorDialog = ColorPickerDialog.newInstance("pref_color1");
					listener = new OnColorPickedListener() {
						@Override
						public void onColorPicked(int swatch, int color) {
							context.color1 = ColorUtils.getColor(swatch, color + 1);
							updateButtons();
						}
					};
					break;
				case 2:
					colorDialog = ColorPickerDialog.newInstance("pref_color2");
					listener = new OnColorPickedListener() {
						@Override
						public void onColorPicked(int swatch, int color) {
							context.color2 = ColorUtils.getColor(swatch, color + 1);
							updateButtons();
						}
					};
					break;
				case 3:
					colorDialog = ColorPickerDialog.newInstance("pref_color3");
					listener = new OnColorPickedListener() {
						@Override
						public void onColorPicked(int swatch, int color) {
							context.color3 = ColorUtils.getColor(swatch, color + 1);
							updateButtons();
						}
					};
					break;
				default:
					colorDialog = ColorPickerDialog.newInstance("pref_color1");
					listener = new OnColorPickedListener() {
						@Override
						public void onColorPicked(int swatch, int color) {
							context.color1 = ColorUtils.getColor(swatch, color + 1);
							updateButtons();
						}
					};
			}

			colorDialog.setOnColorPickedListener(listener);
			colorDialog.show(fm, "ColorPickerDialog");
		}
	}

	public void updatePreferences(SharedPreferences sp) {
		if (sp.getBoolean("pref_use_wallpaper_palette", true)) {
			hideColors(true);
		} else {
			showColors(true);
		}
	}

	public void showColors(boolean animate) {
		if (colorContainer != null && container != null) {
			if (animate) {
				colorContainer.animate()
						.alpha(1f)
						.setDuration(300)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.start();

				container.animate()
						.setDuration(300)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.translationY(0)
						.start();
			}
			else {
				container.setTranslationY(0);
				colorContainer.setAlpha(1f);
			}
		}
	}

	public void hideColors(boolean animate) {
		if (colorContainer != null && container != null) {
			if (animate) {
				colorContainer.animate()
						.alpha(0f)
						.setDuration(300)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.start();

				container.animate()
						.setDuration(300)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.translationY(-colorContainer.getMeasuredHeight())
						.start();
			}
			else {
				container.setTranslationY(-colorContainer.getMeasuredHeight());
				colorContainer.setAlpha(0f);
			}
		}
	}

	// Get a color from wallpaper and guess what it's closest to. Used to change color of pref widgets.
	private int getThemeFromWallpaper() {
		int[] themes = { R.style.AppTheme,
				R.style.Grey, R.style.Red, R.style.DeepOrange, R.style.Amber,
				R.style.Green, R.style.Blue, R.style.DeepPurple, R.style.Pink};
		return themes[Utils.getThemeKeyFromWallpaper(context)];
	}
}
