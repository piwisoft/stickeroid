package com.piwi.stickeroid;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class StickersView extends View implements GestureDetector.OnGestureListener
{
    private static final int MISSING_COLOR = Color.RED;

    private static final int NORMAL_COLOR = Color.rgb(148, 239, 148);

    private static final int DUPLICATED_COLOR = Color.rgb(0, 132, 0);

    private static final int SELECTED_COLOR = Color.argb(128, 255, 255, 255);

    private byte[] mBytes;

    private int[] mIndexes;

    private int mNbMissing;

    private int mNbDuplicated;

    private int mNbTotalDuplicated;

    private Paint mPaint;

    private int mItemW;

    private int mItemH;

    private int mOffsetX;

    private int mOffsetY;

    private int mNbRows;

    private int mNbColumns;

    private int mFilterType;

    private int mNbDisplayed;

    private int mFocusedItem;

    private PropertyChangeSupport mPropertyChangeSupport;

    private PropertyChangeEvent mDummyEvent;

    private GestureDetector mGestureDetector;
    
    private int mMinItemHeight;

    public StickersView(Context context)
    {
        super(context);

        init();
    }

    public StickersView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        init();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if(mNbDisplayed == 0)
        {
            return;
        }

        mPaint.setColor(Color.GRAY);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setTextAlign(Paint.Align.CENTER);

        int to = (int) (mPaint.ascent() / 2);

        Rect mEltRect = new Rect(0, 0, mItemW, mItemH);

        int w = mEltRect.width();
        int h = mEltRect.height();

        int pos = 0;
        int ymin = getScrollY();
        int ymax = ymin + getHeight();

        for(int r = 0; r < mNbRows; r++)
        {
            int y = mOffsetY + r * h;

            for(int c = 0; c < mNbColumns; c++)
            {
                int x = mOffsetX + c * w;

                if(y + h >= ymin && y <= ymax)
                {
                    int idx = mIndexes[pos];
                    byte val = mBytes[idx];

                    mEltRect.offsetTo(x, y);

                    mPaint.setStyle(Paint.Style.FILL);
                    if(val == 0)
                    {
                        mPaint.setColor(MISSING_COLOR);
                    }
                    else if(val == 1)
                    {
                        mPaint.setColor(NORMAL_COLOR);
                    }
                    else
                    {
                        mPaint.setColor(DUPLICATED_COLOR);
                    }

                    canvas.drawRect(mEltRect, mPaint);

                    if(pos == mFocusedItem)
                    {
                        mPaint.setColor(SELECTED_COLOR);
                        canvas.drawRect(mEltRect, mPaint);
                    }

                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setColor(Color.BLACK);
                    canvas.drawRect(mEltRect, mPaint);

                    String text = Integer.toString(idx + 1);
                    if(val > 1)
                    {
                        StringBuilder sb = new StringBuilder();
                        sb.append(text);
                        sb.append('+');
                        sb.append(val - 1);
                        text = sb.toString();
                    }

                    canvas.drawText(text, mEltRect.centerX(), mEltRect.centerY() - to, mPaint);
                }

                pos++;

                if(pos == mNbDisplayed)
                {
                    break;
                }
            }

            if(pos == mNbDisplayed)
            {
                break;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    public void init(byte[] d, int pos, int filter)
    {
        mBytes = d;
        mIndexes = new int[mBytes.length];

        scrollTo(0, pos);
        
        mFocusedItem = -1;
        mFilterType = filter;

        updateIndexes();

        invalidate();
    }

    public boolean canIncrement()
    {
        return mFocusedItem >= 0 && mBytes[mIndexes[mFocusedItem]] < 99;
    }

    public boolean canDecrement()
    {
        return mFocusedItem >= 0 && mBytes[mIndexes[mFocusedItem]] > 0;
    }

    public void increment()
    {
        if(canIncrement())
        {
            mBytes[mIndexes[mFocusedItem]]++;
            updateIndexes();
            invalidate();
        }
    }

    public void decrement()
    {
        if(canDecrement())
        {
            mBytes[mIndexes[mFocusedItem]]--;
            updateIndexes();
            invalidate();
        }
    }

    public int getPosition()
    {
        return getScrollY();
    }

    public int getFilterType()
    {
        return mFilterType;
    }

    public void setFilterType(int type)
    {
        if(mFilterType != type)
        {
            mFilterType = type;
            updateIndexes();
            invalidate();
        }
    }

    public int getNbMissing()
    {
        return mNbMissing;
    }

    public int getNbDuplicated()
    {
        return mNbDuplicated;
    }

    public int getTotalNbDuplicated()
    {
        return mNbTotalDuplicated;
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        mPropertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        mPropertyChangeSupport.removePropertyChangeListener(l);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int w = measureWidth(widthMeasureSpec);
        int h = measureHeight(heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    public boolean onDown(MotionEvent e)
    {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e)
    {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e)
    {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        mFocusedItem = -1;

        int column = (int) ((e.getX() - mOffsetX) / mItemW);
        int row = (int) ((getScrollY() + e.getY() - mOffsetY) / mItemH);
        if(column >= 0 && column < mNbColumns && row >= 0 && row < mNbRows)
        {
            mFocusedItem =  column + row * mNbColumns;
        }

        if(mFocusedItem >= mNbDisplayed)
        {
            mFocusedItem = -1;
        }

        mPropertyChangeSupport.firePropertyChange(mDummyEvent);
        invalidate();

        return true;
    }

    private void init()
    {
        mGestureDetector = new GestureDetector(this);

        setFocusable(true);

        mPropertyChangeSupport = new PropertyChangeSupport(this);
        mDummyEvent = new PropertyChangeEvent(this, "", null, null);

        mPaint = new Paint();
        float density = getContext().getResources().getDisplayMetrics().density;
        mPaint.setTextSize(15.0f * density);
        
        float tw = mPaint.measureText("000+00") + 4.0f;
        int minSize = (int) (45.0f * density + 0.5f);

        mMinItemHeight = Math.max(minSize, (int) (2.0f * tw / 3.0f + 0.5f));
    }

    /**
     * Determines the width of this view
     * 
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec)
    {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        mItemW = (int) (3.0f * mMinItemHeight / 2.0f + 0.5f);
        
        if(specMode == MeasureSpec.EXACTLY)
        {
            // We were told how big to be
            result = specSize;
        }
        else
        {
            result = 4 * mItemW;
            
            if(specMode == MeasureSpec.AT_MOST)
            {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }

        mNbColumns = result / mItemW;
        mItemW = result / mNbColumns;
        mOffsetX = (result - mNbColumns * mItemW) / 2;
        
        return result;
    }

    /**
     * Determines the height of this view
     * 
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec)
    {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        mItemH = mMinItemHeight;
        
        if(specMode == MeasureSpec.EXACTLY)
        {
            // We were told how big to be
            result = specSize;
        }
        else
        {
            result = (mNbDisplayed / 4) * mItemH;
            
            if(specMode == MeasureSpec.AT_MOST)
            {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        
        mNbRows = result / mItemH;
        mItemH = result / mNbRows;
        mOffsetY = (result - mNbRows * mItemH) / 2;
        
        return result;
    }

    private void updateIndexes()
    {
        int oldNb = mNbDisplayed;

        mNbDuplicated = 0;
        mNbMissing = 0;
        mNbTotalDuplicated = 0;

        int focus = -1;
        if(mFocusedItem >= 0)
        {
            focus = mIndexes[mFocusedItem];
            mFocusedItem = -1;
        }

        int idxPos = 0;
        int srcPos = 0;
        for(byte b : mBytes)
        {
            if(b > 1)
            {
                mNbDuplicated++;
                mNbTotalDuplicated += b - 1;
                if(mFilterType == Collection.DISPLAY_DUPLICATED
                        || mFilterType == Collection.NO_FILTER)
                {
                    mIndexes[idxPos] = srcPos;
                    if(focus == srcPos)
                    {
                        mFocusedItem = idxPos;
                    }
                    idxPos++;
                }
            }
            else if(b == 0)
            {
                mNbMissing++;
                if(mFilterType == Collection.DISPLAY_MISSING || mFilterType == Collection.NO_FILTER)
                {
                    mIndexes[idxPos] = srcPos;
                    if(focus == srcPos)
                    {
                        mFocusedItem = idxPos;
                    }
                    idxPos++;
                }
            }
            else
            {
                if(mFilterType == Collection.NO_FILTER)
                {
                    mIndexes[idxPos] = srcPos;
                    if(focus == srcPos)
                    {
                        mFocusedItem = idxPos;
                    }
                    idxPos++;
                }
            }
            srcPos++;
        }

        if(mFilterType == Collection.DISPLAY_DUPLICATED)
        {
            mNbDisplayed = mNbDuplicated;
        }
        else if(mFilterType == Collection.DISPLAY_MISSING)
        {
            mNbDisplayed = mNbMissing;
        }
        else
        {
            mNbDisplayed = mBytes.length;
        }

        if(oldNb != mNbDisplayed)
        {
            requestLayout();
        }

        mPropertyChangeSupport.firePropertyChange(mDummyEvent);
    }
}
