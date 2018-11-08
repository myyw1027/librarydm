package com.trecyclerview.headview;

/**
 * @author tqzhang
 */
public interface BaseRefreshHeader {


	/**
	 * 下拉
	 */
	int STATE_NORMAL = 0;
	/**
	 * 松开手刷新
	 */
	int STATE_RELEASE_TO_REFRESH = 1;
	/**
	 * 刷新中
	 */
	int STATE_REFRESHING = 2;
	/**
	 * 刷新完成
	 */
	int STATE_DONE = 3;

	void onMove(float delta);

	boolean releaseAction();

	void refreshComplete();

}