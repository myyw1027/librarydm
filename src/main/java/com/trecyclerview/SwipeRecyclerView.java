package com.trecyclerview;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;

import com.trecyclerview.listener.OnLoadMoreListener;
import com.trecyclerview.listener.OnNetWorkListener;
import com.trecyclerview.listener.OnScrollStateListener;
import com.trecyclerview.listener.OnRefreshListener;
import com.trecyclerview.listener.OnTScrollListener;
import com.trecyclerview.adapter.DelegateAdapter;
import com.trecyclerview.pojo.FootVo;
import com.trecyclerview.footview.AbsFootView;
import com.trecyclerview.adapter.ViewTypes;

import java.util.List;

import static com.trecyclerview.footview.LoadingMoreFooter.STATE_LOADING;
import static com.trecyclerview.footview.LoadingMoreFooter.STATE_NOMORE;
import static com.trecyclerview.footview.LoadingMoreFooter.STATE_NO_NET_WORK;

/**
 * @author：tqzhang on 18/6/22 16:03
 */
public class SwipeRecyclerView extends RecyclerView {

    private DelegateAdapter mDelegateAdapter;

    private boolean loadingMoreEnabled = false;

    private boolean mRefreshing = false;

    private boolean isNoMore = false; //true 没有更多

    private boolean isBottom;

    private int lastVisibleItemPosition;


    protected boolean isLoading = false;

    /**
     * 加载更多
     */
    protected boolean isShowLoadMore = false;

    private boolean hsNetWork = true;

    private OnRefreshListener mOnRefreshListener;

    private OnTScrollListener mOnScrollListener;

    private OnScrollStateListener mOnScrollStateListener;

    private OnLoadMoreListener mOnLoadMoreListener;

    private OnNetWorkListener mOnNetWorkListener;

    private State appbarState = State.EXPANDED;


    public SwipeRecyclerView(Context context) {
        this(context, null);
    }

    public SwipeRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @param list
     * @param noMore 是否有更多
     */
    public void refreshComplete(List<Object> list, boolean noMore) {
        if (null == list || list.size() == 0) {
            return;
        }
        if (loadingMoreEnabled) {
            if (noMore) {
                list.add(new FootVo(STATE_NOMORE));
            } else {
                list.add(new FootVo(STATE_LOADING));
            }
        }
        mDelegateAdapter.setDatas(list);
        mDelegateAdapter.notifyDataSetChanged();
        //刷新完成
        mRefreshing = false;
        isNoMore = noMore;
    }


    /**
     *
     */
    public void loadMoreComplete() {
        loadMoreComplete(null, true);
    }

    public void loadMoreComplete(List<?> list, boolean noMore) {
        if (null == list) {
            mDelegateAdapter.getItems().remove(mDelegateAdapter.getItems().size() - 1);
            ((List) mDelegateAdapter.getItems()).add(new FootVo(STATE_NOMORE));
            mDelegateAdapter.notifyItemRangeChanged(mDelegateAdapter.getItems().size() - 1, mDelegateAdapter.getItems().size());
        } else {
            mDelegateAdapter.getItems().remove(mDelegateAdapter.getItems().size() - 1 - list.size());
            if (!isNoMore) {
                ((List) mDelegateAdapter.getItems()).add(new FootVo(STATE_LOADING));
            } else {
                ((List) mDelegateAdapter.getItems()).add(new FootVo(STATE_NOMORE));
            }
            mDelegateAdapter.notifyItemRangeChanged(mDelegateAdapter.getItems().size() - list.size() - 1, mDelegateAdapter.getItems().size());
        }
        isNoMore = noMore;
        isLoading = false;
        isShowLoadMore = false;

    }

    public void notifyItemRangeChanged(int positionStart, int itemCount) {
        mDelegateAdapter.notifyItemRangeChanged(positionStart, itemCount);

    }

    public void notifyItemChanged(int position) {
        mDelegateAdapter.notifyItemChanged(position);
    }


    public void setRefreshing(boolean mRefreshing) {
        this.mRefreshing = mRefreshing;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        this.mDelegateAdapter = (DelegateAdapter) adapter;
        super.setAdapter(adapter);
        ViewTypes mTypePool = mDelegateAdapter.getTypes();
        for (int i = 0; i < mTypePool.size(); i++) {
            if (mTypePool.getItemView(i) instanceof AbsFootView) {
                setLoadingMoreEnabled(true);
            }
        }
    }

    public void setLoadingMoreEnabled(boolean enabled) {
        loadingMoreEnabled = enabled;
    }

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrolled(dx, dy);
        }
        int mAdapterCount = mDelegateAdapter.getItemCount();
        LayoutManager layoutManager = getLayoutManager();

        if (layoutManagerType == null) {
            if (layoutManager instanceof LinearLayoutManager) {
                layoutManagerType = LayoutManagerType.LinearLayout;
            } else if (layoutManager instanceof GridLayoutManager) {
                layoutManagerType = LayoutManagerType.GridLayout;
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                layoutManagerType = LayoutManagerType.StaggeredGridLayout;
            } else {
                throw new RuntimeException(
                        "Unsupported LayoutManager used. Valid ones are LinearLayoutManager, GridLayoutManager and StaggeredGridLayoutManager");
            }
        }
        switch (layoutManagerType) {
            case LinearLayout:
                lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition() + 1;
                break;
            case GridLayout:
                lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
                break;
            case StaggeredGridLayout:
                int[] into = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
                ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(into);
                lastVisibleItemPosition = findMax(into) + 1;
                break;
            default:
                break;
        }
        isBottom = mAdapterCount == lastVisibleItemPosition;
        if (mOnLoadMoreListener != null && loadingMoreEnabled && !mRefreshing && isBottom && !isLoading && !isNoMore) {
            isShowLoadMore = true;
        }
        Log.e("onScrollStateChanged","mRefreshing:"+mRefreshing+"-----"+"isLoading:"+isLoading+"-----");
    }


    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (isShowLoadMore && state == RecyclerView.SCROLL_STATE_IDLE && isBottom) {
            if (mOnLoadMoreListener != null) {
                if (mOnNetWorkListener != null) {
                    hsNetWork = mOnNetWorkListener.onNetWork();
                    if (!hsNetWork) {
                        refreshFootView(STATE_NO_NET_WORK);
                    } else {
                        //加载更多种
                        isLoading = true;
                        mOnLoadMoreListener.onLoadMore();
                    }
                }else {
                    //加载更多种
                    isLoading = true;
                    mOnLoadMoreListener.onLoadMore();
                }

            }
        }

        if (mOnScrollStateListener != null) {
            mOnScrollStateListener.onScrollStateChanged(state);
        }
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(state);
        }
    }


    public void refreshFootView(int state) {
        if (mDelegateAdapter == null || mDelegateAdapter.getItems() == null) {
            return;
        }
        if (mDelegateAdapter.getItems().get(mDelegateAdapter.getItems().size() - 1) instanceof FootVo) {
            ((FootVo) mDelegateAdapter.getItems().get(mDelegateAdapter.getItems().size() - 1)).state = state;
            notifyItemRangeChanged(mDelegateAdapter.getItems().size() - 1, mDelegateAdapter.getItems().size());
        }
    }


    private int findMax(int[] lastPositions) {
        int max = lastPositions[0];
        for (int value : lastPositions) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    protected LayoutManagerType layoutManagerType;

    public enum LayoutManagerType {
        LinearLayout,
        StaggeredGridLayout,
        GridLayout
    }

    public void addOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;

    }

    public void addOnTScrollListener(OnTScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }

    public void addOnLoadImageListener(OnScrollStateListener listener) {
        mOnScrollStateListener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        AppBarLayout appBarLayout = null;
        ViewParent p = getParent();
        while (p != null) {
            if (p instanceof CoordinatorLayout) {
                break;
            }
            p = p.getParent();
        }
        if (p instanceof CoordinatorLayout) {
            CoordinatorLayout coordinatorLayout = (CoordinatorLayout) p;
            final int childCount = coordinatorLayout.getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                final View child = coordinatorLayout.getChildAt(i);
                if (child instanceof AppBarLayout) {
                    appBarLayout = (AppBarLayout) child;
                    break;
                }
            }
            if (appBarLayout != null) {
                appBarLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {
                    @Override
                    public void onStateChanged(AppBarLayout appBarLayout, State state) {
                        appbarState = state;
                    }
                });
            }
        }
    }

    @Override
    public boolean canScrollVertically(int direction) {
        final int offset = computeVerticalScrollOffset();
        final int range = computeVerticalScrollRange() - computeVerticalScrollExtent();
        if (range == 0) {
            return false;
        }
        if (direction < 0) {
            return offset > 0;
        } else {
            return offset < range - 1;
        }
    }

    public enum State {
        EXPANDED,
        COLLAPSED,
        IDLE
    }

    public abstract class AppBarStateChangeListener implements AppBarLayout.OnOffsetChangedListener {

        private State mCurrentState = State.IDLE;

        @Override
        public final void onOffsetChanged(AppBarLayout appBarLayout, int i) {
            if (i == 0) {
                if (mCurrentState != State.EXPANDED) {
                    if (mAppBarStateListener != null) {
                        mAppBarStateListener.onChanged(appBarLayout, State.EXPANDED);
                    }
                }
                mCurrentState = State.EXPANDED;
            } else if (Math.abs(i) >= appBarLayout.getTotalScrollRange()) {
                if (mCurrentState != State.COLLAPSED) {
                    if (mAppBarStateListener != null) {
                        mAppBarStateListener.onChanged(appBarLayout, State.COLLAPSED);
                    }
                }
                mCurrentState = State.COLLAPSED;
            } else {
                if (mCurrentState != State.IDLE) {
                    if (mAppBarStateListener != null) {
                        mAppBarStateListener.onChanged(appBarLayout, State.IDLE);
                    }

                }
                mCurrentState = State.IDLE;
            }
        }

        public abstract void onStateChanged(AppBarLayout appBarLayout, State state);
    }

    public AppBarStateListener mAppBarStateListener;

    public void setAppBarStateListener(AppBarStateListener Listener) {
        mAppBarStateListener = Listener;
    }

    public interface AppBarStateListener {
        /**
         * AppBarStateListener
         *
         * @param appBarLayout AppBarLayout
         * @param state        State
         */
        void onChanged(AppBarLayout appBarLayout, State state);
    }

    public void setOnNetWorkListener(final OnNetWorkListener onNetWorkListener) {
        mOnNetWorkListener = onNetWorkListener;
    }
}