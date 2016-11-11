package com.aries.scaleimageview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.aries.scaleimageview.animation.AnimationOption;
import com.aries.scaleimageview.animation.FlingAnimator;
import com.aries.scaleimageview.animation.ImageAnimator;
import com.aries.scaleimageview.gesture.DragDetector;
import com.aries.scaleimageview.gesture.OnGesture;
import com.aries.scaleimageview.gesture.ScaleDetector;

/**
 * Reference to PhotoView, https://github.com/chrisbanes/PhotoView
 */
public class ScaleImageView extends ImageView implements OnGesture {

    private static final float MAX_RANGE_ALLOW_INTERCEPT = 1.02f;
    private static final float MIN_RANGE_ALLOW_INTERCEPT = 0.98f;
    private static final String TAG = "ScaleImageView";

    private ScaleDetector mScaleDetector;
    private DragDetector mDragDetector;
    private GestureDetector gestureDetector;

    private ImageAnimator mImageAnimator;
    private FlingAnimator mFlingAnimator;

    private Matrix mMatrix;
    private Matrix mBaseMatrix;
    private float mMaxRate;
    private float mMinRate;
    private float mMidRate;

    private float leftBound;
    private float rightBound;
    private float topBound;
    private float bottomBound;

    public ScaleImageView(Context context) {
        super(context);
        initial();
    }

    public ScaleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initial();
    }

    public ScaleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initial();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);

        if (drawable != null) {
            ViewTreeObserver observer = this.getViewTreeObserver();
            if (null != observer) {
                observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        Drawable d = getDrawable();
                        if (d != null) {
                            final float viewWidth = getMeasuredWidth();
                            final float viewHeight = getMeasuredHeight();
                            final int drawableWidth = d.getIntrinsicWidth();
                            final int drawableHeight = d.getIntrinsicHeight();

                            float scaleRate;
                            mMatrix.reset();

                            scaleRate = drawableWidth >= drawableHeight ? viewWidth / drawableWidth : viewHeight / drawableHeight;
                            mMinRate = scaleRate;
                            mMidRate = mMinRate * 2.5f;
                            mMaxRate = mMinRate * 5f;

                            final float scaledWidth = drawableWidth * scaleRate;
                            final float scaledHeight = drawableHeight * scaleRate;

                            mMatrix.setScale(scaleRate, scaleRate);
                            mMatrix.postTranslate((viewWidth - scaledWidth) / 2F,
                                    (viewHeight - scaledHeight) / 2F);

                            setImageMatrix(mMatrix);
                            mBaseMatrix = new Matrix(mMatrix);

                            updateBounds();
                        }
                    }
                });
            }
            requestLayout();
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mScaleDetector.onTouch(e) && !mScaleDetector.isScaling()) {
            mDragDetector.onTouch(e);
            gestureDetector.onTouchEvent(e);
        }

        ViewParent parent = getParent();

        int action = MotionEventCompat.getActionMasked(e);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                mFlingAnimator.abortFling();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                mImageAnimator.abortAnimation();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                resetRateIfNeed();
                break;

            case MotionEvent.ACTION_UP:
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
                resetRateIfNeed();
                break;
        }
        return true;
    }

    @Override
    public void onDrag(float dx, float dy) {
        ViewParent parent = this.getParent();
        RectF rect = getRect(mMatrix);
        boolean allowScrollX = rect.width() > getMeasuredWidth();
        boolean allowScrollY = rect.height() > getMeasuredHeight();

        if (!allowScrollX && !allowScrollY) {
            parent.requestDisallowInterceptTouchEvent(false);
            return;
        }

        boolean touchLeftEdge = rect.left + dx >= leftBound;
        boolean touchRightEdge = rect.right + dx <= rightBound;
        boolean touchTopEdge = rect.top + dy >= topBound;
        boolean touchBottomEdge = rect.bottom + dy <= bottomBound;

        if (allowScrollX) {
            if (touchLeftEdge) {
                dx = leftBound - rect.left;
            } else if (touchRightEdge) {
                dx = rightBound - rect.right;
            }
        } else {
            dx = 0;
        }

        if (allowScrollY) {
            if (touchTopEdge) {
                dy = topBound - rect.top;
            } else if (touchBottomEdge) {
                dy = bottomBound - rect.bottom;
            }
        } else
            dy = 0;

        dx = allowScrollX ? dx : 0;
        dy = allowScrollY ? dy : 0;

        mMatrix.postTranslate(dx, dy);
        setImageMatrix(mMatrix);

        if (parent != null) {
            if (allowParentInterceptTouchEvent()) {
                parent.requestDisallowInterceptTouchEvent(false);
            } else {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }
    }

    @Override
    public void onFling(float startX, float startY, float velocityX, float velocityY) {
        mFlingAnimator.fling(
                new AnimationOption()
                        .setView(this)
                        .setVelocityX(velocityX)
                        .setVelocityY(velocityY)
                        .setLeftBound(leftBound)
                        .setRightBound(rightBound)
                        .setTopBound(topBound)
                        .setBottomBound(bottomBound)
                        .setRect(getRect(mMatrix)
                        )
        );
    }

    @Override
    public void onScale(float rate, float focusX, float focusY) {

        int viewHeight = getMeasuredHeight();
        int viewWidth = getMeasuredWidth();

        if (rate == 1.0f) {
            return;
        }

        mMatrix.postScale(rate, rate, focusX, focusY);
        updateBounds();

        RectF currRect = getRect(mMatrix);
        float dx = 0;
        float dy = 0;

        //If the image after scale no align bounds, adjust to image align left or right.
        if (currRect.right < rightBound) {
            dx = rightBound - currRect.right;
        } else if (currRect.left > leftBound) {
            dx = leftBound - currRect.left;
        }

        //If the image after scale no align bounds, adjust to image align top or bottom.
        if (currRect.bottom < bottomBound) {
            dy = bottomBound - currRect.bottom;
        } else if (currRect.top > topBound) {
            dy = topBound - currRect.top;
        }

        mMatrix.postTranslate(dx, dy);
        currRect = getRect(mMatrix);

        //If the image width after scale is smaller than view width, move the image to center align
        if (currRect.width() <= viewWidth) {
            float srcX = currRect.left;
            float dstX = (viewWidth / 2) - (currRect.width() / 2);
            dx = dstX - srcX;
        } else {
            dx = 0;
        }

        //If the image height after scale is smaller than view height, move the image to center align
        if (currRect.height() <= viewHeight) {
            float srcY = currRect.top;
            float dstY = (viewHeight / 2) - (currRect.height() / 2);
            dy = dstY - srcY;
        } else {
            dy = 0;
        }

        mMatrix.postTranslate(dx, dy);
        setImageMatrix(mMatrix);
    }

    private boolean allowParentInterceptTouchEvent() {
        float nowScale = getScale();
        return (nowScale <= mMinRate * MAX_RANGE_ALLOW_INTERCEPT && nowScale >= mMinRate * MIN_RANGE_ALLOW_INTERCEPT) &&
                !mImageAnimator.isAnimationRunning();
    }

    private void initial() {

        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent e) {

                AnimationOption option = new AnimationOption();
                option.setView(ScaleImageView.this);
                option.setDuration(300);
                option.setScaleFocusX(MotionEventCompat.getX(e, 0));
                option.setScaleFocusY(MotionEventCompat.getY(e, 0));

                float nowScale = getScale();
                option.setSrcRate(nowScale);

                if (nowScale > mMinRate) {
                    option.setDstRate(mMinRate);
                } else {
                    option.setDstRate(mMidRate);
                }

                mImageAnimator.animate(option);
                return true;
            }

        });

        mDragDetector = new DragDetector(getContext(), this);
        mScaleDetector = new ScaleDetector(getContext(), this);

        mFlingAnimator = new FlingAnimator(getContext(), this);
        mImageAnimator = new ImageAnimator(this);

        setScaleType(ScaleType.MATRIX);
        mMatrix = new Matrix();
        setImageMatrix(mMatrix);
    }

    private RectF getRect(Matrix matrix) {
        RectF rect = new RectF();
        Drawable d = getDrawable();
        if (d != null) {
            rect.set(0, 0, d.getIntrinsicWidth(),
                    d.getIntrinsicHeight());
            matrix.mapRect(rect);
            return rect;
        }
        return null;
    }

    private void updateBounds() {
        RectF baseRect = getRect(mBaseMatrix);
        RectF rect = getRect(mMatrix);

        int viewWidth = getMeasuredWidth();
        int viewHeight = getMeasuredHeight();

        leftBound = rect.width() > viewWidth ? 0 : baseRect.left;
        topBound = rect.height() > viewHeight ? 0 : baseRect.top;
        rightBound = rect.width() > viewWidth ? viewWidth : baseRect.right;
        bottomBound = rect.height() > viewHeight ? viewHeight : baseRect.bottom;
    }

    private void resetRateIfNeed() {
        float scale = getScale();

        AnimationOption option = new AnimationOption();
        option.setView(this);
        option.setDuration(300);

        boolean resetRate = scale > mMaxRate || scale < mMinRate || allowParentInterceptTouchEvent();

        if (resetRate) {
            option.setSrcRate(scale);
            option.setDstRate(scale > mMaxRate ? mMaxRate : mMinRate);
        }

        if (resetRate) {
            mImageAnimator.animate(option);
        }
    }

    private float getScale() {
        float values[] = new float[9];
        mMatrix.getValues(values);
        return (float) Math.sqrt(values[Matrix.MSCALE_X] * values[Matrix.MSCALE_X]
                + values[Matrix.MSKEW_Y] * values[Matrix.MSKEW_Y]);
    }

    //http://www.cnblogs.com/plokmju/p/android_Matrix.html
    public void rotate (float angle){
        mMatrix.postRotate(angle);
        setImageMatrix(mMatrix);
    }
}
