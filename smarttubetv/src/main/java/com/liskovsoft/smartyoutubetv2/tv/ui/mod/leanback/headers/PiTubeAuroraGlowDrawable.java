package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.headers;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A thin, softly feathered aurora around the focused video card. Fine,
 * overlapping translucent bands approximate a blurred falloff, instead of
 * rendering a clearly visible border or using a costly software blur.
 * The shader animates only for the focused card, at 20fps.
 */
public final class PiTubeAuroraGlowDrawable extends Drawable {
    private static final long FRAME_INTERVAL_MS = 50L;
    private static final int GLOW_LAYERS = 16;
    private static final float GLOW_SPREAD_DP = 7.5f;
    private static final float CORNER_RADIUS_DP = 22f;
    private static final float STROKE_WIDTH_DP = 1.5f;

    private final Paint mGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mCardBounds = new RectF();
    private final RectF mBandBounds = new RectF();
    private final Path mOutsideOnly = new Path();
    private final Matrix mMotion = new Matrix();
    private final float mDensity;
    private Shader mShader;
    private float mPhase;
    private int mAlpha;
    private float mCornerRadiusPx;
    private boolean mActive;

    private final Runnable mAnimation = new Runnable() {
        @Override public void run() {
            if (!mActive) return;
            mPhase = (mPhase + 0.0035f) % 1f;
            moveShader();
            invalidateSelf();
            scheduleSelf(this, SystemClock.uptimeMillis() + FRAME_INTERVAL_MS);
        }
    };

    public PiTubeAuroraGlowDrawable(float density) {
        mDensity = density;
        mCornerRadiusPx = CORNER_RADIUS_DP * density;
        mGlow.setStyle(Paint.Style.STROKE);
        mGlow.setStrokeJoin(Paint.Join.ROUND);
        mGlow.setStrokeWidth(STROKE_WIDTH_DP * density);
        setAlpha(0);
    }

    public void setActive(boolean active) {
        if (mActive == active) return;
        mActive = active;
        unscheduleSelf(mAnimation);
        if (active) {
            scheduleSelf(mAnimation, SystemClock.uptimeMillis() + FRAME_INTERVAL_MS);
        }
        invalidateSelf();
    }

    public void setCornerRadiusPx(float radius) {
        if (Math.abs(mCornerRadiusPx - radius) < 0.15f) return;
        mCornerRadiusPx = radius;
        rebuildOutsideClip();
        invalidateSelf();
    }

    private void rebuildOutsideClip() {
        float margin = (GLOW_SPREAD_DP + STROKE_WIDTH_DP + 1f) * mDensity;
        mOutsideOnly.reset();
        mOutsideOnly.setFillType(Path.FillType.EVEN_ODD);
        mOutsideOnly.addRect(mCardBounds.left - margin,
                mCardBounds.top - margin,
                mCardBounds.right + margin,
                mCardBounds.bottom + margin, Path.Direction.CW);
        mOutsideOnly.addRoundRect(mCardBounds, mCornerRadiusPx,
                mCornerRadiusPx, Path.Direction.CW);
    }

    private void moveShader() {
        mMotion.setRotate(360f * mPhase,
                mCardBounds.centerX(), mCardBounds.centerY());
        if (mShader != null) mShader.setLocalMatrix(mMotion);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mCardBounds.set(bounds);
        rebuildOutsideClip();

        mShader = new SweepGradient(mCardBounds.centerX(),
                mCardBounds.centerY(),
                new int[] {0xFF6BBFD8, 0xFF759CE5, 0xFFAD9BDC,
                        0xFFD3A5C5, 0xFF8BACEA, 0xFF6BBFD8},
                new float[] {0f, 0.21f, 0.45f, 0.67f, 0.84f, 1f});
        mGlow.setShader(mShader);
        moveShader();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mAlpha == 0 || mCardBounds.isEmpty()) return;

        int saved = canvas.save();
        canvas.clipPath(mOutsideOnly);
        float radius = mCornerRadiusPx;
        float spread = GLOW_SPREAD_DP * mDensity;

        for (int i = 0; i < GLOW_LAYERS; i++) {
            float distance = i / (float) (GLOW_LAYERS - 1);
            // Fade continuously, from subtle illumination at the card to
            // fully transparent at the outer edge. No visible contour.
            float falloff = 1f - distance;
            int alpha = Math.round(29f * falloff * falloff * mAlpha / 255f);
            if (alpha == 0) continue;

            float expand = spread * distance;
            mBandBounds.set(mCardBounds.left - expand,
                    mCardBounds.top - expand,
                    mCardBounds.right + expand,
                    mCardBounds.bottom + expand);
            mGlow.setAlpha(alpha);
            canvas.drawRoundRect(mBandBounds, radius + expand,
                    radius + expand, mGlow);
        }
        canvas.restoreToCount(saved);
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = Math.max(0, Math.min(255, alpha));
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter filter) {
        mGlow.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
