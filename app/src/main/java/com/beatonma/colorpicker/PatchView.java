package com.beatonma.colorpicker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.Utils;

/**
 * Created by Michael on 15/02/2015.
 */
public class PatchView extends ImageView {
    private final static String TAG = "ColorPicker";
    private final static int ALPHA = 120;

    Context context;
    int width = -1;
    int height = -1;
    int outerRadius; // border
    int innerRadius; // main
    int diffRadius; // difference between above radii

    RectF selectedRingBounds; // Boundary in which to draw the arc when this item is selected
    Point center;
    Paint paint;

    int color = -1;

    // Position in layout
    int position = 0;

    boolean touched = false;

    boolean firstDraw = true;
    int startingAngle = 0;
    boolean animationSelect = false;
    boolean animationDeselect = false;
    int selectedRingDelay = 0;
    float enterDelay = 0f;

    boolean enableRipple = true; // Allow ripples to be drawn
    boolean touchDown = false; // Is a finger currently touching this view
    Paint ripplePaint;
    RectF maskRingBounds; // Boundary for drawing mask (to cover any ripple paint going over the edge)
    float hotspotX = -1;
    float hotspotY = -1;
    float rippleRadius = -1;
    int rippleRate = 1;
    Handler handler = new Handler();
    Runnable rippleRunner = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };

	boolean isPreview = false;

    public PatchView(Context context) {
        this(context, null);
    }

    public PatchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initPaint();
    }

    @Override
    protected void onDraw(final Canvas c) {
        if (width == -1 || height == -1 || center == null) {
            width = c.getWidth();
            height = c.getHeight();
            center = new Point(width / 2, height / 2);

            outerRadius = Math.min(width, height) / 2;
            innerRadius = outerRadius - Utils.dpToPx(context, 6);
            diffRadius = outerRadius - innerRadius;

            int padding = Utils.dpToPx(context, 2);
            selectedRingBounds = new RectF(padding, padding, width - padding, height - padding);
            if (isSelected()) {
                maskRingBounds = new RectF(-padding, -padding, width + padding, height + padding);
            }
            else {
                padding = Utils.dpToPx(context, 32);
                maskRingBounds = new RectF(-padding, -padding, width + padding, height + padding);
            }
            startingAngle = (int) (Math.floor(Math.random() * 360));
        }

        int size = Math.round(enterDelay * outerRadius);
        c.drawCircle(center.x, center.y, size, normalPaint());

        if (enableRipple) {
            drawRipple(c);
        }

        if (isSelected()) {
            if (animationSelect) {
                c.drawArc(selectedRingBounds, startingAngle, selectedRingDelay, false, blankPaint());
                c.drawArc(selectedRingBounds, startingAngle, selectedRingDelay, false, selectedPaint());
            }
            else {
                animationSelect = true;
                animationDeselect = false;
                animateSelectionRing(true);
            }
        }
        else {
            if (firstDraw) {
                return;
            }

            if (animationDeselect) {
                c.drawArc(selectedRingBounds, startingAngle, selectedRingDelay, false, blankPaint());
                c.drawArc(selectedRingBounds, startingAngle, selectedRingDelay, false, selectedPaint());
            }
            else {
                animationDeselect = true;
                animationSelect = false;
                animateSelectionRing(false);
            }
        }

        firstDraw = false;
        if (touchDown) {
            rippleRadius++;
            animateNext();
        }
    }

    private void drawRipple(Canvas c) {
        if (hotspotX >= 0 && hotspotY >= 0 && rippleRadius >= 0) {
            c.drawCircle(hotspotX, hotspotY, rippleRadius, ripplePaint());
        }

        // Draw circle mask
        c.drawCircle(center.x, center.y, outerRadius + Utils.dpToPx(context, 16), maskPaint());
    }

    public void stopDrawingRipple() {
        touchDown = false;
        handler.removeCallbacks(rippleRunner);
        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(150);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (animation.getAnimatedFraction() == 1f) {
                    reset();
                } else {
                    if (animation.getAnimatedFraction() > 0.6f) {
                        ripplePaint.setAlpha(ripplePaint.getAlpha() - 10);
                    }
                    rippleRate += Utils.dpToPx(context, 2);
                    rippleRadius += rippleRate;
                }
                invalidate();
            }
        });
        animator.start();
    }

    public void animateEntry(int position) {
        ValueAnimator enterAnimator = ValueAnimator.ofInt(0,100);
        enterAnimator
                .setDuration(150)
                .setInterpolator(new AccelerateDecelerateInterpolator());

        long animationDelay = (long) (((position % 4) * 50) + ((position / 4) * 40));
        enterAnimator.setStartDelay(animationDelay);

        enterAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                enterDelay = animation.getAnimatedFraction();
                invalidate();
            }
        });
        enterAnimator.start();
    }

    public void animateExit(int position) {
        ValueAnimator enterAnimator = ValueAnimator.ofInt(100,0);
        enterAnimator
                .setDuration(150)
                .setInterpolator(new AccelerateDecelerateInterpolator());

        long animationDelay = (long) (((position % 4) * 50) + ((position / 4) * 40));
        enterAnimator.setStartDelay(animationDelay);

        enterAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                enterDelay = animation.getAnimatedFraction();
                invalidate();
            }
        });
        enterAnimator.start();
    }

    // Shrink, change color, grow back.
    public void animateColorChange(final int newColor) {
        ValueAnimator animator = ValueAnimator.ofInt(100,0);
        animator
                .setDuration(150)
                .setInterpolator(new AccelerateDecelerateInterpolator());

        long animationDelay = (long) (((position % 4) * 50) + ((position / 4) * 40));
        animator.setStartDelay(animationDelay);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                enterDelay = animation.getAnimatedFraction();
                invalidate();
            }
        });
        final ValueAnimator enterAnimator = animator;

        ValueAnimator exitAnimator = ValueAnimator.ofInt(100,0);
        exitAnimator
                .setDuration(150)
                .setInterpolator(new AccelerateDecelerateInterpolator());

        animationDelay = (long) (((position % 4) * 50) + ((position / 4) * 40));
        exitAnimator.setStartDelay(animationDelay);

        exitAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                enterDelay = animation.getAnimatedFraction();
                invalidate();
                if (enterDelay == 1f) {
                    color = newColor;
                    enterAnimator.start();
                }
            }
        });
        exitAnimator.start();
    }

    private void animateSelectionRing(boolean selected) {
        ValueAnimator offsetAnimator = selected ? ValueAnimator.ofInt(0, 360) : ValueAnimator.ofInt(360, 0);
        offsetAnimator
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator());

        if (!touched) {
            long animationDelay = (long) (((position % 4) * 50) + ((position / 4) * 40));
            offsetAnimator.setStartDelay(animationDelay);
        }
        else {
            touched = false;
        }

        offsetAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                selectedRingDelay = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        offsetAnimator.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (isPreview) {
			return false;
        }

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDown = true;
                hotspotX = e.getX();
                hotspotY = e.getY();
                rippleRadius = 0;
                break;
            case MotionEvent.ACTION_UP:
                touchDown = false;
                stopDrawingRipple();
                break;
            case MotionEvent.ACTION_CANCEL:
                touchDown = false;
                stopDrawingRipple();
                break;
            case MotionEvent.ACTION_MOVE:
                hotspotX = e.getX();
                hotspotY = e.getY();
                break;
            default:
                break;
        }
        invalidate();
        return true;
    }

    private void initPaint() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        if (enableRipple) {
            ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            ripplePaint.setStyle(Paint.Style.FILL);
            ripplePaint.setAlpha(ALPHA);
        }
    }

    private Paint selectedPaint() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Utils.dpToPx(context, 4));
        paint.setColor(getOuterColor(color));
        return paint;
    }

    private Paint normalPaint() {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        return paint;
    }

    private Paint blankPaint() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(diffRadius + Utils.dpToPx(context, 2));
        paint.setColor(getResources().getColor(R.color.Dialog));
        return paint;
    }

    private Paint ripplePaint() {
        ripplePaint.setColor(getOuterColor(color));
        ripplePaint.setAlpha(ALPHA);
        return ripplePaint;
    }

    private Paint maskPaint() {
        paint.setColor(getResources().getColor(R.color.Dialog));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Utils.dpToPx(context, 32));
        return paint;
    }

    public void setColor(int c) {
        color = c;
        invalidate();
    }

    public int getColor() {
        return color;
    }

    private int getOuterColor(int color) {
        int r, g, b;
        float[] hsv = new float[3];

        r = Color.red(color);
        g = Color.green(color);
        b = Color.blue(color);

        Color.RGBToHSV(r, g, b, hsv);

        // Change colour brightness
        if (hsv[2] > 0.4) {
            hsv[2] -= 0.2;
        }
        else {
            hsv[2] += 0.3;
        }

        return Color.HSVToColor(hsv);
    }

    public void setPosition(int p) {
        position = p;
    }

    public void setTouched(boolean b) {
        touched = true;
    }

    public void reset() {
        hotspotX = -1;
        hotspotY = -1;
        rippleRadius = -1;
        ripplePaint.setAlpha(ALPHA);
        rippleRate = Utils.dpToPx(context, 8);

        invalidate();
    }

    private void animateNext() {
        handler.postDelayed(rippleRunner, 15);
    }

	public boolean isPreview() {
		return isPreview;
	}

	public void setIsPreview(boolean b) {
		isPreview = b;
	}
}
