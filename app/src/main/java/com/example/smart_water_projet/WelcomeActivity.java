package com.example.smart_water_projet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smart_water_projet.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends AppCompatActivity {

    private ActivityWelcomeBinding binding;

    // Hero card
    private ObjectAnimator plantTiltY;
    private ObjectAnimator plantTiltX;
    private ObjectAnimator plantFloatY;

    // Halo rings
    private ObjectAnimator haloSpinOuter;
    private ObjectAnimator haloSpinInner;

    // Brand
    private ObjectAnimator brandGlow;
    private ObjectAnimator dotPulseScaleX;
    private ObjectAnimator dotPulseScaleY;
    private ObjectAnimator dotPulseAlpha;

    // Ripple rings
    private ObjectAnimator ripple1ScaleX;
    private ObjectAnimator ripple1ScaleY;
    private ObjectAnimator ripple1Alpha;
    private ObjectAnimator ripple2ScaleX;
    private ObjectAnimator ripple2ScaleY;
    private ObjectAnimator ripple2Alpha;

    // Particles
    private ObjectAnimator[] particleAnimators;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // Hide everything for staggered entrance
        hideForEntrance();

        // Start background / ambient animations immediately
        setupPlantDepthMotion();
        startHaloRotation();
        startPulsingDot();
        startRippleRings();
        startParticleSparkle();

        // Staggered entrance for UI elements
        startEntranceAnimations();

        binding.btnBienvenue.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    // ── Hide views before entrance ───────────────────────────────────────────
    private void hideForEntrance() {
        int[] ids = {
                binding.plantHeroCard.getId(),
                binding.brandBadge.getId(),
                binding.tvWelcomeTitle.getId(),
                binding.tvWelcomeSubtitle.getId(),
                binding.statsRow.getId(),
                binding.cardWelcomeHint.getId(),
                binding.tvCtaSub.getId(),
                binding.btnBienvenue.getId()
        };
        View root = binding.getRoot();
        for (int id : ids) {
            View v = root.findViewById(id);
            if (v != null) {
                v.setAlpha(0f);
                v.setTranslationY(40f);
            }
        }
    }

    // ── Hero card — 3-D depth tilt + float ───────────────────────────────────
    private void setupPlantDepthMotion() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float dist = 9000f * dm.density;
        binding.plantHeroCard.setCameraDistance(dist);
        binding.plantHeroCard.setRotationX(-4f);
        binding.plantHeroCard.setRotationY(0f);

        // Tilt Y (left ↔ right)
        plantTiltY = ObjectAnimator.ofFloat(binding.plantHeroCard, View.ROTATION_Y, -7f, 7f);
        plantTiltY.setDuration(5200);
        plantTiltY.setRepeatCount(ObjectAnimator.INFINITE);
        plantTiltY.setRepeatMode(ObjectAnimator.REVERSE);
        plantTiltY.setInterpolator(new AccelerateDecelerateInterpolator());
        plantTiltY.start();

        // Tilt X (top ↔ bottom)
        plantTiltX = ObjectAnimator.ofFloat(binding.plantHeroCard, View.ROTATION_X, -6f, -2f);
        plantTiltX.setDuration(4100);
        plantTiltX.setRepeatCount(ObjectAnimator.INFINITE);
        plantTiltX.setRepeatMode(ObjectAnimator.REVERSE);
        plantTiltX.setInterpolator(new AccelerateDecelerateInterpolator());
        plantTiltX.setStartDelay(200);
        plantTiltX.start();

        // Gentle vertical float on the plant image itself
        plantFloatY = ObjectAnimator.ofFloat(binding.ivWelcomePlant, View.TRANSLATION_Y, 0f, -18f);
        plantFloatY.setDuration(2200);
        plantFloatY.setRepeatCount(ObjectAnimator.INFINITE);
        plantFloatY.setRepeatMode(ObjectAnimator.REVERSE);
        plantFloatY.setInterpolator(new AccelerateDecelerateInterpolator());
        plantFloatY.start();
    }

    // ── Halo rings rotate (outer CW, inner CCW) ──────────────────────────────
    private void startHaloRotation() {
        haloSpinOuter = ObjectAnimator.ofFloat(binding.haloRingOuter, View.ROTATION, 0f, 360f);
        haloSpinOuter.setDuration(20_000);
        haloSpinOuter.setRepeatCount(ObjectAnimator.INFINITE);
        haloSpinOuter.setInterpolator(new LinearInterpolator());
        haloSpinOuter.start();

        haloSpinInner = ObjectAnimator.ofFloat(binding.haloRingInner, View.ROTATION, 360f, 0f);
        haloSpinInner.setDuration(14_000);
        haloSpinInner.setRepeatCount(ObjectAnimator.INFINITE);
        haloSpinInner.setInterpolator(new LinearInterpolator());
        haloSpinInner.start();
    }

    // ── Brand badge dot pulses ───────────────────────────────────────────────
    private void startPulsingDot() {
        // Keep old brand glow too
        brandGlow = ObjectAnimator.ofFloat(binding.brandBadge, View.ALPHA, 0.85f, 1f);
        brandGlow.setDuration(1600);
        brandGlow.setRepeatCount(ObjectAnimator.INFINITE);
        brandGlow.setRepeatMode(ObjectAnimator.REVERSE);
        brandGlow.setInterpolator(new AccelerateDecelerateInterpolator());
        brandGlow.start();

        dotPulseScaleX = ObjectAnimator.ofFloat(binding.brandPulsingDot, View.SCALE_X, 1f, 1.4f);
        dotPulseScaleY = ObjectAnimator.ofFloat(binding.brandPulsingDot, View.SCALE_Y, 1f, 1.4f);
        dotPulseAlpha  = ObjectAnimator.ofFloat(binding.brandPulsingDot, View.ALPHA,   0.6f, 1f);

        AnimatorSet dotSet = new AnimatorSet();
        dotSet.playTogether(dotPulseScaleX, dotPulseScaleY, dotPulseAlpha);
        dotSet.setDuration(900);
        dotSet.setInterpolator(new AccelerateDecelerateInterpolator());

        // Manually loop via listener (AnimatorSet doesn't support repeatCount)
        dotSet.addListener(new AnimatorListenerAdapter() {
            boolean forward = true;
            @Override
            public void onAnimationEnd(Animator animation) {
                forward = !forward;
                if (forward) {
                    dotPulseScaleX.setFloatValues(1f, 1.4f);
                    dotPulseScaleY.setFloatValues(1f, 1.4f);
                    dotPulseAlpha.setFloatValues(0.6f, 1f);
                } else {
                    dotPulseScaleX.setFloatValues(1.4f, 1f);
                    dotPulseScaleY.setFloatValues(1.4f, 1f);
                    dotPulseAlpha.setFloatValues(1f, 0.6f);
                }
                dotSet.start();
            }
        });
        dotSet.start();
    }

    // ── Ripple rings expand + fade (staggered) ───────────────────────────────
    private void startRippleRings() {
        animateRipple(binding.rippleRing1, 0);
        animateRipple(binding.rippleRing2, 800);
    }

    private void animateRipple(View ring, long startDelay) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(ring, View.SCALE_X, 0.9f, 1.6f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(ring, View.SCALE_Y, 0.9f, 1.6f);
        ObjectAnimator al = ObjectAnimator.ofFloat(ring, View.ALPHA,   0.7f, 0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy, al);
        set.setDuration(2500);
        set.setStartDelay(startDelay);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) { set.start(); }
        });
        set.start();
    }

    // ── Particles sparkle in/out ─────────────────────────────────────────────
    private void startParticleSparkle() {
        View[] particles = {
                binding.particle1,
                binding.particle2,
                binding.particle3,
                binding.particle4,
                binding.particle5
        };
        long[] delays = { 300L, 1000L, 700L, 2000L, 500L };

        particleAnimators = new ObjectAnimator[particles.length * 2];

        for (int i = 0; i < particles.length; i++) {
            View p = particles[i];
            long delay = delays[i];

            ObjectAnimator alpha = ObjectAnimator.ofFloat(p, View.ALPHA, 0f, 0.85f);
            alpha.setDuration(1800);
            alpha.setStartDelay(delay);
            alpha.setRepeatCount(ObjectAnimator.INFINITE);
            alpha.setRepeatMode(ObjectAnimator.REVERSE);
            alpha.setInterpolator(new AccelerateDecelerateInterpolator());
            alpha.start();

            ObjectAnimator scale = ObjectAnimator.ofFloat(p, View.SCALE_X, 0.3f, 1f);
            scale.setDuration(1800);
            scale.setStartDelay(delay);
            scale.setRepeatCount(ObjectAnimator.INFINITE);
            scale.setRepeatMode(ObjectAnimator.REVERSE);
            scale.setInterpolator(new AccelerateDecelerateInterpolator());
            scale.start();

            particleAnimators[i * 2]     = alpha;
            particleAnimators[i * 2 + 1] = scale;
        }
    }

    // ── Staggered entrance slide-up ──────────────────────────────────────────
    private void startEntranceAnimations() {
        View root = binding.getRoot();
        int[][] entries = {
                { binding.plantHeroCard.getId(),    0   },
                { binding.brandBadge.getId(),        200 },
                { binding.tvWelcomeTitle.getId(),    350 },
                { binding.tvWelcomeSubtitle.getId(), 480 },
                { binding.statsRow.getId(),          560 },
                { binding.cardWelcomeHint.getId(),   680 },
                { binding.tvCtaSub.getId(),          760 },
                { binding.btnBienvenue.getId(),      820 },
        };

        for (int[] entry : entries) {
            View v = root.findViewById(entry[0]);
            if (v == null) continue;
            long delay = entry[1];

            ObjectAnimator fadeIn  = ObjectAnimator.ofFloat(v, View.ALPHA, 0f, 1f);
            ObjectAnimator slideUp = ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, 40f, 0f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(fadeIn, slideUp);
            set.setDuration(600);
            set.setStartDelay(delay);
            set.setInterpolator(new AccelerateDecelerateInterpolator());
            set.start();
        }
    }

    // ── Lifecycle — cancel all animators to avoid leaks ──────────────────────
    @Override
    protected void onDestroy() {
        cancelIfRunning(
                plantTiltY, plantTiltX, plantFloatY,
                haloSpinOuter, haloSpinInner,
                brandGlow,
                dotPulseScaleX, dotPulseScaleY, dotPulseAlpha,
                ripple1ScaleX, ripple1ScaleY, ripple1Alpha,
                ripple2ScaleX, ripple2ScaleY, ripple2Alpha
        );
        if (particleAnimators != null) {
            cancelIfRunning(particleAnimators);
        }
        super.onDestroy();
    }

    private void cancelIfRunning(ObjectAnimator... animators) {
        for (ObjectAnimator a : animators) {
            if (a != null) a.cancel();
        }
    }
}