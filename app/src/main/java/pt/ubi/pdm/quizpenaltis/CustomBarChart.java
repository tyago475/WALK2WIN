package pt.ubi.pdm.quizpenaltis;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class CustomBarChart extends View {
    private List<ChartData> data = new ArrayList<>();
    private Paint barPaint, textPaint, gridPaint, axisPaint, valuePaint;
    private int maxValue = Constants.DAILY_STEP_GOAL; // Escala fixa até à meta diária
    private String title = "";
    private String yAxisLabel = "";

    private int[] barColors;

    private int color(int resId) {
        return ContextCompat.getColor(getContext(), resId);
    }

    public CustomBarChart(Context context) {
        super(context);
        init();
    }

    public CustomBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barColors = new int[]{
                color(R.color.chart_bar_1), color(R.color.chart_bar_2), color(R.color.chart_bar_3),
                color(R.color.chart_bar_4), color(R.color.chart_bar_5), color(R.color.chart_bar_6),
                color(R.color.chart_bar_7)
        };
        // Paint para as barras
        barPaint = new Paint();
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setAntiAlias(true);

        // Paint para o texto principal
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        // Paint para valores (melhorado)
        valuePaint = new Paint();
        valuePaint.setColor(Color.WHITE);
        valuePaint.setTextSize(30f);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setAntiAlias(true);
        valuePaint.setFakeBoldText(true);

        // Paint para a grid
        gridPaint = new Paint();
        gridPaint.setColor(color(R.color.chart_grid));
        gridPaint.setStrokeWidth(2f);
        gridPaint.setAntiAlias(true);

        // Paint para os eixos
        axisPaint = new Paint();
        axisPaint.setColor(color(R.color.chart_axis));
        axisPaint.setStrokeWidth(3f);
        axisPaint.setAntiAlias(true);
    }

    public void setData(List<ChartData> data, String title, String yAxisLabel) {
        this.data = data != null ? data : new ArrayList<>();
        this.title = title != null ? title : "Gráfico de Passos";
        this.yAxisLabel = yAxisLabel != null ? yAxisLabel : "Passos";

        // Escala fixa de 0 a 10000
        maxValue = Constants.DAILY_STEP_GOAL;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data.isEmpty()) {
            drawNoData(canvas);
            return;
        }

        int width = getWidth();
        int height = getHeight();

        // Margens otimizadas
        int marginLeft = 120;
        int marginRight = 40;
        int marginTop = 80;
        int marginBottom = 100;

        int chartWidth = width - marginLeft - marginRight;
        int chartHeight = height - marginTop - marginBottom;

        // Desenhar título
        textPaint.setTextSize(38f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(title, width / 2, marginTop - 30, textPaint);

        // Desenhar eixos
        canvas.drawLine(marginLeft, marginTop, marginLeft, marginTop + chartHeight, axisPaint);
        canvas.drawLine(marginLeft, marginTop + chartHeight, marginLeft + chartWidth, marginTop + chartHeight, axisPaint);

        // Desenhar label do eixo Y
        textPaint.setTextSize(32f);
        canvas.save();
        canvas.rotate(-90, marginLeft - 70, marginTop + chartHeight / 2);
        canvas.drawText(yAxisLabel, marginLeft - 70, marginTop + chartHeight / 2, textPaint);
        canvas.restore();

        // Desenhar linhas da grid
        drawGrid(canvas, marginLeft, marginTop, chartWidth, chartHeight);

        // Desenhar barras
        drawBars(canvas, marginLeft, marginTop, chartWidth, chartHeight);
    }

    private void drawGrid(Canvas canvas, int startX, int startY, int width, int height) {
        int gridLines = 10; // 10 linhas para 0-10000 (incrementos de 1000)
        textPaint.setTextSize(26f);
        textPaint.setColor(color(R.color.chart_label));
        textPaint.setTextAlign(Paint.Align.RIGHT);

        for (int i = 0; i <= gridLines; i++) {
            float y = startY + height - (height * i / gridLines);
            int value = i * 1000; // Valores: 0, 1000, 2000, ..., 10000

            // Linha horizontal
            canvas.drawLine(startX, y, startX + width, y, gridPaint);

            // 🔥 VALOR FORMATADO MELHOR - com separador de milhar
            String valueText = formatStepValue(value);
            canvas.drawText(valueText, startX - 10, y + 8, textPaint);
        }

        // Restaurar alinhamento
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void drawBars(Canvas canvas, int startX, int startY, int width, int height) {
        int barCount = data.size();
        int barWidth = (width - 40) / (barCount * 2);
        int barSpacing = barWidth;

        for (int i = 0; i < barCount; i++) {
            ChartData item = data.get(i);
            int color = barColors[i % barColors.length];
            barPaint.setColor(color);

            int barLeft = startX + (i * (barWidth + barSpacing)) + barSpacing;
            int barRight = barLeft + barWidth;

            // Calcular altura da barra usando escala fixa de 0-10000
            float barHeight = ((float) item.value / maxValue) * height;
            int barTop = (int) (startY + height - barHeight);
            int barBottom = startY + height;

            // Desenhar barra
            canvas.drawRect(barLeft, barTop, barRight, barBottom, barPaint);

            // 🔥 VALOR FORMATADO MELHOR - compacto para números grandes
            String valueText = formatStepValue(item.value);

            // Desenhar valor no topo da barra
            valuePaint.setTextSize(28f);
            valuePaint.setColor(Color.WHITE);

            // Posicionar texto de forma inteligente
            float textY = barTop - 12;
            if (textY < startY + 25) {
                textY = barTop + 25; // Colocar dentro da barra se não houver espaço
            }

            canvas.drawText(valueText, (barLeft + barRight) / 2, textY, valuePaint);

            // Desenhar label (abreviatura do dia) abaixo da barra
            valuePaint.setTextSize(26f);
            valuePaint.setColor(color(R.color.chart_value));
            canvas.drawText(item.label, (barLeft + barRight) / 2, barBottom + 32, valuePaint);
        }
    }

    // 🔥 MÉTODO NOVO - Formatar valores de forma mais legível
    private String formatStepValue(int value) {
        if (value >= Constants.DAILY_STEP_GOAL) {
            return String.format("%.1fk", value / 1000.0);
        } else if (value >= 1000) {
            return String.format("%dk", value / 1000);
        } else {
            return String.valueOf(value);
        }
    }

    private void drawNoData(Canvas canvas) {
        textPaint.setTextSize(36f);
        textPaint.setColor(color(R.color.chart_empty));

        // Mensagem mais informativa
        String noDataText = "📊\nSem dados disponíveis\n\nOs teus passos aparecerão aqui!";

        // Dividir em linhas
        String[] lines = noDataText.split("\n");
        float centerX = getWidth() / 2;
        float centerY = getHeight() / 2;

        for (int i = 0; i < lines.length; i++) {
            float y = centerY - (lines.length * 20) / 2 + (i * 40);
            canvas.drawText(lines[i], centerX, y, textPaint);
        }
    }

    // 🔥 MÉTODOS ÚTEIS ADICIONAIS
    public void clearData() {
        this.data.clear();
        invalidate();
    }

    public boolean hasData() {
        return !this.data.isEmpty();
    }

    public int getDataCount() {
        return this.data.size();
    }

    // Classe para dados do gráfico (mantida igual)
    public static class ChartData {
        public String label;
        public int value;

        public ChartData(String label, int value) {
            this.label = label;
            this.value = value;
        }

        // 🔥 MÉTODO toString útil para debugging
        @Override
        public String toString() {
            return "ChartData{label='" + label + "', value=" + value + "}";
        }
    }
}