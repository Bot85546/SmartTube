package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * A single borderless loading surface shared by the lightweight TV launcher
 * and the actual app. Only alpha animates, so an older BRAVIA can keep drawing
 * this while another process initializes the media services.
 */
final class PiTubeLaunchScreen {
    private PiTubeLaunchScreen() {
    }

    static ObjectAnimator install(Activity activity) {
        FrameLayout canvas = new FrameLayout(activity);
        canvas.setBackgroundResource(R.drawable.pitube_launch_surface);

        LinearLayout center = new LinearLayout(activity);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);

        ImageView logo = new ImageView(activity);
        logo.setImageResource(R.drawable.pitube_wordmark);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        center.addView(logo, new LinearLayout.LayoutParams(
                dp(activity, 380), dp(activity, 214)));

        ProgressBar progress = new ProgressBar(activity);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(ColorStateList.valueOf(
                Color.rgb(133, 187, 231)));
        LinearLayout.LayoutParams progressLayout = new LinearLayout.LayoutParams(
                dp(activity, 25), dp(activity, 25));
        progressLayout.topMargin = dp(activity, 9);
        center.addView(progress, progressLayout);

        TextView hint = new TextView(activity);
        hint.setText("Preparing PiTube");
        hint.setTextSize(13f);
        hint.setTextColor(0xFF93A7C2);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLayout = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hintLayout.topMargin = dp(activity, 13);
        center.addView(hint, hintLayout);

        canvas.addView(center, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        activity.setContentView(canvas);

        ObjectAnimator pulse = ObjectAnimator.ofFloat(
                logo, View.ALPHA, 0.82f, 1f);
        pulse.setDuration(1450L);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.start();
        return pulse;
    }

    private static int dp(Activity activity, float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
