/*
 * Copyright (c) 2010, Sony Ericsson Mobile Communication AB. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 *    * Redistributions of source code must retain the above copyright notice, this 
 *      list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 *    * Neither the name of the Sony Ericsson Mobile Communication AB nor the names
 *      of its contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

package com.dslr.dashboard.imgzoom;

import java.util.Currency;

import android.content.Context;
import android.os.Vibrator;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Listener for controlling zoom state through touch events
 */
public class LongPressZoomListener implements View.OnTouchListener {

	private static String TAG = "LongPressZoomListener";
    /**
     * Enum defining listener modes. Before the view is touched the listener is
     * in the UNDEFINED mode. Once touch starts it can enter either one of the
     * other two modes: If the user scrolls over the view the listener will
     * enter PAN mode, if the user lets his finger rest and makes a longpress
     * the listener will enter ZOOM mode.
     */
    private enum Mode {
        UNDEFINED, PAN, ZOOM, PINCH
    }

    /** Time of tactile feedback vibration when entering zoom mode */
    private static final long VIBRATE_TIME = 50;

    /** Current listener mode */
    private Mode mMode = Mode.UNDEFINED;

    /** Zoom control to manipulate */
    private DynamicZoomControl mZoomControl;

    /** X-coordinate of previously handled touch event */
    private float mX;

    /** Y-coordinate of previously handled touch event */
    private float mY;

    /** X-coordinate of latest down event */
    private float mDownX;

    /** Y-coordinate of latest down event */
    private float mDownY;

    /** Velocity tracker for touch events */
    private VelocityTracker mVelocityTracker;

    /** Distance touch can wander before we think it's scrolling */
    private final int mScaledTouchSlop;

    /** Duration in ms before a press turns into a long press */
    private final int mLongPressTimeout;

    /** Vibrator for tactile feedback */
    private final Vibrator mVibrator;

    /** Maximum velocity for fling */
    private final int mScaledMaximumFlingVelocity;

    float dist0, distCurrent;
    
    /**
     * Creates a new instance
     * 
     * @param context Application context
     */
    public LongPressZoomListener(Context context) {
        mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScaledMaximumFlingVelocity = ViewConfiguration.get(context)
                .getScaledMaximumFlingVelocity();
        mVibrator = (Vibrator)context.getSystemService("vibrator");
    }

    /**
     * Sets the zoom control to manipulate
     * 
     * @param control Zoom control
     */
    public void setZoomControl(DynamicZoomControl control) {
        mZoomControl = control;
    }

    /**
     * Runnable that enters zoom mode
     */
    private final Runnable mLongPressRunnable = new Runnable() {
        public void run() {
//            mMode = Mode.ZOOM;
//            mVibrator.vibrate(VIBRATE_TIME);
        }
    };

    // implements View.OnTouchListener
    public boolean onTouch(View v, MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        final float x = event.getX();
        final float y = event.getY();
        
        float distx, disty;
        
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mZoomControl.stopFling();
                v.postDelayed(mLongPressRunnable, mLongPressTimeout);
                mDownX = x;
                mDownY = y;
                mX = x;
                mY = y;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
            	   //Get the distance when the second pointer touch
            	mMode = Mode.PINCH;
                distx = Math.abs(event.getX(0) - event.getX(1));
                disty = Math.abs(event.getY(0) - event.getY(1));
                dist0 = FloatMath.sqrt((distx * distx) + (disty * disty));
                Log.d(TAG, "Pinch mode");
          	break;
            case MotionEvent.ACTION_POINTER_UP:
            	mMode = Mode.PAN;
            	break;
            case MotionEvent.ACTION_MOVE: {
                final float dx = (x - mX) / v.getWidth();
                final float dy = (y - mY) / v.getHeight();

                if (mMode == Mode.ZOOM) {
                	//Log.d(TAG, "dy: " + dy + "zoom: " + (float)Math.pow(20, -dy));
                    mZoomControl.zoom((float)Math.pow(20, -dy), mDownX / v.getWidth(), mDownY
                            / v.getHeight());
                } else if (mMode == Mode.PAN) {
                    mZoomControl.pan(-dx, -dy);
                } else if (mMode == Mode.PINCH) {
                    //Get the current distance
                    distx = Math.abs(event.getX(0) - event.getX(1));
                    disty = Math.abs(event.getY(0) - event.getY(1));
                    distCurrent = FloatMath.sqrt((distx * distx) + (disty * disty));
                    float factor = (distCurrent / dist0) * 2f;
                    //Log.d(TAG, "Pinch distance: "+  distCurrent + "rate: " + (distCurrent/dist0));
                    mZoomControl.zoom(factor, mDownX / v.getWidth(), mDownY / v.getHeight());
//                    mZoomControl.zoom((float)Math.pow(1.5, (distCurrent - dist0)/v.getWidth()), mDownX / v.getWidth(), mDownY
//                            / v.getHeight());
                } else {
                    final float scrollX = mDownX - x;
                    final float scrollY = mDownY - y;

                    final float dist = (float)Math.sqrt(scrollX * scrollX + scrollY * scrollY);

                    if (dist >= mScaledTouchSlop) {
                        v.removeCallbacks(mLongPressRunnable);
                        mMode = Mode.PAN;
                    }
                }

                mX = x;
                mY = y;
                break;
            }

            case MotionEvent.ACTION_UP:
                if (mMode == Mode.PAN) {
                    mVelocityTracker.computeCurrentVelocity(1000, mScaledMaximumFlingVelocity);
                    mZoomControl.startFling(-mVelocityTracker.getXVelocity() / v.getWidth(),
                            -mVelocityTracker.getYVelocity() / v.getHeight());
                } else if (mMode == Mode.PINCH) {
                	
                } else {
                    mZoomControl.startFling(0, 0);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                v.removeCallbacks(mLongPressRunnable);
                mMode = Mode.UNDEFINED;
                break;

            default:
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                v.removeCallbacks(mLongPressRunnable);
                mMode = Mode.UNDEFINED;
                break;

        }

        return true;
    }

}
