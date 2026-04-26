package rafayel.hakobyan.EatForFit;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ScannerOverlayView extends View {

    private final Paint dimPaint    = new Paint();
    private final Paint clearPaint  = new Paint();
    private final Paint cornerPaint = new Paint();
    private final Paint laserPaint  = new Paint();
    private final Paint glowPaint   = new Paint();
    private final Paint gridPaint   = new Paint();
    private final Paint dotPaint    = new Paint();

    private float laserY = 0f;
    private float boxLeft, boxTop, boxRight, boxBottom, scanBox;
    private ValueAnimator laserAnimator;
    private boolean scanning = false;

    // Corner dot pulse
    private float dotAlpha = 1f;
    private ValueAnimator dotAnimator;

    public ScannerOverlayView(Context ctx) {
        super(ctx);
        init();
    }

    public ScannerOverlayView(Context ctx, AttributeSet a) {
        super(ctx, a);
        init();
    }

    public ScannerOverlayView(Context ctx, AttributeSet a, int s) {
        super(ctx, a, s);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        // Semi-transparent dark overlay
        dimPaint.setColor(0xBB000000);
        dimPaint.setStyle(Paint.Style.FILL);

        // Punch hole through the overlay
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // Corner bracket lines — brand orange
        cornerPaint.setColor(0xFFCC5803);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(7f);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);
        cornerPaint.setShadowLayer(8f, 0f, 0f, 0xFFFF9505);

        // Laser line core
        laserPaint.setColor(0xFFFF9505);
        laserPaint.setStyle(Paint.Style.STROKE);
        laserPaint.setStrokeWidth(3.5f);

        // Laser glow (wider, transparent)
        glowPaint.setColor(0x55FFB627);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(14f);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(
                12f, android.graphics.BlurMaskFilter.Blur.NORMAL));

        // Faint grid inside scan box
        gridPaint.setColor(0x18FF9505);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);

        // Corner dots
        dotPaint.setColor(0xFFFFB627);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        scanBox  = Math.min(w, h) * 0.68f;
        boxLeft  = (w - scanBox) / 2f;
        boxTop   = (h - scanBox) / 2f;
        boxRight = boxLeft + scanBox;
        boxBottom= boxTop  + scanBox;
        laserY   = boxTop;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!scanning) return;

        // 1. Dark overlay over the whole view
        canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);

        // 2. Clear the scan box (transparent cutout)
        RectF box = new RectF(boxLeft, boxTop, boxRight, boxBottom);
        canvas.drawRoundRect(box, 18f, 18f, clearPaint);

        // 3. Grid lines inside the box
        float step = scanBox / 3f;
        for (int i = 1; i < 3; i++) {
            float x = boxLeft + i * step;
            float y = boxTop  + i * step;
            canvas.drawLine(x, boxTop, x, boxBottom, gridPaint);
            canvas.drawLine(boxLeft, y, boxRight, y, gridPaint);
        }

        // 4. Corner brackets
        float cs = 40f;
        // top-left
        canvas.drawLine(boxLeft,      boxTop + cs, boxLeft,      boxTop,      cornerPaint);
        canvas.drawLine(boxLeft,      boxTop,      boxLeft + cs, boxTop,      cornerPaint);
        // top-right
        canvas.drawLine(boxRight - cs,boxTop,      boxRight,     boxTop,      cornerPaint);
        canvas.drawLine(boxRight,     boxTop,      boxRight,     boxTop + cs, cornerPaint);
        // bottom-left
        canvas.drawLine(boxLeft,      boxBottom - cs, boxLeft,  boxBottom,    cornerPaint);
        canvas.drawLine(boxLeft,      boxBottom,   boxLeft + cs, boxBottom,   cornerPaint);
        // bottom-right
        canvas.drawLine(boxRight - cs,boxBottom,   boxRight,    boxBottom,    cornerPaint);
        canvas.drawLine(boxRight,     boxBottom,   boxRight,    boxBottom - cs, cornerPaint);

        // 5. Corner dots (pulsing)
        float dotR = 7f;
        dotPaint.setAlpha((int)(dotAlpha * 255));
        canvas.drawCircle(boxLeft,  boxTop,    dotR, dotPaint);
        canvas.drawCircle(boxRight, boxTop,    dotR, dotPaint);
        canvas.drawCircle(boxLeft,  boxBottom, dotR, dotPaint);
        canvas.drawCircle(boxRight, boxBottom, dotR, dotPaint);

        // 6. Laser line (only inside the box)
        if (laserY >= boxTop && laserY <= boxBottom) {
            // Glow pass
            canvas.drawLine(boxLeft + 4f, laserY, boxRight - 4f, laserY, glowPaint);
            // Core pass (gradient left→right)
            laserPaint.setShader(new LinearGradient(
                    boxLeft, laserY, boxRight, laserY,
                    new int[]{0x00FF9505, 0xFFFF9505, 0xFFFFB627, 0xFFFF9505, 0x00FF9505},
                    null, Shader.TileMode.CLAMP));
            canvas.drawLine(boxLeft + 4f, laserY, boxRight - 4f, laserY, laserPaint);
        }
    }

    /** Call this to start the scanning animation. */
    public void startScanning() {
        scanning = true;
        setVisibility(VISIBLE);
        laserY = boxTop;

        laserAnimator = ValueAnimator.ofFloat(boxTop, boxBottom);
        laserAnimator.setDuration(1300);
        laserAnimator.setRepeatCount(ValueAnimator.INFINITE);
        laserAnimator.setRepeatMode(ValueAnimator.REVERSE);
        laserAnimator.setInterpolator(new LinearInterpolator());
        laserAnimator.addUpdateListener(a -> {
            laserY = (float) a.getAnimatedValue();
            invalidate();
        });
        laserAnimator.start();

        dotAnimator = ValueAnimator.ofFloat(1f, 0.25f, 1f);
        dotAnimator.setDuration(900);
        dotAnimator.setRepeatCount(ValueAnimator.INFINITE);
        dotAnimator.setRepeatMode(ValueAnimator.RESTART);
        dotAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        dotAnimator.addUpdateListener(a -> {
            dotAlpha = (float) a.getAnimatedValue();
            // invalidate already called by laserAnimator
        });
        dotAnimator.start();
    }

    public void stopScanning() {
        scanning = false;
        if (laserAnimator != null) laserAnimator.cancel();
        if (dotAnimator   != null) dotAnimator.cancel();
        setVisibility(GONE);
        invalidate();
    }
}