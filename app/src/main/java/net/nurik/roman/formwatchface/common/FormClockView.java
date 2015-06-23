/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nurik.roman.formwatchface.common;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.Utils;

import java.util.Calendar;

public class FormClockView extends View {
	public final static int SHADOW_ALPHA = 100;
	private Handler mMainThreadHandler = new Handler();
	protected FormClockRenderer mHourMinRenderer;
	protected FormClockRenderer mSecondsRenderer;

	protected int mWidth, mHeight;

	protected int mColor1, mColor2, mColor3;

	protected FormClockRenderer.Options mHourMinOptions, mSecondsOptions;

	// Complications
	protected boolean showSeconds = false;
	protected boolean showDate = false;
	protected boolean showAlarm = false;
	protected int dateSize = 96;
	protected String dateStr = "";
	protected String dateFormat = "EEEE d MMMM";
	protected String alarmStr = "";
	protected String alarmFormat = "EEEE H:mm";
	protected AlarmContainer alarm;

	protected boolean showShadow = false;
	protected int clockSecondsSpacing;
	private float clockTranslation = 0;

	FormClockRenderer.ClockPaints paints;
	Paint shadowPaint;

	protected boolean isPreview = false;

	public FormClockView(Context context) {
		super(context);
		init(context, null, 0, 0);
	}

	public FormClockView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, 0, 0);
	}

	public FormClockView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs, defStyleAttr, 0);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public FormClockView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context, attrs, defStyleAttr, defStyleRes);
	}

	private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		// Attribute initialization
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FormClockView,
				defStyleAttr, defStyleRes);

		// Configure renderers
		mHourMinOptions = new FormClockRenderer.Options();
		mHourMinOptions.textSize = a.getDimension(R.styleable.FormClockView_textSize,
				TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20,
						getResources().getDisplayMetrics()));
		mHourMinOptions.charSpacing = a.getDimension(R.styleable.FormClockView_charSpacing,
				TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 6,
						getResources().getDisplayMetrics()));
		mHourMinOptions.is24hour = DateFormat.is24HourFormat(context);

		mHourMinOptions.glyphAnimAverageDelay = 500;
		mHourMinOptions.glyphAnimDuration = 2000;

		mHourMinOptions.orientation = FormClockRenderer.ORIENTATION_HORIZONTAL;

		clockSecondsSpacing = getResources().getDimensionPixelSize(R.dimen.seconds_clock_spacing);
		dateSize = getResources().getDimensionPixelSize(R.dimen.seconds_clock_height);

		mSecondsOptions = new FormClockRenderer.Options(mHourMinOptions);
		mSecondsOptions.onlySeconds = true;
		mSecondsOptions.textSize /= 2;
		mSecondsOptions.glyphAnimAverageDelay = 0;
		mSecondsOptions.glyphAnimDuration = 750;

		mColor1 = a.getColor(R.styleable.FormClockView_color1, 0xff000000);
		mColor2 = a.getColor(R.styleable.FormClockView_color2, 0xff888888);
		mColor3 = a.getColor(R.styleable.FormClockView_color3, 0xffcccccc);

		isPreview = a.getBoolean(R.styleable.FormClockView_isPreview, false);

		a.recycle();

		alarmFormat = mHourMinOptions.is24hour ? "EEEE H:mm" : "EEEE K:mm";
		alarm = new AlarmContainer();

		regenerateRenderers();
	}

	public void regenerateRenderers() {
		mHourMinRenderer = new FormClockRenderer(mHourMinOptions, null);
		mSecondsRenderer = new FormClockRenderer(mSecondsOptions, null);
		updatePaints();
	}

	public void setTextSize(int size) {
		mHourMinOptions.textSize = size;
		mSecondsOptions.textSize = size / 2;
		mHourMinOptions.charSpacing = size / 14;
		regenerateRenderers();
	}

	public void set24Hour(boolean b) {
		mHourMinOptions.is24hour = b;
		regenerateRenderers();
	}

	public void setShowDate(boolean b) {
		this.showDate = b;
		regenerateRenderers();
	}

	public void setShowAlarm(boolean b) {
		this.showAlarm = b;
		regenerateRenderers();
	}

	public void setOrientation(int n) {
		mHourMinOptions.orientation = n;
		regenerateRenderers();
	}

	private void updatePaints() {
		paints = new FormClockRenderer.ClockPaints();
		Paint paint = new Paint();
		paint.setAntiAlias(true);

		paint.setColor(mColor1);
		paints.fills[0] = paint;

		paint = new Paint(paint);
		paint.setColor(mColor2);
		paints.fills[1] = paint;

		paint = new Paint(paint);
		paint.setColor(mColor3);
		paints.fills[2] = paint;

		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Typeface typeface = Typeface.createFromAsset(getResources().getAssets(), "Roboto-Regular.ttf");
		paint.setTypeface(typeface);
		paint.setTextSize(dateSize);
		paint.setColor(mColor3);
		paints.date = paint;

		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.STROKE);
		float strokeWidth = Math.max(Utils.dpToPx(getContext(), 2), mHourMinOptions.textSize / 40f);
		paint.setStrokeWidth(strokeWidth);
		paint.setColor(Color.BLACK);
		paints.shadow = paint;
		paints.hasShadow = showShadow;

		mHourMinRenderer.setPaints(paints);
		mSecondsRenderer.setPaints(paints);

		paint.setTypeface(typeface);
		shadowPaint = new Paint(paint);

		invalidate();
	}

	public void setShowShadow(boolean b) {
		showShadow = b;
		regenerateRenderers();
	}

	public void setColors(int color1, int color2, int color3) {
		mColor1 = color1;
		mColor2 = color2;
		mColor3 = color3;
		regenerateRenderers();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mWidth = w;
		mHeight = h;
	}

	protected String updateDateStr() {
		dateStr = DateFormat.format(dateFormat, Calendar.getInstance()).toString().toUpperCase();
		if (dateStr == null) {
			dateStr = "";
		}
		return dateStr;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		dateStr = updateDateStr();
		alarmStr = alarm.getAsString();

		clockSecondsSpacing = (int) (mHourMinOptions.textSize / 15f);

		float complicationSize = mHourMinOptions.textSize / 5;
		if (!isPreview) {
			if (showDate || (showAlarm && !alarmStr.equals(""))) {
				if (mHourMinOptions.orientation == FormClockRenderer.ORIENTATION_HORIZONTAL) {
					clockTranslation = -(complicationSize / 2) - (mHourMinOptions.charSpacing / 3);
					canvas.translate(0, -(complicationSize / 2) - (mHourMinOptions.charSpacing / 3));
				}
				else {
					clockTranslation = -(complicationSize + mHourMinOptions.charSpacing);
					canvas.translate(0, -(complicationSize + mHourMinOptions.charSpacing));
				}
			}
		}

		mHourMinRenderer.updateTime();
		PointF hourMinSize = mHourMinRenderer.measure();
		mHourMinRenderer.draw(canvas,
				(mWidth - hourMinSize.x) / 2,
				(mHeight - hourMinSize.y) / 2,
				false);

		if (showSeconds) {
			mSecondsRenderer.updateTime();
			PointF secondsSize = mSecondsRenderer.measure();
			mSecondsRenderer.draw(canvas,
					(mWidth + hourMinSize.x) / 2 - secondsSize.x,
					(mHeight + hourMinSize.y) / 2
							+ TypedValue.applyDimension(5, TypedValue.COMPLEX_UNIT_DIP,
							getResources().getDisplayMetrics()),
					false);


			long timeToNextSecondsAnimation = mSecondsRenderer.timeToNextAnimation();
			long timeToNextHourMinAnimation = mHourMinRenderer.timeToNextAnimation();
			if (timeToNextHourMinAnimation < 0 || timeToNextSecondsAnimation < 0) {
				postInvalidateOnAnimation();
			} else {
				postInvalidateDelayed(Math.min(timeToNextHourMinAnimation, timeToNextSecondsAnimation));
			}
		}
		else {
			long timeToNextHourMinAnimation = mHourMinRenderer.timeToNextAnimation();
			if (timeToNextHourMinAnimation < 0) {
				postInvalidateOnAnimation();
			} else {
				postInvalidateDelayed(timeToNextHourMinAnimation);
			}
		}

		// Draw complications
		Paint paint = paints.date;
		paint.setTextSize(complicationSize);

		shadowPaint.setTextSize(complicationSize);
		shadowPaint.setStyle(Paint.Style.STROKE);
		shadowPaint.setStrokeWidth(complicationSize / 10);
		shadowPaint.setAlpha(SHADOW_ALPHA);

		float x;
		float y = (this.getBottom() + hourMinSize.y) / 2 + clockSecondsSpacing - paint.ascent();

		// Show both date and alarm
		if (showDate && showAlarm && alarmStr != null && !alarmStr.equals("")) {
			float totalStringLength = paint.measureText(dateStr + "  " + alarmStr + complicationSize);

			if (mHourMinOptions.orientation == FormClockRenderer.ORIENTATION_HORIZONTAL) {
				x = ((mWidth - totalStringLength) / 2);
				if (showShadow) {
					canvas.drawText(dateStr, x, y, shadowPaint);
				}
				canvas.drawText(dateStr, x, y, paint);
				x += paint.measureText(dateStr + "   ") + complicationSize;
			}
			else { // Vertical orientation
				x = ((mWidth - paint.measureText(dateStr)) / 2);
				if (showShadow) {
					canvas.drawText(dateStr, x, y, shadowPaint);
				}
				canvas.drawText(dateStr, x, y, paint);
				x = ((mWidth - paint.measureText(alarmStr)) / 2) + (complicationSize / 2);
				y += complicationSize + clockSecondsSpacing;
			}
			alarm.setColor(mColor3);
			alarm.draw(canvas, x - (complicationSize / 1.5f),
					y - (complicationSize / 3), complicationSize);
			if (showShadow) {
				canvas.drawText(alarmStr, x, y, shadowPaint);
			}
			canvas.drawText(alarmStr, x, y, paint);
		}
		else if (showDate) {
			x = (mWidth - paint.measureText(dateStr)) / 2;
			if (showShadow) {
				canvas.drawText(dateStr, x, y, shadowPaint);
			}
			canvas.drawText(
					dateStr,
					x,
					y,
					paint);
		}
		else if (showAlarm) {
			if (alarmStr != null && !alarmStr.equals("")) {
				x = ((mWidth - paint.measureText(alarmStr)) / 2) + (complicationSize / 2);
				alarm.setColor(mColor3);
				alarm.draw(
						canvas,
						x - (complicationSize / 1.5f),
						y - (complicationSize / 3),
						complicationSize);
				if (showShadow) {
					canvas.drawText(alarmStr, x, y, shadowPaint);
				}
				canvas.drawText(
						alarmStr,
						x,
						y,
						paint);
			}
		}
	}

	private Paint getShadowPaint() {
		return shadowPaint;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		registerSystemSettingsListener();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		unregisterSystemSettingsListener();
	}

	private void registerSystemSettingsListener() {
		getContext().getContentResolver().registerContentObserver(
				Settings.System.getUriFor(Settings.System.TIME_12_24),
				false, mSystemSettingsObserver);
	}

	private void unregisterSystemSettingsListener() {
		getContext().getContentResolver().unregisterContentObserver(mSystemSettingsObserver);
	}

	private ContentObserver mSystemSettingsObserver = new ContentObserver(mMainThreadHandler) {
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHourMinOptions.is24hour = DateFormat.is24HourFormat(getContext());
			if (showSeconds) {
				mSecondsOptions.is24hour = mHourMinOptions.is24hour;
			}
			regenerateRenderers();
		}
	};

	/**
	 * Added by Michael Beaton on 8 June 2015
	 * AlarmContainer helps get the next alarm, correctly format it as a string
	 * and draw a simple icon
	 */
	protected class AlarmContainer {
		private int hour = 3;
		private int minute = 0;
		private String asString = "";

		Paint paint;
		Paint shadow;
		Paint tempPaint;

		public AlarmContainer() {
			update();
			initPaint();
		}

		private void initPaint() {
			paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setStyle(Paint.Style.STROKE);

			this.shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
			shadow.setStyle(Paint.Style.STROKE);
			shadow.setColor(Color.BLACK);
		}

		public void setColor(int color) {
			paint.setColor(color);
		}

		public String getAsString() {
			update();
			if (asString == null) {
				asString = "";
			}
			return asString;
		}

		@SuppressWarnings({"NewApi", "Deprecated"})
		public void update() {
			if (Utils.isLollipop()) {
				AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
				AlarmManager.AlarmClockInfo info = am.getNextAlarmClock();

				if (info != null) {
					Calendar time = Calendar.getInstance();
					String now = "" + time.get(Calendar.DAY_OF_MONTH) + time.get(Calendar.MONTH) + time.get(Calendar.YEAR);
					time.setTimeInMillis(info.getTriggerTime());
					String alarmTime = "" + time.get(Calendar.DAY_OF_MONTH) + time.get(Calendar.MONTH) + time.get(Calendar.YEAR);

					boolean isToday = now.equals(alarmTime);

					if (isToday) {
						alarmFormat = mHourMinOptions.is24hour ? "H:mm" : "K:mm";
					} else {
						alarmFormat = mHourMinOptions.is24hour ? "EEEE H:mm" : "EEEE K:mm";
					}

					asString = DateFormat.format(alarmFormat, time).toString().toUpperCase();
					hour = time.get(Calendar.HOUR);
					minute = time.get(Calendar.MINUTE);
				}
				else {
					asString = "";
				}
			}
			else {
				asString = Settings.System.getString(getContext().getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
			}
		}

		public void draw(Canvas c, float x, float y, float size) {
			Bitmap shadowBitmap = null;
			Canvas shadowCanvas = null;
			Bitmap tempBitmap = null;
			Canvas tempCanvas = null;
			boolean isShadow = false;

			float centerX = x;
			float centerY = y;

			Canvas[] canvasArray;

			if (showShadow) {
				shadowBitmap = Bitmap.createBitmap((int) size, (int) size, Bitmap.Config.ARGB_8888);
				shadowCanvas = new Canvas(shadowBitmap);

				tempBitmap = Bitmap.createBitmap((int) size, (int) size, Bitmap.Config.ARGB_8888);
				tempCanvas = new Canvas(tempBitmap);

				canvasArray = new Canvas[] { shadowCanvas, tempCanvas };

				x = size / 2;
				y = size / 2;
			}
			else {
				canvasArray = new Canvas[] { c };
			}

			for (Canvas canvas : canvasArray) {
				isShadow = canvas.equals(shadowCanvas);
				tempPaint = isShadow ? shadow : paint;
				float radius = size / 2 * 0.8f;
				float hourRadius = radius * 0.4f;
				float minRadius = radius * 0.8f;

				tempPaint.setStrokeWidth(radius / (isShadow ? 3 : 6));

				// body
				canvas.drawCircle(x, y, radius, tempPaint);

				// bells
				PointF center = new PointF(x, y);
				PointF point = new PointF(x, y - (size / 2));
				PointF start = rotateAround(point, center, 25);
				PointF end = rotateAround(point, center, 55);

				canvas.drawLine(start.x, start.y, end.x, end.y, tempPaint);

				start = rotateAround(point, center, -25);
				end = rotateAround(point, center, -55);

				canvas.drawLine(start.x, start.y, end.x, end.y, tempPaint);

				tempPaint.setStrokeWidth(radius / (isShadow ? 4 : 8));


				// minute hand
				float endX = (float) (x + minRadius * Math.sin(2 * Math.PI * minute / 60));
				float endY = (float) (y - minRadius * Math.cos(2 * Math.PI * minute / 60));

				canvas.drawLine(x, y, endX, endY, tempPaint);

				// hour hand
				endX = (float) (x + hourRadius * Math.sin(2 * Math.PI * getTotalSeconds() / 43200));
				endY = (float) (y - hourRadius * Math.cos(2 * Math.PI * getTotalSeconds() / 43200));

				canvas.drawLine(x, y, endX, endY, tempPaint);
			}

			if (showShadow) {
				tempPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
				tempPaint.setAlpha(SHADOW_ALPHA);
				c.drawBitmap(shadowBitmap, centerX - x, centerY - y, tempPaint);

				tempPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
				c.drawBitmap(tempBitmap, centerX - x, centerY - y, tempPaint);
			}

			if (shadowBitmap != null) {
				shadowBitmap.recycle();
			}
			if (tempBitmap != null) {
				tempBitmap.recycle();
			}
		}

		// get number of seconds from noon/midnight to alarm time
		private int getTotalSeconds() {
			return (hour * 60 * 60) + (minute * 60);
		}

		private PointF rotateAround(PointF point, PointF center, float angle) {
			float cos = (float) Math.cos(Math.toRadians(angle));
			float sin = (float) Math.sin(Math.toRadians(angle));

			float x = (center.x + cos * (point.x - center.x)) - (sin * (point.y - center.y));
			float y = (center.y + sin * (point.x - center.x)) + (cos * (point.y - center.y));

			return new PointF(x, y);
		}
	}
}