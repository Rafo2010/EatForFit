package rafayel.hakobyan.EatForFit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CalorieArcView extends View {

    private Paint trackPaint;
    private Paint arcPaint;
    private Paint textPaint;
    private Paint subTextPaint;
    private RectF arcRect;
    private int consumed = 0;
    private int goal     = 2000;

    public CalorieArcView(Context context) {
        super(context);
        init();
    }

    public CalorieArcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CalorieArcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(dp(20));
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(0xFFE0E0E0);

        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(dp(20));
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setColor(0xFF2196F3);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(dp(38));
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textPaint.setColor(0xFF212121);

        subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setTextSize(dp(13));
        subTextPaint.setColor(0xFF888888);

        arcRect = new RectF();
    }

    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    public void setValues(int consumed, int goal) {
        this.consumed = consumed;
        this.goal     = goal > 0 ? goal : 2000;
        updateArcColor();
        invalidate();
    }

    private void updateArcColor() {
        float pct = goal > 0 ? (float) consumed / goal : 0f;
        if (consumed == 0) {
            arcPaint.setColor(0xFF2196F3);
        } else if (pct < 0.60f) {
            arcPaint.setColor(0xFFE53935);
        } else if (pct < 1.0f) {
            arcPaint.setColor(0xFFFFC107);
        } else {
            arcPaint.setColor(0xFF4CAF50);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w      = getWidth();
        float h      = getHeight();
        float stroke = dp(20);
        float pad    = stroke / 2f + dp(8);
        float cx     = w / 2f;
        float cy     = h * 0.80f;
        float radius = Math.min(cx - pad, cy - pad);

        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        canvas.drawArc(arcRect, 180f, 180f, false, trackPaint);

        float pct   = goal > 0 ? Math.min((float) consumed / goal, 1f) : 0f;
        float sweep = 180f * pct;
        if (consumed > 0 && sweep < 4f) sweep = 4f;
        if (consumed > 0) {
            canvas.drawArc(arcRect, 180f, sweep, false, arcPaint);
        }

        canvas.drawText(String.valueOf(consumed), cx, cy - dp(12), textPaint);
        canvas.drawText("kcal today", cx, cy + dp(8), subTextPaint);
        canvas.drawText("/ " + goal + " goal", cx, cy + dp(24), subTextPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(w, (int) (w * 0.60f));
    }
}