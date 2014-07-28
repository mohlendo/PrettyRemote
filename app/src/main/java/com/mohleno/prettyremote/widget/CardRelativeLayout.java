package com.mohleno.prettyremote.widget;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.mohleno.prettyremote.R;


@SuppressWarnings("UnusedDeclaration")
public class CardRelativeLayout extends RelativeLayout {
    public CardRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        final float radius = getResources().getDimensionPixelSize(R.dimen.card_corner_radius);

        Outline outline = new Outline();
        outline.setRoundRect(0, 0, w, h, radius);

        setOutline(outline);
        setClipToOutline(true);
    }
}
