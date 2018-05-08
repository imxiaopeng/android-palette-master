package com.beyondsw.palette;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by Maurice on 2017/6/7.
 * email:zhang_mingxu@126.com
 */

public class ZoomView extends View {
    private static final int FACTOR = 2;
//    private static final int RADIUS = 150;
    private static final int RADIUS = 100;
    private final int width;
    private final int height;
    private ShapeDrawable mShapeDrawable;
    private Bitmap mBitmap;
    private Bitmap mBitmapScale;
    private Matrix mMatrix;

    public ZoomView(Context context) {
        super(context);
        WindowManager manager= (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        width = manager.getDefaultDisplay().getWidth();
        height = manager.getDefaultDisplay().getHeight();
        mShapeDrawable = new ShapeDrawable(new OvalShape());
        mMatrix = new Matrix();
    }

    public void setInitCurBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        mBitmapScale = mBitmap;
        mBitmapScale = Bitmap.createScaledBitmap(mBitmapScale,
                mBitmapScale.getWidth() * FACTOR, mBitmapScale.getHeight() * FACTOR, true);
        BitmapShader bitmapShader = new BitmapShader(mBitmapScale
                , Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mShapeDrawable.getPaint().setShader(bitmapShader);
        mShapeDrawable.setBounds(0, 0, 0, 0);
        invalidate();
    }

    public void setCurShowPos(int x, int y) {
        mMatrix.setTranslate(RADIUS - x * FACTOR, RADIUS - y * FACTOR);
        mShapeDrawable.getPaint().getShader().setLocalMatrix(mMatrix);
        int left=0,top=0,right=0,bottom=0;
        if(y<RADIUS){
            top=0;
            bottom=RADIUS *2;
        }else if((height-y)<RADIUS){
            top= height-RADIUS *2;
            bottom=height;
        }else{
            top= y - 3 * RADIUS / 2;
            bottom=y + RADIUS / 2;
        }
        if(x<RADIUS){
            left=0;
            right=RADIUS*2;
        }else if((width-x)<RADIUS){
            left=width - RADIUS * 2;
            right=width;
        }else{
            left=x - RADIUS / 2;
            right=x + 3 * RADIUS / 2;
        }
        mShapeDrawable.setBounds(left,top,right,bottom);
        /*mShapeDrawable.setBounds(x - RADIUS / 2, y - 3 * RADIUS / 2
                , x + 3 * RADIUS / 2, y + RADIUS / 2);*/
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, null);

        //画放大镜
        mShapeDrawable.draw(canvas);
    }

}
