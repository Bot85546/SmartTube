package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.headers;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;

import androidx.leanback.app.HeadersSupportFragment;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowHeaderPresenter;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.helpers.KeyHelpers;

/**
 * Leanback navigation with long-press support.
 *
 * Focus glass now belongs to each header row's background (in
 * IconHeaderItemPresenter). This is important: a floating view inserted below
 * the VerticalGridView was covered by its opaque drawing layer, while one
 * inserted above it obscured text and could crash during view re-parenting.
 * Row-owned focus never needs coordinates, scroll observers, or z-order hacks.
 */
public class ExtendedHeadersSupportFragment extends HeadersSupportFragment {
    public interface OnHeaderLongPressedListener {
        void onHeaderLongPressed(RowHeaderPresenter.ViewHolder viewHolder, Row row);
    }

    private OnHeaderLongPressedListener mOnHeaderLongPressedListener;

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
                    ItemBridgeAdapter.Wrapper wrapper = (ItemBridgeAdapter.Wrapper)
                            Helpers.getField(ExtendedHeadersSupportFragment.this, "mWrapper");
                    OnLayoutChangeListener layoutChangeListener = (OnLayoutChangeListener)
                            Helpers.getField(ExtendedHeadersSupportFragment.this, "sLayoutChangeListener");

                    View headerView = viewHolder.getViewHolder().view;
                    headerView.setOnClickListener(v -> {
                        OnHeaderClickedListener listener = (OnHeaderClickedListener)
                                Helpers.getField(ExtendedHeadersSupportFragment.this,
                                        "mOnHeaderClickedListener");
                        if (listener != null) {
                            listener.onHeaderClicked(
                                    (RowHeaderPresenter.ViewHolder) viewHolder.getViewHolder(),
                                    (Row) viewHolder.getItem());
                        }
                    });

                    headerView.setOnLongClickListener(v -> {
                        if (mOnHeaderLongPressedListener == null) {
                            return false;
                        }
                        mOnHeaderLongPressedListener.onHeaderLongPressed(
                                (RowHeaderPresenter.ViewHolder) viewHolder.getViewHolder(),
                                (Row) viewHolder.getItem());
                        return true;
                    });

                    headerView.setOnKeyListener((v, keyCode, event) -> {
                        if (mOnHeaderLongPressedListener != null
                                && KeyHelpers.isMenuKey(keyCode)
                                && event.getAction() == KeyEvent.ACTION_DOWN) {
                            mOnHeaderLongPressedListener.onHeaderLongPressed(
                                    (RowHeaderPresenter.ViewHolder) viewHolder.getViewHolder(),
                                    (Row) viewHolder.getItem());
                        }
                        return false;
                    });

                    if (layoutChangeListener != null) {
                        if (wrapper != null) {
                            viewHolder.itemView.addOnLayoutChangeListener(layoutChangeListener);
                        } else {
                            headerView.addOnLayoutChangeListener(layoutChangeListener);
                        }
                    }
                }
            };
}
