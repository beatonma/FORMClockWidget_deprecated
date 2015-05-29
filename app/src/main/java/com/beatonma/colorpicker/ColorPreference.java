package com.beatonma.colorpicker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.Preference;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.beatonma.formclockwidget.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Michael Beaton on 14/02/2015.
 *
 * Dual-level color picker.
 *
 * When opening the dialog for the first time the user will see a grid
 * of ColorPickerPatchViews, each representing a different swatch.
 *
 * If multi_select_swatches is true, this is all the user will see and
 * they can select one or more swatches. This is useful for theming as
 * you can get general color preferences without locking it down to
 * individual colors.
 *
 * Else if multi_select_colors is true, touching a Patch reloads the
 * grid with the colors of the chosen swatch (e.g. Tapping the red patch
 * displays the red swatch containing colors from pale pink to dark red).
 * Users can choose multiple specific colors from multiple swatches.
 *
 * Else, same as above but only a single color can be picked.
 */

public class ColorPreference extends Preference {
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

    // If true, show a patch on the preference view displaying current values
    boolean showPreview = true;
    PatchView previewPatch;

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ColorPicker,
                0, 0);

        try {
            showPreview = a.getBoolean(R.styleable.ColorPicker_show_preview, true);
            String tempMode = a.getString(R.styleable.ColorPicker_mode);
            if (tempMode.equals(ATTR_MODE_COLOR_SINGLE)) {
                mode = MODE_COLOR_SINGLE;
            }
            else if (tempMode.equals(ATTR_MODE_COLOR_MULTI)) {
                mode = MODE_COLOR_MULTI;
            }
            else if (tempMode.equals(ATTR_MODE_SWATCH_MULTI)) {
                mode = MODE_SWATCH_MULTI;
            }
            else {
                mode = MODE_COLOR_SINGLE;
            }

            defaultSwatch = a.getInteger(R.styleable.ColorPicker_default_swatch, 0);
            defaultColor = a.getInteger(R.styleable.ColorPicker_default_color, 0);
        }
        finally {
            a.recycle();
        }

        mDataset = new ArrayList<String>();
        selectedItems = new ArrayList<ColorContainer>();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        if (showPreview && mode == MODE_COLOR_SINGLE) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.color_picker_preference_widget, parent, false);
            previewPatch = (PatchView) v.findViewById(R.id.patch);

            return v;
        }
        else {
            return super.onCreateView(parent);
        }
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        if (mDataset == null) {
            mDataset = new ArrayList<String>();
        }

        loadSelectedItems();

        if (showPreview) {
            renderPreview();
        }
    }

    @Override
    public void onClick() {
        dialogTitle = (String) getTitle();

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

    private void renderPreview() {
        if (previewPatch != null) {
			previewPatch.setIsPreview(true);

            if (selectedItems.isEmpty()) {
                previewPatch.color = ColorUtils.getColor(defaultSwatch, defaultColor);
            }
			else {
                switch (mode) {
                    case MODE_COLOR_SINGLE:
                        ColorContainer container = selectedItems.get(0);
                        previewPatch.color = ColorUtils.getColor(container.swatch, container.color);
                        break;
                    case MODE_COLOR_MULTI:
                        break;
                    case MODE_SWATCH_MULTI:
                        break;
                }
            }

			previewPatch.animateEntry(0);
			previewPatch.setSelected(true);
        }
    }

    private void saveSelectedItems() {
        SharedPreferences sp = getSharedPreferences();
        SharedPreferences.Editor editor = sp.edit();
        Set<String> set = new HashSet<String>();

        Iterator<ColorContainer> iterator = selectedItems.iterator();

        while (iterator.hasNext()) {
            ColorContainer container = iterator.next();
            Log.d(TAG, "Saving item " + container.toString());
            set.add(container.toString());
        }

        editor.putStringSet(getKey(), set);
        editor.apply();
    }

    private void loadSelectedItems() {
        selectedItems = new ArrayList<ColorContainer>();
        SharedPreferences sp = getSharedPreferences();
        Set set = sp.getStringSet(getKey(), new HashSet<String>());

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
                } catch (Exception e) {
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
                    dialog.cancel();
                    if (showPreview) {
                        previewPatch.animateColorChange(ColorUtils.getColor(openSwatch, position + 1));
                    }
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
}
