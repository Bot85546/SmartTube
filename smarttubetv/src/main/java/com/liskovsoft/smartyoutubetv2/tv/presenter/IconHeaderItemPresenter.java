package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.os.Build;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowHeaderPresenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.PiTubeCrashReporter;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.headers.PiTubeGlassFocusDrawable;

public class IconHeaderItemPresenter extends RowHeaderPresenter {
    private static final String TAG = IconHeaderItemPresenter.class.getSimpleName();
    private float mUnselectedAlpha;
    private final int mResId;
    private final String mIconUrl;
    private Drawable mDefaultIcon;

    private static final class GlassHeaderViewHolder extends ViewHolder {
        final PiTubeGlassFocusDrawable focusGlass;

        GlassHeaderViewHolder(View view, PiTubeGlassFocusDrawable focusGlass) {
            super(view);
            this.focusGlass = focusGlass;
        }
    }

    public IconHeaderItemPresenter(int resId, String iconUrl) {
        mResId = resId;
        mIconUrl = iconUrl;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        mUnselectedAlpha = 0.72f;
        LayoutInflater inflater = (LayoutInflater) viewGroup.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDefaultIcon = new ColorDrawable(ContextCompat.getColor(viewGroup.getContext(), R.color.lb_grey));

        View view = inflater.inflate(R.layout.icon_header_item, viewGroup, false);
        view.setAlpha(mUnselectedAlpha); // Initialize icons to be at half-opacity.
        // The focus surface is the row's background, not a floating view
        // behind the opaque headers grid. Text and icons draw over the glass.
        PiTubeGlassFocusDrawable glass = null;
        if (!PiTubeCrashReporter.isSafeMode(view.getContext())) {
            glass = new PiTubeGlassFocusDrawable(
                    view.getResources().getDisplayMetrics().density);
            glass.setAlpha(0);
            view.setBackground(glass);

            final PiTubeGlassFocusDrawable focusGlass = glass;
            view.setOnFocusChangeListener((v, focused) -> {
                focusGlass.setActive(focused);
                focusGlass.setAlpha(focused ? 255 : (v.isSelected() ? 220 : 0));
                v.animate().cancel();
                // A subtle scale response makes D-pad movement feel immediate
                // without moving the menu row or clipping its label.
                v.animate().scaleX(focused ? 1.025f : 1f)
                        .scaleY(focused ? 1.025f : 1f)
                        .setDuration(155).start();
            });
        }

        return new GlassHeaderViewHolder(view, glass);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        HeaderItem headerItem;

        if (item instanceof PageRow) {
            headerItem = ((PageRow) item).getHeaderItem();
        } else {
            headerItem = ((ListRow) item).getHeaderItem();
        }

        View rootView = viewHolder.view;
        rootView.setFocusable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !PiTubeCrashReporter.isSafeMode(rootView.getContext())) {
            rootView.setDefaultFocusHighlightEnabled(false);
        }

        ImageView iconView = rootView.findViewById(R.id.header_icon);
        if (iconView != null) {
            if (mIconUrl != null) {
                Glide.with(rootView.getContext())
                        .load(mIconUrl)
                        .apply(ViewUtil.glideOptions().error(mDefaultIcon))
                        .listener(mErrorListener)
                        .into(iconView);

                //ViewUtil.makeMonochrome(iconView);
            } else {
                Drawable icon = mResId > 0 ? ContextCompat.getDrawable(rootView.getContext(), mResId) : mDefaultIcon;
                iconView.setImageDrawable(icon);
            }
        }

        TextView label = rootView.findViewById(R.id.header_label);
        if (label != null) {
            label.setText(headerItem.getName());
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        GlassHeaderViewHolder holder = (GlassHeaderViewHolder) viewHolder;
        holder.view.animate().cancel();
        holder.view.setScaleX(1f);
        holder.view.setScaleY(1f);
        if (holder.focusGlass != null) {
            holder.focusGlass.setActive(false);
            holder.focusGlass.setAlpha(0);
        }
    }

    @Override
    public void onViewDetachedFromWindow(Presenter.ViewHolder viewHolder) {
        GlassHeaderViewHolder holder = (GlassHeaderViewHolder) viewHolder;
        holder.view.animate().cancel();
        if (holder.focusGlass != null) {
            holder.focusGlass.setActive(false);
        }
        super.onViewDetachedFromWindow(viewHolder);
    }

    // TODO: This is a temporary fix. Remove me when leanback onCreateViewHolder no longer sets the
    // mUnselectAlpha, and also assumes the xml inflation will return a RowHeaderView.
    @Override
    protected void onSelectLevelChanged(RowHeaderPresenter.ViewHolder holder) {
        float progress = holder.getSelectLevel();
        holder.view.setAlpha(mUnselectedAlpha + progress *
                (1.0f - mUnselectedAlpha));
        holder.view.setSelected(progress > 0.5f);
        if (holder instanceof GlassHeaderViewHolder) {
            GlassHeaderViewHolder glassHolder = (GlassHeaderViewHolder) holder;
            if (glassHolder.focusGlass != null) {
                boolean focused = holder.view.hasFocus();
                glassHolder.focusGlass.setAlpha(focused ? 255 : Math.round(220f * progress));
                glassHolder.focusGlass.setActive(focused);
            }
        }
    }

    private final RequestListener<Drawable> mErrorListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };
}
