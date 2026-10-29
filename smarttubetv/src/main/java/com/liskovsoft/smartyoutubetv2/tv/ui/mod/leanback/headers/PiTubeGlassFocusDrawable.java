package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.headers;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Hardware-friendly focus surface for TV remote navigation.
 *
 * A shared deterministic noise tile supplies frosted texture, and gradients
 * provide colour, glass reflection, and a luminous outline. Only the focused
 * item animates, at a modest frame rate; no live blur or image allocations
 * happen in the drawing loop.
 */
public final class PiTubeGlassFocusDrawable extends Drawable {
    private static final int NOISE_SIZE = 40;
    private static final long FRAME_INTERVAL_MS = 50L;
    private static Bitmap sNoiseTile;

    private final RectF mRect = new RectF();
    private final Paint mBase = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mColour = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mReflection = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mNoise = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint mEdgeGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mColourMatrix = new Matrix();
    private final Matrix mNoiseMatrix = new Matrix();
    private final boolean mOutlineOnly;
    private final boolean mCircular;
    private final boolean mStatefulFocus;
    private final float mDensity;
    private final float mRadius;
    private final float mInset;
    private Shader mColourShader;
    private Shader mEdgeGlowShader;
    private Shader mEdgeShader;
    private Shader mNoiseShader;
    private float mPhase;
    private boolean mActive;
    private final Runnable mAnimation = new Runnable() {
        @Override
        public void run() {
            if (!mActive) return;
            mPhase = (mPhase + 0.010f) % 1f;
            moveShaders();
            invalidateSelf();
            scheduleSelf(this, SystemClock.uptimeMillis() + FRAME_INTERVAL_MS);
        }
    };

    public PiTubeGlassFocusDrawable(float density) {
        this(density, false, false);
    }

    /** Outline mode is suitable for entire video cards and setting tiles. */
    public PiTubeGlassFocusDrawable(float density, boolean outlineOnly) {
        this(density, outlineOnly, false);
    }

    /** Circular stateful outline for SearchOrbView and its companion buttons. */
    public PiTubeGlassFocusDrawable(float density, boolean outlineOnly,
                                   boolean circular) {
        mDensity = density;
        mOutlineOnly = outlineOnly;
        mCircular = circular;
        mStatefulFocus = circular;
        mRadius = (outlineOnly ? 20f : 18f) * density;
        mInset = (outlineOnly ? 3f : 1.5f) * density;
        mBase.setColor(Color.argb(205, 17, 28, 48));
        mEdge.setStyle(Paint.Style.STROKE);
        mEdge.setStrokeWidth((outlineOnly ? 2f : 1.2f) * density);
        mEdgeGlow.setStyle(Paint.Style.STROKE);
        mEdgeGlow.setStrokeWidth((outlineOnly ? 5f : 3f) * density);
        if (outlineOnly) {
            mNoise.setStyle(Paint.Style.STROKE);
            mNoise.setStrokeWidth(2.4f * density);
        }

        // The bitmap is shared by all card outlines; each gets an independent
        // shader matrix so only the selected item's noise tile moves.
        mNoiseShader = new BitmapShader(noiseTile(),
                Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        mNoise.setShader(mNoiseShader);
    }

    private static synchronized Bitmap noiseTile() {
        if (sNoiseTile != null) return sNoiseTile;
        Bitmap tile = Bitmap.createBitmap(
                NOISE_SIZE, NOISE_SIZE, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[NOISE_SIZE * NOISE_SIZE];
        int seed = 0x519BAA27;
        for (int i = 0; i < pixels.length; i++) {
            seed ^= seed << 13;
            seed ^= seed >>> 17;
            seed ^= seed << 5;
            int value = 190 + ((seed >>> 8) & 63);
            int opacity = 10 + ((seed >>> 16) & 31);
            pixels[i] = Color.argb(opacity, value, value, value);
        }
        tile.setPixels(pixels, 0, NOISE_SIZE, 0, 0,
                NOISE_SIZE, NOISE_SIZE);
        sNoiseTile = tile;
        return tile;
    }

    /** Schedule inexpensive shader motion only for the currently focused item. */
    public void setActive(boolean active) {
        if (mActive == active) return;
        mActive = active;
        unscheduleSelf(mAnimation);
        if (active) {
            scheduleSelf(mAnimation, SystemClock.uptimeMillis() + FRAME_INTERVAL_MS);
        }
        invalidateSelf();
    }

    @Override
    public boolean isStateful() {
        return mStatefulFocus || super.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        if (!mStatefulFocus) return super.onStateChange(state);
        boolean focused = false;
        for (int value : state) {
            if (value == android.R.attr.state_focused) {
                focused = true;
                break;
            }
        }
        setAlpha(focused ? 255 : 0);
        setActive(focused);
        return true;
    }

    private void moveShaders() {
        float offset = (float) Math.sin(mPhase * Math.PI * 2f)
                * mRect.width() * 0.18f;
        mColourMatrix.setTranslate(offset, 0f);
        if (mColourShader != null) mColourShader.setLocalMatrix(mColourMatrix);
        if (mEdgeGlowShader != null) mEdgeGlowShader.setLocalMatrix(mColourMatrix);
        if (mEdgeShader != null) mEdgeShader.setLocalMatrix(mColourMatrix);
        mNoiseMatrix.setTranslate(mPhase * 62f * mDensity,
                mPhase * 28f * mDensity);
        if (mNoiseShader != null) mNoiseShader.setLocalMatrix(mNoiseMatrix);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mRect.set(bounds.left + mInset, bounds.top + mInset,
                bounds.right - mInset, bounds.bottom - mInset);
        float left = mRect.left;
        float top = mRect.top;
        float width = mRect.width();
        float height = mRect.height();

        // Cyan, indigo, violet and rose, restrained for readable white text.
        mColourShader = new LinearGradient(left, top, left + width, top,
                new int[] {0xA039BBD9, 0xA85C86F6, 0xAA9A7BFA,
                        0x91D572CF, 0x8842BFD5},
                new float[] {0f, 0.22f, 0.51f, 0.77f, 1f},
                Shader.TileMode.CLAMP);
        mColour.setShader(mColourShader);
        mReflection.setShader(new LinearGradient(left, top, left, top + height,
                new int[] {0x58FFFFFF, 0x1DFFFFFF, 0x00FFFFFF, 0x0DFFFFFF},
                new float[] {0f, 0.24f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        mEdgeGlowShader = new LinearGradient(left, top,
                left + width, top,
                new int[] {0x6540D9FF, 0x4A768AFE, 0x5EDC8AF5, 0x4F81DDFC},
                null, Shader.TileMode.CLAMP);
        mEdgeGlow.setShader(mEdgeGlowShader);
        mEdgeShader = new LinearGradient(left, top, left + width, top,
                new int[] {0xD5E6FAFF, 0xBD9CEBFE, 0xBFC4B5FF,
                        0xD3FFD0F1, 0xD2DCF8FF},
                null, Shader.TileMode.CLAMP);
        mEdge.setShader(mEdgeShader);
        moveShaders();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        float radius = mCircular ? Math.min(mRect.width(), mRect.height()) / 2f : mRadius;
        if (!mOutlineOnly) {
            canvas.drawRoundRect(mRect, radius, radius, mBase);
            canvas.drawRoundRect(mRect, radius, radius, mColour);
            canvas.drawRoundRect(mRect, radius, radius, mReflection);
            canvas.drawRoundRect(mRect, radius, radius, mNoise);
        }
        canvas.drawRoundRect(mRect, radius, radius, mEdgeGlow);
        if (mOutlineOnly) canvas.drawRoundRect(mRect, radius, radius, mNoise);
        canvas.drawRoundRect(mRect, radius, radius, mEdge);
    }

    @Override
    public void setAlpha(int alpha) {
        mBase.setAlpha(alpha);
        mColour.setAlpha(alpha);
        mReflection.setAlpha(alpha);
        mNoise.setAlpha(alpha);
        mEdgeGlow.setAlpha(alpha);
        mEdge.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mBase.setColorFilter(colorFilter);
        mColour.setColorFilter(colorFilter);
        mReflection.setColorFilter(colorFilter);
        mNoise.setColorFilter(colorFilter);
        mEdgeGlow.setColorFilter(colorFilter);
        mEdge.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
