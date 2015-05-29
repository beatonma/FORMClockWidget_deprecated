package com.beatonma.colorpicker;

import android.content.SharedPreferences;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Michael Beaton on 12/02/2015.
 *
 * Swatches and colors below taken from Google's Material Design color palette page
 * found at http://www.google.com/design/spec/style/color.html#color-color-palette
 *
 * Each swatch starts with its main '500' color, followed by the full swatch
 * including accent colours.
 *
 * All swatches are accessible via PALETTES and a few subsets are included for
 * convenience: HOT, WARM, COOL, FRESH.
 *
 * Use method getColor(String[][] collections, int swatch, int color) to get
 * the integer value of a color. For convenience, getColor(swatch, color)
 * returns getColor(PALETTES, swatch, color).
 *
 */
public class ColorUtils {
	public final static String[] RED = {
			"#F44336",
			"#FFEBEE",
			"#FFCDD2",
			"#EF9A9A",
			"#FF8A80",
			"#E57373",
			"#FF5252",
			"#EF5350",
			"#FF1744",
			"#F44336",
			"#E53935",
			"#D32F2F",
			"#D50000",
			"#C62828",
			"#B71C1C"
	};
	public final static String[] PINK = {
			"#E91E63",
			"#FCE4EC",
			"#F8BBD0",
			"#F48FB1",
			"#FF80AB",
			"#F06292",
			"#EC407A",
			"#FF4081",
			"#F50057",
			"#E91E63",
			"#D81B60",
			"#C2185B",
			"#C51162",
			"#AD1457",
			"#880E4F"
	};
	public final static String[] PURPLE = {
			"#9C27B0",
			"#F3E5F5",
			"#E1BEE7",
			"#CE93D8",
			"#AB47BC",
			"#BA68C8",
			"#EA80FC",
			"#D500F9",
			"#E040FB",
			"#9C27B0",
			"#8E24AA",
			"#7B1FA2",
			"#AA00FF",
			"#6A1B9A",
			"#4A148C"
	};
	public final static String[] DEEP_PURPLE = {
			"#673AB7",
			"#EDE7F6",
			"#D1C4E9",
			"#B39DDB",
			"#B388FF",
			"#7C4DFF",
			"#9575CD",
			"#7E57C2",
			"#651FFF",
			"#673AB7",
			"#5E35B1",
			"#512DA8",
			"#6200EA",
			"#4527A0",
			"#311B92"
	};
	public final static String[] INDIGO = {
			"#3F51B5",
			"#E8EAF6",
			"#C5CAE9",
			"#9FA8DA",
			"#8C9EFF",
			"#536DFE",
			"#7986CB",
			"#5C6BC0",
			"#3D5AFE",
			"#3F51B5",
			"#3949AB",
			"#303F9F",
			"#304FFE",
			"#283593",
			"#1A237E"
	};
	public final static String[] BLUE = {
			"#2196F3",
			"#E3F2FD",
			"#BBDEFB",
			"#90CAF9",
			"#82B1FF",
			"#448AFF",
			"#64B5F6",
			"#42A5F5",
			"#2979FF",
			"#2196F3",
			"#1E88E5",
			"#1976D2",
			"#2962FF",
			"#1565C0",
			"#0D47A1"
	};
	public final static String[] LIGHT_BLUE = {
			"#03A9F4",
			"#E1F5FE",
			"#B3E5FC",
			"#80D8FF",
			"#81D4FA",
			"#40C4FF",
			"#4FC3F7",
			"#29B6F6",
			"#00B0FF",
			"#03A9F4",
			"#039BE5",
			"#0288D1",
			"#0091EA",
			"#0277BD",
			"#01579B"
	};
	public final static String[] CYAN = {
			"#00BCD4",
			"#E0F7FA",
			"#B2EBF2",
			"#84FFFF",
			"#80DEEA",
			"#18FFFF",
			"#4DD0E1",
			"#26C6DA",
			"#00E5FF",
			"#00BCD4",
			"#00ACC1",
			"#0097A7",
			"#00B8D4",
			"#00838F",
			"#006064"
	};
	public final static String[] TEAL = {
			"#009688",
			"#E0F2F1",
			"#B2DFDB",
			"#A7FFEB",
			"#80CBC4",
			"#64FFDA",
			"#4DB6AC",
			"#26A69A",
			"#1DE9B6",
			"#009688",
			"#00897B",
			"#00796B",
			"#00BFA5",
			"#00695C",
			"#004D40"
	};
	public final static String[] GREEN = {
			"#4CAF50",
			"#E8F5E9",
			"#C8E6C9",
			"#A5D6A7",
			"#B9F6CA",
			"#69F0AE",
			"#81C784",
			"#66BB6A",
			"#00E676",
			"#4CAF50",
			"#43A047",
			"#388E3C",
			"#00C853",
			"#2E7D32",
			"#1B5E20"
	};
	public final static String[] LIGHT_GREEN = {
			"#8BC34A",
			"#F1F8E9",
			"#DCEDC8",
			"#CCFF90",
			"#C5E1A5",
			"#B2FF59",
			"#AED581",
			"#9CCC65",
			"#76FF03",
			"#8BC34A",
			"#7CB342",
			"#689F38",
			"#64DD17",
			"#558B2F",
			"#33691E"
	};
	public final static String[] LIME = {
			"#CDDC39",
			"#F9FBE7",
			"#F0F4C3",
			"#F4FF81",
			"#E6EE9C",
			"#EEFF41",
			"#DCE775",
			"#D4E157",
			"#C6FF00",
			"#CDDC39",
			"#C0CA33",
			"#AFB42B",
			"#AEEA00",
			"#9E9D24",
			"#827717"
	};
	public final static String[] YELLOW = {
			"#FFEB3B",
			"#FFFDE7",
			"#FFF9C4",
			"#FFFF8D",
			"#FFF59D",
			"#FFFF00",
			"#FFF176",
			"#FFEE58",
			"#FFEA00",
			"#FFEB3B",
			"#FDD835",
			"#FBC02D",
			"#FFD600",
			"#F9A825",
			"#F57F17"
	};
	public final static String[] AMBER = {
			"#FFC107",
			"#FFF8E1",
			"#FFECB3",
			"#FFE57F",
			"#FFE082",
			"#FFD740",
			"#FFD54F",
			"#FFCA28",
			"#FFC400",
			"#FFC107",
			"#FFB300",
			"#FFA000",
			"#FFAB00",
			"#FF8F00",
			"#FF6F00"
	};
	public final static String[] ORANGE = {
			"#FF9800",
			"#FFF3E0",
			"#FFE0B2",
			"#FFD180",
			"#FFCC80",
			"#FFAB40",
			"#FFB74D",
			"#FFA726",
			"#FF9100",
			"#FF9800",
			"#FB8C00",
			"#F57C00",
			"#FF6D00",
			"#EF6C00",
			"#E65100"
	};
	public final static String[] DEEP_ORANGE = {
			"#FF5722",
			"#FBE9E7",
			"#FFCCBC",
			"#FF9E80",
			"#FFAB91",
			"#FF6E40",
			"#FF8A65",
			"#FF7043",
			"#FF3D00",
			"#FF5722",
			"#F4511E",
			"#E64A19",
			"#DD2C00",
			"#D84315",
			"#BF360C"
	};
	public final static String[] BROWN = {
			"#795548",
			"#EFEBE9",
			"#D7CCC8",
			"#BCAAA4",
			"#A1887F",
			"#8D6E63",
			"#795548",
			"#6D4C41",
			"#5D4037",
			"#4E342E",
			"#3E2723"
	};
	public final static String[] GREY = {
			"#9E9E9E",
			"#FFFFFF",
			"#FAFAFA",
			"#F5F5F5",
			"#EEEEEE",
			"#E0E0E0",
			"#BDBDBD",
			"#9E9E9E",
			"#757575",
			"#616161",
			"#424242",
			"#212121",
			"#000000"
	};
	public final static String[] BLUE_GREY = {
			"#607D8B",
			"#ECEFF1",
			"#CFD8DC",
			"#B0BEC5",
			"#90A4AE",
			"#78909C",
			"#607D8B",
			"#546E7A",
			"#455A64",
			"#37474F",
			"#263238"
	};

	public final static String[][] PALETTES = {
			RED,            // 0
			PINK,           // 1
			PURPLE,         // 2
			DEEP_PURPLE,    // 3
			INDIGO,         // 4
			BLUE,           // 5
			LIGHT_BLUE,     // 6
			CYAN,           // 7
			TEAL,           // 8
			GREEN,          // 9
			LIGHT_GREEN,    // 10
			LIME,           // 11
			YELLOW,         // 12
			AMBER,          // 13
			ORANGE,         // 14
			DEEP_ORANGE,    // 15
			BROWN,          // 16
			GREY,           // 17
			BLUE_GREY       // 18
	};

	public final static String[][] HOT = {
			DEEP_ORANGE,
			RED,
			PINK,
			BROWN
	};

	public final static String[][] WARM = {
			ORANGE,
			AMBER,
			YELLOW
	};

	public final static String[][] COOL = {
			BLUE,
			INDIGO,
			DEEP_PURPLE,
			PURPLE
	};

	public final static String[][] FRESH = {
			BLUE_GREY,
			LIGHT_BLUE,
			CYAN,
			TEAL,
			GREEN,
			LIGHT_GREEN,
			LIME
	};

	public static int getColor(String[][] collection, int swatch, int color) {
		int output = Color.parseColor(collection[swatch][color]);
		return output;
	}

	public static int getColor(int swatch, int color) {
		return getColor(PALETTES, swatch, color);
	}

	/**
	 * Converts a saved StringSet to an integer color value.
	 * <p>
	 * For use with ColorPreference mode="color_single"
	 *
	 * @param sharedPreferences The SharedPreferences of your activity
	 * @param key               The key for the required preference
	 * @param defaultColor      Value to return if no value is found
	 * @return int value representing the color for given ColorPreference
	 */
	public static int getColorFromPreference(SharedPreferences sharedPreferences, String key, int defaultColor) {
			Set<String> set = sharedPreferences.getStringSet(key, null);

			if (set != null) {
					Iterator<String> iterator = set.iterator();
					if (iterator.hasNext()) {
							ColorContainer container = new ColorContainer(iterator.next());
							int color = getColor(PALETTES, container.swatch, container.color + 1);
							return color;
					}
			}
			return defaultColor;
	}

	public static int getColorFromPreference(SharedPreferences sharedPreferences, String key) {
		return getColorFromPreference(sharedPreferences, key, Color.BLACK);
	}


	/**
	 * Converts a saved StringSet to an integer array containing selected color values
	 * <p>
	 * For use with ColorPreference mode="color_multi"
	 *
	 * @param sharedPreferences    The SharedPreferences of your activity
	 * @param key                  The key for the required preference
	 * @return int array containing color values for all selected colors
	 */
	public static int[] getColorsFromPreferences(SharedPreferences sharedPreferences, String key) {
		Set<String> set = sharedPreferences.getStringSet(key, null);
		int[] output = new int[0];

		if (set != null) {
			output = new int[set.size()];
			Iterator<String> iterator = set.iterator();
			int i = 0;
			while (iterator.hasNext()) {
				ColorContainer container = new ColorContainer(iterator.next());
				output[i++] = getColor(PALETTES, container.swatch, container.color);
			}
		}

		return output;
	}

	/**
	 * Converts a saved StringSet to an array of swatches - i.e. a String[][]
	 * <p>
	 * For use with ColorPreference mode="swatch_multi"
	 * @param sharedPreferences
	 * @param key
	 * @return
	 */
	public static String[][] getSwatchesFromPreferences(SharedPreferences sharedPreferences, String key) {
		Set<String> set = sharedPreferences.getStringSet(key, null);
		String[][] output = new String[0][];

		if (set != null) {
			output = new String[set.size()][];
			Iterator<String> iterator = set.iterator();
			int i = 0;
			while (iterator.hasNext()) {
				ColorContainer container = new ColorContainer(iterator.next());
				output[i++] = PALETTES[container.swatch];
			}
		}
		return output;
	}
}