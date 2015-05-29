package com.beatonma.colorpicker;



import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.beatonma.formclockwidget.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Michael on 14/02/2015.
 */
public class PatchAdapter extends RecyclerView.Adapter<PatchAdapter.ViewHolder> {
    private static final String TAG = "ColorPickerAdapter";
    private Context context;
    private List<String> mDataset;
    int positionInDataset = -1;
    boolean isPreview;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private PatchView colorPatch;

        public ViewHolder(View v) {
            super(v);
            colorPatch = (PatchView) v.findViewById(R.id.patch);
        }
    }

    public PatchAdapter(List<String> dataset, boolean isPreview) {
        mDataset = dataset;
        this.isPreview = isPreview;
    }

    public PatchAdapter(List<String> dataset) {
        mDataset = dataset;
    }

    @Override
    public PatchAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View v;
        if (isPreview) {
            v = LayoutInflater.from(context).inflate(R.layout.color_picker_patch_preview, parent, false);
        }
        else {
            v = LayoutInflater.from(context).inflate(R.layout.color_picker_patch, parent, false);
        }

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (mDataset == null) {
            Log.d(TAG, "Results are null");
        }
        else if (mDataset.isEmpty()) {
            Log.d(TAG, "No results to display.");
        }
        else {
            if (isPreview) {
                try {
                    holder.colorPatch.setColor(Color.parseColor("#ff0000"));
                    holder.colorPatch.setPosition(position);
                    holder.colorPatch.setSelected(false);
                    holder.colorPatch.animateEntry(position);
                }
                catch (Exception e) {
                    Log.e(TAG, "Error setting patch color: " + e.toString());
                }
            }
            else {
                try {
                    holder.colorPatch.setColor(Color.parseColor(mDataset.get(position)));
                    holder.colorPatch.setPosition(position);
                    holder.colorPatch.setSelected(false);
                    holder.colorPatch.animateEntry(position);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting patch color: " + e.toString());
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mDataset == null) {
            return 0;
        }
        else if ((mDataset.isEmpty()) || (mDataset.size() == 0)) {
            return 1;
        }
        else {
            return mDataset.size();
        }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.colorPatch.animateExit(0);
    }

    @Override
    public void onViewAttachedToWindow(ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
    }

    public void updateDataset(List<String> dataset) {
        mDataset = dataset;
        notifyDataSetChanged();
    }

    // TODO make this work or remove before publishing
    // commonElement = position of element to be maintained
    public void animatedUpdateDataset(List<String> dataset, int commonElementPosition) {
        if (commonElementPosition == -1) {
            updateDataset(dataset);
            return;
        }

        int datasetSize = dataset.size();
        String commonElement = "";
        ArrayList<String> removeItems = new ArrayList<String>();

        // Find common element in new dataset
        positionInDataset = dataset.lastIndexOf(mDataset.get(commonElementPosition));
        commonElement = mDataset.get(commonElementPosition);


        // Add everything except common element to a new list to be removed later
        Iterator<String> iterator = mDataset.iterator();
        while (iterator.hasNext()) {
            String item = iterator.next();
            if (!item.equals(commonElement)) {
                removeItems.add(item);
            }
        }

        // Remove everything from newly populated list from mDataset
        iterator = removeItems.iterator();
        while(iterator.hasNext()) {
            int i = mDataset.lastIndexOf(iterator.next());
            removeItem(i);
        }

        if (positionInDataset != -1) {
            // Add new elements
            for (int i = 0; i < datasetSize; i++) {
                if (i < positionInDataset) {
                    addItem(dataset.get(i),mDataset.size() - 1); // Add item before common element
                }
                else if (i > positionInDataset) {
                    addItem(dataset.get(i)); // Add item to end
                }
                else {
                    // item is the common element
                    notifyItemChanged(i);
                }
            }
        }
        else {
            Log.d(TAG, "Element not found. Updating mDataset vanilla style.");
            mDataset = dataset;
            notifyDataSetChanged();
        }
    }

    public void addItem(String item, int position) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void addItem(String item) {
        addItem(item, mDataset.size());
//        mDataset.add(item);
//        notifyItemInserted(mDataset.size());
    }

    public void removeItem(int position) {
        try {
            mDataset.remove(position);
            notifyItemRemoved(position);
        }
        catch (Exception e) {
            Log.e(TAG, "Error removing item: " + e.toString());
        }
    }

}
