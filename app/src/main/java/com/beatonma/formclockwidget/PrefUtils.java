package com.beatonma.formclockwidget;

/**
 * Created by Michael on 03/06/2015.
 */
public class PrefUtils {
	public static boolean DEBUG = false;

	public final static String PREFS = "widget_preferences";

	// Theme
	public final static String PREF_USE_WALLPAPER_PALETTE = "pref_use_wallpaper_palette";

	public final static String PREF_THEME_SHADOW = "pref_theme_show_shadows";
	public final static String PREF_THEME_ORIENTATION = "pref_theme_orientation";

	public final static String PREF_COLOR1 = "pref_color1";
	public final static String PREF_COLOR2 = "pref_color2";
	public final static String PREF_COLOR3 = "pref_color3";

	public final static String WALLPAPER_COLORS_UPDATED = "wallpaper_colors_updated";
	public final static String WALLPAPER_COLOR1 = "wallpaper_color1";
	public final static String WALLPAPER_COLOR2 = "wallpaper_color2";
	public final static String WALLPAPER_COLOR3 = "wallpaper_color3";

	// Complications
	public final static String PREF_SHOW_DATE = "pref_complication_date";
	public final static String PREF_SHOW_ALARM = "pref_complication_alarm";
	public final static String PREF_ON_TOUCH = "pref_on_touch";

	// Other
	public final static String PREF_ENABLE_ANIMATION = "pref_enable_animation";
	public final static String PREF_ON_TOUCH_PACKAGE = "pref_on_touch_package";
	public final static String PREF_ON_TOUCH_LAUNCHER = "pref_on_touch_launcher";
	public final static String PREF_ON_TOUCH_DEFAULT_PACKAGE = "com.beatonma.formclockwidget";
	public final static String PREF_ON_TOUCH_DEFAULT_LAUNCHER = "com.beatonma.formclockwidget.ConfigActivity";

	public final static String MAX_WIDGET_WIDTH = "max_widget_width";
	public final static String MAX_WIDGET_HEIGHT = "max_widget_height";
}
