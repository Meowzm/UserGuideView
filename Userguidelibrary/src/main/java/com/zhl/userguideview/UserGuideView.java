package com.zhl.userguideview;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.zhl.userguideview.userguidelibrary.R;


/**
 * 描述：一个用于“应用新特性”的用户指引view
 * Created by zhaohl on 2015-11-26.
 */
public class UserGuideView extends View {
    public static final int VIEWSTYLE_RECT=0;
    public static final int VIEWSTYLE_CIRCLE=1;
    public static final int VIEWSTYLE_OVAL=2;
    private Bitmap fgBitmap;// 前景
    private Bitmap jtUpLeft,jtUpRight,jtDownRight,jtDownLeft;// 指示箭头
    private Canvas mCanvas;// 绘制蒙版层的画布
    private Paint mPaint;// 绘制蒙版层画笔
    private int screenW, screenH;// 屏幕宽高
    private View targetView;
    private boolean touchOutsideCancel = true;
    private int borderWitdh=10;
    private int margin=40;
    private int highLightStyle = VIEWSTYLE_RECT;
    private Bitmap tipBitmap;
    private int radius;
    private int maskColor = 0x99000000;// 蒙版层颜色
    private Activity activity;
    private OnDismissListener onDismissListener;
    private int statusBarHeight = 66;// 状态栏高度

    public UserGuideView(Context context){
        this(context,null);
    }
    public UserGuideView(Context context, AttributeSet set) {
        this(context, set, -1);
    }

    public UserGuideView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context,attrs,defStyleAttr);
        if(attrs!=null){
            TypedArray array =  context.obtainStyledAttributes(attrs, R.styleable.UserGuideView);
            highLightStyle = array.getInt(R.styleable.UserGuideView_HighlightViewStyle, VIEWSTYLE_RECT);
            BitmapDrawable drawable = (BitmapDrawable) array.getDrawable(R.styleable.UserGuideView_tipView);
            maskColor = array.getColor(R.styleable.UserGuideView_maskColor,maskColor);
            if(drawable!=null){
                tipBitmap = drawable.getBitmap();
            }
            array.recycle();
        }
        // 计算参数
        cal(context);

        // 初始化对象
        init(context);
    }

    /**
     * 计算参数
     *
     * @param context
     *            上下文环境引用
     */
    private void cal(Context context) {
        // 获取屏幕尺寸数组
        int[] screenSize = MeasureUtil.getScreenSize((Activity) context);

        // 获取屏幕宽高
        screenW = screenSize[0];
        screenH = screenSize[1];
    }

    /**
     * 初始化对象
     */
    private void init(Context context) {

        // 关闭硬件加速
//        setLayerType(LAYER_TYPE_SOFTWARE,null);
        // 实例化画笔并开启其抗锯齿和抗抖动
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

        // 设置画笔透明度为0是关键！
        mPaint.setARGB(0, 255, 0, 0);
        // 设置混合模式为DST_IN
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        mPaint.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.SOLID));

        // 生成前景图Bitmap
        fgBitmap = Bitmap.createBitmap(screenW, screenH, Bitmap.Config.ARGB_8888);

        // 将其注入画布
        mCanvas = new Canvas(fgBitmap);

        // 绘制前景画布
        mCanvas.drawColor(maskColor);

        // 实例化箭头图片
        jtDownRight = BitmapFactory.decodeResource(getResources(), R.drawable.jt_down_right);
        jtDownLeft = BitmapFactory.decodeResource(getResources(), R.drawable.jt_down_left);
        jtUpLeft = BitmapFactory.decodeResource(getResources(), R.drawable.jt_up_left);
        jtUpRight = BitmapFactory.decodeResource(getResources(), R.drawable.jt_up_right);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(targetView==null){
            return;
        }
        // 绘制前景
        canvas.drawBitmap(fgBitmap, 0, 0, null);

        int left = targetView.getLeft();
        int top = targetView.getTop();
        int right = 0;
        int bottom = 0;
        int vWidth = targetView.getWidth();
        int vHeight = targetView.getHeight();

        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View temp = targetView;
        while(temp!=decorView){
            temp = (View) temp.getParent();
            left+=temp.getLeft();
            top+=temp.getTop();
        }
        top -= statusBarHeight;
        right = left+vWidth;
        bottom = top+vHeight;

        switch (highLightStyle){
            case VIEWSTYLE_RECT:
                RectF rect = new RectF(left-borderWitdh,top-borderWitdh,right+borderWitdh,bottom+borderWitdh);
                mCanvas.drawRoundRect(rect, 20, 20, mPaint);
                break;
            case VIEWSTYLE_CIRCLE:
                radius = vWidth > vHeight ? vWidth / 2 -100: vHeight / 2-100;
                if(radius<50){
                    radius = 100;
                }
                mCanvas.drawCircle(left+vWidth / 2, top+vHeight / 2,radius, mPaint);
                break;
            case VIEWSTYLE_OVAL:
                RectF rectf = new RectF(left,top,right,bottom);
                mCanvas.drawOval(rectf, mPaint);
                break;

        }

        if(bottom<screenH/2||(screenH/2-top>bottom-screenH/2)){// 偏上
            int jtTop = highLightStyle==VIEWSTYLE_CIRCLE?bottom+radius-margin:bottom+margin;
            if(right<screenW/2||(screenW/2-left>right-screenW/2)){//偏左
                canvas.drawBitmap(jtUpLeft,left+vWidth/2,jtTop,null);
                if(tipBitmap!=null){
                    canvas.drawBitmap(tipBitmap,left+vWidth/2,jtTop+jtUpLeft.getHeight(),null);
                }
            }else{
                canvas.drawBitmap(jtUpRight,left+vWidth/2-100-margin,jtTop,null);
                if(tipBitmap!=null){
                    canvas.drawBitmap(tipBitmap,left+vWidth/2-100-tipBitmap.getWidth()/2,jtTop+jtUpRight.getHeight(),null);
                }
            }
        }else{
            int jtTop = highLightStyle==VIEWSTYLE_CIRCLE?top-radius-margin:top - jtDownLeft.getHeight()-margin;
            if(right<screenW/2||(screenW/2-left>right-screenW/2)){
                canvas.drawBitmap(jtDownLeft, left+vWidth / 2, jtTop,null);
                if(tipBitmap!=null){
                    canvas.drawBitmap(tipBitmap,left+vWidth/2,jtTop-tipBitmap.getHeight(),null);
                }
            }else{
                canvas.drawBitmap(jtDownRight, left+vWidth / 2-100-margin, jtTop,null);
                if(tipBitmap!=null){
                    canvas.drawBitmap(tipBitmap,left+vWidth/2-100-tipBitmap.getWidth()/2-margin,jtTop-tipBitmap.getHeight(),null);
                }
            }
        }


    }

    /**
     * 设置需要高亮的View
     * @param activty
     * @param targetView
     */
    public void setHighLightView(Activity activty,View targetView){
            this.activity = activty;
            this.targetView = targetView;
            invalidate();
    }
    public void setTouchOutsideDismiss(boolean cancel){
        this.touchOutsideCancel = cancel;
    }

    /**
     * 设置提示的图片
     * @param bitmap
     */
    public void setTipView(Bitmap bitmap){
        this.tipBitmap = bitmap;
    }

    /**
     * 设置蒙版颜色
     * @param maskColor
     */
    public void setMaskColor(int maskColor){
        this.maskColor = maskColor;
    }

    /**
     * 设置状态栏高度 默认是减去了一个状态栏高度 如果主题设置android:windowTranslucentStatus=true
     * 需要设置状态栏高度为0
     * @param statusBarHeight
     */
    public void setStatusBarHeight(int statusBarHeight){
        this.statusBarHeight = statusBarHeight;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP://
                if(touchOutsideCancel){
                    this.setVisibility(View.GONE);
                    if(this.onDismissListener!=null){
                        onDismissListener.onDismiss();
                    }
                    return true;
                }
                break;
        }
        return true;
    }

    public void setOnDismissListener(OnDismissListener listener){
        this.onDismissListener = listener;
    }

    public interface OnDismissListener{
        public void onDismiss();
    }
}
