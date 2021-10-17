package com.baiyu.gallerylayoutmanager;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;


import org.jetbrains.annotations.NotNull;

/**
 * 等间距画廊
 */
@SuppressWarnings("JavaDoc")
public class GalleryLayoutManager extends RecyclerView.LayoutManager implements RecyclerView.SmoothScroller.ScrollVectorProvider {

    private static final String TAG = "GalleryLayoutManager";

    RecyclerView mRecyclerView;

    private Interpolator mSmoothScrollInterpolator;

    //item间距
    private int itemSpacing = 40;

    //缩放的的View数量(应该是奇数)
    private int scaleCount = 5;
    //缩放系数
    private float scaleRatio = 0.72f;
    //item原始宽
    private int mCenterItemWidth;

    private boolean mShouldReverseLayout = false;

    private boolean mInfinite = false;

    private RecyclerView.Recycler mRecycler;

    final static int LAYOUT_START = -1;

    final static int LAYOUT_END = 1;

    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;

    public static final int VERTICAL = OrientationHelper.VERTICAL;

    private int mFirstVisiblePosition = 0;
    private int mLastVisiblePos = 0;
    private int mInitialSelectedPosition = 0;

    int mCurSelectedPosition = -1;

    View mCurSelectedView;
    /**
     * Scroll state
     */
    private State mState;

    private PagerSnapHelper mSnapHelper = new PagerSnapHelper();

    /**
     * Current orientation. Either {@link #HORIZONTAL} or {@link #VERTICAL}
     */
    private int mOrientation;

    private OrientationHelper mHorizontalHelper;
    private OrientationHelper mVerticalHelper;

    public GalleryLayoutManager(int orientation) {
        mOrientation = orientation;
    }

    public GalleryLayoutManager(int itemSpacing, int scaleCount, float scaleRatio) {
        this.itemSpacing = itemSpacing;
        this.scaleCount = scaleCount;
        this.scaleRatio = scaleRatio;
        this.mOrientation = HORIZONTAL;
    }

    public void attach(RecyclerView recyclerView, int selectedPosition) {
        if (recyclerView == null) {
            throw new IllegalArgumentException("The attach RecycleView must not null!!");
        }
        mRecyclerView = recyclerView;
        mInitialSelectedPosition = Math.max(0, selectedPosition);
        recyclerView.setLayoutManager(this);
        mSnapHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        view.addOnScrollListener(scrollCenterListener());
        super.onAttachedToWindow(view);
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        view.removeOnScrollListener(scrollCenterListener());
        super.onDetachedFromWindow(view, recycler);
    }

    private RecyclerView.OnScrollListener scrollCenterListener;

    private RecyclerView.OnScrollListener scrollCenterListener() {
        if (scrollCenterListener == null) {
            scrollCenterListener = new RecyclerView.OnScrollListener() {
                int currentPage = -1;

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        View snapView = mSnapHelper.findSnapView(GalleryLayoutManager.this);
                        int currentIndex = 0;
                        if (snapView != null) {
                            currentIndex = GalleryLayoutManager.this.getPosition(snapView);
                        }
                        if (currentPage != currentIndex) {
                            currentPage = currentIndex;
                            if (mOnItemSelectedListener != null) {
                                mOnItemSelectedListener.onItemSelected(currentIndex);
                            }
                        }
                    }
                }

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {

//                    super.onScrolled(recyclerView, dx, dy);
                }
            };
        }
        return scrollCenterListener;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        if (mOrientation == VERTICAL) {
            return new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            return new LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
    }

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    float getMaxOffset() {
        //recyclerview中心点x坐标位置
        int parentCenter = (getOrientationHelper().getEndAfterPadding() - getOrientationHelper().getStartAfterPadding()) / 2
                + getOrientationHelper().getStartAfterPadding();
        //确定recyclerview右边缘的位置
        int rightEdge = getOrientationHelper().getEndAfterPadding();
        int rightArea = rightEdge - parentCenter;
        int scaleDistance = mCenterItemWidth / 2;
        int minItemDistance = (int) (mCenterItemWidth * Math.pow(scaleRatio, (scaleCount - 1) >> 1)) + itemSpacing;
        for (int i = 0; i < (scaleCount - 1) / 2; i++) {
            scaleDistance += itemSpacing + mCenterItemWidth * Math.pow(scaleRatio, i + 1);
        }
        int multiple = 0;
        if (rightArea - scaleDistance > 0) {
            multiple = (rightArea - scaleDistance) / minItemDistance;
        }
        int screenItemCount = (scaleCount - 1) / 2 + multiple;
        int maxSelectedPosition;
        if (getItemCount() - 1 - mInitialSelectedPosition >= screenItemCount) {
            maxSelectedPosition = getItemCount() - 1 - screenItemCount;
        } else {
            if (mInitialSelectedPosition % screenItemCount == 0) {
                maxSelectedPosition = getItemCount() - 1 - screenItemCount;
            } else {
                maxSelectedPosition = getItemCount() - 1 - mInitialSelectedPosition % screenItemCount - 1;
            }
        }
        return (maxSelectedPosition - mInitialSelectedPosition) * (itemSpacing + mCenterItemWidth);
    }

    float getMinOffset() {
        //recyclerview中心点x坐标位置
        int parentCenter = (getOrientationHelper().getEndAfterPadding() - getOrientationHelper().getStartAfterPadding()) / 2
                + getOrientationHelper().getStartAfterPadding();
        //确定recyclerview左边缘的位置
        int leftEdge = getOrientationHelper().getStartAfterPadding();
        int leftArea = parentCenter - leftEdge;
        int scaleDistance = mCenterItemWidth / 2;
        int minItemDistance = (int) (mCenterItemWidth * Math.pow(scaleRatio, (scaleCount - 1) >> 1)) + itemSpacing;
        for (int i = 0; i < (scaleCount - 1) / 2; i++) {
            scaleDistance += itemSpacing + mCenterItemWidth * Math.pow(scaleRatio, i + 1);
        }
        int multiple = 0;
        if (leftArea - scaleDistance > 0) {
            multiple = (leftArea - scaleDistance) / minItemDistance;
        }
        int screenItemCount = (scaleCount - 1) / 2 + multiple;
        int minSelectedPosition;
        if (mInitialSelectedPosition > screenItemCount) {
            minSelectedPosition = screenItemCount;
        } else {
            if (mInitialSelectedPosition % screenItemCount == 0) {
                minSelectedPosition = screenItemCount;
            } else {
                minSelectedPosition = mInitialSelectedPosition % screenItemCount + 1;
            }
        }
        return (minSelectedPosition - mInitialSelectedPosition) * (itemSpacing + mCenterItemWidth);
    }

    public int getOffsetToPosition(int position) {
        View snapView = mSnapHelper.findSnapView(GalleryLayoutManager.this);
        int currentIndex = 0;
        if (snapView != null) {
            currentIndex = GalleryLayoutManager.this.getPosition(snapView);
        }
        int scale = (int) Math.pow(scaleRatio, (scaleCount - 1) >> 1);
        int minItemDistance = mCenterItemWidth * scale;
        int pos = position - currentIndex;
        int itemSpace = pos * itemSpacing;
        int scrollBy = (int) (mCenterItemWidth + mCenterItemWidth * 0.72f + mCenterItemWidth * 0.28f + mCenterItemWidth * (1 - scale) * (pos - 2) + minItemDistance * pos + itemSpace);
        return scrollBy;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mRecycler == null) {
            mRecycler = recycler;
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLayoutChildren() called with: state = [" + state + "]");
        }
        if (getItemCount() == 0) {
            reset();
            detachAndScrapAttachedViews(recycler);
            return;
        }
        if (state.isPreLayout()) {
            return;
        }
        if (state.getItemCount() != 0 && !state.didStructureChange()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onLayoutChildren: ignore extra layout step");
            }
            return;
        }
        if (getChildCount() == 0 || state.didStructureChange()) {
            reset();
        }
        //设置首次选中item的位置
        mInitialSelectedPosition = Math.min(Math.max(0, mInitialSelectedPosition), getItemCount() - 1);
        //移除所有attach过的Views
        detachAndScrapAttachedViews(recycler);
        //首次填充画面
        firstFillCover(recycler);
    }

    private void reset() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "reset: ");
        }
        if (mState != null) {
            mState.mItemsFrames.clear();
        }
        //when data set update keep the last selected position
        if (mCurSelectedPosition != -1) {
            mInitialSelectedPosition = mCurSelectedPosition;
        }
        mInitialSelectedPosition = Math.min(Math.max(0, mInitialSelectedPosition), getItemCount() - 1);
        mFirstVisiblePosition = mInitialSelectedPosition;
        mLastVisiblePos = mInitialSelectedPosition;
        mCurSelectedPosition = -1;
        if (mCurSelectedView != null) {
            mCurSelectedView.setSelected(false);
            mCurSelectedView = null;
        }
    }

    private void firstFillCover(RecyclerView.Recycler recycler) {
        if (mOrientation == HORIZONTAL) {
            firstFillWithHorizontal(recycler);
        } else {
            firstFillWithVertical(recycler);
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "firstFillCover finish:first: " + mFirstVisiblePosition + ",last:" + mLastVisiblePos);
        }
        //首次填充后主动调用一次Scrolled
        mCurSelectedPosition = mInitialSelectedPosition;
    }

    /**
     * Layout the item view witch position specified by {@link GalleryLayoutManager#mInitialSelectedPosition} first and then layout the other
     *
     * @param recycler
     */
    private void firstFillWithHorizontal(RecyclerView.Recycler recycler) {
        //来自英文渣的翻译: scrap-->碎片,这里理解为每个itemView
        detachAndScrapAttachedViews(recycler);
        //RecyclerView内容区域起始位置(即去除padding后的位置)
        int leftEdge = getOrientationHelper().getStartAfterPadding();
        //RecyclerView内容区域结束位置(即去除padding后的位置)
        int rightEdge = getOrientationHelper().getEndAfterPadding();
        //起始选中位置
        int startPosition = mInitialSelectedPosition;
        //选中item的宽和高
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        //垂直方向可用空间高度(view高度减去padding距离)
        int height = getVerticalSpace();
        //item顶部距离RecyclerView顶部的距离
        int topOffset;
        //layout the init position view
        View scrap = recycler.getViewForPosition(mInitialSelectedPosition);
        scrap.setScaleX(1f);
        scrap.setScaleY(1f);
        //添加初始化时设置的选中item(即中间的item)
        addView(scrap, 0);
        //测量子view
        measureChildWithMargins(scrap, 0, 0);
        //获取测量后的宽
        scrapWidth = getDecoratedMeasuredWidth(scrap);
        mCenterItemWidth = scrapWidth;
        //获取测量后的高
        scrapHeight = getDecoratedMeasuredHeight(scrap);
        topOffset = (int) (getPaddingTop() + (height - scrapHeight) / 2.0f);
        int left = (int) (getPaddingLeft() + (getHorizontalSpace() - scrapWidth) / 2.f);
        scrapRect.set(left, topOffset, left + scrapWidth, topOffset + scrapHeight);
        //绘制选中的item
        layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
        //设置或更新item frame缓存内容
        if (getState().mItemsFrames.get(startPosition) == null) {
            getState().mItemsFrames.put(startPosition, scrapRect);
        } else {
            getState().mItemsFrames.get(startPosition).set(scrapRect);
        }
        //对第一个和最后一个可见item的position进行记录
        mFirstVisiblePosition = mLastVisiblePos = startPosition;
        //选中item左边缘到RecyclerView左边缘的距离(包含Decoration)
        int leftStartOffset = getDecoratedLeft(scrap);
        //选中item右边缘到RecyclerView左边缘的距离(包含Decoration)
        int rightStartOffset = getDecoratedRight(scrap);
        //fill left of center
        //从选中item向左填充绘制
        fillLeft(recycler, mInitialSelectedPosition - 1, leftStartOffset, leftEdge);
        //fill right of center
        //从选中item向右填充绘制
        fillRight(recycler, mInitialSelectedPosition + 1, rightStartOffset, rightEdge);
    }

    @Override
    public void onItemsRemoved(@NotNull RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsRemoved(recyclerView, positionStart, itemCount);
    }

    /**
     * Layout the item view witch position special by {@link GalleryLayoutManager#mInitialSelectedPosition} first and then layout the other
     */
    private void firstFillWithVertical(RecyclerView.Recycler recycler) {
        detachAndScrapAttachedViews(recycler);
        int topEdge = getOrientationHelper().getStartAfterPadding();
        int bottomEdge = getOrientationHelper().getEndAfterPadding();
        int startPosition = mInitialSelectedPosition;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int width = getHorizontalSpace();
        int leftOffset;
        //layout the init position view
        View scrap = recycler.getViewForPosition(mInitialSelectedPosition);
        addView(scrap, 0);
        measureChildWithMargins(scrap, 0, 0);
        scrapWidth = getDecoratedMeasuredWidth(scrap);
        scrapHeight = getDecoratedMeasuredHeight(scrap);
        leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
        int top = (int) (getPaddingTop() + (getVerticalSpace() - scrapHeight) / 2.f);
        scrapRect.set(leftOffset, top, leftOffset + scrapWidth, top + scrapHeight);
        layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
        if (getState().mItemsFrames.get(startPosition) == null) {
            getState().mItemsFrames.put(startPosition, scrapRect);
        } else {
            getState().mItemsFrames.get(startPosition).set(scrapRect);
        }
        mFirstVisiblePosition = mLastVisiblePos = startPosition;
        int topStartOffset = getDecoratedTop(scrap);
        int bottomStartOffset = getDecoratedBottom(scrap);
        //fill left of center
        fillTop(recycler, mInitialSelectedPosition - 1, topStartOffset, topEdge);
        //fill right of center
        fillBottom(recycler, mInitialSelectedPosition + 1, bottomStartOffset, bottomEdge);
    }

    /**
     * Fill left of the center view
     *
     * @param recycler
     * @param startPosition start position to fill left
     * @param startOffset   layout start offset
     * @param leftEdge
     */
    private void fillLeft(RecyclerView.Recycler recycler, int startPosition, int startOffset, int leftEdge) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        for (int i = startPosition; i >= 0 && startOffset > leftEdge; i--) {
            scrap = recycler.getViewForPosition(i);
            addView(scrap, 0);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            int gamma = Math.min((startPosition - i + 1), (scaleCount - 1) / 2);
            float spacing = (float) Math.pow(scaleRatio, gamma);
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int rightPosition = (int) (startOffset - (itemSpacing - scrapWidth * (1 - spacing) / 2));
            scrapRect.set(rightPosition - scrapWidth, topPosition, rightPosition, topPosition + scrapHeight);
            startOffset = (int) (startOffset - scrapWidth * spacing - itemSpacing);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            mFirstVisiblePosition = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * Fill right of the center view
     *
     * @param recycler
     * @param startPosition start position to fill right
     * @param startOffset   layout start offset
     * @param rightEdge
     */
    private void fillRight(RecyclerView.Recycler recycler, int startPosition, int startOffset, int rightEdge) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        for (int i = startPosition; i < getItemCount() && startOffset < rightEdge; i++) {
            scrap = recycler.getViewForPosition(i);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);

            int gamma = Math.min((i - startPosition + 1), (scaleCount - 1) / 2);
            float spacing = (float) Math.pow(scaleRatio, gamma);
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int leftPosition = (int) (startOffset + (itemSpacing - scrapWidth * (1 - spacing) / 2));
            scrapRect.set(leftPosition, topPosition, leftPosition + scrapWidth, topPosition + scrapHeight);
            startOffset = (int) (startOffset + scrapWidth * spacing + itemSpacing);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            mLastVisiblePos = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * Fill top of the center view
     *
     * @param recycler
     * @param startPosition start position to fill top
     * @param startOffset   layout start offset
     * @param topEdge       top edge of the RecycleView
     */
    private void fillTop(RecyclerView.Recycler recycler, int startPosition, int startOffset, int topEdge) {
        View scrap;
        int leftOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int width = getHorizontalSpace();
        for (int i = startPosition; i >= 0 && startOffset > topEdge; i--) {
            scrap = recycler.getViewForPosition(i);
            addView(scrap, 0);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
            scrapRect.set(leftOffset, startOffset - scrapHeight, leftOffset + scrapWidth, startOffset);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            startOffset = scrapRect.top;
            mFirstVisiblePosition = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * Fill bottom of the center view
     *
     * @param recycler
     * @param startPosition start position to fill bottom
     * @param startOffset   layout start offset
     * @param bottomEdge    bottom edge of the RecycleView
     */
    private void fillBottom(RecyclerView.Recycler recycler, int startPosition, int startOffset, int bottomEdge) {
        View scrap;
        int leftOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int width = getHorizontalSpace();
        for (int i = startPosition; i < getItemCount() && startOffset < bottomEdge; i++) {
            scrap = recycler.getViewForPosition(i);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
            scrapRect.set(leftOffset, startOffset, leftOffset + scrapWidth, startOffset + scrapHeight);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            startOffset = scrapRect.bottom;
            mLastVisiblePos = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }


    private void fillCover(RecyclerView.Recycler recycler, int scrollDelta) {
        if (getItemCount() == 0) {
            return;
        }
        if (mOrientation == HORIZONTAL) {
            fillHorizontal(recycler, getState().mScrollDelta);
        } else {
            fillWithVertical(recycler, scrollDelta);
        }
    }

    /**
     * @param recycler
     * @param dy
     */
    private void fillWithVertical(RecyclerView.Recycler recycler, int dy) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "fillWithVertical: dy:" + dy);
        }
        int topEdge = getOrientationHelper().getStartAfterPadding();
        int bottomEdge = getOrientationHelper().getEndAfterPadding();

        //1.remove and recycle the view that disappear in screen
        View child;
        if (getChildCount() > 0) {
            if (dy >= 0) {
                //remove and recycle the top off screen view
                int fixIndex = 0;
                for (int i = 0; i < getChildCount(); i++) {
                    child = getChildAt(i + fixIndex);
                    if (getDecoratedBottom(child) - dy < topEdge) {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "fillWithVertical: removeAndRecycleView:" + getPosition(child) + ",bottom:" + getDecoratedBottom(child));
                        }
                        removeAndRecycleView(child, recycler);
                        mFirstVisiblePosition++;
                        fixIndex--;
                    } else {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "fillWithVertical: break:" + getPosition(child) + ",bottom:" + getDecoratedBottom(child));
                        }
                        break;
                    }
                }
            } else { //dy<0
                //remove and recycle the bottom off screen view
                for (int i = getChildCount() - 1; i >= 0; i--) {
                    child = getChildAt(i);
                    if (getDecoratedTop(child) - dy > bottomEdge) {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "fillWithVertical: removeAndRecycleView:" + getPosition(child));
                        }
                        removeAndRecycleView(child, recycler);
                        mLastVisiblePos--;
                    } else {
                        break;
                    }
                }
            }

        }

        int startPosition = mFirstVisiblePosition;
        int startOffset = -1;
        int scrapWidth, scrapHeight;
        Rect scrapRect;
        int width = getHorizontalSpace();
        int leftOffset;
        View scrap;
        //2.Add or reattach item view to fill screen
        if (dy >= 0) {
            if (getChildCount() != 0) {
                View lastView = getChildAt(getChildCount() - 1);
                startPosition = getPosition(lastView) + 1;
                startOffset = getDecoratedBottom(lastView);
            }
            for (int i = startPosition; i < getItemCount() && startOffset < bottomEdge + dy; i++) {
                scrapRect = getState().mItemsFrames.get(i);
                scrap = recycler.getViewForPosition(i);
                addView(scrap);
                if (scrapRect == null) {
                    scrapRect = new Rect();
                    getState().mItemsFrames.put(i, scrapRect);
                }
                measureChildWithMargins(scrap, 0, 0);
                scrapWidth = getDecoratedMeasuredWidth(scrap);
                scrapHeight = getDecoratedMeasuredHeight(scrap);
                leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
                if (startOffset == -1 && startPosition == 0) {
                    //layout the first position item in center
                    int top = (int) (getPaddingTop() + (getVerticalSpace() - scrapHeight) / 2.f);
                    scrapRect.set(leftOffset, top, leftOffset + scrapWidth, top + scrapHeight);
                } else {
                    scrapRect.set(leftOffset, startOffset, leftOffset + scrapWidth, startOffset + scrapHeight);
                }
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
                startOffset = scrapRect.bottom;
                mLastVisiblePos = i;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithVertical: add view:" + i + ",startOffset:" + startOffset + ",mLastVisiblePos:" + mLastVisiblePos + ",bottomEdge" + bottomEdge);
                }
            }
        } else {
            //dy<0
            if (getChildCount() > 0) {
                View firstView = getChildAt(0);
                startPosition = getPosition(firstView) - 1; //前一个View的position
                startOffset = getDecoratedTop(firstView);
            }
            for (int i = startPosition; i >= 0 && startOffset > topEdge + dy; i--) {
                scrapRect = getState().mItemsFrames.get(i);
                scrap = recycler.getViewForPosition(i);
                addView(scrap, 0);
                if (scrapRect == null) {
                    scrapRect = new Rect();
                    getState().mItemsFrames.put(i, scrapRect);
                }
                measureChildWithMargins(scrap, 0, 0);
                scrapWidth = getDecoratedMeasuredWidth(scrap);
                scrapHeight = getDecoratedMeasuredHeight(scrap);
                leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
                scrapRect.set(leftOffset, startOffset - scrapHeight, leftOffset + scrapWidth, startOffset);
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
                startOffset = scrapRect.top;
                mFirstVisiblePosition = i;
            }
        }
    }

    private void fillHorizontal(RecyclerView.Recycler recycler, int dx) {
        detachAndScrapAttachedViews(recycler);
        //recyclerview中心点x坐标位置
        int parentCenter = (getOrientationHelper().getEndAfterPadding() - getOrientationHelper().getStartAfterPadding()) / 2
                + getOrientationHelper().getStartAfterPadding();
        //确定recyclerview左边缘的位置
        int leftEdge = getOrientationHelper().getStartAfterPadding();
        //确定recyclerview右边缘的位置
        int rightEdge = getOrientationHelper().getEndAfterPadding();
        //中心item到两边最近的item的偏移量
        int offetOneFromCenter = mCenterItemWidth + itemSpacing;
        //根据mCurSelectedPosition先绘制中间item(中间item此时可能也处于偏移状态)
        int offsetDx = Math.abs(dx) % offetOneFromCenter;
        int topOffset;
        int scrapWidth, scrapHeight;
        int height = getVerticalSpace();
        Rect scrapRect;
        float spacing;
        int beishu = Math.abs(dx) / offetOneFromCenter;
        if (dx > 0) {
            if ((mInitialSelectedPosition + beishu) < getItemCount()) {
                mCurSelectedPosition = mInitialSelectedPosition + beishu;
            } else {
                mCurSelectedPosition = getItemCount() - 1;
            }
            //从右向左滑
            spacing = 1f - (1f - scaleRatio) * offsetDx / (float) offetOneFromCenter;
            scrapRect = getState().mItemsFrames.get(mCurSelectedPosition);
            View scrap = recycler.getViewForPosition(mCurSelectedPosition);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            if (scrapRect == null) {
                scrapRect = new Rect();
            }
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int rightPosition = (int) (parentCenter + mCenterItemWidth / 2 - offsetDx +
                    (scrapWidth * (1 - spacing) / 2));
            scrapRect.set(rightPosition - scrapWidth, topPosition, rightPosition, topPosition + scrapHeight);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            //画左面
            fillLeftTest(recycler, mCurSelectedPosition - 1,
                    (int) (rightPosition - scrapWidth + scrapWidth * (1f - spacing) / 2),
                    leftEdge, dx);
            //画右面
            fillRightTest(recycler, mCurSelectedPosition + 1,
                    (int) (rightPosition - scrapWidth * (1f - spacing) / 2),
                    rightEdge, dx);
        } else {
            //从左向右滑
            mCurSelectedPosition = Math.max((mInitialSelectedPosition - beishu), 0);
            spacing = 1f - (1f - scaleRatio) * offsetDx / (float) offetOneFromCenter;
            scrapRect = getState().mItemsFrames.get(mCurSelectedPosition);
            View scrap = recycler.getViewForPosition(mCurSelectedPosition);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            if (scrapRect == null) {
                scrapRect = new Rect();
            }
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int leftPosition = (int) (parentCenter - mCenterItemWidth / 2 + offsetDx -
                    (scrapWidth * (1 - spacing) / 2));
            scrapRect.set(leftPosition, topPosition, leftPosition + scrapWidth, topPosition + scrapHeight);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);

            //画左面
            fillLeftTest1(recycler, mCurSelectedPosition - 1,
                    (int) (leftPosition + scrapWidth * (1f - spacing) / 2),
                    leftEdge, dx);
            //画右面
            fillRightTest1(recycler, mCurSelectedPosition + 1,
                    (int) (leftPosition + scrapWidth - scrapWidth * (1f - spacing) / 2),
                    rightEdge, dx);
        }
    }

    private void fillLeftTest1(RecyclerView.Recycler recycler, int startPosition, int startOffset, int leftEdge, int dx) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        int offetOneFromCenter = mCenterItemWidth + itemSpacing;
        for (int i = startPosition; i >= 0 && startOffset > leftEdge + itemSpacing; i--) {
            int gamma = startPosition - i + 1;
            float tempScale = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, (float) gamma - 1));
            float preSpacing = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, gamma));
            scrap = recycler.getViewForPosition(i);
            addView(scrap, 0);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            int offsetDx = Math.abs(dx) % offetOneFromCenter;
            float spacing = preSpacing + (tempScale - preSpacing) * offsetDx / (float) offetOneFromCenter;
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int rightPosition = (int) (startOffset - (itemSpacing - scrapWidth * (1 - spacing) / 2));
            scrapRect.set(rightPosition - scrapWidth, topPosition, rightPosition, topPosition + scrapHeight);
            startOffset = (int) (startOffset - scrapWidth * spacing - itemSpacing);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            mFirstVisiblePosition = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    private void fillRightTest1(RecyclerView.Recycler recycler, int startPosition, int startOffset, int rightEdge, int dx) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        int offetOneFromCenter = mCenterItemWidth + itemSpacing;
        for (int i = startPosition; i < getItemCount() && startOffset < rightEdge - itemSpacing; i++) {
            int gamma = i - startPosition + 1;
            float tempScale = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, (float) gamma));
            float nextSpacing = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, gamma + 1));
            scrap = recycler.getViewForPosition(i);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            int offsetDx = Math.abs(dx) % offetOneFromCenter;
            float spacing = tempScale - (tempScale - nextSpacing) * offsetDx / (float) offetOneFromCenter;
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int leftPosition = (int) (startOffset + (itemSpacing - scrapWidth * (1 - spacing) / 2));
            scrapRect.set(leftPosition, topPosition, leftPosition + scrapWidth, topPosition + scrapHeight);
            startOffset = (int) (startOffset + scrapWidth * spacing + itemSpacing);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            mLastVisiblePos = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * 动态绘制左侧view
     */
    private void fillLeftTest(RecyclerView.Recycler recycler, int startPosition, int startOffset, int leftEdge, int dx) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        int offetOneFromCenter = mCenterItemWidth + itemSpacing;
        for (int i = startPosition; i >= 0 && startOffset > leftEdge + itemSpacing; i--) {
            int gamma = Math.min((startPosition - i + 1), (scaleCount - 1) / 2);
            float tempScale = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, (float) gamma));
            float preSpacing = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, gamma + 1));
            scrap = recycler.getViewForPosition(i);
            addView(scrap, 0);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            int offsetDx = Math.abs(dx) % offetOneFromCenter;
            float spacing = tempScale - (tempScale - preSpacing) * offsetDx / (float) offetOneFromCenter;
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int rightPosition = (int) (startOffset - (itemSpacing - scrapWidth * (1 - spacing) / 2));
            scrapRect.set(rightPosition - scrapWidth, topPosition, rightPosition, topPosition + scrapHeight);
            startOffset = (int) (startOffset - scrapWidth * spacing - itemSpacing);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            mFirstVisiblePosition = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * 动态绘制右侧view
     */
    private void fillRightTest(RecyclerView.Recycler recycler, int startPosition, int startOffset, int rightEdge, int dx) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        int offetOneFromCenter = mCenterItemWidth + itemSpacing;
        for (int i = startPosition; i < getItemCount() && startOffset < rightEdge - itemSpacing; i++) {
            int gamma = i - startPosition + 1;
            float tempScale = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, (float) gamma));
            float preSpacing = (float) Math.max(Math.pow(scaleRatio, (scaleCount - 1) / 2f),
                    Math.pow(scaleRatio, gamma - 1));
            scrap = recycler.getViewForPosition(i);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            int offsetDx = Math.abs(dx) % offetOneFromCenter;
            float spacing = tempScale + (preSpacing - tempScale) * offsetDx / (float) offetOneFromCenter;
            scrap.setScaleX(spacing);
            scrap.setScaleY(spacing);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight * spacing) / 2.0f);
            int topPosition = (int) (topOffset - scrapHeight * (1 - spacing) / 2.0f);
            int leftPosition = (int) (startOffset + (itemSpacing - scrapWidth * (1 - spacing) / 2));
            scrapRect.set(leftPosition, topPosition, leftPosition + scrapWidth, topPosition + scrapHeight);
            startOffset = (int) (startOffset + scrapWidth * spacing + itemSpacing);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            mLastVisiblePos = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * 获取水平方向可用距离
     */
    private int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    /**
     * 获取垂直方向可用距离
     */
    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    public State getState() {
        if (mState == null) {
            mState = new State();
        }
        return mState;
    }

    private int calculateScrollDirectionForPosition(int position) {
        if (getChildCount() == 0) {
            return LAYOUT_START;
        }
        final int firstChildPos = mFirstVisiblePosition;
        return position < firstChildPos ? LAYOUT_START : LAYOUT_END;
    }

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        final int direction = calculateScrollDirectionForPosition(targetPosition);
        PointF outVector = new PointF();
        if (direction == 0) {
            return null;
        }
        if (mOrientation == HORIZONTAL) {
            outVector.x = direction;
            outVector.y = 0;
        } else {
            outVector.x = 0;
            outVector.y = direction;
        }
        return outVector;
    }

    /**
     * @author chensuilun
     */
    static class State {
        /**
         * Record all item view 's last position after last layout
         */
        SparseArray<Rect> mItemsFrames;

        /**
         * RecycleView 's current scroll distance since first layout
         */
        int mScrollDelta;

        public State() {
            mItemsFrames = new SparseArray<>();
            mScrollDelta = 0;
        }
    }


    @Override
    public boolean canScrollHorizontally() {
        return mOrientation == HORIZONTAL;
    }


    @Override
    public boolean canScrollVertically() {
        return mOrientation == VERTICAL;
    }

    protected float getDistanceRatio() {
        return 1f;
    }

    //横向滑动触发
    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mRecycler == null) {
            mRecycler = recycler;
        }
        // When dx is positive，finger fling from right to left(←)，scrollX+
        if (getChildCount() == 0 || dx == 0) {
            return 0;
        }
        int willScroll = dx;
        float realDx = dx / getDistanceRatio();
        if (Math.abs(realDx) < 0.00000001f) {
            return 0;
        }
        float targetOffset = getState().mScrollDelta + realDx;
        //handle the boundary
        if (!mInfinite && targetOffset < getMinOffset()) {
            willScroll -= (targetOffset - getMinOffset()) * getDistanceRatio();
        } else if (!mInfinite && targetOffset > getMaxOffset()) {
            willScroll = (int) ((getMaxOffset() - getState().mScrollDelta) * getDistanceRatio());
        }
        realDx = willScroll / getDistanceRatio();
        getState().mScrollDelta += realDx;
        fillCover(recycler, 0);
        return willScroll;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }
        int delta = -dy;
        int parentCenter = (getOrientationHelper().getEndAfterPadding() - getOrientationHelper().getStartAfterPadding()) / 2 + getOrientationHelper().getStartAfterPadding();
        View child;
        if (dy > 0) {
            //If we've reached the last item, enforce limits
            if (getPosition(getChildAt(getChildCount() - 1)) == getItemCount() - 1) {
                child = getChildAt(getChildCount() - 1);
                delta = -Math.max(0, Math.min(dy, (getDecoratedBottom(child) - getDecoratedTop(child)) / 2 + getDecoratedTop(child) - parentCenter));
            }
        } else {
            //If we've reached the first item, enforce limits
            if (mFirstVisiblePosition == 0) {
                child = getChildAt(0);
                delta = -Math.min(0, Math.max(dy, (getDecoratedBottom(child) - getDecoratedTop(child)) / 2 + getDecoratedTop(child) - parentCenter));
            }
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "scrollVerticallyBy: dy:" + dy + ",fixed:" + delta);
        }
        getState().mScrollDelta += -delta;
        fillCover(recycler, -delta);
        offsetChildrenVertical(delta);
        return -delta;
    }

    public OrientationHelper getOrientationHelper() {
        if (mOrientation == HORIZONTAL) {
            if (mHorizontalHelper == null) {
                mHorizontalHelper = OrientationHelper.createHorizontalHelper(this);
            }
            return mHorizontalHelper;
        } else {
            if (mVerticalHelper == null) {
                mVerticalHelper = OrientationHelper.createVerticalHelper(this);
            }
            return mVerticalHelper;
        }
    }

    public static class LayoutParams extends RecyclerView.LayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }
    }

    public interface OnItemSelectedListener {
        /**
         * @param position The current selected view's position
         */
        void onItemSelected(int position);
    }

    private OnItemSelectedListener mOnItemSelectedListener;

    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        mOnItemSelectedListener = onItemSelectedListener;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        recyclerView.smoothScrollBy(getOffsetToPosition(position), 0, mSmoothScrollInterpolator);
    }


    @Override
    public void scrollToPosition(int position) {
        mInitialSelectedPosition = Math.min(Math.max(0, mInitialSelectedPosition), getItemCount() - 1);
        mFirstVisiblePosition = mInitialSelectedPosition;
        mLastVisiblePos = mInitialSelectedPosition;
        mCurSelectedPosition = -1;
        getState().mScrollDelta = 0;
        if (mCurSelectedView != null) {
            mCurSelectedView.setSelected(false);
            mCurSelectedView = null;
        }
        //设置首次选中item的位置
        mInitialSelectedPosition = Math.min(Math.max(0, position), getItemCount() - 1);
        //移除所有attach过的Views
        detachAndScrapAttachedViews(mRecycler);
        //首次填充画面
        firstFillCover(mRecycler);
    }

    /**
     * 设置滚动插值器
     */
    public void setSmoothScrollInterpolator(Interpolator smoothScrollInterpolator) {
        this.mSmoothScrollInterpolator = smoothScrollInterpolator;
    }

    public int getOrientation() {
        return mOrientation;
    }

    /**
     * 获取当前选中的位置
     */
    public int getCurSelectedPosition() {
        return mCurSelectedPosition;
    }

    public int centerPosition() {
        if (mSnapHelper == null) {
            return -1;
        }
        View snapView = mSnapHelper.findSnapView(this);
        if (snapView == null) {
            return -1;
        }
        return getPosition(snapView);
    }

}
