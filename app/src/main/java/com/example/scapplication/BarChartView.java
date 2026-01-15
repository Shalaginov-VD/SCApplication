package com.example.scapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {

    private List<BarData> data = new ArrayList<>();
    private Paint barPaint, textPaint, axisPaint;
    private float maxValue = 0;
    private float padding = 20;
    private float barWidthRatio = 0.6f;

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#2196F3"));
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(28);
        textPaint.setTextAlign(Paint.Align.CENTER);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.GRAY);
        axisPaint.setStrokeWidth(2);
    }

    public void setData(List<BarData> data) {
        this.data = data;
        calculateMaxValue();
        invalidate();
    }

    private void calculateMaxValue() {
        maxValue = 0;
        for (BarData bar : data) {
            if (bar.value > maxValue) {
                maxValue = bar.value;
            }
        }

        if (maxValue == 0) {
            maxValue = 1;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data == null || data.isEmpty()) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        float axisY = height - padding - 50;
        canvas.drawLine(padding, axisY, width - padding, axisY, axisPaint);

        float availableWidth = width - 2 * padding;
        float barWidth = (availableWidth / data.size()) * barWidthRatio;
        float spaceBetweenBars = (availableWidth / data.size()) * (1 - barWidthRatio);

        for (int i = 0; i < data.size(); i++) {
            BarData bar = data.get(i);

            if (bar.value == 0) {
                continue;
            }

            float left = padding + i * (barWidth + spaceBetweenBars) + spaceBetweenBars / 2;
            float right = left + barWidth;

            float barHeight = (bar.value / maxValue) * (axisY - padding - 50);
            float top = axisY - barHeight;
            float bottom = axisY;

            canvas.drawRect(left, top, right, bottom, barPaint);

            String valueText = String.valueOf((int) bar.value);
            canvas.drawText(valueText, left + barWidth / 2, top - 10, textPaint);

            canvas.drawText(bar.label, left + barWidth / 2, axisY + 40, textPaint);
        }
    }

    public static class BarData {
        public String label;
        public float value;

        public BarData(String label, float value) {
            this.label = label;
            this.value = value;
        }
    }
}