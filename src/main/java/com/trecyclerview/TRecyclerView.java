package com.trecyclerview;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.trecyclerview.footview.FootViewHolder;
import com.trecyclerview.listener.OnNetWorkListener;
import com.trecyclerview.listener.OnScrollStateListener;
import com.trecyclerview.listener.OnRefreshListener;
import com.trecyclerview.listener.OnTScrollListener;
import com.trecyclerview.adapter.DelegateAdapter;
import com.trecyclerview.pojo.FootVo;
import com.trecyclerview.pojo.HeaderVo;
import com.trecyclerview.footview.AbsFootView;
import com.trecyclerview.headview.AbsHeaderView;
import com.trecyclerview.headview.ArrowRefreshHeader;
import com.trecyclerview.adapter.ViewTypes;

import java.util.List;

import static com.trecyclerview.footview.LoadingMoreFooter.STATE_NO_NET_WORK;
import static com.trecyclerview.util.Preconditions.checkNotNull;
import static com.trecyclerview.footview.LoadingMoreFooter.STATE_LOADING;
import static com.trecyclerview.footview.LoadingMoreFooter.STATE_NOMORE;


/**
 * @author：tqzhang on 18/6/22 16:03
 */
public class TRecyclerView extends RecyclerView {

    private DelegateAdapter mDelegateAdapter;
    /**
     * 是否开启加载更多
     */
    private boolean loadingMoreEnabled = false;

    /**
     * 是否开启刷新
     */
    private boolean pullRefreshEnabled = false;

    /**
     * 加载更多
     */
    protected boolean isShowLoadMore = false;

    /**
     * 加载更多中
     */
    protected boolean isLoading = false;

    /**
     * 刷新中
     */
    private boolean mRefreshing = false;

    /**
     * true 没有更多
     */
    private boolean isNoMore = false;


    private boolean isLoadingMore = false;

    /**
     * 最后一个可见的item的位置
     */
    private int lastVisibleItemPosition;

    private float mLastY = -1;

    private boolean isBottom;


    private OnRefreshListener mOnRefreshListener;

    private OnTScrollListener mOnScrollListener;

    private OnScrollStateListener mOnScrollStateListener;

    private OnNetWorkListener mOnNetWorkListener;

    private static final float DRAG_RATE = 2.0f;

    private ArrowRefreshHeader mRefreshHeader = null;

    private ViewTypes mTypePool;

    private boolean hsNetWork = true;

    public TRecyclerView(Context context) {
        this(context, null);
    }

    public TRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * refresh complete
     *
     * @param list
     * @param noMore 是否有更多
     */
    public void refreshComplete(List<Object> list, boolean noMore) {
        if (mRefreshHeader != null) {
            mRefreshHeader.refreshComplete();
        }
        if (pullRefreshEnabled) {
            list.add(0, new HeaderVo());
            if (loadingMoreEnabled) {
                if (noMore) {
                    list.add(new FootVo(STATE_NOMORE));
                } else {
                    list.add(new FootVo(STATE_LOADING));
                }
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

    /**
     * 加载更多完成
     *
     * @param list
     * @param noMore 是否有更多
     */
    public void loadMoreComplete(List<?> list, boolean noMore) {
        if (null == list) {
            //没有更多
            mDelegateAdapter.getItems().remove(mDelegateAdapter.getItems().size() - 1);
            ((List) mDelegateAdapter.getItems()).add(new FootVo(STATE_NOMORE));
            mDelegateAdapter.notifyItemRangeChanged(mDelegateAdapter.getItems().size() - 1, mDelegateAdapter.getItems().size());

        } else {
            mDelegateAdapter.getItems().remove(mDelegateAdapter.getItems().size() - 1 - list.size());
            if (noMore) {
                ((List) mDelegateAdapter.getItems()).add(new FootVo(STATE_NOMORE));
            } else {
                ((List) mDelegateAdapter.getItems()).add(new FootVo(STATE_LOADING));
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

    /**
     * set adapter
     */
    @Override
    public void setAdapter(Adapter adapter) {
        checkNotNull(adapter);
        this.mDelegateAdapter = (DelegateAdapter) adapter;
        super.setAdapter(adapter);
        mTypePool = mDelegateAdapter.getTypes();
        for (int i = 0; i < mTypePool.size(); i++) {
            if (mTypePool.getItemView(i) instanceof AbsFootView) {
                setLoadingMoreEnabled(true);
            } else if (mTypePool.getItemView(i) instanceof AbsHeaderView) {
                AbsHeaderView mHeaderItemView = (AbsHeaderView) mTypePool.getItemView(i);
                mRefreshHeader = mHeaderItemView.getRefreshHeaderView();
                pullRefreshEnabled = true;
            }
        }
    }

    public void setLoadingMoreEnabled(boolean enabled) {
        loadingMoreEnabled = enabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mLastY == -1) {
            mLastY = ev.getRawY();
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float deltaY = (ev.getRawY() - mLastY) / DRAG_RATE;
                mLastY = ev.getRawY();
                if (isOnTop() && pullRefreshEnabled && !mRefreshing) {
                    mRefreshHeader.onMove(deltaY);
                    if (mRefreshHeader.getVisibleHeight() > 0) {
                        return false;
                    }
                }
                break;
            default:
                mLastY = -1;
                if (isOnTop() && pullRefreshEnabled && !mRefreshing) {
                    if (mRefreshHeader.releaseAction()) {
                        if (mOnRefreshListener != null) {
                            //刷新开始
                            mRefreshing = true;
                            mOnRefreshListener.onRefresh();
                        }
                    }
                }
                break;
        }
        return super.onTouchEvent(ev);
    }


    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrolled(dx, dy);
        }
        int mAdapterCount = mDelegateAdapter.getItemCount();
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
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
        if (mOnRefreshListener != null && loadingMoreEnabled && !mRefreshing && isBottom && !isLoading && !isNoMore) {
            isShowLoadMore = true;
        }

    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (isShowLoadMore && state == RecyclerView.SCROLL_STATE_IDLE && isBottom) {
            if (mOnRefreshListener != null) {
                if (mOnNetWorkListener != null) {
                    hsNetWork = mOnNetWorkListener.onNetWork();
                    if (!hsNetWork) {
                        refreshFootView(STATE_NO_NET_WORK);
                    } else {
                        //加载更多种
                        isLoading = true;
                        mOnRefreshListener.onLoadMore();
                    }
                }else {
                    //加载更多种
                    isLoading = true;
                    mOnRefreshListener.onLoadMore();
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

    private boolean isOnTop() {
        return mRefreshHeader != null && mRefreshHeader.getParent() != null;
    }


    /**
     * RecyclerView type
     */
    protected LayoutManagerType layoutManagerType;

    public enum LayoutManagerType {
        LinearLayout,
        StaggeredGridLayout,
        GridLayout
    }

    public void addOnScrollStateListener(OnScrollStateListener listener) {
        mOnScrollStateListener = listener;
    }

    public void addOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    public void addOnTScrollListener(OnTScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }

    public void setOnNetWorkListener(final OnNetWorkListener onNetWorkListener) {
        mOnNetWorkListener = onNetWorkListener;
    }

}
