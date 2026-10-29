package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.headers;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Hardware-friendly focus surface for TV remote navigation.
 *
 * A deterministic 40x40 noise tile supplies frosted texture, and gradients
 * provide colour, glass reflection, and a luminous outline. No live blur,
 * offscreen bitmap per frame, or continuously running animation is needed.
 */
public final class PiTubeGlassFocusDrawable extends Drawable {
    private static final int NOISE_SIZE = 40;

    private final RectF mRect = new RectF();
    private final Paint mBase = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mColour = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mReflection = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mNoise = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint mEdgeGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float mRadius;
    private final float mInset;

    public PiTubeGlassFocusDrawable(float density) {
        mRadius = 18f * density;
        mInset = 1.5f * density;
        mBase.setColor(Color.argb(205, 17, 28, 48));
        mEdge.setStyle(Paint.Style.STROKE);
        mEdge.setStrokeWidth(1.2f * density);
        mEdgeGlow.setStyle(Paint.Style.STROKE);
        mEdgeGlow.setStrokeWidth(3f * density);

        // Created once per sidebar, not on every draw or focus movement.
        Bitmap noiseTile = Bitmap.createBitmap(
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
        noiseTile.setPixels(pixels, 0, NOISE_SIZE, 0, 0,
                NOISE_SIZE, NOISE_SIZE);
        mNoise.setShader(new BitmapShader(noiseTile,
                Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
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
        mColour.setShader(new LinearGradient(left, top, left + width, top,
                new int[] {0xA039BBD9, 0xA85C86F6, 0xAA9A7BFA,
                        0x91D572CF, 0x8842BFD5},
                new float[] {0f, 0.22f, 0.51f, 0.77f, 1f},
                Shader.TileMode.CLAMP));
        mReflection.setShader(new LinearGradient(left, top, left, top + height,
                new int[] {0x58FFFFFF, 0x1DFFFFFF, 0x00FFFFFF, 0x0DFFFFFF},
                new float[] {0f, 0.24f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        mEdgeGlow.setShader(new LinearGradient(left, top,
                left + width, top,
                new int[] {0x6540D9FF, 0x4A768AFE, 0x5EDC8AF5, 0x4F81DDFC},
                null, Shader.TileMode.CLAMP));
        mEdge.setShader(new LinearGradient(left, top, left + width, top,
                new int[] {0xD5E6FAFF, 0xBD9CEBFE, 0xBFC4B5FF,
                        0xD3FFD0F1, 0xD2DCF8FF},
                null, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawRoundRect(mRect, mRadius, mRadius, mBase);
        canvas.drawRoundRect(mRect, mRadius, mRadius, mColour);
        canvas.drawRoundRect(mRect, mRadius, mRadius, mReflection);
        canvas.drawRoundRect(mRect, mRadius, mRadius, mNoise);
        canvas.drawRoundRect(mRect, mRadius, mRadius, mEdgeGlow);
        canvas.drawRoundRect(mRect, mRadius, mRadius, mEdge);
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
