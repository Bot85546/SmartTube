package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.app.Activity;
import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Minimal Android-native recovery UI, independent of Leanback and the home screen. */
public class PiTubeRecoveryActivity extends Activity {
    private static final int SAVE_REPORT_REQUEST = 81;
    private ObjectAnimator mLoadingPulse;
    private boolean mLaunchingApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PiTubeCrashReporter.shouldShowRecovery(this)) {
            // Render our real splash BEFORE starting the heavyweight main
            // process. The short post allows the launcher to draw its first
            // frame; its pulse then runs independently of main-process setup.
            mLoadingPulse = PiTubeLaunchScreen.install(this);
            getWindow().getDecorView().postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) launchPiTube();
            }, 70L);
            return;
        }
        showRecovery();
    }

    private void showRecovery() {
        getWindow().setStatusBarColor(Color.rgb(9, 14, 24));
        getWindow().setNavigationBarColor(Color.rgb(9, 14, 24));
        boolean pending = PiTubeCrashReporter.hasPendingReport(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(9, 14, 24));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(34), dp(28), dp(34), dp(28));
        scroll.addView(body);
        setContentView(scroll);

        TextView eyebrow = text("PITUBE  /  RECOVERY", 15, 0xFF92B8F4);
        body.addView(eyebrow);

        TextView heading = text(pending ? "We saved the crash report" : "Safe mode is active", 30, Color.WHITE);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setPadding(0, dp(12), 0, dp(10));
        body.addView(heading);

        TextView explanation = text(
                "PiTube stopped unexpectedly. The technical details below are stored only on this device. "
                        + "Copy or save them and send them to the developer. Safe start disables "
                        + "the experimental home navigation and inline search until you return to normal mode.",
                17, 0xFFD1DEEF);
        explanation.setPadding(0, 0, 0, dp(16));
        body.addView(explanation);

        LinearLayout firstRow = new LinearLayout(this);
        firstRow.setOrientation(LinearLayout.HORIZONTAL);
        body.addView(firstRow);
        Button copy = button("Copy report", () -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("PiTube crash report",
                        PiTubeCrashReporter.readReport(this)));
                Toast.makeText(this, "Crash report copied", Toast.LENGTH_SHORT).show();
            }
        });
        firstRow.addView(copy);

        firstRow.addView(button("Save to file", this::saveReport));
        firstRow.addView(button("Share report", this::shareReport));

        LinearLayout secondRow = new LinearLayout(this);
        secondRow.setOrientation(LinearLayout.HORIZONTAL);
        secondRow.setPadding(0, dp(12), 0, dp(16));
        body.addView(secondRow);
        secondRow.addView(button("Safe start", () -> continueToApp(true)));
        secondRow.addView(button("Try normal start", () -> continueToApp(false)));

        TextView label = text("SAVED DIAGNOSTIC REPORT", 14, 0xFF8CAED9);
        body.addView(label);

        TextView report = text(PiTubeCrashReporter.readReport(this), 13, 0xFFE0E7F4);
        report.setTypeface(Typeface.MONOSPACE);
        report.setTextIsSelectable(true);
        report.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable panel = new GradientDrawable();
        panel.setColor(0xFF182339);
        panel.setCornerRadius(dp(18));
        panel.setStroke(dp(1), 0xFF364D6C);
        report.setBackground(panel);
        LinearLayout.LayoutParams reportParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        reportParams.topMargin = dp(10);
        body.addView(report, reportParams);
        copy.requestFocus();
    }

    private TextView text(String value, int sizeSp, int color) {
        TextView result = new TextView(this);
        result.setText(value);
        result.setTextSize(sizeSp);
        result.setTextColor(color);
        return result;
    }

    private Button button(String label, Runnable click) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setMinHeight(dp(52));
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        StateListDrawable backgrounds = new StateListDrawable();
        backgrounds.addState(new int[]{android.R.attr.state_focused}, buttonBackground(0xFF3569AB, 0xFFDBEEFF));
        backgrounds.addState(new int[]{android.R.attr.state_pressed}, buttonBackground(0xFF3569AB, 0xFFDBEEFF));
        backgrounds.addState(new int[]{}, buttonBackground(0xFF253754, 0xFF6684A9));
        button.setBackground(backgrounds);
        button.setOnClickListener(view -> click.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dp(10);
        button.setLayoutParams(params);
        return button;
    }

    private GradientDrawable buttonBackground(int fill, int stroke) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(dp(16));
        background.setStroke(dp(1), stroke);
        return background;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void continueToApp(boolean safeMode) {
        PiTubeCrashReporter.setSafeMode(this, safeMode);
        PiTubeCrashReporter.acknowledgeReport(this);
        PiTubeCrashReporter.note(safeMode ? "User selected safe start" : "User selected normal start");
        launchPiTube();
    }

    private void launchPiTube() {
        if (mLaunchingApp) return;
        mLaunchingApp = true;
        // PiTubeRecoveryActivity runs in :launch, while SplashActivity starts
        // the actual app in the default process. Keep this animated splash
        // visible behind Android's transition until the new window appears.
        startActivity(new Intent(this, SplashActivity.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mLaunchingApp) finish();
    }

    @Override
    protected void onDestroy() {
        if (mLoadingPulse != null) {
            mLoadingPulse.cancel();
            mLoadingPulse = null;
        }
        super.onDestroy();
    }

    private void saveReport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "PiTube-crash-report.txt");
        try {
            startActivityForResult(intent, SAVE_REPORT_REQUEST);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "File picker unavailable; use Copy or Share", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SAVE_REPORT_REQUEST || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri destination = data.getData();
        if (destination == null) return;
        try (OutputStream output = getContentResolver().openOutputStream(destination)) {
            if (output == null) throw new java.io.IOException("Unable to open destination");
            output.write(PiTubeCrashReporter.readReport(this).getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Crash report saved", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "Save failed; use Copy or Share", Toast.LENGTH_LONG).show();
        }
    }

    private void shareReport() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "PiTube crash report");
        intent.putExtra(Intent.EXTRA_TEXT, PiTubeCrashReporter.readReport(this));
        try {
            startActivity(Intent.createChooser(intent, "Share PiTube crash report"));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "No sharing app found; use Copy", Toast.LENGTH_LONG).show();
        }
    }
}
