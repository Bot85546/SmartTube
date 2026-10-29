package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;

import com.liskovsoft.smartyoutubetv2.tv.ui.search.tags.SearchTagsFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.PiTubeCrashReporter;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;

public class BrowseActivity extends LeanbackActivity {
    private static final String TAG = BrowseActivity.class.getSimpleName();
    private static final long SEARCH_REVEAL_MS = 240;
    private FrameLayout mInlineSearchContainer;
    private SearchTagsFragment mInlineSearch;
    private View mBrowseFocusBeforeSearch;
    private boolean mInlineSearchVisible;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            boolean safeMode = PiTubeCrashReporter.isSafeMode(this);
            PiTubeCrashReporter.note(safeMode ? "BrowseActivity: safe layout" : "BrowseActivity: main layout");
            setContentView(safeMode ? R.layout.fragment_main_safe : R.layout.fragment_main);
            mInlineSearchContainer = findViewById(R.id.pitube_inline_search_container);
            PiTubeCrashReporter.note("BrowseActivity: layout inflated");
        } catch (NoClassDefFoundError e) {
            // Failed resolution of: Landroidx/lifecycle/ViewTreeLifecycleOwner;
            MessageHelpers.showMessage(this, e.getMessage());
        }
    }

    /** Show real search content in this activity, without opening another task. */
    public void openInlineSearch() {
        if (mInlineSearchContainer == null) {
            SearchPresenter.instance(this).startSearch(null);
            return;
        }
        if (mInlineSearchVisible) {
            return;
        }

        mInlineSearchVisible = true;
        mBrowseFocusBeforeSearch = getCurrentFocus();
        mInlineSearch = new SearchTagsFragment();
        mInlineSearchContainer.animate().cancel();
        mInlineSearchContainer.setAlpha(0f);
        mInlineSearchContainer.setTranslationY(dp(18));
        mInlineSearchContainer.setVisibility(View.VISIBLE);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.pitube_inline_search_container, mInlineSearch, "pitube_inline_search")
                .runOnCommit(() -> {
                    if (!mInlineSearchVisible || mInlineSearchContainer == null) {
                        return;
                    }
                    mInlineSearchContainer.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(SEARCH_REVEAL_MS)
                            .setInterpolator(new DecelerateInterpolator())
                            .withEndAction(() -> {
                                if (mInlineSearchVisible) {
                                    setBrowseVisibility(View.INVISIBLE);
                                }
                            })
                            .start();
                })
                .commitAllowingStateLoss();
    }

    /** Close the search panel and return to the same selected Browse item. */
    public void closeInlineSearch() {
        if (!mInlineSearchVisible || mInlineSearchContainer == null) {
            return;
        }

        mInlineSearchVisible = false;
        if (mInlineSearch != null) {
            mInlineSearch.onFinish();
        }
        setBrowseVisibility(View.VISIBLE);
        mInlineSearchContainer.animate().cancel();
        mInlineSearchContainer.animate()
                .alpha(0f)
                .translationY(dp(15))
                .setDuration(190)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    if (mInlineSearch != null) {
                        getSupportFragmentManager().beginTransaction()
                                .remove(mInlineSearch)
                                .commitAllowingStateLoss();
                        mInlineSearch = null;
                    }
                    mInlineSearchContainer.setVisibility(View.GONE);
                    if (mBrowseFocusBeforeSearch != null
                            && mBrowseFocusBeforeSearch.isAttachedToWindow()) {
                        mBrowseFocusBeforeSearch.requestFocus();
                    }
                    mBrowseFocusBeforeSearch = null;
                })
                .start();
    }

    private void setBrowseVisibility(int visibility) {
        Fragment browse = getSupportFragmentManager().findFragmentById(R.id.main_frame);
        if (browse != null && browse.getView() != null) {
            browse.getView().setVisibility(visibility);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    public boolean onSearchRequested() {
        openInlineSearch();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mInlineSearchVisible) {
            closeInlineSearch();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void initTheme() {
        int browseThemeResId = MainUIData.instance(this).getColorScheme().browseThemeResId;
        if (browseThemeResId > 0) {
            setTheme(browseThemeResId);
        }
    }
}
