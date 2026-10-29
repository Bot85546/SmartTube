package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class SettingsCardPresenter extends Presenter {
    private int mDefaultTextColor;
    private int mSelectedTextColor;
    private static final DecelerateInterpolator FOCUS_EASING = new DecelerateInterpolator();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();

        mDefaultTextColor =
                ContextCompat.getColor(context, R.color.pradi_text);
        mSelectedTextColor =
                ContextCompat.getColor(context, R.color.pradi_accent_soft);

        @SuppressLint("InflateParams")
        View container = LayoutInflater.from(context).inflate(R.layout.settings_card, null);
        container.setBackgroundResource(R.drawable.pitube_settings_glass);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            container.setDefaultFocusHighlightEnabled(false);
        }

        TextView textView = container.findViewById(R.id.settings_title);
        textView.setBackgroundColor(Color.TRANSPARENT);
        textView.setTextColor(mDefaultTextColor);

        ViewUtil.setTextScrollSpeed(textView, getCardTextScrollSpeed(context));

        container.setOnFocusChangeListener((v, hasFocus) -> {
            int textColor = hasFocus ? mSelectedTextColor : mDefaultTextColor;
            textView.setTextColor(textColor);
            // Transform-only focus transition: no live blur or new bitmap layer.
            container.animate().cancel();
            container.animate()
                    .scaleX(hasFocus ? 1.035f : 1f)
                    .scaleY(hasFocus ? 1.035f : 1f)
                    .setDuration(150)
                    .setInterpolator(FOCUS_EASING)
                    .start();

            if (hasFocus) {
                ViewUtil.enableMarquee(textView);
            } else {
                ViewUtil.disableMarquee(textView);
            }
        });

        return new ViewHolder(container);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        SettingsItem settingsItem = (SettingsItem) item;

        TextView textView = viewHolder.view.findViewById(R.id.settings_title);

        textView.setText(settingsItem.title);

        if (settingsItem.imageResId > 0) {
            Context context = viewHolder.view.getContext();
            ImageView imageView = viewHolder.view.findViewById(R.id.settings_image);
            imageView.setImageDrawable(ContextCompat.getDrawable(context, settingsItem.imageResId));
            imageView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        viewHolder.view.animate().cancel();
        viewHolder.view.setScaleX(1f);
        viewHolder.view.setScaleY(1f);
    }

    protected boolean isCardTextAutoScrollEnabled(Context context) {
        return MainUIData.instance(context).isCardTextAutoScrollEnabled();
    }

    protected float getCardTextScrollSpeed(Context context) {
        return MainUIData.instance(context).getCardTextScrollSpeed();
    }
}
