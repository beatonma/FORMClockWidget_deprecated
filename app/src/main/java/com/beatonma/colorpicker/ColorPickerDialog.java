package com.beatonma.colorpicker;

/**
 * Created by Michael on 18/02/2015.
 */

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.beatonma.formclockwidget.ConfigActivity;
import com.beatonma.formclockwidget.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Michael on 14/02/2015.
 */
public class ColorPickerDialog extends DialogFragment implements OnColorPickedListener {
	private final static String TAG = "ColorPicker";
	private final static int VIEW_LEVEL_SWATCHES = 0;
	private final static int VIEW_LEVEL_COLORS = 1;

	private final static int MODE_COLOR_SINGLE = 0;
	private final static int MODE_COLOR_MULTI = 1;
	private final static int MODE_SWATCH_MULTI = 2;

	private final static String ATTR_MODE_COLOR_SINGLE = "color_single";
	private final static String ATTR_MODE_COLOR_MULTI = "color_multi";
	private final static String ATTR_MODE_SWATCH_MULTI = "swatch_multi";

	private ArrayList<String> mDataset;
	private RecyclerView recyclerView;
	private PatchAdapter adapter;
	View backButton;
	View okButton;

	Context context;
	private int level = VIEW_LEVEL_SWATCHES;
	private int mode = MODE_COLOR_SINGLE;

	// Dialog bits
	MaterialDialog dialog;
	private String dialogTitle;

	// If a swatch is currently in view this will hold its identifier, else value should be -1
	private int openSwatch = -1;

	private int defaultSwatch = 0;
	private int defaultColor = 0;

	// Remember which colours and/or swatches have been selected
	private ArrayList<ColorContainer> selectedItems;

    OnColorPickedListener listener;

	String key = "";

    public static ColorPickerDialog newInstance(String key) {
        ColorPickerDialog d = new ColorPickerDialog();
        Bundle args = new Bundle();
		args.putString("key", key);
        d.setArguments(args);
        return d;
    }

	@Override
	public void onCreate(Bundle saved) {
		super.onCreate(saved);

		context = getActivity();
		Bundle args = getArguments();
		if (args != null) {
			key = args.getString("key", "");
		}

		mDataset = new ArrayList<String>();
		loadSelectedItems();
	}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		dialogTitle = "Color";

		dialog = new MaterialDialog.Builder(context)
				.title(dialogTitle)
				.customView(R.layout.color_picker, true)
//                .btnSelector(R.drawable.dialog_button_selector)
				.autoDismiss(false)
				.callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						level = VIEW_LEVEL_SWATCHES;
						openSwatch = -1;
						saveSelectedItems();
						dialog.cancel();
					}

					@Override
					public void onNegative(MaterialDialog dialog) {
						level = VIEW_LEVEL_SWATCHES;
						openSwatch = -1;
						new PaletteFiller(VIEW_LEVEL_SWATCHES).execute();
					}
				})
				.positiveText("OK")
				.negativeText("BACK")
				.build();

		dialog.show();

		backButton = dialog.getActionButton(DialogAction.NEGATIVE);
		okButton = dialog.getActionButton(DialogAction.POSITIVE);

		if (mode == MODE_COLOR_SINGLE) {
			okButton.setVisibility(View.GONE);
		}

		recyclerView = (RecyclerView) dialog.findViewById(R.id.recycler_view);
		initRecycler(recyclerView);

		level = VIEW_LEVEL_SWATCHES;
		openSwatch = -1;
		new PaletteFiller(VIEW_LEVEL_SWATCHES).execute();

        return dialog;
    }

	private void initRecycler(RecyclerView recyclerView) {
		GridLayoutManager layoutManager = new GridLayoutManager(context, 4);
		recyclerView.setLayoutManager(layoutManager);
		adapter = new PatchAdapter(mDataset);
		recyclerView.setAdapter(adapter);

		recyclerView.addOnItemTouchListener(new ColorPatchOnClickListener());
	}

	private void updateSelection() {
		switch (mode) {
			case MODE_COLOR_SINGLE:
				updateSingleColorSelection();
				break;
			case MODE_COLOR_MULTI:
				updateMultiColorSelection();
				break;
			case MODE_SWATCH_MULTI:
				updateMultiSwatchSelection();
				break;
		}
	}

	private void updateSingleColorSelection() {
		if (!selectedItems.isEmpty()) {
			ColorContainer container = selectedItems.get(0);
			Log.d(TAG, "Selecting single color " + container.toString());
			if (openSwatch == -1) {
				selectPatchAt(container.swatch);
			}
			else if (openSwatch == container.swatch) {
				selectPatchAt(container.color);
			}
		}
	}

	private void updateMultiColorSelection() {
		Iterator<ColorContainer> iterator = selectedItems.iterator();
		while (iterator.hasNext()) {
			ColorContainer container = iterator.next();
			Log.d(TAG, "Selecting multi color " + container.toString());
			if (openSwatch == -1) {
				selectPatchAt(container.swatch);
			}
			else if (openSwatch == container.swatch) {
				selectPatchAt(container.color);
			}
		}
	}

	private void updateMultiSwatchSelection() {
		Iterator<ColorContainer> iterator = selectedItems.iterator();
		while (iterator.hasNext()) {
			ColorContainer container = iterator.next();
			Log.d(TAG, "Selecting multi swatch " + container.toString());
			selectPatchAt(container.swatch);
		}
	}

	private void selectPatchAt(int position) {
		FrameLayout frame = (FrameLayout) recyclerView.getChildAt(position);
		if (frame != null) {
			PatchView patch = (PatchView) frame.findViewById(R.id.patch);
			patch.setSelected(true);
			patch.invalidate();
		}
	}



	private void saveSelectedItems() {
		SharedPreferences sp = context.getSharedPreferences(ConfigActivity.PREFS, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		Set<String> set = new HashSet<String>();

		Iterator<ColorContainer> iterator = selectedItems.iterator();

		while (iterator.hasNext()) {
			ColorContainer container = iterator.next();
			Log.d(TAG, "Saving item " + container.toString());
			set.add(container.toString());
		}

		editor.putStringSet(key, set);
		editor.apply();
	}

	private void loadSelectedItems() {
		selectedItems = new ArrayList<ColorContainer>();
		SharedPreferences sp = context.getSharedPreferences(ConfigActivity.PREFS, Context.MODE_PRIVATE);
		Set set = sp.getStringSet(key, new HashSet<String>());

		Iterator<String> iterator = set.iterator();

		while (iterator.hasNext()) {
			String s = iterator.next();
			selectedItems.add(new ColorContainer(s));
		}
	}

	private class ColorPatchOnClickListener implements RecyclerView.OnItemTouchListener {
		@Override
		public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				View childView = view.findChildViewUnder(event.getX(), event.getY());
				int position = view.getChildAdapterPosition(childView);

				try {
					FrameLayout chosenFrame = (FrameLayout) childView;
					PatchView chosenPatch = (PatchView) chosenFrame.findViewById(R.id.patch);

					switch (mode) {
						case MODE_COLOR_SINGLE:
							modeSingleColor(position);
							break;
						case MODE_COLOR_MULTI:
							modeMultiColor(chosenPatch, position);
							break;
						case MODE_SWATCH_MULTI:
							modeMultiSwatch(chosenPatch, position);
							break;
					}
				}
				catch (Exception e) {
					Log.e(TAG, "Error handling click: " + e.toString());
				}
			}

			return false;
		}

		@Override
		public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) { }

		private void modeSingleColor(int position) {
			switch (level) {
				case VIEW_LEVEL_SWATCHES:
					level = VIEW_LEVEL_COLORS;
					openSwatch = position;
					new PaletteFiller(level, position).execute();
					break;
				case VIEW_LEVEL_COLORS:
					level = VIEW_LEVEL_SWATCHES;
					selectedItems.clear();
					selectedItems.add(new ColorContainer(openSwatch, position));
					saveSelectedItems();
					listener.onColorPicked(openSwatch, position);
					dialog.cancel();
					break;
			}
		}

		private void modeMultiColor(PatchView patch, int position) {
			switch (level) {
				case VIEW_LEVEL_SWATCHES:
					level = VIEW_LEVEL_COLORS;
					openSwatch = position;
					new PaletteFiller(level, position).execute();
					break;
				case VIEW_LEVEL_COLORS:
					if (patch.isSelected()) {
						patch.setSelected(false);
						patch.setTouched(true);
						selectedItems.remove(new ColorContainer(openSwatch, position));
						patch.invalidate();
					}
					else {
						patch.setSelected(true);
						patch.setTouched(true);
						selectedItems.add(new ColorContainer(openSwatch, position));
						patch.invalidate();
					}
					break;
			}
		}

		private void modeMultiSwatch(PatchView patch, int position) {
			if (patch.isSelected()) {
				patch.setSelected(false);
				patch.setTouched(true);
				selectedItems.remove(new ColorContainer(position, -1));
				patch.invalidate();
			}
			else {
				patch.setSelected(true);
				patch.setTouched(true);
				selectedItems.add(new ColorContainer(position, -1));
				patch.invalidate();
			}
		}
	}

	// Populate recyclerView with the correct colors
	public class PaletteFiller extends AsyncTask<Integer, Void, String> {
		int level = -1;
		int chosenPosition = -1;

		public PaletteFiller(int newLevel) {
			this.level = newLevel;
		}

		public PaletteFiller(int newLevel, int chosen) {
			this.level = newLevel;
			this.chosenPosition = chosen;
		}

		@Override
		protected String doInBackground(Integer... n) {
			switch (level) {
				case VIEW_LEVEL_SWATCHES:
					try {
						String[][] palettes = ColorUtils.PALETTES;

						for (int i = 0; i < palettes.length; i++) {
							String[] swatch = palettes[i];
							// Add color 500 from each color swatch
							mDataset.add(swatch[0]);
						}
					} catch (Exception e) {
						Log.e(TAG, "AsyncTask error: " + e.toString());
					}
					break;
				case VIEW_LEVEL_COLORS:
					try {
						String[][] palettes = ColorUtils.PALETTES;

						String[] swatch = palettes[chosenPosition];
						for (int i = 0; i < swatch.length; i++) {
							if (i == 0) {
								// Skip 500 colour at start of list
							} else {
								mDataset.add(swatch[i]);
							}
						}
					} catch (Exception e) {
						Log.e(TAG, "AsyncTask error." + e.toString());
						e.printStackTrace();
					}
					break;
				default:
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			mDataset = new ArrayList<String>();
		}

		@Override
		protected void onPostExecute(String file) {
			if (level == VIEW_LEVEL_SWATCHES) {
				backButton.setVisibility(View.INVISIBLE);
				adapter.animatedUpdateDataset(mDataset, -1); // TODO change this to adapter.positionInDataset when animation works nicely
				dialog.setTitle(dialogTitle + (dialogTitle.contains("palette") ? "" : " palette"));
			} else if (level == VIEW_LEVEL_COLORS) {
				backButton.setVisibility(View.VISIBLE);
				adapter.animatedUpdateDataset(mDataset, -1); // TODO change this to chosenPosition when animation works nicely
				dialog.setTitle(dialogTitle);
			}
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					updateSelection();
				}
			}, 200);
		}
	}

	@Override
	public void onColorPicked(int swatch, int color) {
		return;
	}

	public void setOnColorPickedListener(OnColorPickedListener listener) {
		this.listener = listener;
	}
}