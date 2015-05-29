package com.beatonma.colorpicker;

import android.util.Log;

/**
 * Created by Michael on 07/05/2015.
 */
public class ColorContainer {
    private final static String TAG = "ColorContainer";

    int swatch = -1;
    int color = -1;
    int colorValue = -1;

    public ColorContainer(int swatch, int color) {
        this.swatch = swatch;
        this.color = color;
    }

    public ColorContainer(int swatch, int color, int colorValue) {
        this.swatch = swatch;
        this.color = color;
        this.colorValue = colorValue;
    }

    public ColorContainer(String container) {
        String[] parts = container.split(";");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.contains("swatch=")) {
                this.swatch = Integer.valueOf(part.split("=")[1]);
            }
            else if (part.contains("color=")) {
                this.color = Integer.valueOf(part.split("=")[1]);
            }
            else if (part.contains("color_value=")) {
                this.colorValue = Integer.valueOf(part.split("=")[1]);
            }
        }
//        Log.d(TAG, "swatch = " + this.swatch + "; color = " + this.color + "; colorValue = " + this.colorValue);
    }

    @Override
    public String toString() {
        String output = "swatch=" + swatch + ";color=" + color + ";color_value=" + colorValue;
        return output;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ColorContainer) {
            if (this.toString().equals(other.toString())) {
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }
}
