package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
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
        mLoadingPulse = PiTubeLaunchScreen.install(this);

        getWindow().getDecorView().post(() -> {
            if (isFinishing() || isDestroyed()) return;
            // Direct deep links can bypass the launcher recovery activity.
            // The splash is already visible before any required initialization.
            ((MainApplication) getApplication()).ensurePiTubeInitialized();
            mPresenter = SplashPresenter.instance(this);
            mPresenter.setView(this);
            mPresenter.onViewInitialized();
            PiTubeCrashReporter.note("SplashActivity: presenter initialized");
        });

        //finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mNewIntent = intent;

        if (mPresenter != null) mPresenter.onViewInitialized();
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
