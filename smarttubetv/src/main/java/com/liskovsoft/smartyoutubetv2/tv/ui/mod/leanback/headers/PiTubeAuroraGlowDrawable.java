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
 * A soft animated halo drawn BEHIND a selected video card, outside its bounds.
 * The card's own image and text cover the inner half of each translucent band:
 * there is deliberately no sharp line or foreground border over the thumbnail.
 *
 * Three simple strokes share one shader, with no per-frame allocations, bitmap
 * blur, offscreen layers or animation on unselected cards.
 */
public final class PiTubeAuroraGlowDrawable extends Drawable {
    private static final long FRAME_INTERVAL_MS = 50L;
    private static final int[] HALO_ALPHA = {27, 38, 50};

    private final Paint[] mHalo = new Paint[3];
    private final RectF mCardBounds = new RectF();
    private final Matrix mMotion = new Matrix();
    private final float mDensity;
    private Shader mShader;
    private float mPhase;
    private int mAlpha;
    private boolean mActive;

    private final Runnable mAnimation = new Runnable() {
        @Override
        public void run() {
            if (!mActive) return;
            mPhase = (mPhase + 0.007f) % 1f;
            moveShader();
            invalidateSelf();
            scheduleSelf(this, SystemClock.uptimeMillis() + FRAME_INTERVAL_MS);
        }
    };

    public PiTubeAuroraGlowDrawable(float density) {
        mDensity = density;
        float[] widths = {29f, 19f, 10f};
        for (int i = 0; i < mHalo.length; i++) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(widths[i] * density);
            mHalo[i] = paint;
        }
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

    private void moveShader() {
        mMotion.setRotate(360f * mPhase,
                mCardBounds.centerX(), mCardBounds.centerY());
        if (mShader != null) mShader.setLocalMatrix(mMotion);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mCardBounds.set(bounds.left + mDensity, bounds.top + mDensity,
                bounds.right - mDensity, bounds.bottom - mDensity);
        mShader = new SweepGradient(mCardBounds.centerX(),
                mCardBounds.centerY(),
                new int[] {0xFF40C7DC, 0xFF5B83F0, 0xFFB287D8,
                        0xFFD28ABB, 0xFF58B7D9, 0xFF40C7DC},
                new float[] {0f, 0.19f, 0.43f, 0.65f, 0.83f, 1f});
        for (Paint paint : mHalo) paint.setShader(mShader);
        moveShader();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mAlpha == 0 || mCardBounds.isEmpty()) return;
        float radius = 19f * mDensity;
        for (Paint paint : mHalo) {
            canvas.drawRoundRect(mCardBounds, radius, radius, paint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = Math.max(0, Math.min(255, alpha));
        for (int i = 0; i < mHalo.length; i++) {
            mHalo[i].setAlpha(HALO_ALPHA[i] * mAlpha / 255);
        }
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        for (Paint paint : mHalo) paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
