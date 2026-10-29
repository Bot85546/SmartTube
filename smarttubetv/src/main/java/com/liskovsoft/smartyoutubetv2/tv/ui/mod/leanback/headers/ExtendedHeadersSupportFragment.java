package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.headers;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.HeadersSupportFragment;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowHeaderPresenter;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.PiTubeCrashReporter;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.helpers.KeyHelpers;

public class ExtendedHeadersSupportFragment extends HeadersSupportFragment {
    public interface OnHeaderLongPressedListener {
        /**
         * Called when a header item has been long pressed.
         *
         * @param viewHolder Row ViewHolder object corresponding to the selected Header.
         * @param row Row object corresponding to the selected Header.
         */
        void onHeaderLongPressed(RowHeaderPresenter.ViewHolder viewHolder, Row row);
    }

    private OnHeaderLongPressedListener mOnHeaderLongPressedListener;
    private ViewGroup mFocusCapsuleHost;
    private View mFocusCapsule;
    private PiTubeGlassFocusDrawable mFocusGlass;
    private View mCurrentHeader;
    private VerticalGridView mHeadersGrid;
    private ViewTreeObserver.OnGlobalFocusChangeListener mHeaderFocusListener;
    private RecyclerView.OnScrollListener mHeaderScrollListener;
    private ViewTreeObserver mFocusObserver;
    private OnLayoutChangeListener mHeaderLayoutListener;
    // Reused coordinates avoid allocations during remote-navigation animations.
    private final int[] mHeaderWindowLocation = new int[2];
    private final int[] mHostWindowLocation = new int[2];
    private final OvershootInterpolator mGlassMotion = new OvershootInterpolator(0.55f);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle state) {
        super.onViewCreated(view, state);

        if (PiTubeCrashReporter.isSafeMode(view.getContext())) {
            PiTubeCrashReporter.note("Headers: safe mode; focus capsule disabled");
            return;
        }
        PiTubeCrashReporter.note("Headers: creating focus capsule");

        mHeadersGrid = getVerticalGridView();
        mFocusCapsuleHost = view.findViewById(R.id.browse_headers_root);
        if (mHeadersGrid == null || mFocusCapsuleHost == null) {
            return;
        }

        mFocusCapsuleHost.setClipChildren(false);
        mFocusCapsuleHost.setClipToPadding(false);
        mFocusCapsule = new View(view.getContext());
        mFocusGlass = new PiTubeGlassFocusDrawable(
                getResources().getDisplayMetrics().density);
        mFocusCapsule.setBackground(mFocusGlass);
        mFocusCapsule.setFocusable(false);
        mFocusCapsule.setClickable(false);
        mFocusCapsule.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mFocusCapsule.setAlpha(0.84f);
        mFocusCapsule.setVisibility(View.INVISIBLE);
        // Place the glass BELOW the transparent grid. Drawing it on top caused
        // the pill's coloured fill to wash out menu icon and label contrast.
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dp(190), dp(48));
        mFocusCapsuleHost.addView(mFocusCapsule, 0, params);

        mHeaderFocusListener = (oldFocus, newFocus) -> {
            if (newFocus != null && isInside(newFocus, mHeadersGrid)) {
                mCurrentHeader = directGridChild(newFocus);
                updateCapsule(true);
            }
        };
        mHeaderScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateSelectedHeader();
                updateCapsule(false);
            }
        };
        mHeadersGrid.addOnScrollListener(mHeaderScrollListener);
        mHeaderLayoutListener = (changed, left, top, right, bottom,
                                 oldLeft, oldTop, oldRight, oldBottom) -> {
            updateSelectedHeader();
            updateCapsule(false);
        };
        mHeadersGrid.addOnLayoutChangeListener(mHeaderLayoutListener);
        mFocusCapsuleHost.post(() -> {
            if (mHeadersGrid == null || mFocusCapsuleHost == null) return;
            // Register only after attachment; a fragment's observer can change.
            mFocusObserver = mFocusCapsuleHost.getViewTreeObserver();
            if (mFocusObserver.isAlive()) {
                mFocusObserver.addOnGlobalFocusChangeListener(mHeaderFocusListener);
            }
            View focused = mHeadersGrid.findFocus();
            if (focused != null && isInside(focused, mHeadersGrid)) {
                mCurrentHeader = directGridChild(focused);
            }
            updateSelectedHeader();
            updateCapsule(false);
        });
        PiTubeCrashReporter.note("Headers: focus capsule ready");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static boolean isInside(View view, ViewGroup parent) {
        if (view == parent) return true;
        ViewParent ancestor = view.getParent();
        while (ancestor != null) {
            if (ancestor == parent) return true;
            ancestor = ancestor.getParent();
        }
        return false;
    }

    private View directGridChild(View child) {
        // findFocus() may return the grid itself. Never walk past that grid:
        // its parent is the sidebar root, not a focused menu item.
        View item = child;
        while (item != null && item != mHeadersGrid) {
            ViewParent parent = item.getParent();
            if (parent == mHeadersGrid) {
                return item;
            }
            item = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    private void updateSelectedHeader() {
        if (mHeadersGrid == null || (mCurrentHeader != null
                && mCurrentHeader.isAttachedToWindow()
                && isInside(mCurrentHeader, mHeadersGrid))) {
            return;
        }
        RecyclerView.ViewHolder holder = mHeadersGrid.findViewHolderForAdapterPosition(
                mHeadersGrid.getSelectedPosition());
        if (holder != null) {
            mCurrentHeader = holder.itemView;
        }
    }

    private void updateCapsule(boolean animate) {
        if (mFocusCapsule == null || mCurrentHeader == null || mHeadersGrid == null
                || mFocusCapsuleHost == null || !mCurrentHeader.isAttachedToWindow()
                || !isInside(mCurrentHeader, mHeadersGrid)
                || !isInside(mCurrentHeader, mFocusCapsuleHost)) {
            return;
        }
        // Coordinates from one window avoid ViewGroup's descendant exception
        // while Leanback moves/reparents a header during focus transitions.
        mCurrentHeader.getLocationInWindow(mHeaderWindowLocation);
        mFocusCapsuleHost.getLocationInWindow(mHostWindowLocation);
        float x = Math.min(mHeaderWindowLocation[0] - mHostWindowLocation[0] + dp(2),
                Math.max(0, mFocusCapsuleHost.getWidth() - dp(202)));
        float y = mHeaderWindowLocation[1] - mHostWindowLocation[1]
                + mCurrentHeader.getHeight() / 2f - dp(24);
        mFocusCapsule.animate().cancel();
        if (animate && mFocusCapsule.getVisibility() == View.VISIBLE) {
            // Only the focus glass animates. Labels and glyphs never translate,
            // and a restrained spring gives quick D-pad presses a fluid finish.
            mFocusCapsule.setScaleX(0.975f);
            mFocusCapsule.setScaleY(0.96f);
            mFocusCapsule.animate().x(x).y(y).scaleX(1f).scaleY(1f)
                    .setDuration(175).setInterpolator(mGlassMotion).start();
        } else {
            mFocusCapsule.setX(x);
            mFocusCapsule.setY(y);
            mFocusCapsule.setVisibility(View.VISIBLE);
        }
        if (mFocusGlass != null) {
            mFocusGlass.setActive(true);
        }
    }

    @Override
    public void onDestroyView() {
        if (mFocusGlass != null) {
            mFocusGlass.setActive(false);
            mFocusGlass = null;
        }
        if (mFocusObserver != null && mHeaderFocusListener != null
                && mFocusObserver.isAlive()) {
            mFocusObserver.removeOnGlobalFocusChangeListener(mHeaderFocusListener);
        }
        if (mHeadersGrid != null && mHeaderScrollListener != null) {
            mHeadersGrid.removeOnScrollListener(mHeaderScrollListener);
        }
        if (mHeadersGrid != null && mHeaderLayoutListener != null) {
            mHeadersGrid.removeOnLayoutChangeListener(mHeaderLayoutListener);
        }
        mFocusObserver = null;
        mHeaderLayoutListener = null;
        mFocusCapsule = null;
        mCurrentHeader = null;
        mHeadersGrid = null;
        mFocusCapsuleHost = null;
        super.onDestroyView();
    }

    public ExtendedHeadersSupportFragment() {
        Helpers.setField(this, "mAdapterListener", mCustomAdapterListener);
    }

    public void setOnHeaderLongPressedListener(OnHeaderLongPressedListener listener) {
        mOnHeaderLongPressedListener = listener;
    }

    private final ItemBridgeAdapter.AdapterListener mCustomAdapterListener =
            new ItemBridgeAdapter.AdapterListener() {
                @Override
                public void onCreate(final ItemBridgeAdapter.ViewHolder viewHolder) {
                    ItemBridgeAdapter.Wrapper wrapper = (ItemBridgeAdapter.Wrapper) Helpers.getField(ExtendedHeadersSupportFragment.this, "mWrapper");
                    OnLayoutChangeListener layoutChangeListener = (OnLayoutChangeListener) Helpers.getField(ExtendedHeadersSupportFragment.this, "sLayoutChangeListener");

                    View headerView = viewHolder.getViewHolder().view;
                    headerView.setOnClickListener(v -> {
                        OnHeaderClickedListener onHeaderClickedListener =
                                (OnHeaderClickedListener) Helpers.getField(ExtendedHeadersSupportFragment.this, "mOnHeaderClickedListener");
                        if (onHeaderClickedListener != null) {
                            onHeaderClickedListener.onHeaderClicked(
                                    (RowHeaderPresenter.ViewHolder) viewHolder.getViewHolder(),
                                    (Row) viewHolder.getItem());
                        }
                    });

                    // NEW CODE
                    headerView.setOnLongClickListener(v -> {
                        if (mOnHeaderLongPressedListener != null) {
                            mOnHeaderLongPressedListener.onHeaderLongPressed(
                                    (RowHeaderPresenter.ViewHolder) viewHolder.getViewHolder(),
                                    (Row) viewHolder.getItem());
                            return true; // don't provoke single click event
                        }

                        return false; // work as usual
                    });

                    // NEW CODE
                    headerView.setOnKeyListener((v, keyCode, event) -> {
                        if (mOnHeaderLongPressedListener != null) {
                            if (KeyHelpers.isMenuKey(keyCode)) {
                                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                    mOnHeaderLongPressedListener.onHeaderLongPressed(
                                            (RowHeaderPresenter.ViewHolder) viewHolder.getViewHolder(),
                                            (Row) viewHolder.getItem());
                                }
                            }
                        }

                        return false; // enable navigation events
                    });

                    if (wrapper != null) {
                        viewHolder.itemView.addOnLayoutChangeListener(layoutChangeListener);
                    } else {
                        headerView.addOnLayoutChangeListener(layoutChangeListener);
                    }
                }

            };
}
