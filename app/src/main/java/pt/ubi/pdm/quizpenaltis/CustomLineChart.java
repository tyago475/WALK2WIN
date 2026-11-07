// CustomLineChart.java
package pt.ubi.pdm.quizpenaltis;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class CustomLineChart extends View {
    private List<ChartData> data = new ArrayList<>();
    private Paint linePaint, pointPaint, textPaint, gridPaint, axisPaint, fillPaint;
    private int maxValue = 0;
    private String title = "";
    private String yAxisLabel = "";

    public CustomLineChart(Context context) {
        super(context);
        init();
    }

    public CustomLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Paint para a linha
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#2196F3"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setAntiAlias(true);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        // Paint para preenchimento abaixo da linha
        fillPaint = new Paint();
        fillPaint.setColor(Color.parseColor("#552196F3"));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        // Paint para os pontos
        pointPaint = new Paint();
        pointPaint.setColor(Color.parseColor("#FF4081"));
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);

        // Paint para o texto
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        // Paint para a grid
        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#444444"));
        gridPaint.setStrokeWidth(2f);
        gridPaint.setAntiAlias(true);

        // Paint para os eixos
        axisPaint = new Paint();
        axisPaint.setColor(Color.WHITE);
        axisPaint.setStrokeWidth(3f);
        axisPaint.setAntiAlias(true);
    }

    public void setData(List<ChartData> data, String title, String yAxisLabel) {
        this.data = data;
        this.title = title;
        this.yAxisLabel = yAxisLabel;

        // Calcular valor máximo
        maxValue = 0;
        for (ChartData item : data) {
            if (item.value > maxValue) {
                maxValue = item.value;
            }
        }

        // Garantir que maxValue não seja 0
        if (maxValue == 0) maxValue = 1;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data.isEmpty() || data.size() < 2) {
            drawNoData(canvas);
            return;
        }

        int width = getWidth();
        int height = getHeight();

        // Margens
        int marginLeft = 120;
        int marginRight = 40;
        int marginTop = 100;
        int marginBottom = 120;

        int chartWidth = width - marginLeft - marginRight;
        int chartHeight = height - marginTop - marginBottom;

        // Desenhar título
        textPaint.setTextSize(42f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(title, width / 2, marginTop - 40, textPaint);

        // Desenhar eixos
        canvas.drawLine(marginLeft, marginTop, marginLeft, marginTop + chartHeight, axisPaint);
        canvas.drawLine(marginLeft, marginTop + chartHeight, marginLeft + chartWidth, marginTop + chartHeight, axisPaint);

        // Desenhar label do eixo Y
        textPaint.setTextSize(32f);
        canvas.save();
        canvas.rotate(-90, marginLeft - 80, marginTop + chartHeight / 2);
        canvas.drawText(yAxisLabel, marginLeft - 80, marginTop + chartHeight / 2, textPaint);
        canvas.restore();

        // Desenhar linhas da grid
        drawGrid(canvas, marginLeft, marginTop, chartWidth, chartHeight);

        // Desenhar linha e preenchimento
        drawLineAndFill(canvas, marginLeft, marginTop, chartWidth, chartHeight);

        // Desenhar pontos
        drawPoints(canvas, marginLeft, marginTop, chartWidth, chartHeight);

        // Desenhar labels
        drawLabels(canvas, marginLeft, marginTop, chartWidth, chartHeight);
    }

    private void drawGrid(Canvas canvas, int startX, int startY, int width, int height) {
        int gridLines = 5;
        textPaint.setTextSize(28f);
        textPaint.setColor(Color.parseColor("#AAAAAA"));

        for (int i = 0; i <= gridLines; i++) {
            float y = startY + height - (height * i / gridLines);
            float value = maxValue * i / gridLines;

            // Linha horizontal
            canvas.drawLine(startX, y, startX + width, y, gridPaint);

            // Valor
            canvas.drawText(String.valueOf((int)value), startX - 20, y + 10, textPaint);
        }
    }

    private void drawLineAndFill(Canvas canvas, int startX, int startY, int width, int height) {
        Path fillPath = new Path();
        Path linePath = new Path();

        int pointCount = data.size();
        float segmentWidth = (float) width / (pointCount - 1);

        // Mover para o primeiro ponto
        float firstX = startX;
        float firstY = startY + height - ((float) data.get(0).value / maxValue * height);
        fillPath.moveTo(firstX, startY + height);
        fillPath.lineTo(firstX, firstY);
        linePath.moveTo(firstX, firstY);

        // Adicionar pontos intermediários
        for (int i = 1; i < pointCount; i++) {
            float x = startX + (i * segmentWidth);
            float y = startY + height - ((float) data.get(i).value / maxValue * height);

            linePath.lineTo(x, y);
            fillPath.lineTo(x, y);
        }

        // Completar o preenchimento
        fillPath.lineTo(startX + width, startY + height);
        fillPath.lineTo(startX, startY + height);
        fillPath.close();

        // Desenhar preenchimento primeiro
        canvas.drawPath(fillPath, fillPaint);

        // Desenhar linha por cima
        canvas.drawPath(linePath, linePaint);
    }

    private void drawPoints(Canvas canvas, int startX, int startY, int width, int height) {
        int pointCount = data.size();
        float segmentWidth = (float) width / (pointCount - 1);

        for (int i = 0; i < pointCount; i++) {
            float x = startX + (i * segmentWidth);
            float y = startY + height - ((float) data.get(i).value / maxValue * height);

            // Desenhar ponto
            canvas.drawCircle(x, y, 12f, pointPaint);

            // Desenhar valor do ponto
            textPaint.setTextSize(30f);
            textPaint.setColor(Color.WHITE);
            canvas.drawText(String.valueOf(data.get(i).value), x, y - 20, textPaint);
        }
    }

    private void drawLabels(Canvas canvas, int startX, int startY, int width, int height) {
        int pointCount = data.size();
        float segmentWidth = (float) width / (pointCount - 1);

        textPaint.setTextSize(32f);
        textPaint.setColor(Color.WHITE);

        for (int i = 0; i < pointCount; i++) {
            float x = startX + (i * segmentWidth);
            float textY = startY + height + 50;

            // Desenhar label
            canvas.drawText(data.get(i).label, x, textY, textPaint);
        }
    }

    private void drawNoData(Canvas canvas) {
        textPaint.setTextSize(42f);
        textPaint.setColor(Color.parseColor("#666666"));
        canvas.drawText("Sem dados disponíveis", getWidth() / 2, getHeight() / 2, textPaint);
    }

    // Mesma classe ChartData do CustomBarChart
    public static class ChartData {
        public String label;
        public int value;

        public ChartData(String label, int value) {
            this.label = label;
            this.value = value;
        }
    }
}