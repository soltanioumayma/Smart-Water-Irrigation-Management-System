package com.example.smart_water_projet.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Random;

/**
 * Gouttes d'eau dynamiques sur la zone de la plante (chute + léger balancement).
 */
public class WelcomeDropsOnPlantView extends View {

    private static final int DROP_COUNT = 14;

    private final Paint dropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path  dropPath  = new Path();
    private final Random rnd = new Random();

    private final float[] x = new float[DROP_COUNT];
    private final float[] y = new float[DROP_COUNT];
    private final float[] vy = new float[DROP_COUNT];
    private final float[] size = new float[DROP_COUNT];
    private final float[] phase = new float[DROP_COUNT];
    private final float[] wobbleAmp = new float[DROP_COUNT];

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!running) return;
            tick();
            invalidate();
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    private boolean running;

    public WelcomeDropsOnPlantView(Context context) {
        this(context, null);
    }

    public WelcomeDropsOnPlantView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dropPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!running) {
            running = true;
            Choreographer.getInstance().postFrameCallback(frameCallback);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        running = false;
        Choreographer.getInstance().removeFrameCallback(frameCallback);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        for (int i = 0; i < DROP_COUNT; i++) {
            respawn(i, w, h, true);
        }
    }

    private void respawn(int i, int w, int h, boolean scatterY) {
        x[i] = w * (0.16f + rnd.nextFloat() * 0.68f);
        y[i] = scatterY ? -rnd.nextFloat() * Math.max(h, 1) * 0.5f : -10f - rnd.nextFloat() * 40f;
        vy[i] = 0.6f + rnd.nextFloat() * 1.15f;
        size[i] = 3f + rnd.nextFloat() * 4.5f;
        phase[i] = rnd.nextFloat() * (float) (Math.PI * 2);
        wobbleAmp[i] = 0.5f + rnd.nextFloat() * 1.5f;
    }

    private void tick() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float wobbleT = System.currentTimeMillis() * 0.004f;

        for (int i = 0; i < DROP_COUNT; i++) {
            vy[i] += 0.085f;
            y[i] += vy[i];
            x[i] += (float) Math.sin(wobbleT + phase[i]) * 0.32f * wobbleAmp[i];

            if (y[i] > h + size[i] * 4f) {
                respawn(i, w, h, false);
            }
        }
    }

    private void drawTeardrop(Canvas canvas, float px, float py, float s, int color) {
        dropPaint.setColor(color);
        float headTop = py - s * 2.1f;
        RectF head = new RectF(px - s, headTop, px + s, headTop + s * 2.2f);
        canvas.drawOval(head, dropPaint);

        dropPath.reset();
        dropPath.moveTo(px - s * 0.95f, headTop + s * 1.1f);
        dropPath.quadTo(px, py + s * 2.4f, px + s * 0.95f, headTop + s * 1.1f);
        dropPath.close();
        canvas.drawPath(dropPath, dropPaint);

        dropPaint.setColor(Color.argb(120, 255, 255, 255));
        canvas.drawCircle(px - s * 0.35f, headTop + s * 0.75f, s * 0.28f, dropPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        long t = System.currentTimeMillis();

        for (int i = 0; i < DROP_COUNT; i++) {
            float s = size[i];
            float px = x[i];
            float py = y[i];
            int alpha = (int) (75 + 120 * (1f - Math.min(1f, py / (h * 1.15f + 1f))));
            int c = Color.argb(alpha, 78, 205, 196);
            drawTeardrop(canvas, px, py, s, c);
        }

        dropPaint.setColor(0x66FFFFFF);
        for (int k = 0; k < 6; k++) {
            float fx = w * (0.28f + 0.44f * (0.5f + 0.5f * (float) Math.sin(t * 0.0022f + k)));
            float fy = h * (0.18f + 0.12f * (0.5f + 0.5f * (float) Math.cos(t * 0.0016f + k * 1.2f)));
            canvas.drawCircle(fx, fy, 1.1f + (k % 3), dropPaint);
        }
    }
}
