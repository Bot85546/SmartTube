package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.complexcardview;

import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
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
    private ViewGroup mGlowOverlayHost;
    private final Rect mGlowPosition = new Rect();
    private final int[] mCardWindowLocation = new int[2];
    private final int[] mHostWindowLocation = new int[2];
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
        if (!PiTubeCrashReporter.isSafeMode(getContext())) {
            mAuroraGlow = new PiTubeAuroraGlowDrawable(
                    getResources().getDisplayMetrics().density);
            // The grid overlay renders outside the view's hardware clip.
            // No foreground or background border is added to this card.
        }
    }

    private void attachGlowOverlay() {
        if (!isAttachedToWindow() || !(getParent() instanceof ViewGroup)) return;
        // Leanback often wraps cards in ShadowOverlayContainer. Its own
        // overlay would be exactly the size of the card and clip the aura.
        // Attach to the ancestor RecyclerView, whose viewport has room for
        // the halo on every side.
        ViewGroup host = (ViewGroup) getParent();
        ViewParent ancestor = host;
        while (ancestor instanceof ViewGroup) {
            if (ancestor instanceof RecyclerView) {
                host = (ViewGroup) ancestor;
                break;
            }
            ancestor = ancestor.getParent();
        }
        if (mGlowOverlayHost == host) return;
        removeGlowOverlay();
        mGlowOverlayHost = host;
        host.getOverlay().add(mAuroraGlow);
        getViewTreeObserver().addOnPreDrawListener(mGlowPositionListener);
        syncGlowPosition();
    }

    private void syncGlowPosition() {
        if (mGlowOverlayHost == null || mAuroraGlow == null) return;
        getLocationInWindow(mCardWindowLocation);
        mGlowOverlayHost.getLocationInWindow(mHostWindowLocation);
        float scaleX = getScaleX();
        float scaleY = getScaleY();
        ViewParent ancestor = getParent();
        while (ancestor instanceof View && ancestor != mGlowOverlayHost) {
            View container = (View) ancestor;
            scaleX *= container.getScaleX();
            scaleY *= container.getScaleY();
            ancestor = container.getParent();
        }
        float scaledWidth = getWidth() * scaleX;
        float scaledHeight = getHeight() * scaleY;
        float left = mCardWindowLocation[0] - mHostWindowLocation[0];
        float top = mCardWindowLocation[1] - mHostWindowLocation[1];
        mGlowPosition.set(Math.round(left), Math.round(top),
                Math.round(left + scaledWidth), Math.round(top + scaledHeight));
        if (!mGlowPosition.equals(mAuroraGlow.getBounds())) {
            mAuroraGlow.setBounds(mGlowPosition);
        }
    }

    private void removeGlowOverlay() {
        if (mGlowOverlayHost == null) return;
        ViewTreeObserver observer = getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnPreDrawListener(mGlowPositionListener);
        }
        mGlowOverlayHost.getOverlay().remove(mAuroraGlow);
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
