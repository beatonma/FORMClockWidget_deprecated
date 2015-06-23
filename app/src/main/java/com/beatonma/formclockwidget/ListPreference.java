package com.beatonma.formclockwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Michael on 20/01/2015.
 */
public class ListPreference extends Preference {
    private final static String TAG = "ListPreference";
    String [] entries;
    String [] entryValues;
    String defaultValue = "-1";

    Context context;

    public ListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        // Get xml attributes
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ListPreference,
                0, 0);

        Resources resources = context.getResources();

        try {
            int idEntries = a.getResourceId(R.styleable.ListPreference_entries, 0);
            if (idEntries != 0) {
                entries = resources.getStringArray(idEntries);
            }

            int idEntryValues = a.getResourceId(R.styleable.ListPreference_entryValues, 0);
            if (idEntryValues != 0) {
                entryValues = resources.getStringArray(idEntryValues);
            }

            defaultValue = a.getString(R.styleable.ListPreference_defaultValue);
        }
        finally {
            a.recycle();
        }
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
    }

    @Override
    public void onClick() {
        String title = (String) getTitle();
        int preselection;

        String selected = getPreference(getKey());

        List v = Arrays.asList(entryValues);
        if (v.contains(selected)) {
            preselection = v.indexOf(selected);
        }
        else if (v.contains(defaultValue)) {
            preselection = v.indexOf(defaultValue);
        }
        else {
            preselection = 0;
        }

        new MaterialDialog.Builder(context)
                .title(title)
                .items(entries)
                .itemsCallbackSingleChoice(preselection, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View v, int which, CharSequence text) {
                        updatePreference(entryValues[which]);
                        return true;
                    }
                })
                .positiveText("OK")
                .show();
    }

    private void updatePreference(String s) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(getKey(), s);
        editor.commit();
    }

    private String getPreference(String key) {
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        String result = "-1";
        if (sp != null) {
            result = sp.getString(key, "-1");
        }
        else {
            Log.e(TAG, "Shared preferences = null");
        }
        return result;
    }
}
