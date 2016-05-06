/*
 * TIANYU: yuxin add for New Design Gallery
 */

package com.android.gallery3d.ui;

import android.content.Context;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.app.TyAddToAlbumListAdapter;
import com.android.gallery3d.app.TyAddToAlbumSetListDataLoader;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.TyAddToAlbumSetListSlidingWindow.AlbumSetEntry;
import com.android.gallery3d.util.GalleryUtils;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.AbsListView;

public class TyAddToAlbumSetListRenderer {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/TyAddToAlbumSetListRenderer";
    private static final int CACHE_SIZE = 96;

    private final GalleryContext mActivity;
    private final SelectionManager mSelectionManager;

    protected TyAddToAlbumSetListSlidingWindow mDataWindow;
    private ListView mListView;

    private int mPressedIndex = -1;
    private boolean mAnimatePressedUp;
    private Path mHighlightItemPath = null;
    private boolean mInSelectionMode;

    private boolean mScollingFinished = true;

    int mStart = 0;
    int mEnd = 0;

    public TyAddToAlbumSetListRenderer(GalleryContext activity,
            SelectionManager selectionManager, ListView listView) {
        mActivity = activity;
        mSelectionManager = selectionManager;
        mListView = listView;
        mListView.setOnScrollListener(new ScrollListener());
    }

    public void setPressedIndex(int index) {
        if (mPressedIndex == index) return;
        mPressedIndex = index;
        Adapter adapter = mListView.getAdapter();
        if (adapter instanceof TyAddToAlbumListAdapter){
            ((TyAddToAlbumListAdapter)adapter).notifyDataSetChanged();
        }
    }

    public void setPressedUp() {
        if (mPressedIndex == -1) return;
        mAnimatePressedUp = true;
        Adapter adapter = mListView.getAdapter();
        if (adapter instanceof TyAddToAlbumListAdapter){
            ((TyAddToAlbumListAdapter)adapter).notifyDataSetChanged();
        }
    }

    public void setHighlightItemPath(Path path) {
        if (mHighlightItemPath == path) return;
        mHighlightItemPath = path;
        Adapter adapter = mListView.getAdapter();
        if (adapter instanceof TyAddToAlbumListAdapter){
            ((TyAddToAlbumListAdapter)adapter).notifyDataSetChanged();
        }
    }
    public void setModel(TyAddToAlbumSetListDataLoader model) {
        if (mDataWindow != null) {
            mDataWindow.setListener(null);
            mDataWindow = null;
            Adapter adapter = mListView.getAdapter();
            if (adapter instanceof TyAddToAlbumListAdapter){
                ((TyAddToAlbumListAdapter)adapter).setDataWindow(null);
                visibleRangeChanged(0, 0);
                ((TyAddToAlbumListAdapter)adapter).notifyDataSetChanged();
            }
        }
        if (model != null) {
            mDataWindow = new TyAddToAlbumSetListSlidingWindow(
                    mActivity, model, CACHE_SIZE);          
            mDataWindow.setListener(new MyCacheListener());
            Adapter adapter = mListView.getAdapter();
            if (adapter instanceof TyAddToAlbumListAdapter){
                ((TyAddToAlbumListAdapter)adapter).setDataWindow(mDataWindow);
                int start = Math.max(0, mListView.getFirstVisiblePosition());
                int end = Math.max(0, mListView.getLastVisiblePosition());
                visibleRangeChanged(start, end);
                ((TyAddToAlbumListAdapter)adapter).notifyDataSetChanged();
            }
        }
    }
    
    private class MyCacheListener implements TyAddToAlbumSetListSlidingWindow.Listener {

        @Override
        public void onSizeChanged(int size) {
            Adapter adapter = mListView.getAdapter();
            if (adapter instanceof TyAddToAlbumListAdapter){
                ((TyAddToAlbumListAdapter)adapter).setDataCount(size);
                ((TyAddToAlbumListAdapter)adapter).notifyDataSetChanged();
            }
        }

        @Override
        public void onContentChanged() {
            Adapter adapter = mListView.getAdapter();
            if (adapter instanceof TyAddToAlbumListAdapter){
                ((TyAddToAlbumListAdapter)adapter).notifyDataSetChanged();
            }
        }
    }

    public void pause() {
        mDataWindow.pause();
        Adapter adapter = mListView.getAdapter();
        if (adapter instanceof TyAddToAlbumListAdapter){
            ((TyAddToAlbumListAdapter)adapter).notifyDataSetChanged();
        }
    }

    public void resume() {
        mDataWindow.resume();
    }

    private void visibleRangeChanged(int visibleStart, int visibleEnd) {
        if (mDataWindow != null) {
            mDataWindow.setActiveWindow(visibleStart, visibleEnd);
        }
    }

    private final class ScrollListener implements ListView.OnScrollListener {
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
                if (mDataWindow != null) {
                    int start = Math.max(0, firstVisibleItem);
                    int end = start + visibleItemCount;
                    if (mStart != start || mEnd != end){
                        mStart = start;
                        mEnd = end;
                        visibleRangeChanged(start, end);
                    }
                }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == ListView.OnScrollListener.SCROLL_STATE_IDLE){
                mScollingFinished = true;
            }else{
                mScollingFinished = false;
            }
        }
    }
}
