package com.example.songle;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * BottomNav
 * Created by Suleiman19 on 6/12/17, used by Paul on 21/11/2017.
 * From https://github.com/Suleiman19/Bottom-Navigation-Demo/blob/master/app/src/main/java/com/grafixartist/bottomnav/NoSwipePager.java
 * Copyright (c) 2017. Suleiman Ali Shakir. All rights reserved.
 */

public class NoSwipePager extends ViewPager {
    private boolean enabled;

    public NoSwipePager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.enabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.enabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.enabled && super.onInterceptTouchEvent(event);
    }

    public void setPagingEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}