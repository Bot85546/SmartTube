package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.complexcardview;

import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.leanback.widget.ImageCardView;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.PiTubeCrashReporter;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.headers.PiTubeAuroraGlowDrawable;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class ComplexImageCardView extends ImageCardView {
    private ComplexImageView mComplexImageView;
    private Handler mHandler;
    private boolean mIsCardTextAutoScrollEnabled;
    private boolean mIsBadgeEnabled;
    private PiTubeAuroraGlowDrawable mAuroraGlow;
    private ValueAnimator mGlowAnimator;
    private int mGlowAlpha;
    private RecyclerView mGlowOverlayHost;
    private boolean mGlowDecorationAttached;
    private ViewTreeObserver mGlowObserver;
    // Android refuses to change item decorations during a layout pass, which
    // Leanback can trigger while moving focus to the first video.
    private final Runnable mInstallGlowDecoration = new Runnable() {
        @Override public void run() {
            RecyclerView host = mGlowOverlayHost;
            if (host == null || mGlowDecorationAttached || !isAttachedToWindow()) return;
            if (host.isComputingLayout()) {
                host.post(this);
                return;
            }
            try {
                host.addItemDecoration(mGlowDecoration, 0);
                mGlowDecorationAttached = true;
                host.invalidate();
            } catch (IllegalStateException duringLayout) {
                // Focus events can race layout on older Android TV firmware.
                host.post(this);
            }
        }
    };
    // Draw before children: the focused card covers the inner aura itself.
    private final RecyclerView.ItemDecoration mGlowDecoration =
            new RecyclerView.ItemDecoration() {
                @Override
                public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent,
                        @NonNull RecyclerView.State state) {
                    if (mAuroraGlow != null) mAuroraGlow.draw(canvas);
                }
            };
    private final Drawable.Callback mGlowCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(@NonNull Drawable drawable) {
            if (mGlowOverlayHost != null) mGlowOverlayHost.invalidate();
        }

        @Override
        public void scheduleDrawable(@NonNull Drawable drawable,
                @NonNull Runnable task, long when) {
            if (mGlowOverlayHost != null) {
                mGlowOverlayHost.postDelayed(task,
                        Math.max(0L, when - SystemClock.uptimeMillis()));
            }
        }

        @Override
        public void unscheduleDrawable(@NonNull Drawable drawable,
                @NonNull Runnable task) {
            if (mGlowOverlayHost != null) mGlowOverlayHost.removeCallbacks(task);
        }
    };
    private final Rect mGlowPosition = new Rect();
    private final RectF mMappedCardBounds = new RectF();
    private final Matrix mGlowTransform = new Matrix();
    private final ViewTreeObserver.OnPreDrawListener mGlowPositionListener = () -> {
        syncGlowPosition();
        return true;
    };

    public ComplexImageCardView(Context context) {
        super(context);

        init();
    }

    public ComplexImageCardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public ComplexImageCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        mComplexImageView = findViewById(R.id.main_image_wrapper);
        mHandler = new Handler(Looper.getMainLooper());
        // Leanback may disable its wrapper outline clipping on older TVs.
        // Clip the entire thumbnail + information panel to ONE rounded shape
        // so all cards have the same continuous silhouette, selected or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int radius = getResources().getDimensionPixelSize(
                            R.dimen.lb_rounded_rect_corner_radius);
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            });
            setClipToOutline(true);
        }
        if (!PiTubeCrashReporter.isSafeMode(getContext())) {
            mAuroraGlow = new PiTubeAuroraGlowDrawable(
                    getResources().getDisplayMetrics().density);
            // The card covers the inner edge of its own underlay halo.
            // No border or glow is ever painted on top of its thumbnail.
        }
    }

    private void attachGlowOverlay() {
        if (!isAttachedToWindow() || mAuroraGlow == null) return;

        // Leanback wraps cards in focus-scaled ShadowOverlayContainer views.
        // The row RecyclerView can paint our shadow BEHIND those children.
        RecyclerView host = null;
        ViewParent ancestor = getParent();
        while (ancestor instanceof ViewGroup) {
            if (ancestor instanceof RecyclerView) {
                host = (RecyclerView) ancestor;
                break;
            }
            ancestor = ancestor.getParent();
        }
        if (host == null || mGlowOverlayHost == host) return;
        removeGlowOverlay();
        mGlowOverlayHost = host;
        mAuroraGlow.setCallback(mGlowCallback);
        // Queue the layout-mutating registration for the next UI turn.
        // Focus changes inside RecyclerView layout must never mutate it.
        host.post(mInstallGlowDecoration);
        mGlowObserver = getViewTreeObserver();
        mGlowObserver.addOnPreDrawListener(mGlowPositionListener);
        syncGlowPosition();
        host.invalidate();
    }

    private void syncGlowPosition() {
        if (mGlowOverlayHost == null || mAuroraGlow == null) return;
        if (getWidth() <= 0 || getHeight() <= 0) return;

        // Match the transformed Leanback card precisely, including focus
        // zoom and translated/scaled ShadowOverlayContainer ancestors.
        mGlowTransform.reset();
        transformMatrixToGlobal(mGlowTransform);
        mGlowOverlayHost.transformMatrixToLocal(mGlowTransform);
        mMappedCardBounds.set(0f, 0f, getWidth(), getHeight());
        mGlowTransform.mapRect(mMappedCardBounds);
        mAuroraGlow.setCornerRadiusPx(
                getResources().getDimension(R.dimen.lb_rounded_rect_corner_radius)
                        * mMappedCardBounds.width() / getWidth());
        mGlowPosition.set(Math.round(mMappedCardBounds.left),
                Math.round(mMappedCardBounds.top),
                Math.round(mMappedCardBounds.right),
                Math.round(mMappedCardBounds.bottom));
        if (!mGlowPosition.equals(mAuroraGlow.getBounds())) {
            mAuroraGlow.setBounds(mGlowPosition);
        }
    }

    private void removeGlowOverlay() {
        if (mGlowOverlayHost == null) return;
        ViewTreeObserver observer = mGlowObserver;
        mGlowObserver = null;
        if (observer != null && observer.isAlive()) {
            observer.removeOnPreDrawListener(mGlowPositionListener);
        }

        final RecyclerView host = mGlowOverlayHost;
        host.removeCallbacks(mInstallGlowDecoration);
        mAuroraGlow.setActive(false);
        if (mGlowDecorationAttached) {
            mGlowDecorationAttached = false;
            // Removal is likewise deferred: RecyclerView recycles/detaches
            // cards while it is computing its layout.
            host.post(new Runnable() {
                @Override public void run() {
                    if (host.isComputingLayout()) {
                        host.post(this);
                        return;
                    }
                    try {
                        host.removeItemDecoration(mGlowDecoration);
                    } catch (IllegalStateException duringLayout) {
                        host.post(this);
                    }
                }
            });
        }
        mAuroraGlow.setCallback(null);
        mGlowOverlayHost = null;
    }

    private void enableTitleAnimation(boolean enable) {
        enableTextAnimation(findViewById(R.id.title_text), enable);
    }

    private void enableContentAnimation(boolean enable) {
        enableTextAnimation(findViewById(R.id.content_text), enable);
    }

    private void enableTextAnimation(TextView view, boolean enable) {
        if (view == null) {
            return;
        }

        if (enable) {
            Runnable enableMarquee = () -> ViewUtil.enableMarquee(view);

            mHandler.postDelayed(enableMarquee, 1_000);

            view.setTag(enableMarquee);
        } else {
            if (view.getTag() instanceof Runnable) {
                mHandler.removeCallbacks((Runnable) view.getTag());
                view.setTag(null);
            }

            ViewUtil.disableMarquee(view);
        }
    }

    /**
     * Sets the badge text.
     */
    public void setBadgeText(String text) {
        if (mIsBadgeEnabled) {
            mComplexImageView.setBadgeText(text);
        }
    }

    public void setBadgeColor(int color) {
        mComplexImageView.setBadgeColor(color);
    }

    /**
     * Sets the progress.
     */
    public void setProgress(int percent) {
        mComplexImageView.setProgress(percent);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);

        updateAuroraGlow(selected);

        if (mIsCardTextAutoScrollEnabled) {
            enableTitleAnimation(selected);
            enableContentAnimation(selected);
        }

        enableVideoPreview(selected);
    }

    private void updateAuroraGlow(boolean selected) {
        if (mAuroraGlow == null) return;
        if (selected) attachGlowOverlay();
        if (mGlowAnimator != null) mGlowAnimator.cancel();
        mAuroraGlow.setActive(selected && isAttachedToWindow()
                && mGlowOverlayHost != null);
        int target = selected ? 255 : 0;
        if (!isAttachedToWindow() || mGlowAlpha == target) {
            mGlowAlpha = target;
            mAuroraGlow.setAlpha(target);
            if (!selected) removeGlowOverlay();
            return;
        }
        mGlowAnimator = ValueAnimator.ofInt(mGlowAlpha, target);
        mGlowAnimator.setDuration(190);
        mGlowAnimator.setInterpolator(new DecelerateInterpolator());
        mGlowAnimator.addUpdateListener(animation -> {
            mGlowAlpha = (int) animation.getAnimatedValue();
            mAuroraGlow.setAlpha(mGlowAlpha);
        });
        mGlowAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isSelected()) removeGlowOverlay();
            }
        });
        mGlowAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mGlowAnimator != null) {
            mGlowAnimator.cancel();
            mGlowAnimator = null;
        }
        if (mAuroraGlow != null) {
            mAuroraGlow.setActive(false);
            mAuroraGlow.setAlpha(0);
            mGlowAlpha = 0;
            removeGlowOverlay();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isSelected()) updateAuroraGlow(true);
    }

    @Override
    public boolean performClick() {
        mComplexImageView.stopPlayback(true);

        return super.performClick();
    }

    @Override
    public boolean performLongClick() {
        mComplexImageView.stopPlayback(true);

        return super.performLongClick();
    }

    private void enableVideoPreview(boolean selected) {
        if (selected) {
            mComplexImageView.startPlayback();
        } else {
            mComplexImageView.stopPlayback();
        }
    }

    public void setPreview(Video video) {
        mComplexImageView.setPreview(video);
    }

    public void setMute(boolean muted) {
        mComplexImageView.setMute(muted);
    }

    public void setTitleLinesNum(int lines) {
        TextView titleView = findViewById(R.id.title_text);

        if (titleView == null || lines <= 0) {
            return;
        }

        titleView.setMaxLines(lines);
        titleView.setLines(lines);
    }

    public void setContentLinesNum(int lines) {
        TextView contentView = findViewById(R.id.content_text);

        if (contentView == null || lines <= 0) {
            return;
        }

        contentView.setMaxLines(lines);
        contentView.setLines(lines);
    }

    public void enableBadge(boolean enabled) {
        mIsBadgeEnabled = enabled;
    }

    public void enableTextAutoScroll(boolean enabled) {
        mIsCardTextAutoScrollEnabled = enabled;
    }

    public void setTextScrollSpeed(float speed) {
        ViewUtil.setTextScrollSpeed(findViewById(R.id.title_text), speed);
        ViewUtil.setTextScrollSpeed(findViewById(R.id.content_text), speed);
    }

    public void enableTitle(boolean enabled) {
        ViewUtil.enableView(findViewById(R.id.title_text), enabled);
    }

    public void enableContent(boolean enabled) {
        ViewUtil.enableView(findViewById(R.id.content_text), enabled);
    }

    /**
     * Enables or disables adjustment of view bounds on the main image.
     */
    @Override
    public void setMainImageAdjustViewBounds(boolean adjustViewBounds) {
        super.setMainImageAdjustViewBounds(adjustViewBounds);
        mComplexImageView.setMainImageAdjustViewBounds(adjustViewBounds);
    }

    /**
     * Sets the ScaleType of the main image.
     */
    @Override
    public void setMainImageScaleType(ScaleType scaleType) {
        super.setMainImageScaleType(scaleType);
        mComplexImageView.setMainImageScaleType(scaleType);
    }

    /**
     * Sets the layout dimensions of the ImageView.
     */
    @Override
    public void setMainImageDimensions(int width, int height) {
        super.setMainImageDimensions(width, height);
        mComplexImageView.setMainImageDimensions(width, height);
    }
}
