package com.liskovsoft.smartyoutubetv2.tv.ui.search.tags;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;

public class SearchTagsActivity extends LeanbackActivity {
    private SearchTagsFragment mFragment;
    private boolean mDownPressed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_search_tags);
        mFragment = (SearchTagsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.search_tags_fragment);

        // A small compositor-only reveal makes the search sheet feel attached
        // to Browse without an expensive blur or a second rendering surface.
        View searchField = findViewById(R.id.lb_search_bar);
        if (searchField != null) {
            searchField.setAlpha(0.65f);
            searchField.setTranslationY(14f * getResources().getDisplayMetrics().density);
            searchField.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(70)
                    .setDuration(230)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If there are no results found, press the left key to reselect the microphone
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && !mFragment.hasResults()) {
            mFragment.focusOnSearchField();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void finishReally() {
        super.finishReally();

        overridePendingTransition(R.anim.pitube_search_return, R.anim.pitube_search_exit);

        mFragment.onFinish();
    }
}
