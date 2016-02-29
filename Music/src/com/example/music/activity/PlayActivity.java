package com.example.music.activity;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.music.R;
import com.example.music.modle.AppConstant;
import com.example.music.modle.CircleImageView;
import com.example.music.modle.LrcRow;
import com.example.music.modle.LrcView;
import com.example.music.modle.LrcView.OnLrcClickListener;
import com.example.music.modle.Mp3Info;
import com.example.music.utils.DefaultLrcParser;
import com.example.music.utils.FastBlur;
import com.example.music.utils.FileUtil;
import com.example.music.utils.HttpCallbackListener;
import com.example.music.utils.HttpUtil;
import com.example.music.utils.MediaUtils;
import com.example.sortmusiclistview.CharacterParser;
import com.example.sortmusiclistview.PinyinComparator;

public class PlayActivity extends Activity {

	private SeekBar mSeekBar;		//������
	private RelativeLayout playActivityLayout;		//����ҳ������
	private RelativeLayout albumLayout;
	private ImageView backBtn;		//���ذ�ť
	private TextView title;			//��������
	private CircleImageView album;		//ר��Բ�η���
	private ImageView loop;			//ѭ��ģʽ��ť
	private ImageView nextBtn;		//��һ��
	private ImageView playBtn;		//���ţ����š���ͣ��
	private ImageView preBtn;		//��һ��
	private ImageView lanmu;		//��Ŀ��ť
	private TextView startTime;		//������ǰ��ʱ��
	private TextView endTime;		//����������ʱ��
	private int status;
	private String titleName;	//��������
	private int listPosition;   //���Ÿ�����mp3Infos��λ��  
	private int currentTime;    //��ǰ��������ʱ��  
	private boolean isPlaying;              // ���ڲ���  
	private boolean isFirstTime;            //��һ�β���
	private PlayerReceiver playerReceiver;
	private PlayThread playThread;
	private int fileSize;		//�ļ�һ���Ĵ�С
	private LrcView mLrcView;
	private Animation mShowAction;
	private Animation mHiddenAction;
	private List<Mp3Info> mp3Infos;  
	private int skinNum;
	private ImageView playImageView;
	public static final String UPDATE_MUSIC  = "com.music.action.UPDATE_MUSIC";  //���Ÿ����ı�
	public static final String CTL_ACTION     = "com.music.action.CTL_ACTION";        //���ƶ���  
	public static final String MUSIC_CURRENT  = "com.music.action.MUSIC_CURRENT";  //���ֵ�ǰʱ��ı䶯��  
	public static final String MUSIC_DURATION = "com.music.action.MUSIC_DURATION";//���ֲ��ų��ȸı䶯��  
	public static final String MUSIC_PLAYING  = "com.music.action.MUSIC_PLAYING";  //�������ڲ��Ŷ���  
	public static final String SHUFFLE_ACTION = "com.music.action.SHUFFLE_ACTION";//����������Ŷ���  
	/*
	 * ҡһҡ
	 */

	private SensorManager sensorManager; 
	private Vibrator vibrator; 
	private static final String TAG = "TestSensor"; 
	private static final int SENSOR_SHAKE = 10; 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.play_activity_layout);

		finViewById();
		setViewOnclickListener();
		playThread = new PlayThread();
		mp3Infos =filledData(MediaUtils.getMp3Infos(getApplicationContext()));
		PinyinComparator pinyinComparator = new PinyinComparator();
		Collections.sort(mp3Infos, pinyinComparator);
		setAnimate();

		playerReceiver = new PlayerReceiver();  
		IntentFilter filter = new IntentFilter();  
		filter.addAction(UPDATE_MUSIC);  
		filter.addAction(MUSIC_CURRENT);  
		filter.addAction(MUSIC_DURATION);
		registerReceiver(playerReceiver, filter); 

		/*
		 * ������ʼ��
		 */
		//		pref = getSharedPreferences("data", MODE_PRIVATE);
		//		editor=pref.edit();
		fileSize = 0;
		status = 3;
		skinNum=1;
		getBunder();
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", status);
		sendBroadcast(intent);
		/*
		 * ҡһҡ
		 */
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); 
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE); 

	}
	private List<Mp3Info> filledData(List<Mp3Info> tMp3Infos) {
		Mp3Info tmp3Info=null;
		CharacterParser characterParser=CharacterParser.getInstance();
		for (int i = 0; i < tMp3Infos.size(); i++) {
			tmp3Info = tMp3Infos.get(i);

			// ����ת����ƴ��
			String pinyin = characterParser.getSelling(tmp3Info.getTitle());
			String sortString = pinyin.substring(0, 1).toUpperCase();

			// ������ʽ���ж�����ĸ�Ƿ���Ӣ����ĸ
			if (sortString.matches("[A-Z]")) {
				tmp3Info.setSortLetters(sortString.toUpperCase());
			} else {
				tmp3Info.setSortLetters("#");
			}
		}
		return tMp3Infos;

	}

	/**
	 * ��onResume�г�ʼ���ͽ���Activity����
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.i("test", "resume---------------->");
		changePlayActivity();
		if(status==1){
			loop.setBackgroundResource(R.drawable.img_appwidget91_playmode_repeat_current);
		}else if(status==3){
			loop.setBackgroundResource(R.drawable.img_appwidget91_playmode_repeat_all);
		}else if(status==4){
			loop.setBackgroundResource(R.drawable.img_appwidget91_playmode_shuffle);
		}
		/*
		 * ҡһҡ
		 */
		if (sensorManager != null) {// ע������� 
			sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL); 
			// ��һ��������Listener���ڶ������������ô��������ͣ�����������ֵ��ȡ��������Ϣ��Ƶ�� 
		} 
	}

	public void getBunder(){
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		listPosition = bundle.getInt("listPosition");
		isPlaying = bundle.getBoolean("isPlaying");
		isFirstTime = bundle.getBoolean("isFirstTime", isFirstTime);
		status = bundle.getInt("status");
		Log.i("status getbunder", status+"----------M");
	}

	@Override
	protected void onStop() {
		super.onStop();
		/*
		 * ҡһҡ
		 */
		if (sensorManager != null) {// ȡ�������� 
			sensorManager.unregisterListener(sensorEventListener); 
		} 
	}
	/**
	 * ������Ӧ����
	 */ 
	private SensorEventListener sensorEventListener = new SensorEventListener() { 

		@Override 
		public void onSensorChanged(SensorEvent event) { 
			// ��������Ϣ�ı�ʱִ�и÷��� 
			float[] values = event.values; 
			float x = values[0]; // x�᷽����������ٶȣ�����Ϊ�� 
			float y = values[1]; // y�᷽����������ٶȣ���ǰΪ�� 
			float z = values[2]; // z�᷽����������ٶȣ�����Ϊ�� 
			Log.i(TAG, "x�᷽����������ٶ�" + x +  "��y�᷽����������ٶ�" + y +  "��z�᷽����������ٶ�" + z); 
			// һ����������������������ٶȴﵽ40�ʹﵽ��ҡ���ֻ���״̬�� 
			int medumValue = 15;// ���� i9250��ô�ζ����ᳬ��20��û�취��ֻ����19�� 
			if (Math.abs(x) > medumValue || Math.abs(y) > medumValue || Math.abs(z) > medumValue) { 
				vibrator.vibrate(200); 
				Message msg = new Message(); 
				msg.what = SENSOR_SHAKE; 
				handler.sendMessage(msg); 
			} 
		} 

		@Override 
		public void onAccuracyChanged(Sensor sensor, int accuracy) { 

		} 
	}; 

	/**
	 * ����ִ��
	 */ 
	@SuppressLint("HandlerLeak")
	Handler handler = new Handler() { 

		@Override 
		public void handleMessage(Message msg) { 
			super.handleMessage(msg); 
			switch (msg.what) { 
			case SENSOR_SHAKE: 
				nextMusic();
				Toast.makeText(PlayActivity.this, "��⵽ҡ�Σ�ִ�в�����", Toast.LENGTH_SHORT).show(); 
				Log.i(TAG, "��һ�׸�����"); 
				break; 
			} 
		} 

	}; 


	@Override
	protected void onDestroy() {
		unregisterReceiver(playerReceiver);
		super.onDestroy();	
	}

	public void changePlayActivity(){
		Mp3Info mp3Info = mp3Infos.get(listPosition);
		title.setText(mp3Info.getTitle());
		Log.i("currrent", Integer.toString(currentTime));
		mSeekBar.setProgress(currentTime);
		mSeekBar.setMax((int) mp3Info.getDuration());
		endTime.setText(MediaUtils.formatTime(mp3Info.getDuration()));

		showArtwork(mp3Info);
		getLrcFromNet();
		albumLayout.setVisibility(View.VISIBLE);
		mLrcView.setVisibility(View.INVISIBLE);
		File file=new File(getPath(mp3Info.getTitle()));
		Log.i("����ļ��Ƿ����", ""+file.exists());
		if (file.exists()) {
			mLrcView.setLrcRows(getLrcRows());
		} else {
			mLrcView.setLrcRows(null);
		}

		if (isPlaying){
			playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
			//			playThread.start();
		}else {
			playBtn.setBackgroundResource(R.drawable.img_lockscreen_play_normal);
		}
	}

	/**
	 * ��ʾר������
	 */
	@SuppressWarnings("deprecation")
	private void showArtwork(Mp3Info mp3Info) {
		Bitmap bm = MediaUtils.getArtwork(this, mp3Info.getId(), mp3Info.getAlbumId(), true, false);
		if(bm != null) {
			album.setImageBitmap(bm);	//��ʾר������ͼƬ
		} else {
			bm = MediaUtils.getDefaultArtwork(this, false);
			album.setImageBitmap(bm);	//��ʾר������ͼƬ
		}
		//���ò��Ž����еı���ͼƬ
		Bitmap bgBitmap =bm.copy(Bitmap.Config.ARGB_8888, true);
		Drawable drawable = new BitmapDrawable(FastBlur.doBlur(bgBitmap, 15, true)); 
		playActivityLayout.setBackgroundDrawable(drawable);

	}


	private void finViewById(){
		playActivityLayout = (RelativeLayout) findViewById(R.id.play_activity_layout);
		mSeekBar = (SeekBar) findViewById(R.id.seekbar);
		backBtn = (ImageView) findViewById(R.id.back_btn);
		title = (TextView) findViewById(R.id.play_title);
		album =  (CircleImageView)findViewById(R.id.album_play);
		startTime = (TextView) findViewById(R.id.start_time);
		endTime = (TextView) findViewById(R.id.end_time);
		loop = (ImageView) findViewById(R.id.loop);
		preBtn = (ImageView) findViewById(R.id.play_pre_btn);
		nextBtn = (ImageView) findViewById(R.id.play_next_btn);
		playBtn = (ImageView) findViewById(R.id.play_play_btn);
		lanmu = (ImageView) findViewById(R.id.lanmu);
		albumLayout = (RelativeLayout) findViewById(R.id.alubnlayout);
		mLrcView = (LrcView) findViewById(R.id.lrcView);
		playImageView = (ImageView) findViewById(R.id.notiplay);
	}

	private void setViewOnclickListener(){
		ViewOnClickListener viewOnClickListener = new ViewOnClickListener();
		SeekBarChangeListener seekBarChangeListener = new SeekBarChangeListener();
		backBtn.setOnClickListener(viewOnClickListener);
		loop.setOnClickListener(viewOnClickListener);
		preBtn.setOnClickListener(viewOnClickListener);
		nextBtn.setOnClickListener(viewOnClickListener);
		playBtn.setOnClickListener(viewOnClickListener);
		lanmu.setOnClickListener(viewOnClickListener);
		mSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
		albumLayout.setOnClickListener(viewOnClickListener);
		mLrcView.setOnLrcClickListener(onLrcClickListener);
		playImageView.setOnClickListener(viewOnClickListener);
	}


	private class ViewOnClickListener implements OnClickListener{


		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.back_btn:		//���ذ�ť
				skinNum=skinNum+1;
				if (skinNum==1) {
					playActivityLayout.setBackgroundResource(R.drawable.bg1);
				
				} 
				if (skinNum==2) {
					playActivityLayout.setBackgroundResource(R.drawable.bg2);
//					skinNum=skinNum+1;
				}
				if (skinNum==3) {
					playActivityLayout.setBackgroundResource(R.drawable.bg3);
//					skinNum=skinNum+1;
				} 
				if (skinNum==4) {
					playActivityLayout.setBackgroundResource(R.drawable.bg4);
//					skinNum=skinNum+1;
				}
				if (skinNum==5) {
					playActivityLayout.setBackgroundResource(R.drawable.bg5);
//					skinNum=skinNum+1;
				} 
				if (skinNum==6) {
					
				playActivityLayout.setBackgroundResource(R.drawable.bg6);
//				skinNum=skinNum+1;
				}
				if (skinNum==7) {
					playActivityLayout.setBackgroundResource(R.drawable.bg7);
//					skinNum=skinNum+1;
				} 
				if (skinNum==8) {
					playActivityLayout.setBackgroundResource(R.drawable.bg8);
//					skinNum=skinNum+1;
				}
				if (skinNum==9) {
					playActivityLayout.setBackgroundResource(R.drawable.bg9);
//					skinNum=skinNum+1;
				} 
				if (skinNum==10) {
					skinNum=0;
					playActivityLayout.setBackgroundResource(R.drawable.bg10);
					
				}

                Log.i("skinnum", ""+skinNum);
				break;
			case R.id.loop:		//ѭ��ģʽ��ť
				if (status==4){
					status=1;
					loop.setBackgroundResource(R.drawable.img_appwidget91_playmode_repeat_current);
					repeatOne();
					Toast.makeText(PlayActivity.this,"��������", Toast.LENGTH_SHORT).show();
				}else if (status==1) {
					status=3;
					loop.setBackgroundResource(R.drawable.img_appwidget91_playmode_repeat_all);
					Toast.makeText(PlayActivity.this,"˳��ѭ��", Toast.LENGTH_SHORT).show();
					repeatNone();				
				}else if (status==3) {
					status=4;
					loop.setBackgroundResource(R.drawable.img_appwidget91_playmode_shuffle);					
					Toast.makeText(PlayActivity.this,"�������", Toast.LENGTH_SHORT).show();									
					shuffleMusic();	
				}
				break;
			case R.id.alubnlayout:
				albumLayout.startAnimation(mHiddenAction);
				albumLayout.setVisibility(View.INVISIBLE);
				mLrcView.setVisibility(View.VISIBLE);
				break;
			case R.id.play_pre_btn:		//��һ��
				previousMusic();
				break;
			case R.id.play_next_btn:		//��һ��
				nextMusic();
				break;
			case R.id.play_play_btn:		//����
				if (isFirstTime) {
					isFirstTime=false;
					if (isPlaying){
						playBtn.setBackgroundResource(R.drawable.img_lockscreen_play_normal);
						toSendingService(AppConstant.PlayerMsg.PAUSE_MSG);					
						Log.i("playactivity ���ڲ���", ""+isPlaying);
						isPlaying=false;
						playThread.setSuspend(true);

					}else {
						playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
						toSendingService(AppConstant.PlayerMsg.PLAY_MSG);
						isPlaying=true;
						Log.i("playactivity ��ͣ", ""+isPlaying);
						playThread.setSuspend(false);
					}
				} else {
					if (isPlaying){
						playBtn.setBackgroundResource(R.drawable.img_lockscreen_play_normal);
						toSendingService(AppConstant.PlayerMsg.PAUSE_MSG);					
						isPlaying = false;			
						Log.i("playactivity ���ڲ���", ""+isPlaying);
						playThread.setSuspend(true);

					}else {
						playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
						toSendingService(AppConstant.PlayerMsg.CONTINUE_MSG);
						isPlaying = true;
						Log.i("playactivity ��ͣ", ""+isPlaying);
						playThread.setSuspend(false);
					}
				}
				break;
			case R.id.lanmu:
				Intent intent=new Intent();
				Log.i("lanmu", "isplaying"+isPlaying);
				intent.putExtra("isPlaying", isPlaying);
				intent.putExtra("status", status);
				setResult(0, intent);//��Ŀ��ť
				finish();
				break;
				
			case R.id.notiplay:
				boolean playflag = true;
				if (playflag = true) {
					playImageView.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
				}
				else {
					playImageView.setBackgroundResource(R.drawable.img_lockscreen_play_normal);
				}
				
				break;
			}
		}
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			Intent intent=new Intent();
			intent.putExtra("isPlaying", isPlaying);
			intent.putExtra("status", status);
			setResult(0, intent);//��Ŀ��ť
		}
		return super.onKeyDown(keyCode, event);
	}
	private class SeekBarChangeListener implements OnSeekBarChangeListener {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			mLrcView.seekTo(progress, true,fromUser);
			if (fromUser){
				audioTrackChange(progress);		//�û����ƽ��ȵĸı䷢�͸�������
			}

		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}

	}


	/**
	 *	�û��ֶ��ı������ 
	 */
	public void audioTrackChange(int progress){
		Intent intent = new Intent();
		intent.setAction("com.example.music.MUSIC_SERVICE");
		intent.putExtra("listPosition", listPosition);
		if(isPlaying){
			intent.putExtra("MSG", AppConstant.PlayerMsg.PROGRESS_CHANGE);

		}else {
			intent.putExtra("MSG", AppConstant.PlayerMsg.PAUSE_MSG);
		}
		intent.putExtra("progress", progress);
		startService(intent);
	}

	/**
	 * ��������
	 */
	public void play(){
		playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
		toSendingService(AppConstant.PlayerMsg.PLAY_MSG);
	}

	/**
	 * �������
	 */
	public void shuffleMusic(){
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 4);
		sendBroadcast(intent);
	}

	/**
	 * ����ѭ��
	 */
	public void repeatOne(){
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 1);
		sendBroadcast(intent);
	}

	/**
	 * ˳�򲥷�
	 */
	public void repeatNone(){
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 3);
		sendBroadcast(intent);
	}

	/**
	 * ��һ��
	 */
	public void previousMusic(){
		playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
		toSendingService(AppConstant.PlayerMsg.PRIVIOUS_MSG);
		isPlaying = true;
	}

	/**
	 * ��һ��
	 */
	public void nextMusic(){
		playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
		toSendingService(AppConstant.PlayerMsg.NEXT_MSG);
		isPlaying = true;
	}
	/*
	 * ��������������Ϣ
	 */
	public void toSendingService( int msg) {

		Intent intent = new Intent();
		intent.setAction("com.example.music.MUSIC_SERVICE");
		intent.putExtra("listPosition", listPosition);
		Log.i("test", "�����������͵ĸ赥"+listPosition);
		intent.putExtra("MSG", msg); 
		startService(intent);
	}
	/*
	 * ������ȡ���www.geci.me
	 */
	void getLrcFromNet(){
		File file = new File(getPath(mp3Infos.get(listPosition).getTitle()));
		if (!file.exists()) {
			queryServer(mp3Infos.get(listPosition).getTitle(),mp3Infos.get(listPosition).getArtist());
			new Thread(){
				@Override
				public void run() {
					super.run();
					Log.i("test download ", "do 1");
					downloadFile(mp3Infos.get(listPosition).getTitle());	
					Log.i("test download ", "do 2");
				}
			}.start();

		}
	}
	public class PlayerReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(MUSIC_CURRENT)){
				currentTime = intent.getIntExtra("currentTime", -1);
				startTime.setText(MediaUtils.formatTime(currentTime));
				mSeekBar.setProgress(currentTime);
			}else if (action.equals(MUSIC_DURATION)) {
				int duration = intent.getIntExtra("duration", -1);
				mSeekBar.setMax(duration);
				endTime.setText(MediaUtils.formatTime(duration));
			}else if (action.equals(UPDATE_MUSIC)) {
				listPosition = intent.getIntExtra("current", -1);
				if (listPosition>=0){
					changePlayActivity();
					isPlaying=true;
				}


			}
		}

	}



	/**
	 *		ר������ ��ת�߳�
	 */
	class PlayThread extends Thread{

		private boolean suspend = false;  //Ĭ������

		private String control = ""; // ֻ����Ҫһ��������ѣ��������û��ʵ������  

		public void setSuspend(boolean suspend) {  
			if (!suspend) {  
				synchronized (control) {  
					control.notifyAll();  
				}  
			}  
			this.suspend = suspend;  
		}

		public boolean isSuspend() {  
			return this.suspend;  
		}  

		public void run() {  
			while (true) {  
				album.setDegree(album.getDegree() + 0.5f);
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				synchronized (control) {  
					if (suspend) {  
						try {  
							control.wait();  
						} catch (InterruptedException e) {  
							e.printStackTrace();  
						}  
					}  
				}  
			}  
		}  		 
	}

	/**
	 * �ļ�����
	 */
	private void downloadFile(String url)
	{
		try {
			URL u = new URL(url);
			Log.i("downloadFile->url", url.toString());
			URLConnection conn = u.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			fileSize = conn.getContentLength();
			if(fileSize<1||is==null){
			}else{
				FileOutputStream fos = new FileOutputStream(getPath(titleName));
				byte[] bytes = new byte[1024];
				int len = -1;
				while((len = is.read(bytes))!=-1)	// ���������ж�ȡһ���������ֽڣ�������洢�ڻ��������� bytes �С�
				{
					fos.write(bytes, 0, len);
				}
				is.close();
				fos.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	/**
	 * �õ��ļ��ı���·��
	 * @return
	 * @throws IOException
	 */
	private String getPath(String title) {
		String path = FileUtil.setMkdir(this)+File.separator+title+".lrc";
		return path;
	}

	/**
	 * �����ʹ�����������ظ������
	 */
	public void handleLrcResponse(String response,String title){
		try {
			JSONObject jsonObject = new JSONObject(response);
			int result = jsonObject.getInt("code");
			if (result ==0){
				JSONArray jsonArray = jsonObject.getJSONArray("result");
				JSONObject jsonObject2 = (JSONObject)jsonArray.opt(0);
				String lrcAddress = jsonObject2.getString("lrc");
				Log.e("lrcAddress",lrcAddress);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	private void queryServer(final String title,String artist){
		String address;
		Log.i("title", title);
		Log.i("artist", artist);
		address = "http://geci.me/api/lyric/"+URLEncoder.encode(title)+"/"+URLEncoder.encode(artist);
		Log.i("address",address);
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {

			@Override
			public void onFinish(String response) {
				handleLrcResponse(response,title);
				Log.e("response", response);
			}
			@Override
			public void onError(Exception e) {
			}
		});
	}

	/**
	 * ��ȡ���List����
	 * @return
	 */
	private List<LrcRow> getLrcRows(){
		List<LrcRow> rows = null;
		InputStream is=null;
		try {
			is = new FileInputStream(getPath(mp3Infos.get(listPosition).getTitle()));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line ;
		StringBuffer sb = new StringBuffer();
		try {
			while((line = br.readLine()) != null){
				sb.append(line+"\n");
			}
			System.out.println(sb.toString());
			br.close();
			rows = DefaultLrcParser.getIstance().getLrcRows(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rows;
	}


	OnLrcClickListener onLrcClickListener = new OnLrcClickListener() {

		@Override
		public void onClick() {
			albumLayout.startAnimation(mShowAction);
			albumLayout.setVisibility(View.VISIBLE);
			mLrcView.setVisibility(View.INVISIBLE);
		}
	};


	/**
	 * ר�����涯��
	 */
	public void setAnimate(){
		mShowAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,     
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,     
				-1.0f, Animation.RELATIVE_TO_SELF, 0.0f);     
		mShowAction.setDuration(500); 

		mHiddenAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF,     
				0.0f, Animation.RELATIVE_TO_SELF, 0.0f,     
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,     
				-1.0f);    
		mHiddenAction.setDuration(500);  
	}


}
