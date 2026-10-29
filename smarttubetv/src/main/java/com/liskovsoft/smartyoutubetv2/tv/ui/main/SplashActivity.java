package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SplashPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;

public class SplashActivity extends MotherActivity implements SplashView {
    private static final String TAG = SplashActivity.class.getSimpleName();
    private Intent mNewIntent;
    private SplashPresenter mPresenter;
    private ObjectAnimator mLoadingPulse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PiTubeCrashReporter.note("SplashActivity: starting presenter");

        mNewIntent = getIntent();

        // The presenter may wait for network/account startup. Show useful
        // feedback immediately instead of an empty dark screen; no decoder,
        // remote assets or additional startup thread required.
        showBrandedLoading();

        mPresenter = SplashPresenter.instance(this);
        mPresenter.setView(this);
        getWindow().getDecorView().post(() -> {
            if (!isFinishing() && !isDestroyed()) {
                mPresenter.onViewInitialized();
                PiTubeCrashReporter.note("SplashActivity: presenter initialized");
            }
        });

        //finish();
    }

    private void showBrandedLoading() {
        FrameLayout canvas = new FrameLayout(this);
        canvas.setBackgroundResource(R.drawable.pitube_main_backdrop);

        LinearLayout center = new LinearLayout(this);
        center.setGravity(Gravity.CENTER);
        center.setOrientation(LinearLayout.VERTICAL);

        ImageView banner = new ImageView(this);
        banner.setImageResource(R.mipmap.app_banner);
        banner.setScaleType(ImageView.ScaleType.FIT_CENTER);
        center.addView(banner, new LinearLayout.LayoutParams(dp(380), dp(214)));

        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(ColorStateList.valueOf(
                Color.rgb(122, 188, 235)));
        LinearLayout.LayoutParams spinnerParams =
                new LinearLayout.LayoutParams(dp(28), dp(28));
        spinnerParams.topMargin = dp(12);
        center.addView(progress, spinnerParams);

        canvas.addView(center, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        setContentView(canvas);

        mLoadingPulse = ObjectAnimator.ofFloat(banner, View.ALPHA, 0.75f, 1f);
        mLoadingPulse.setDuration(1400L);
        mLoadingPulse.setRepeatCount(ValueAnimator.INFINITE);
        mLoadingPulse.setRepeatMode(ValueAnimator.REVERSE);
        mLoadingPulse.start();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mNewIntent = intent;

        mPresenter.onViewInitialized();
    }

    @Override
    protected void onDestroy() {
        if (mLoadingPulse != null) {
            mLoadingPulse.cancel();
            mLoadingPulse = null;
        }
        super.onDestroy();
        if (mPresenter != null) mPresenter.onViewDestroyed();
    }

    @Override
    public Intent getNewIntent() {
        return mNewIntent;
    }

    @Override
    public void finishView() {
        try {
            finish();
        } catch (NullPointerException e) {
            // NullPointerException: Attempt to invoke virtual method 'void com.android.server.wm.DisplayContent.moveStack(com.android.server.wm.TaskStack, boolean)'
            e.printStackTrace();
        }
    }
}
