package com.example.smart_water_projet.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * Fond cinématique : dégradé type « 3D » (sweep animé), halos profondeur, vignette.
 */
public class WelcomeAtmosphereView extends View {

    private final Paint sweepPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint vignettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix sweepMatrix = new Matrix();

    private final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
    private float sweepAngle = 0f;
    private float pulse      = 0f;

    public WelcomeAtmosphereView(Context context) {
        this(context, null);
    }

    public WelcomeAtmosphereView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        animator.setDuration(18_000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            float t = a.getAnimatedFraction();
            sweepAngle = t * 360f;
            pulse = (float) (0.5f + 0.5f * Math.sin(t * Math.PI * 2));
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        animator.cancel();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w * 0.5f;
        float cy = h * 0.22f;

        int[] sweepColors = new int[]{
                Color.parseColor("#01080E"),
                Color.parseColor("#08324A"),
                Color.parseColor("#1A6EBE"),
                Color.parseColor("#4ECDC4"),
                Color.parseColor("#06202C"),
                Color.parseColor("#01080E")
        };
        float[] sweepPos = new float[]{0f, 0.18f, 0.42f, 0.58f, 0.82f, 1f};
        SweepGradient sweep = new SweepGradient(cx, cy, sweepColors, sweepPos);
        sweepMatrix.setRotate(sweepAngle, cx, cy);
        sweep.setLocalMatrix(sweepMatrix);
        sweepPaint.setShader(sweep);
        canvas.drawRect(0, 0, w, h, sweepPaint);

        // Profondeur : bande inférieure plus sombre (sol « 3D »)
        Paint floor = new Paint(Paint.ANTI_ALIAS_FLAG);
        Shader floorGrad = new LinearGradient(
                0, h * 0.55f, 0, h,
                Color.parseColor("#00000000"),
                Color.parseColor("#D9000000"),
                Shader.TileMode.CLAMP);
        floor.setShader(floorGrad);
        canvas.drawRect(0, h * 0.45f, w, h, floor);

        // Halos doux (bokeh)
        float breathe = 0.85f + 0.15f * pulse;
        drawOrb(canvas, w * 0.18f, h * 0.12f, 140f * breathe, 0x334ECDC4);
        drawOrb(canvas, w * 0.88f, h * 0.28f, 110f * breathe, 0x281A6EBE);
        drawOrb(canvas, w * 0.72f, h * 0.08f, 90f * breathe, 0x224ECDC4);

        // Reflet « nappe » d’eau horizontale
        Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
        float sheenY = h * (0.38f + 0.02f * (float) Math.sin(sweepAngle * Math.PI / 180f * 0.05f));
        Shader sheenShader = new LinearGradient(
                0, sheenY - 40, w, sheenY + 60,
                new int[]{0x0000E5FF, 0x2200E5FF, 0x0000E5FF},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP);
        sheen.setShader(sheenShader);
        canvas.drawRect(0, sheenY - 50, w, sheenY + 70, sheen);

        // Vignette bords
        Shader vig = new RadialGradient(
                cx, cy * 1.25f, Math.max(w, h) * 0.92f,
                new int[]{0x00000000, 0x00000000, 0xBB000000},
                new float[]{0f, 0.62f, 1f},
                Shader.TileMode.CLAMP);
        vignettePaint.setShader(vig);
        canvas.drawRect(0, 0, w, h, vignettePaint);
    }

    private void drawOrb(Canvas canvas, float x, float y, float r, int color) {
        orbPaint.setShader(new RadialGradient(x, y, r, color, 0x00000000, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, r, orbPaint);
        orbPaint.setShader(null);
    }
}
