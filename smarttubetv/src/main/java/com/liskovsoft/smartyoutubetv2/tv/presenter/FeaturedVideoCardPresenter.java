package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.util.Pair;

import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.GridFragmentHelper;

/**
 * Larger first-shelf artwork for Home. Uses the same recycled card views and
 * thumbnail loading as normal rows; no extra image pipeline or autoplay.
 */
public final class FeaturedVideoCardPresenter extends VideoCardPresenter {
    @Override
    protected Pair<Integer, Integer> getCardDimensPx(Context context) {
        return GridFragmentHelper.getCardDimensPx(
                context,
                R.dimen.pitube_featured_card_width,
                R.dimen.pitube_featured_card_height,
                MainUIData.instance(context).getVideoGridScale());
    }
}
