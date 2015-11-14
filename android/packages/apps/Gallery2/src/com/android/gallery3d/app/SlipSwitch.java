package com.android.gallery3d.app; 

import com.android.gallery3d.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class SlipSwitch extends View implements OnTouchListener{
	private final static String TAG = "SlipSwitch";

	private boolean mInitSwitch;
	private boolean NowChoose = false;//��¼��ǰ��ť�Ƿ��,trueΪ��,flaseΪ�ر�
	private boolean OnSlip = false;//��¼�û��Ƿ��ڻ����ı���
	private float DownX,NowX;//����ʱ��x,��ǰ��x,
	private Rect Btn_On,Btn_Off;//�򿪺͹ر�״̬��,�α��Rect
	private OnSwitchChangedListener ChgLsn;
	
	private Bitmap backgroud, slip_btn;
	
	public interface OnSwitchChangedListener {  
	    abstract void OnSwitchChanged(boolean switchOn);  
	}	
	
	public SlipSwitch(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init();
	}

	public SlipSwitch(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init();
	}

	public SlipSwitch(Context context, OnSwitchChangedListener listener, boolean initSwitch){
		super(context);
		ChgLsn = listener;
		mInitSwitch = initSwitch;
		NowChoose = mInitSwitch;		
		init();
	}
	
	private void init(){//��ʼ��
		//����ͼƬ��Դ
		backgroud = BitmapFactory.decodeResource(getResources(), R.drawable.slipswitch_bg);
		if(mInitSwitch){
			slip_btn = BitmapFactory.decodeResource(getResources(), R.drawable.slipswitch_uf_on);
		}
		else{
			slip_btn = BitmapFactory.decodeResource(getResources(), R.drawable.slipswitch_uf_off);
		}
		//�����Ҫ��Rect����
		Btn_On = new Rect(0,0,slip_btn.getWidth(),slip_btn.getHeight());
		Btn_Off = new Rect(backgroud.getWidth()-slip_btn.getWidth(), 0, backgroud.getWidth(), slip_btn.getHeight());
		setOnTouchListener(this);//���ü�����,Ҳ����ֱ�Ӹ�дOnTouchEvent
	}
	
	@Override
	protected void onDraw(Canvas canvas) {//��ͼ����
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		Matrix matrix = new Matrix();
		Paint paint = new Paint();
		float x;
		
		{
			canvas.drawBitmap(backgroud,matrix, paint);
			
			if(OnSlip)//�Ƿ����ڻ���״̬,
			{
				if(NowX >= backgroud.getWidth())//�Ƿ񻮳�ָ����Χ,�������α��ܵ���ͷ,����������ж�
					x = backgroud.getWidth()-slip_btn.getWidth()/2;//��ȥ�α�1/2�ĳ���...
				else
					x = NowX - slip_btn.getWidth()/2;
			}else{//�ǻ���״̬
				if(NowChoose)//�������ڵĿ���״̬���û��α��λ��
					x = Btn_On.left;
				else
					x = Btn_Off.left;
			}
		if(x<0)//���α�λ�ý����쳣�ж�...
			x = 0;
		else if(x > backgroud.getWidth() - slip_btn.getWidth())
			x = backgroud.getWidth() - slip_btn.getWidth();
		canvas.drawBitmap(slip_btn,x, 0, paint);//�����α�.
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		setMeasuredDimension(backgroud.getWidth(), backgroud.getHeight());
	}

	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		switch(event.getAction())//���ݶ�����ִ�д���
		{
			case MotionEvent.ACTION_MOVE://����
				NowX = event.getX();
				invalidate();//�ػ��ؼ�
				break;
			case MotionEvent.ACTION_DOWN://����
				if(event.getX() > backgroud.getWidth()||event.getY()>backgroud.getHeight())
					return false;
				slip_btn = BitmapFactory.decodeResource(getResources(), R.drawable.slipswitch_f);
				OnSlip = true;
				DownX = event.getX();
				NowX = DownX;
				invalidate();//�ػ��ؼ�
				break;
			case MotionEvent.ACTION_UP://�ɿ�
				OnSlip = false;
				if(event.getX() >= (backgroud.getWidth()/2)){
					NowChoose = false;
					slip_btn = BitmapFactory.decodeResource(getResources(), R.drawable.slipswitch_uf_off);
				}
				else{
					NowChoose = true;
					slip_btn = BitmapFactory.decodeResource(getResources(), R.drawable.slipswitch_uf_on);
				}
				invalidate();//�ػ��ؼ�
				Log.e(TAG, "ACTION_UP, NowChoose = " + NowChoose);
				if(ChgLsn != null){
					ChgLsn.OnSwitchChanged(NowChoose);
				}
				break;
			default:
		
		}
		return true;
	}
		
}