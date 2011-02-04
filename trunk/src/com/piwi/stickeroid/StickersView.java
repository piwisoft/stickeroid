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
import android.view.MotionEvent;
import android.view.View;

public class StickersView extends View
{
    private static final float MOVE_THRESHOLD = 15;

    private static final int MISSING_COLOR = Color.RED;

    private static final int NORMAL_COLOR = Color.rgb(148, 239, 148);

    private static final int DUPLICATED_COLOR = Color.rgb(0, 132, 0);

    private static final int SELECTED_COLOR = Color.argb(128, 255, 255, 255);

    private byte[] mBytes;

    private int[] mIndexes;

    private int mPosition;

    private int mNbMissing;

    private int mNbDuplicated;

    private int mNbTotalDuplicated;

    private Paint mPaint = new Paint();

    private int mItemW;

    private int mItemH;

    private int mOffsetX;

    private int mOffsetY;

    private int mNbRows;

    private int mNbColumns;

    private int mNbDisplayable;

    private int mFilterType;

    private int mNbDisplayed;

    private int mFocusedItem;

    private PropertyChangeSupport mPropertyChangeSupport;

    private PropertyChangeEvent mDummyEvent;

    private float mStartX;

    private float mStartY;

    private int mStartOffsetY;

    private int mMinOffsetY;

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

        int pos = mPosition;

        for(int r = 0; r < mNbRows; r++)
        {
            int y = mOffsetY + r * h;

            for(int c = 0; c < mNbColumns; c++)
            {
                int x = mOffsetX + c * w;

                if(x + w >= 0 && y + h >= 0)
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
        float x = event.getX();
        float y = event.getY();

        switch(event.getAction())
        {
        case MotionEvent.ACTION_DOWN:
            // Memorize start point
            mStartX = x;
            mStartY = y;
            mStartOffsetY = mOffsetY;
            break;

        case MotionEvent.ACTION_MOVE:
            if(Math.abs(y - mStartY) > MOVE_THRESHOLD)
            {
                mOffsetY = (int) (mStartOffsetY + (y - mStartY));
                if(mOffsetY > 0)
                {
                    mOffsetY = 0;
                }
                if(mOffsetY < mMinOffsetY)
                {
                    mOffsetY = mMinOffsetY;
                }
                invalidate();
            }
            break;

        case MotionEvent.ACTION_UP:
            // Check if we have move or not
            if(Math.abs(y - mStartY) < MOVE_THRESHOLD)
            {
                mFocusedItem = -1;

                int column = (int) ((event.getX() - mOffsetX) / mItemW);
                int row = (int) ((event.getY() - mOffsetY) / mItemH);
                if(column >= 0 && column < mNbColumns && row >= 0 && row < mNbRows)
                {
                    mFocusedItem = mPosition + column + row * mNbColumns;
                }

                if(mFocusedItem >= mNbDisplayed)
                {
                    mFocusedItem = -1;
                }

                mPropertyChangeSupport.firePropertyChange(mDummyEvent);
                invalidate();
            }
            break;
        }
        return true;
    }

    public void init(byte[] d, int position, int filter)
    {
        mBytes = d;
        mIndexes = new int[mBytes.length];

        mPosition = position;
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
        return mPosition;
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        float tw = mPaint.measureText("000+00");

        float density = getContext().getResources().getDisplayMetrics().density;
        int minSize = (int) (45.0f * density + 0.5f);

        mItemH = Math.max(minSize, (int) (2.0f * (tw + 4.0f) / 3.0f + 0.5f));
        mItemW = (int) (3.0f * mItemH / 2.0f + 0.5f);
        mNbColumns = w / mItemW;
        mNbRows = h / mItemH;

        mNbDisplayable = mNbRows * mNbColumns;

        mItemW = w / mNbColumns;
        mItemH = h / mNbRows;

        mOffsetX = (w - mNbColumns * mItemW) / 2;
        mOffsetY = (h - mNbRows * mItemH) / 2;

        mPosition = (mIndexes[mPosition] / mNbDisplayable) * mNbDisplayable;

        computeGrid();

        mPropertyChangeSupport.firePropertyChange(mDummyEvent);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect)
    {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if(gainFocus)
        {
            if(direction == FOCUS_DOWN || direction == FOCUS_RIGHT)
            {
                mFocusedItem = mPosition;
            }
            else if(direction == FOCUS_UP)
            {
                mFocusedItem = Math.min(mPosition + mNbDisplayable - 1, mNbDisplayed - 1);
            }
            else
            {
                mFocusedItem = Math.min(mPosition + mNbColumns - 1, mNbDisplayed - 1);
            }
        }
        else
        {
            mFocusedItem = -1;
        }

        mPropertyChangeSupport.firePropertyChange(mDummyEvent);

        invalidate();
    }

    private void init()
    {
        setFocusable(true);

        mPropertyChangeSupport = new PropertyChangeSupport(this);
        mDummyEvent = new PropertyChangeEvent(this, "", null, null);

        float density = getContext().getResources().getDisplayMetrics().density;
        mPaint.setTextSize(15.0f * density);
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
            computeGrid();
        }

        mPropertyChangeSupport.firePropertyChange(mDummyEvent);
    }

    private void computeGrid()
    {
        if(mItemW > 0 && mItemH > 0)
        {
            int w = getWidth();
            int h = getHeight();

            mNbColumns = w / mItemW;
            mNbRows = (int) Math.ceil((double) mNbDisplayed / (double) mNbColumns);

            mItemW = w / mNbColumns;

            mOffsetX = (w - mNbColumns * mItemW) / 2;
            mOffsetY = 0;

            int nbRowsDisplayed = h / mItemH;
            if(nbRowsDisplayed < mNbRows)
            {
                mMinOffsetY = -(mNbRows - nbRowsDisplayed) * mItemH;
            }
            else
            {
                mMinOffsetY = 0;
            }
        }
    }
}
