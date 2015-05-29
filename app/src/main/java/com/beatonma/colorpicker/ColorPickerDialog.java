//package com.beatonma.colorpicker;
//
///**
// * Created by Michael on 18/02/2015.
// */
//
//import android.app.Dialog;
//import android.app.DialogFragment;
//import android.content.Context;
//import android.os.Bundle;
//import android.support.v7.widget.GridLayoutManager;
//import android.support.v7.widget.RecyclerView;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//
//import com.afollestad.materialdialogs.MaterialDialog;
//import com.beatonma.formclockwidget.R;
//
//import java.util.ArrayList;
//
///**
// * Created by Michael on 14/02/2015.
// */
//public class ColorPickerDialog extends DialogFragment {// implements OnColorPickedListener {
//    private final static String TAG = "ColorPicker";
//    private ArrayList<String> mDataset;
//    private RecyclerView recyclerView;
//    private GridLayoutManager layoutManager;
//    private PatchAdapter adapter;
//    Button backButton;
//
//    Context context;
//    private int level = 0;
//    MaterialDialog dialog;
//    private String dialogTitle;
//
//    // Single select
//    private int selectedSwatch = -1;
//    private int selectedColor = -1;
//
//    // If a swatch is currently in view this will hold its identifier
//    private int openSwatch = -1;
//
//    // Multi select
//    private boolean multiSelect = false;
//    private ArrayList<String> selectedColors;
//
//    OnColorPickedListener listener;
//
//    String appEntry = "";
//
//    static ColorPickerDialog newInstance(String s) {
//        ColorPickerDialog d = new ColorPickerDialog();
//        Bundle args = new Bundle();
//        args.putString("appEntry", s);
//        d.setArguments(args);
//        return d;
//    }
//
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        context = getActivity();
//
//        appEntry = getArguments().getString("appEntry", "");
//
//        mDataset = new ArrayList<String>();
//        selectedColors = new ArrayList<String>();
//
//        if (appEntry.equals("")) {
//            selectedColor = -1;
//            selectedSwatch = -1;
//        } else {
//            String[] parts = appEntry.split(";");
//            selectedSwatch = Integer.valueOf(parts[1]);
//            selectedColor = Integer.valueOf(parts[2]) - 1;
//        }
//
//        Log.d(TAG, "Loaded color #" + selectedColor + " from swatch #" + selectedSwatch);
//
//        dialogTitle = "Choose a colour";
//        boolean wrapInScrollView = true;
//        dialog = new MaterialDialog.Builder(context)
//                .title(dialogTitle)
//                .customView(R.layout.color_picker, wrapInScrollView)
//                .build();
//
//        dialog.show();
//
//        recyclerView = (RecyclerView) dialog.findViewById(R.id.recycler_view);
//        initRecycler(recyclerView);
//
//        backButton = (Button) dialog.findViewById(R.id.back_button);
//        backButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                level = 0;
//                openSwatch = -1;
//                new PaletteFiller().execute(0);
//            }
//        });
//
//        level = 0;
//        openSwatch = -1;
//        new PaletteFiller().execute(0);
//
//        return dialog;
//    }
//
//    private void initRecycler(RecyclerView recyclerView) {
//        // Grid layout with 4 items in each row
//        layoutManager = new GridLayoutManager(context, 4);
//        recyclerView.setLayoutManager(layoutManager);
//        adapter = new ColorPickerAdapter(mDataset);
//        recyclerView.setAdapter(adapter);
//
//        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
//            @Override
//            public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_UP) {
//                    View childView = view.findChildViewUnder(event.getX(), event.getY());
//                    int position = view.getChildPosition(childView);
//
//                    try {
//                        FrameLayout chosenFrame = (FrameLayout) childView;
//                        ColorPickerPatchView chosenColor = (ColorPickerPatchView) chosenFrame.findViewById(R.id.patch);
//
//                        switch (level) {
//                            case 0:
//                                // Swatch chosen
//                                level = 1;
//                                openSwatch = position;
//                                new PaletteFiller().execute(1, position);
//                                break;
//                            case 1:
//                                // Color chosen
//                                level = 0;
//                                selectedSwatch = openSwatch;
//                                selectedColor = position;
//                                listener.onColorPicked(selectedSwatch, selectedColor);
//                                //saveColor(openSwatch, position);
//                                dialog.cancel();
//                                break;
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, "Error handling click: " + e.toString());
//                    }
//                }
//
//                return false;
//            }
//
//            @Override
//            public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) {
//            }
//        });
//    }
//
//    private void updateSelection() {
//        int position = level == 0 ? Integer.valueOf(selectedSwatch) : Integer.valueOf(selectedColor);
//        if (level == 1 && openSwatch != selectedSwatch) {
//            // Don't select anything if we're in a new swatch.
//            Log.d(TAG, "Current colour is not in visible swatch.");
//            return;
//        } else {
//            FrameLayout frame = (FrameLayout) recyclerView.getChildAt(position);
//            if (frame != null) {
//                ColorPickerPatchView colorPatch = (ColorPickerPatchView) frame.findViewById(R.id.patch);
//                colorPatch.setSelected(true);
//                colorPatch.invalidate();
//            } else {
//                Log.e(TAG, "Error updating selection: frame=null for child at " + position);
//            }
//        }
//    }
//
//    @Override
//    public void onColorPicked(int swatch, int color) {
//        return;
//    }
//
//    public void setOnColorPickedListener(OnColorPickedListener listener) {
//        this.listener = listener;
//    }
//
//    public class PaletteFiller extends AsyncTask<Integer, Void, String> {
//        int chosenPosition = -1;
//
//        @Override
//        protected String doInBackground(Integer... n) {
//            int level = n[0];
//
//            switch (level) {
//                case 0:
//                    try {
//                        String[][] palettes = ColorUtils.PALETTES;
//
//                        for (int i = 0; i < palettes.length; i++) {
//                            String[] swatch = palettes[i];
//                            // Add color 500 from each color swatch
//                            mDataset.add(swatch[0]);
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, "AsyncTask error." + e.toString());
//                        e.printStackTrace();
//                    }
//                    break;
//                case 1:
//                    try {
//                        int chosen = n[1];
//                        chosenPosition = n[1];
//                        String[][] palettes = ColorUtils.PALETTES;
//
//                        String[] swatch = palettes[chosen];
//                        for (int i = 0; i < swatch.length; i++) {
//                            if (i == 0) {
//                                // Skip 500 colour at start of list
//                            } else {
//                                mDataset.add(swatch[i]);
//                            }
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, "AsyncTask error." + e.toString());
//                        e.printStackTrace();
//                    }
//                    break;
//                default:
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            mDataset = new ArrayList<String>();
//        }
//
//        @Override
//        protected void onPostExecute(String file) {
//            if (level == 0) {
//                backButton.setVisibility(View.INVISIBLE);
//                adapter.animatedUpdateDataset(mDataset, -1);
//                dialog.setTitle(dialogTitle + " palette");
//            } else if (level == 1) {
//                backButton.setVisibility(View.VISIBLE);
//                adapter.animatedUpdateDataset(mDataset, -1);
//                dialog.setTitle(dialogTitle);
//            }
//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    updateSelection();
//                }
//            }, 200);
//        }
//    }
//}
//
//interface OnColorPickedListener {
//    public abstract void onColorPicked(int swatch, int color);
//}