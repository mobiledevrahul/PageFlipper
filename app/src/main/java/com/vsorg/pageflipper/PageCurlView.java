package com.vsorg.pageflipper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class PageCurlView extends View {

	private Paint mTextPaint;
	private TextPaint mTextPaintShadow;
	

	private int mCurlSpeed;
	

	private int mUpdateRate;
	

	private int mInitialEdgeOffset;
	

	private int mCurlMode;
	

	public static final int CURLMODE_SIMPLE = 0;
	

	public static final int CURLMODE_DYNAMIC = 1;
	

	private boolean bEnableDebugMode = false;
	

	private WeakReference<Context> mContext;
	

	private FlipAnimationHandler mAnimationHandler;
	

	private float mFlipRadius;
	

	private Vector2D mMovement;
	

	private Vector2D mFinger;
	

	private Vector2D mOldMovement;
	

	private Paint mCurlEdgePaint;
	

	private Vector2D mA, mB, mC, mD, mE, mF, mOldF, mOrigin;
	

	private int mCurrentLeft, mCurrentTop;
	

	private boolean bViewDrawn;
	

	private boolean bFlipRight;
	

	private boolean bFlipping;
	

	private boolean bUserMoves;


	private boolean bBlockTouchInput = false;
	

	private boolean bEnableInputAfterDraw = false;
	

	private Bitmap mForeground;
	

	private Bitmap mBackground;
	

	private ArrayList<Bitmap> mPages;

	private int mIndex = 0;
	

	private class Vector2D
	{
		public float x,y;
		public Vector2D(float x, float y)
		{
			this.x = x;
			this.y = y;
		}
		
		@Override
		public String toString() {
			return "("+this.x+","+this.y+")";
		}
		
		 public float length() {
             return (float) Math.sqrt(x * x + y * y);
	     }
	
	     public float lengthSquared() {
	             return (x * x) + (y * y);
	     }
		
		public boolean equals(Object o) {
			if (o instanceof Vector2D) {
				Vector2D p = (Vector2D) o;
				return p.x == x && p.y == y;
	        }
	        return false;
		}
		
		public Vector2D reverse() {
			return new Vector2D(-x,-y);
		}
		
		public Vector2D sum(Vector2D b) {
            return new Vector2D(x+b.x,y+b.y);
		}
		
		public Vector2D sub(Vector2D b) {
            return new Vector2D(x-b.x,y-b.y);
		}		

		public float dot(Vector2D vec) {
            return (x * vec.x) + (y * vec.y);
		}

	    public float cross(Vector2D a, Vector2D b) {
	            return a.cross(b);
	    }
	
	    public float cross(Vector2D vec) {
	            return x * vec.y - y * vec.x;
	    }
	    
	    public float distanceSquared(Vector2D other) {
	    	float dx = other.x - x;
	    	float dy = other.y - y;

            return (dx * dx) + (dy * dy);
	    }
	
	    public float distance(Vector2D other) {
	            return (float) Math.sqrt(distanceSquared(other));
	    }
	    
	    public float dotProduct(Vector2D other) {
            return other.x * x + other.y * y;
	    }
		
		public Vector2D normalize() {
			float magnitude = (float) Math.sqrt(dotProduct(this));
            return new Vector2D(x / magnitude, y / magnitude);
		}
		
		public Vector2D mult(float scalar) {
	            return new Vector2D(x*scalar,y*scalar);
	    }
	}

	class FlipAnimationHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			PageCurlView.this.FlipAnimationStep();
		}

		public void sleep(long millis) {
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), millis);
		}
	}
    

	public PageCurlView(Context context) {
		super(context);
		init(context);
		ResetClipEdge();
	}

	public PageCurlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
		

		{
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PageCurlView);
	

			bEnableDebugMode = a.getBoolean(R.styleable.PageCurlView_enableDebugMode, bEnableDebugMode);
			mCurlSpeed = a.getInt(R.styleable.PageCurlView_curlSpeed, mCurlSpeed);
			mUpdateRate = a.getInt(R.styleable.PageCurlView_updateRate, mUpdateRate);
			mInitialEdgeOffset = a.getInt(R.styleable.PageCurlView_initialEdgeOffset, mInitialEdgeOffset);
			mCurlMode = a.getInt(R.styleable.PageCurlView_curlMode, mCurlMode);
			

			a.recycle();
		}
		
		ResetClipEdge();
	}
	

	private final void init(Context context) {

		mTextPaint = new Paint();
		mTextPaint.setAntiAlias(true);
		mTextPaint.setTextSize(16);
		mTextPaint.setColor(0xFF000000);
		

		mTextPaintShadow = new TextPaint();
		mTextPaintShadow.setAntiAlias(true);
		mTextPaintShadow.setTextSize(16);
		mTextPaintShadow.setColor(0x00000000);
		

		mContext = new WeakReference<Context>(context);
		

		setPadding(3, 3, 3, 3);
		

		setFocusable(true);
		setFocusableInTouchMode(true);
		
		mMovement =  new Vector2D(0,0);
		mFinger = new Vector2D(0,0);
		mOldMovement = new Vector2D(0,0);
		

		mAnimationHandler = new FlipAnimationHandler();
		

		mCurlEdgePaint = new Paint();
		mCurlEdgePaint.setColor(Color.WHITE);
		mCurlEdgePaint.setAntiAlias(true);
		mCurlEdgePaint.setStyle(Style.FILL);
		mCurlEdgePaint.setShadowLayer(10, -5, 5, 0x99000000);
		

		mCurlSpeed = 30;
		mUpdateRate = 33;
		mInitialEdgeOffset = 20;
		mCurlMode = 1;
		

		

	}

	public void setImageResources(ArrayList<Bitmap> listOfPages){

		mPages = new ArrayList<Bitmap>();
		mPages.addAll(listOfPages);


		mForeground = mPages.get(0);
		mBackground = mPages.get(1);
	}

	public void ResetClipEdge()
	{

		mMovement.x = mInitialEdgeOffset;
		mMovement.y = mInitialEdgeOffset;		
		mOldMovement.x = 0;
		mOldMovement.y = 0;		
		

		mA = new Vector2D(mInitialEdgeOffset, 0);
		mB = new Vector2D(this.getWidth(), this.getHeight());
		mC = new Vector2D(this.getWidth(), 0);
		mD = new Vector2D(0, 0);
		mE = new Vector2D(0, 0);
		mF = new Vector2D(0, 0);		
		mOldF = new Vector2D(0, 0);
		

		mOrigin = new Vector2D(this.getWidth(), 0);
	}
	

	private Context GetContext() {
		return mContext.get();
	}
	

	public boolean IsCurlModeDynamic()
	{
		return mCurlMode == CURLMODE_DYNAMIC;
	}
	

	public void SetCurlSpeed(int curlSpeed)
	{
		if ( curlSpeed < 1 )
			throw new IllegalArgumentException("curlSpeed must be greated than 0");
		mCurlSpeed = curlSpeed;
	}
	

	public int GetCurlSpeed()
	{
		return mCurlSpeed;
	}
	

	public void SetUpdateRate(int updateRate)
	{
		if ( updateRate < 1 )
			throw new IllegalArgumentException("updateRate must be greated than 0");
		mUpdateRate = updateRate;
	}
	

	public int GetUpdateRate()
	{
		return mUpdateRate;
	}
	

	public void SetInitialEdgeOffset(int initialEdgeOffset)
	{
		if ( initialEdgeOffset < 0 )
			throw new IllegalArgumentException("initialEdgeOffset can not negative");
		mInitialEdgeOffset = initialEdgeOffset;
	}
	

	public int GetInitialEdgeOffset()
	{
		return mInitialEdgeOffset;
	}

	public void SetCurlMode(int curlMode)
	{
		if ( curlMode != CURLMODE_SIMPLE &&
			 curlMode != CURLMODE_DYNAMIC )
			throw new IllegalArgumentException("Invalid curlMode");
		mCurlMode = curlMode;
	}

	public int GetCurlMode()
	{
		return mCurlMode;
	}
	

	public void SetEnableDebugMode(boolean bFlag)
	{
		bEnableDebugMode = bFlag;
	}
	

	public boolean IsDebugModeEnabled()
	{
		return bEnableDebugMode;
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int finalWidth, finalHeight;
		finalWidth = measureWidth(widthMeasureSpec);
		finalHeight = measureHeight(heightMeasureSpec);
		setMeasuredDimension(finalWidth, finalHeight);
	}

	private int measureWidth(int measureSpec) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		
		if (specMode == MeasureSpec.EXACTLY) {

			result = specSize;
		} else {

			result = specSize;
		}
		
		return result;
	}


	private int measureHeight(int measureSpec) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		
		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		} else {

			result = specSize;
		}
		return result;
	}

	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!bBlockTouchInput) {
			

			mFinger.x = event.getX();
			mFinger.y = event.getY();
			int width = getWidth();
			

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:				
				mOldMovement.x = mFinger.x;
				mOldMovement.y = mFinger.y;
				

				if (mOldMovement.x > (width >> 1)) {
					mMovement.x = mInitialEdgeOffset;
					mMovement.y = mInitialEdgeOffset;
					

					bFlipRight = true;
				} else {

					bFlipRight = false;
					

					previousView();
					

					mMovement.x = IsCurlModeDynamic()?width<<1:width;
					mMovement.y = mInitialEdgeOffset;
				}
				
				break;
			case MotionEvent.ACTION_UP:				
				bUserMoves=false;
				bFlipping=true;
				FlipAnimationStep();
				break;
			case MotionEvent.ACTION_MOVE:
				bUserMoves=true;
				

				mMovement.x -= mFinger.x - mOldMovement.x;
				mMovement.y -= mFinger.y - mOldMovement.y;
				mMovement = CapMovement(mMovement, true);
				

				if ( mMovement.y  <= 1 )
					mMovement.y = 1;
				

				if (mFinger.x < mOldMovement.x ) {
					bFlipRight = true;
				} else {
					bFlipRight = false;
				}
				

				mOldMovement.x  = mFinger.x;
				mOldMovement.y  = mFinger.y;
				

				DoPageCurl();
				this.invalidate();
				break;
			}

		}

		return true;
	}

	private Vector2D CapMovement(Vector2D point, boolean bMaintainMoveDir)
	{

		if (point.distance(mOrigin) > mFlipRadius)
		{
			if ( bMaintainMoveDir )
			{

				point = mOrigin.sum(point.sub(mOrigin).normalize().mult(mFlipRadius));
			}
			else
			{

				if ( point.x > (mOrigin.x+mFlipRadius))
					point.x = (mOrigin.x+mFlipRadius);
				else if ( point.x < (mOrigin.x-mFlipRadius) )
					point.x = (mOrigin.x-mFlipRadius);
				point.y = (float) (Math.sin(Math.acos(Math.abs(point.x-mOrigin.x)/mFlipRadius))*mFlipRadius);
			}
		}
		return point;
	}
	

	public void FlipAnimationStep() {
		if ( !bFlipping )
			return;
		
		int width = getWidth();
			

		bBlockTouchInput = true;
		

		float curlSpeed = mCurlSpeed;
		if ( !bFlipRight )
			curlSpeed *= -1;
		

		mMovement.x += curlSpeed;
		mMovement = CapMovement(mMovement, false);
		

		DoPageCurl();
		

		if (mA.x < 1 || mA.x > width - 1) {
			bFlipping = false;
			if (bFlipRight) {

				nextView();
			} 
			ResetClipEdge();
			

			DoPageCurl();


			bEnableInputAfterDraw = true;
		}
		else
		{
			mAnimationHandler.sleep(mUpdateRate);
		}
		

		this.invalidate();
	}

	private void DoPageCurl()
	{
		if(bFlipping){
			if ( IsCurlModeDynamic() )
				doDynamicCurl();
			else
				doSimpleCurl();
			
		} else {
			if ( IsCurlModeDynamic() )
				doDynamicCurl();
			else
				doSimpleCurl();
		}
	}

	private void doSimpleCurl() {
		int width = getWidth();
		int height = getHeight();
		

		mA.x = width - mMovement.x;
		mA.y = height;


		mD.x = 0;
		mD.y = 0;
		if (mA.x > width / 2) {
			mD.x = width;
			mD.y = height - (width - mA.x) * height / mA.x;
		} else {
			mD.x = 2 * mA.x;
			mD.y = 0;
		}
		

		double angle = Math.atan((height - mD.y) / (mD.x + mMovement.x - width));
		double _cos = Math.cos(2 * angle);
		double _sin = Math.sin(2 * angle);


		mF.x = (float) (width - mMovement.x + _cos * mMovement.x);
		mF.y = (float) (height - _sin * mMovement.x);
		

		if (mA.x > width / 2) {
			mE.x = mD.x;
			mE.y = mD.y;
		}
		else
		{
			// So get E
			mE.x = (float) (mD.x + _cos * (width - mD.x));
			mE.y = (float) -(_sin * (width - mD.x));
		}
	}


	private void doDynamicCurl() {
		int width = getWidth();
		int height = getHeight();


		mF.x = width - mMovement.x+0.1f;
		mF.y = height - mMovement.y+0.1f;
		

		if(mA.x==0) {
			mF.x= Math.min(mF.x, mOldF.x);
			mF.y= Math.max(mF.y, mOldF.y);
		}

		float deltaX = width-mF.x;
		float deltaY = height-mF.y;

		float BH = (float) (Math.sqrt(deltaX * deltaX + deltaY * deltaY) / 2);
		double tangAlpha = deltaY / deltaX;
		double alpha = Math.atan(deltaY / deltaX);
		double _cos = Math.cos(alpha);
		double _sin = Math.sin(alpha);
		
		mA.x = (float) (width - (BH / _cos));
		mA.y = height;
		
		mD.y = (float) (height - (BH / _sin));
		mD.x = width;

		mA.x = Math.max(0,mA.x);
		if(mA.x==0) {
			mOldF.x = mF.x;
			mOldF.y = mF.y;
		}
		
		// Get W
		mE.x = mD.x;
		mE.y = mD.y;
		
		// Correct
		if (mD.y < 0) {
			mD.x = width + (float) (tangAlpha * mD.y);
			mE.y = 0;
			mE.x = width + (float) (Math.tan(2 * alpha) * mD.y);
		}
	}


	@Deprecated
	private void SwapViews() {
		Bitmap temp = mForeground;
		mForeground = mBackground;
		mBackground = temp;
	}

	private void nextView() {
		int foreIndex = mIndex + 1;
		if(foreIndex >= mPages.size()) {
			foreIndex = 0;
		}
		int backIndex = foreIndex + 1;
		if(backIndex >= mPages.size()) {
			backIndex = 0;
		}

		if (foreIndex != 0){
			mIndex = foreIndex;
			setViews(foreIndex, backIndex);
		}

	}

	private void previousView() {
		int backIndex = mIndex;
		int foreIndex = backIndex - 1;
		if(foreIndex < 0) {
			foreIndex = mPages.size()-1;
		}else {
			mIndex = foreIndex;
			setViews(foreIndex, backIndex);
		}

	}

	private void setViews(int foreground, int background) {
		mForeground = mPages.get(foreground);
		mBackground = mPages.get(background);
	}


	@Override
	protected void onDraw(Canvas canvas) {
		mCurrentLeft = getLeft();
		mCurrentTop = getTop();

		if ( !bViewDrawn ) {
			bViewDrawn = true;
			onFirstDrawEvent(canvas);
		}
		
		canvas.drawColor(Color.WHITE);

		Rect rect = new Rect();
		rect.left = 0;
		rect.top = 0;
		rect.bottom = getHeight();
		rect.right = getWidth();
		

		Paint paint = new Paint();
		

		drawForeground(canvas, rect, paint);
		drawBackground(canvas, rect, paint);
		drawCurlEdge(canvas);
		

		if ( bEnableDebugMode )
			drawDebug(canvas);

		if ( bEnableInputAfterDraw )
		{
			bBlockTouchInput = false;
			bEnableInputAfterDraw = false;
		}

	}
	

	protected void onFirstDrawEvent(Canvas canvas) {
		
		mFlipRadius = getWidth();
		
		ResetClipEdge();
		DoPageCurl();
	}
	

	private void drawForeground( Canvas canvas, Rect rect, Paint paint ) {
		canvas.drawBitmap(mForeground, null, rect, paint);

		drawPageNum(canvas, mIndex);
	}
	

	private Path createBackgroundPath() {
		Path path = new Path();
		path.moveTo(mA.x, mA.y);
		path.lineTo(mB.x, mB.y);
		path.lineTo(mC.x, mC.y);
		path.lineTo(mD.x, mD.y);
		path.lineTo(mA.x, mA.y);
		return path;
	}
	

	private void drawBackground( Canvas canvas, Rect rect, Paint paint ) {
		Path mask = createBackgroundPath();

		canvas.save();
		canvas.clipPath(mask);
		canvas.drawBitmap(mBackground, null, rect, paint);

		drawPageNum(canvas, mIndex);
		
		canvas.restore();
	}
	

	private Path createCurlEdgePath() {
		Path path = new Path();
		path.moveTo(mA.x, mA.y);
		path.lineTo(mD.x, mD.y);
		path.lineTo(mE.x, mE.y);
		path.lineTo(mF.x, mF.y);
		path.lineTo(mA.x, mA.y);
		return path;
	}
	

	private void drawCurlEdge( Canvas canvas )
	{
		Path path = createCurlEdgePath();
		canvas.drawPath(path, mCurlEdgePaint);
	}
	

	private void drawPageNum(Canvas canvas, int pageNum)
	{
		mTextPaint.setColor(Color.WHITE);
		String pageNumText = "- "+pageNum+" -";
		drawCentered(canvas, pageNumText,canvas.getHeight()-mTextPaint.getTextSize()-5,mTextPaint,mTextPaintShadow);
	}

	public static void drawTextShadowed(Canvas canvas, String text, float x, float y, Paint textPain, Paint shadowPaint) {
    	canvas.drawText(text, x-1, y, shadowPaint);
    	canvas.drawText(text, x, y+1, shadowPaint);
    	canvas.drawText(text, x+1, y, shadowPaint);
    	canvas.drawText(text, x, y-1, shadowPaint);    	
    	canvas.drawText(text, x, y, textPain);
    }
	

	public static void drawCentered(Canvas canvas, String text, float y, Paint textPain, Paint shadowPaint)
	{
		float posx = (canvas.getWidth() - textPain.measureText(text))/2;
		drawTextShadowed(canvas, text, posx, y, textPain, shadowPaint);
	}

	private void drawDebug(Canvas canvas)
	{
		float posX = 10;
		float posY = 20;
		
		Paint paint = new Paint();
		paint.setStrokeWidth(5);
		paint.setStyle(Style.STROKE);
		
		paint.setColor(Color.BLACK);		
		canvas.drawCircle(mOrigin.x, mOrigin.y, getWidth(), paint);
		
		paint.setStrokeWidth(3);
		paint.setColor(Color.RED);		
		canvas.drawCircle(mOrigin.x, mOrigin.y, getWidth(), paint);
		
		paint.setStrokeWidth(5);
		paint.setColor(Color.BLACK);
		canvas.drawLine(mOrigin.x, mOrigin.y, mMovement.x, mMovement.y, paint);
		
		paint.setStrokeWidth(3);
		paint.setColor(Color.RED);
		canvas.drawLine(mOrigin.x, mOrigin.y, mMovement.x, mMovement.y, paint);
		
		posY = debugDrawPoint(canvas,"A",mA,Color.RED,posX,posY);
		posY = debugDrawPoint(canvas,"B",mB,Color.GREEN,posX,posY);
		posY = debugDrawPoint(canvas,"C",mC,Color.BLUE,posX,posY);
		posY = debugDrawPoint(canvas,"D",mD,Color.CYAN,posX,posY);
		posY = debugDrawPoint(canvas,"E",mE,Color.YELLOW,posX,posY);
		posY = debugDrawPoint(canvas,"F",mF,Color.LTGRAY,posX,posY);
		posY = debugDrawPoint(canvas,"Mov",mMovement,Color.DKGRAY,posX,posY);
		posY = debugDrawPoint(canvas,"Origin",mOrigin,Color.MAGENTA,posX,posY);
		posY = debugDrawPoint(canvas,"Finger",mFinger,Color.GREEN,posX,posY);
	}
	
	private float debugDrawPoint(Canvas canvas, String name, Vector2D point, int color, float posX, float posY) {	
		return debugDrawPoint(canvas,name+" "+point.toString(),point.x, point.y, color, posX, posY);
	}
	
	private float debugDrawPoint(Canvas canvas, String name, float X, float Y, int color, float posX, float posY) {
		mTextPaint.setColor(color);
		drawTextShadowed(canvas,name,posX , posY, mTextPaint,mTextPaintShadow);
		Paint paint = new Paint();
		paint.setStrokeWidth(5);
		paint.setColor(color);	
		canvas.drawPoint(X, Y, paint);
		return posY+15;
	}

}
