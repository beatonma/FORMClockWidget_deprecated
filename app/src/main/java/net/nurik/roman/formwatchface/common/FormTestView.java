package net.nurik.roman.formwatchface.common;

/**
 * Created by Michael on 22/06/2015.
 */
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.beatonma.formclockwidget.R;

public class FormTestView extends View {
	public final static int SHADOW_ALPHA = 100;
	private Handler mMainThreadHandler = new Handler();
	protected FormClockRenderer mRenderer;

	protected int mWidth, mHeight;

	protected int mColor1, mColor2, mColor3;

	protected FormClockRenderer.Options mOptions;

	protected boolean showShadow = false;

	FormClockRenderer.ClockPaints paints;
	Paint shadowPaint;

	protected boolean isPreview = false;

	public FormTestView(Context context) {
		super(context);
		init(context, null, 0, 0);
	}

	public FormTestView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, 0, 0);
	}

	public FormTestView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs, defStyleAttr, 0);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public FormTestView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context, attrs, defStyleAttr, defStyleRes);
	}

	private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		// Attribute initialization
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FormClockView,
				defStyleAttr, defStyleRes);

		// Configure renderers
		mOptions = new FormClockRenderer.Options();
		mOptions.textSize = a.getDimension(R.styleable.FormClockView_textSize,
				TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20,
						getResources().getDisplayMetrics()));
		mOptions.charSpacing = a.getDimension(R.styleable.FormClockView_charSpacing,
				TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 6,
						getResources().getDisplayMetrics()));
		mOptions.is24hour = DateFormat.is24HourFormat(context);

		mOptions.glyphAnimAverageDelay = 500;
		mOptions.glyphAnimDuration = 2000;

		mColor1 = a.getColor(R.styleable.FormClockView_color1, 0xff000000);
		mColor2 = a.getColor(R.styleable.FormClockView_color2, 0xff888888);
		mColor3 = a.getColor(R.styleable.FormClockView_color3, 0xffcccccc);

		isPreview = a.getBoolean(R.styleable.FormClockView_isPreview, false);

		a.recycle();

		regenerateRenderers();
	}

	public void regenerateRenderers() {
		mRenderer = new FormClockRenderer(mOptions, null);
		updatePaints();
	}

	public void setTextSize(int size) {
		mOptions.textSize = size;
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
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.BLACK);
		paints.shadow = paint;
		paints.hasShadow = showShadow;

		mRenderer.setPaints(paints);

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

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

//		mHourMinRenderer.updateTime();
//		PointF hourMinSize = mHourMinRenderer.measure();
//		mHourMinRenderer.draw(canvas,
//				(mWidth - hourMinSize.x) / 2,
//				(mHeight - hourMinSize.y) / 2,
//				false);
//
//
//
//		long timeToNextHourMinAnimation = mHourMinRenderer.timeToNextAnimation();
//		if (timeToNextHourMinAnimation < 0) {
//			postInvalidateOnAnimation();
//		} else {
//			postInvalidateDelayed(timeToNextHourMinAnimation);
//		}
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
			mOptions.is24hour = DateFormat.is24HourFormat(getContext());
			regenerateRenderers();
		}
	};
}