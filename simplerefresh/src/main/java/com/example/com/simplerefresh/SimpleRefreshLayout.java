package com.example.com.simplerefresh;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.v4.widget.ListViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by rhm on 2018/2/6.
 */

public class SimpleRefreshLayout extends ViewGroup {
    private final static float SCROLL_RESISTANCE = 3f;//滑动阻力
    private static String TAG = "SimpleRefreshLayout";

    private boolean isRefreshing;

    private ImageView ivArrowPullDown;   // 下拉状态指示器(箭头)
    private ImageView ivProgressPullDown;// 下拉加载进度条(圆形)
    private ImageView ivProgressPullUp;  // 上拉加载进度条(圆形)

    private TextView tvHintPullDown; // 下拉状态文本指示
    private TextView tvHintPullUp;   // 上拉状态文本指示

    private AnimationDrawable upProgressAnimation;   // 上拉加载进度条帧动画
    private AnimationDrawable downProgressAnimation; // 下拉加载进度条帧动画

    private View mTarget;//触发滑动手势的目标View
    private View mPullHeader;//头部
    private View mPullFooter;//底部
    private Context mContext;

    private Scroller mScroller;
    private int mTouchSlop;//最小滑动距离
    private int mEffectiveScrollY;//设置有效滑动距离

    private boolean pullDownEnable = true;
    private boolean pullUpEnable = true;
    private boolean isPullDown = false;

    private int mLastDownY;
    private int mCurrentState = PULL_IDLE; //当前状态
    private onRefreshListener refreshListener;//监听

    public SimpleRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        //获取自定义属性
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SimpleRefreshLayout
        );
        pullUpEnable = typedArray.getBoolean(R.styleable.SimpleRefreshLayout_upEnable, true);
        pullDownEnable = typedArray.getBoolean(R.styleable.SimpleRefreshLayout_downEnable, true);
        typedArray.recycle();

//        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
//        mTouchSlop = (int) getResources().getDimension(R.dimen.touch_slop);
        mEffectiveScrollY = (int) getResources().getDimension(R.dimen.effect_scroll);
        mScroller = new Scroller(context);
        mContext = context;
    }


    //动态设置下拉刷新是否可用
    public void setPullDownEnable(boolean pullDownEnable) {
        this.pullDownEnable = pullDownEnable;
    }

    //动态设置上拉加载是否可用
    public void setPullUpEnable(boolean pullUpEnable) {
        this.pullUpEnable = pullUpEnable;
    }


    //设置刷新回调监听
    public void setOnRefresh(onRefreshListener onRefresh) {
        this.refreshListener = onRefresh;
    }

    public void stopRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isPullDown) {
                    resetDown();
                } else {
                    resetUp();
                }
                mScroller.startScroll(0, getScrollY(), 0, -getScrollY());
                invalidate();
            }
        }, 1000);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (pullDownEnable) {
            initHeader();
        }
        if (pullUpEnable) {
            initFooter();
        }
        ensureTarget();
    }

    private void initFooter() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mPullFooter = LayoutInflater.from(mContext).inflate(R.layout.simple_footer_layout, null);
        mPullFooter.setLayoutParams(params);
        ivProgressPullUp = mPullFooter.findViewById(R.id.pull_up_loading);
        tvHintPullUp = mPullFooter.findViewById(R.id.tv_pull_up_des);
        upProgressAnimation = (AnimationDrawable) ivProgressPullUp.getBackground();
        this.addView(mPullFooter, getChildCount());//放置在最后一个
    }

    private void initHeader() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mPullHeader = LayoutInflater.from(mContext).inflate(R.layout.simple_header_layout, null);
        mPullHeader.setLayoutParams(params);
        ivArrowPullDown = mPullHeader.findViewById(R.id.pull_down_arrow);
        ivProgressPullDown = mPullHeader.findViewById(R.id.pull_down_loading);
        tvHintPullDown = mPullHeader.findViewById(R.id.tv_pull_down_des);
        downProgressAnimation = (AnimationDrawable) ivProgressPullDown.getBackground();
        this.addView(mPullHeader, 0);//将头部添加到第一个
    }

    public void ensureTarget() {
        if (mTarget == null) {
            if (pullDownEnable) {
                mTarget = getChildAt(1);
            } else {
                mTarget = getChildAt(0);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            return;
        }
        //测量子类,设置为铺满全屏
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(
                    MeasureSpec.makeMeasureSpec(
                            getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(
                            getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                            MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (mTarget == null) {
            return;
        }
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == mPullHeader) {
                child.layout(0, 0 - child.getMeasuredHeight(), child.getMeasuredWidth(), 0);
            } else if (child == mTarget) {
                child.layout(0, 0, 0 + getMeasuredWidth(), getMeasuredHeight());//内容位置
            } else if (child == mPullFooter) {
                child.layout(0, height, width, height + child.getMeasuredHeight());
            }
        }
    }

    //判断子类是否可上滑
    private boolean canChildScrollUp() {
        if (mTarget == null) {
            return false;
        }
        if (mTarget instanceof ListView) {
            return ListViewCompat.canScrollList((ListView) mTarget, -1);
        }
        return mTarget.canScrollVertically(-1);
    }

    //判断子类是否可下滑
    private boolean canChildScrollDown() {
        if (mTarget == null) {
            return false;
        }
        if (mTarget instanceof ListView) {
            return ListViewCompat.canScrollList((ListView) mTarget, 1);
        }
        return mTarget.canScrollVertically(1);
    }

    //拦截操作
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;
        int y = (int) ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                intercept = false;
                break;
            case MotionEvent.ACTION_MOVE:
                int deltay = mLastDownY - y;
                if (!isRefreshing) {
                    if (deltay < 0) {//下拉
                        if (!canChildScrollUp()) { //子类到达顶部不能上滑
                            intercept = true;
                        }
                    } else if (deltay > 0) {//上拉
                        if (!canChildScrollDown()) {//子类到达底部不能下滑
                            intercept = true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                intercept = false;
                break;
        }
        mLastDownY = y;
        return intercept;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                int deltaY = mLastDownY - y;
                doScroll(deltaY);
                break;
            case MotionEvent.ACTION_UP:
                doScrollStop();
                break;
        }
        mLastDownY = y;
        return true;
    }

    //滑动结束
    private void doScrollStop() {
        if (isPullDown) {
            if (Math.abs(getScrollY()) >= mEffectiveScrollY) {
                if (refreshListener != null) {
                    refreshListener.onDownRefresh();
                }
                updateState(PULL_DOWN_REFRESH);
                mScroller.startScroll(0, getScrollY(), 0, -getScrollY() - mEffectiveScrollY);

            } else {
                mScroller.startScroll(0, getScrollY(), 0, -getScrollY());
                resetDown();

            }
        } else {
            if (Math.abs(getScrollY()) >= mEffectiveScrollY) {
                if (refreshListener != null) {
                    refreshListener.onUpRefresh();
                }
                updateState(PULL_UP_REFRESH);
                mScroller.startScroll(0, getScrollY(), 0, -getScrollY() + mEffectiveScrollY);


            } else {
                mScroller.startScroll(0, getScrollY(), 0, -getScrollY());
                resetUp();

            }
        }
        invalidate();//刷新
    }

    //滑动过程中的变化
    private void doScroll(int deltaY) {
        if (Math.abs(deltaY) > mTouchSlop) {//超过最小滑动距离
            if (deltaY < 0) {//下拉
                if (getScrollY() < 0) {//顶部向下拉
                    if (!pullDownEnable) {
                        return;
                    }
                    isPullDown = true;
                    if (Math.abs(getScrollY()) <= mPullHeader.getMeasuredHeight() / 2) {
                        if (Math.abs(getScrollY()) >= mEffectiveScrollY) {
                            deltaY /= SCROLL_RESISTANCE;//滑动阻力
                            updateState(PULL_DOWN_RELEASE);
                        } else {
                            updateState(PULL_DOWN_NORMAL);
                        }
                    }
                } else { //底部向下滑动时
                    if (!pullUpEnable) {
                        return;
                    }
                    if (Math.abs(getScrollY()) < mEffectiveScrollY) {
                        updateState(PULL_UP_NORMAL);
                    }
                }
            } else {//上拉
                if (getScrollY() < 0) {//顶部向上滑动
                    if (!pullDownEnable) {
                        return;
                    }
                    if (Math.abs(getScrollY()) < mEffectiveScrollY) {
                        updateState(PULL_DOWN_NORMAL);
                    }
                } else {//底部上拉
                    if (!pullUpEnable) {
                        return;
                    }
                    isPullDown = false;
                    if (Math.abs(getScrollY()) + Math.abs(deltaY) < mPullFooter.getMeasuredHeight() / 2) {
                        if (Math.abs(getScrollY()) >= mEffectiveScrollY) {
                            updateState(PULL_UP_RELEASE);
                            deltaY /= SCROLL_RESISTANCE;//添加滑动阻力
                        } else {
                            updateState(PULL_UP_NORMAL);
                        }
                    }
                }
            }
            scrollBy(0, deltaY);
        }
    }

    private void updateState(@State int state) {
        switch (state) {
            case PULL_DOWN_NORMAL:
                if (mCurrentState == PULL_DOWN_NORMAL) {
                    return;
                }
                mCurrentState = PULL_DOWN_NORMAL;
                ivArrowPullDown.setRotation(180);
                rotateArrow();
                ivArrowPullDown.setVisibility(VISIBLE);
                ivArrowPullDown.setRotation(0);
                tvHintPullDown.setText("下拉刷新");
                downProgressAnimation.stop();
                ivProgressPullDown.setVisibility(GONE);
                break;
            case PULL_DOWN_RELEASE:
                if (mCurrentState == PULL_DOWN_RELEASE) {
                    return;
                }
                mCurrentState = PULL_DOWN_RELEASE;
                tvHintPullDown.setText("释放刷新");
                ivArrowPullDown.setRotation(0); //为了避免旋转过程中拖动而导致旋转方向改变
                rotateArrow();
                break;
            case PULL_DOWN_REFRESH:
                if (mCurrentState == PULL_DOWN_REFRESH) {
                    return;
                }
                isRefreshing = true;
                mCurrentState = PULL_DOWN_REFRESH;
                ivProgressPullDown.setVisibility(VISIBLE);
                ivArrowPullDown.setVisibility(INVISIBLE);
                tvHintPullDown.setText("正在刷新");
                downProgressAnimation.start();
                break;
            case PULL_UP_NORMAL:
                if (mCurrentState == PULL_UP_NORMAL) {
                    return;
                }
                mCurrentState = PULL_UP_NORMAL;
                resetUp();
                break;
            case PULL_UP_RELEASE:
                if (mCurrentState == PULL_UP_RELEASE) {
                    return;
                }
                mCurrentState = PULL_UP_RELEASE;
                tvHintPullUp.setText("释放加载");
                break;
            case PULL_UP_REFRESH:
                if (mCurrentState == PULL_UP_REFRESH) {
                    return;
                }
                isRefreshing = true;
                mCurrentState = PULL_UP_REFRESH;
                tvHintPullUp.setText("正在加载中...");
                ivProgressPullUp.setVisibility(VISIBLE);
                upProgressAnimation.start();
                break;
        }
    }

    //旋转箭头
    private void rotateArrow() {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(ivArrowPullDown, "rotation", ivArrowPullDown.getRotation(), ivArrowPullDown.getRotation() + 180);
        objectAnimator.setDuration(135);
        objectAnimator.start();
    }

    private void resetDown() {
        if (pullDownEnable) {
            mCurrentState = PULL_IDLE;
            isRefreshing = false;
            ivArrowPullDown.setVisibility(VISIBLE);
            ivArrowPullDown.setRotation(0);
            tvHintPullDown.setText("下拉刷新");
            downProgressAnimation.stop();
            ivProgressPullDown.setVisibility(GONE);
        }
    }

    private void resetUp() {
        if (pullUpEnable) {
            mCurrentState = PULL_IDLE;
            isRefreshing = false;
            tvHintPullUp.setText("上拉加载更多");
            upProgressAnimation.stop();
            ivProgressPullUp.setVisibility(GONE);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
        }
        invalidate();
    }

    static final int PULL_IDLE = -1;//无状态
    static final int PULL_DOWN_NORMAL = 0;//下拉刷新
    static final int PULL_DOWN_RELEASE = 1;//释放刷新
    static final int PULL_DOWN_REFRESH = 2;//正在刷新
    static final int PULL_UP_NORMAL = 3;//上拉加载更多
    static final int PULL_UP_RELEASE = 4;//上拉释放
    static final int PULL_UP_REFRESH = 5;//正在加载

    @IntDef({
            PULL_IDLE,
            PULL_DOWN_NORMAL,
            PULL_DOWN_RELEASE,
            PULL_DOWN_REFRESH,
            PULL_UP_NORMAL,
            PULL_UP_RELEASE,
            PULL_UP_REFRESH
    })

    @Retention(RetentionPolicy.SOURCE)
    @interface State {
    }

    public interface onRefreshListener {
        public void onUpRefresh();

        public void onDownRefresh();
    }
}