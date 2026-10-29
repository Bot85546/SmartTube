package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.headers;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
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
 * A smoothly feathered animated shadow BEHIND the focused video.
 * Filled, overlapping translucent round rectangles avoid software blur,
 * offscreen bitmaps and the aliasing of stroked or clipped contours.
 * The RecyclerView paints its card on top, completely hiding the underlap.
 * Only the focused card animates, with a 50ms frame interval.
 */
public final class PiTubeAuroraGlowDrawable extends Drawable {
    private static final long FRAME_INTERVAL_MS = 50L;
    private static final int GLOW_LAYERS = 32;
    private static final float GLOW_SPREAD_DP = 20f;
    private static final float INNER_UNDERLAP_DP = 8f;
    private static final float CORNER_RADIUS_DP = 22f;

    private final Paint mGlow = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final RectF mCardBounds = new RectF();
    private final RectF mLayerBounds = new RectF();
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
        mGlow.setStyle(Paint.Style.FILL);
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
        invalidateSelf();
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
        mShader = new SweepGradient(mCardBounds.centerX(),
                mCardBounds.centerY(),
                new int[] {0xFF6FB7E6, 0xFF829DED, 0xFFB4A0D9,
                        0xFFD19FCD, 0xFF8F9BE4, 0xFF72C0DC, 0xFF6FB7E6},
                new float[] {0f, 0.17f, 0.34f, 0.51f, 0.68f, 0.85f, 1f});
        mGlow.setShader(mShader);
        moveShader();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mAlpha == 0 || mCardBounds.isEmpty()) return;

        float spread = GLOW_SPREAD_DP * mDensity;
        float underlap = INNER_UNDERLAP_DP * mDensity;
        for (int i = 0; i < GLOW_LAYERS; i++) {
            float progress = i / (float) (GLOW_LAYERS - 1);
            float expand = spread - (spread + underlap) * progress;
            mLayerBounds.set(mCardBounds.left - expand,
                    mCardBounds.top - expand,
                    mCardBounds.right + expand,
                    mCardBounds.bottom + expand);
            float intensity = 1.1f + 5.8f * progress * progress;
            mGlow.setAlpha(Math.round(intensity * mAlpha / 255f));
            canvas.drawRoundRect(mLayerBounds,
                    Math.max(0f, mCornerRadiusPx + expand),
                    Math.max(0f, mCornerRadiusPx + expand), mGlow);
        }
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
