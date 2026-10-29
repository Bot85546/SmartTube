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
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Hardware-friendly focus surface for TV remote navigation.
 *
 * A shared deterministic noise tile supplies frosted texture. Four broad,
 * independently animated radial colour fields give the glass two-dimensional
 * depth without harsh bands or a dark end cap. Only the focused
 * item animates, at a modest frame rate; no live blur or image allocations
 * happen in the drawing loop.
 */
public final class PiTubeGlassFocusDrawable extends Drawable {
    private static final int NOISE_SIZE = 40;
    private static final long FRAME_INTERVAL_MS = 50L;
    private static final int CLOUD_COUNT = 4;
    private static Bitmap sNoiseTile;

    private final RectF mRect = new RectF();
    private final Paint mBase = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint[] mClouds = new Paint[CLOUD_COUNT];
    private final Shader[] mCloudShaders = new Shader[CLOUD_COUNT];
    private final Matrix[] mCloudMatrices = new Matrix[CLOUD_COUNT];
    private final Paint mReflection = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mNoise = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint mEdgeGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mEdgeMatrix = new Matrix();
    private final Matrix mNoiseMatrix = new Matrix();
    private final boolean mOutlineOnly;
    private final boolean mCircular;
    private final boolean mStatefulFocus;
    private final float mDensity;
    private final float mRadius;
    private final float mInset;
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
        for (int i = 0; i < CLOUD_COUNT; i++) {
            mClouds[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCloudMatrices[i] = new Matrix();
        }
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
        for (int i = 0; i < CLOUD_COUNT; i++) {
            float angle = (float) (2 * Math.PI * (mPhase + i * 0.22f));
            // Different X/Y oscillations make the colour drift in two axes,
            // not sweep horizontally as one compressed rainbow band.
            mCloudMatrices[i].setTranslate(
                    (float) Math.sin(angle) * mRect.width() * 0.085f,
                    (float) Math.cos(angle * 0.8f) * mRect.height() * 0.34f);
            if (mCloudShaders[i] != null) {
                mCloudShaders[i].setLocalMatrix(mCloudMatrices[i]);
            }
        }
        mEdgeMatrix.setRotate(mPhase * 360f, mRect.centerX(), mRect.centerY());
        if (mEdgeGlowShader != null) mEdgeGlowShader.setLocalMatrix(mEdgeMatrix);
        if (mEdgeShader != null) mEdgeShader.setLocalMatrix(mEdgeMatrix);
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

        // Each colour occupies a broad overlapping ellipse instead of a
        // narrow section of a left-to-right linear gradient.
        final float[][] positions = {
                {0.12f, 0.03f, 0.86f},  // aqua, upper left
                {0.34f, 1.10f, 0.95f},  // cobalt, lower half
                {0.70f, 0.62f, 0.87f},  // lavender, middle right
                {1.02f, -0.22f, 0.89f}  // rose and cyan blend at right
        };
        final int[][] colours = {
                {0xAA45B5CD, 0x6E469CCC, 0x00469CCC},
                {0x8E5688D6, 0x655F8BD8, 0x005F8BD8},
                {0x907D78DC, 0x617E86DE, 0x007E86DE},
                {0x90C788B9, 0x5B86AADD, 0x0086AADD}
        };
        for (int i = 0; i < CLOUD_COUNT; i++) {
            float[] p = positions[i];
            mCloudShaders[i] = new RadialGradient(
                    left + width * p[0], top + height * p[1],
                    Math.max(width * p[2], 1f),
                    colours[i], new float[] {0f, 0.46f, 1f},
                    Shader.TileMode.CLAMP);
            mClouds[i].setShader(mCloudShaders[i]);
        }
        mReflection.setShader(new LinearGradient(left, top, left, top + height,
                new int[] {0x58FFFFFF, 0x1DFFFFFF, 0x00FFFFFF, 0x0DFFFFFF},
                new float[] {0f, 0.24f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        mEdgeGlowShader = new SweepGradient(mRect.centerX(), mRect.centerY(),
                new int[] {0x4446D8FA, 0x547A96FF, 0x57CB9DE5,
                        0x4C97C8F4, 0x4446D8FA}, null);
        mEdgeGlow.setShader(mEdgeGlowShader);
        mEdgeShader = new SweepGradient(mRect.centerX(), mRect.centerY(),
                new int[] {0xB0D3F7FF, 0xAFB9C9FF, 0xADB9AFF3,
                        0xB0F4CCE6, 0xB0D3F7FF}, null);
        mEdge.setShader(mEdgeShader);
        moveShaders();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        float radius = mCircular ? Math.min(mRect.width(), mRect.height()) / 2f : mRadius;
        if (!mOutlineOnly) {
            canvas.drawRoundRect(mRect, radius, radius, mBase);
            for (Paint cloud : mClouds) {
                canvas.drawRoundRect(mRect, radius, radius, cloud);
            }
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
        for (Paint cloud : mClouds) cloud.setAlpha(alpha);
        mReflection.setAlpha(alpha);
        mNoise.setAlpha(alpha);
        mEdgeGlow.setAlpha(alpha);
        mEdge.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mBase.setColorFilter(colorFilter);
        for (Paint cloud : mClouds) cloud.setColorFilter(colorFilter);
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
