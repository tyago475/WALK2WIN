package pt.ubi.pdm.quizpenaltis.ui.progress;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.util.Constants;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import java.util.ArrayList;
import java.util.List;

/**
 * Gráfico de barras de passos por dia. Escala fixa de 0 até à meta diária; as barras que
 * atingem a meta ficam douradas e a meta aparece como linha tracejada.
 */
public class CustomBarChart extends View {
    private static final int GRID_LINES = 5;

    private List<ChartData> data = new ArrayList<>();
    private Paint barPaint, goalBarPaint, labelPaint, dayPaint, valuePaint, gridPaint, goalLinePaint, emptyPaint;
    private final RectF barRect = new RectF();
    private final int maxValue = Constants.DAILY_STEP_GOAL;

    private float marginLeft, marginRight, marginTop, marginBottom, barRadius;

    public CustomBarChart(Context context) {
        super(context);
        init();
    }

    public CustomBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private int color(int resId) {
        return ContextCompat.getColor(getContext(), resId);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    private Paint textPaint(int colorRes, float sizeSp, Paint.Align align, Typeface typeface) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color(colorRes));
        p.setTextSize(sp(sizeSp));
        p.setTextAlign(align);
        if (typeface != null) p.setTypeface(typeface);
        return p;
    }

    private void init() {
        Typeface body = ResourcesCompat.getFont(getContext(), R.font.barlow);
        Typeface heading = ResourcesCompat.getFont(getContext(), R.font.barlow_condensed);

        marginLeft = dp(40);
        marginRight = dp(8);
        marginTop = dp(24);
        marginBottom = dp(28);
        barRadius = dp(4);

        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(color(R.color.grass_500));

        goalBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        goalBarPaint.setColor(color(R.color.gold_500));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(color(R.color.chart_grid));
        gridPaint.setStrokeWidth(dp(1));

        goalLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        goalLinePaint.setColor(color(R.color.gold_500));
        goalLinePaint.setStrokeWidth(dp(1.5f));
        goalLinePaint.setPathEffect(new DashPathEffect(new float[]{dp(6), dp(4)}, 0));

        labelPaint = textPaint(R.color.chart_label, 12, Paint.Align.RIGHT, body);
        dayPaint = textPaint(R.color.chart_label, 12, Paint.Align.CENTER, body);
        valuePaint = textPaint(R.color.chart_value, 12, Paint.Align.CENTER, heading);
        valuePaint.setFakeBoldText(true);
        emptyPaint = textPaint(R.color.chart_empty, 16, Paint.Align.CENTER, body);
    }

    public void setData(List<ChartData> data) {
        this.data = data != null ? data : new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data.isEmpty()) {
            canvas.drawText(getContext().getString(R.string.progress_chart_empty),
                    getWidth() / 2f, getHeight() / 2f, emptyPaint);
            return;
        }

        float left = marginLeft;
        float top = marginTop;
        float chartWidth = getWidth() - marginLeft - marginRight;
        float chartHeight = getHeight() - marginTop - marginBottom;
        float bottom = top + chartHeight;

        drawGrid(canvas, left, top, chartWidth, chartHeight);
        drawBars(canvas, left, top, chartWidth, chartHeight, bottom);
    }

    private void drawGrid(Canvas canvas, float left, float top, float width, float height) {
        for (int i = 0; i <= GRID_LINES; i++) {
            float y = top + height - (height * i / GRID_LINES);
            canvas.drawLine(left, y, left + width, y, i == GRID_LINES ? goalLinePaint : gridPaint);
            canvas.drawText(formatStepValue(maxValue * i / GRID_LINES), left - dp(6), y + dp(4), labelPaint);
        }
    }

    private void drawBars(Canvas canvas, float left, float top, float width, float height, float bottom) {
        int count = data.size();
        float slot = width / count;
        float barWidth = slot * 0.55f;

        for (int i = 0; i < count; i++) {
            ChartData item = data.get(i);
            float centerX = left + slot * i + slot / 2f;
            float barHeight = Math.min(1f, (float) item.value / maxValue) * height;
            float barTop = bottom - barHeight;

            barRect.set(centerX - barWidth / 2f, barTop, centerX + barWidth / 2f, bottom);
            canvas.drawRoundRect(barRect, barRadius, barRadius,
                    item.value >= maxValue ? goalBarPaint : barPaint);

            // Valor por cima da barra; se não houver espaço, fica dentro dela.
            float valueY = barTop - dp(4);
            if (valueY < top) valueY = barTop + dp(14);
            canvas.drawText(formatStepValue(item.value), centerX, valueY, valuePaint);

            canvas.drawText(item.label, centerX, bottom + dp(18), dayPaint);
        }
    }

    private String formatStepValue(int value) {
        if (value >= 1000) {
            return value % 1000 == 0 ? (value / 1000) + "k" : String.format(java.util.Locale.getDefault(), "%.1fk", value / 1000.0);
        }
        return String.valueOf(value);
    }

    public static class ChartData {
        public final String label;
        public final int value;

        public ChartData(String label, int value) {
            this.label = label;
            this.value = value;
        }
    }
}
