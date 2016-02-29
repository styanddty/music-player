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

	private SeekBar mSeekBar;		//进度条
	private RelativeLayout playActivityLayout;		//播放页主界面
	private RelativeLayout albumLayout;
	private ImageView backBtn;		//返回按钮
	private TextView title;			//歌曲名称
	private CircleImageView album;		//专辑圆形封面
	private ImageView loop;			//循环模式按钮
	private ImageView nextBtn;		//下一首
	private ImageView playBtn;		//播放（播放、暂停）
	private ImageView preBtn;		//上一首
	private ImageView lanmu;		//栏目按钮
	private TextView startTime;		//歌曲当前的时间
	private TextView endTime;		//歌曲结束的时间
	private int status;
	private String titleName;	//歌曲名称
	private int listPosition;   //播放歌曲在mp3Infos的位置  
	private int currentTime;    //当前歌曲播放时间  
	private boolean isPlaying;              // 正在播放  
	private boolean isFirstTime;            //第一次播放
	private PlayerReceiver playerReceiver;
	private PlayThread playThread;
	private int fileSize;		//文件一共的大小
	private LrcView mLrcView;
	private Animation mShowAction;
	private Animation mHiddenAction;
	private List<Mp3Info> mp3Infos;  
	private int skinNum;
	private ImageView playImageView;
	public static final String UPDATE_MUSIC  = "com.music.action.UPDATE_MUSIC";  //播放歌曲改变
	public static final String CTL_ACTION     = "com.music.action.CTL_ACTION";        //控制动作  
	public static final String MUSIC_CURRENT  = "com.music.action.MUSIC_CURRENT";  //音乐当前时间改变动作  
	public static final String MUSIC_DURATION = "com.music.action.MUSIC_DURATION";//音乐播放长度改变动作  
	public static final String MUSIC_PLAYING  = "com.music.action.MUSIC_PLAYING";  //音乐正在播放动作  
	public static final String SHUFFLE_ACTION = "com.music.action.SHUFFLE_ACTION";//音乐随机播放动作  
	/*
	 * 摇一摇
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
		 * 参数初始化
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
		 * 摇一摇
		 */
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); 
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE); 

	}
	private List<Mp3Info> filledData(List<Mp3Info> tMp3Infos) {
		Mp3Info tmp3Info=null;
		CharacterParser characterParser=CharacterParser.getInstance();
		for (int i = 0; i < tMp3Infos.size(); i++) {
			tmp3Info = tMp3Infos.get(i);

			// 汉字转换成拼音
			String pinyin = characterParser.getSelling(tmp3Info.getTitle());
			String sortString = pinyin.substring(0, 1).toUpperCase();

			// 正则表达式，判断首字母是否是英文字母
			if (sortString.matches("[A-Z]")) {
				tmp3Info.setSortLetters(sortString.toUpperCase());
			} else {
				tmp3Info.setSortLetters("#");
			}
		}
		return tMp3Infos;

	}

	/**
	 * 在onResume中初始化和接收Activity数据
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
		 * 摇一摇
		 */
		if (sensorManager != null) {// 注册监听器 
			sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL); 
			// 第一个参数是Listener，第二个参数是所得传感器类型，第三个参数值获取传感器信息的频率 
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
		 * 摇一摇
		 */
		if (sensorManager != null) {// 取消监听器 
			sensorManager.unregisterListener(sensorEventListener); 
		} 
	}
	/**
	 * 重力感应监听
	 */ 
	private SensorEventListener sensorEventListener = new SensorEventListener() { 

		@Override 
		public void onSensorChanged(SensorEvent event) { 
			// 传感器信息改变时执行该方法 
			float[] values = event.values; 
			float x = values[0]; // x轴方向的重力加速度，向右为正 
			float y = values[1]; // y轴方向的重力加速度，向前为正 
			float z = values[2]; // z轴方向的重力加速度，向上为正 
			Log.i(TAG, "x轴方向的重力加速度" + x +  "；y轴方向的重力加速度" + y +  "；z轴方向的重力加速度" + z); 
			// 一般在这三个方向的重力加速度达到40就达到了摇晃手机的状态。 
			int medumValue = 15;// 三星 i9250怎么晃都不会超过20，没办法，只设置19了 
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
	 * 动作执行
	 */ 
	@SuppressLint("HandlerLeak")
	Handler handler = new Handler() { 

		@Override 
		public void handleMessage(Message msg) { 
			super.handleMessage(msg); 
			switch (msg.what) { 
			case SENSOR_SHAKE: 
				nextMusic();
				Toast.makeText(PlayActivity.this, "检测到摇晃，执行操作！", Toast.LENGTH_SHORT).show(); 
				Log.i(TAG, "下一首歌曲！"); 
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
		Log.i("歌词文件是否存在", ""+file.exists());
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
	 * 显示专辑封面
	 */
	@SuppressWarnings("deprecation")
	private void showArtwork(Mp3Info mp3Info) {
		Bitmap bm = MediaUtils.getArtwork(this, mp3Info.getId(), mp3Info.getAlbumId(), true, false);
		if(bm != null) {
			album.setImageBitmap(bm);	//显示专辑封面图片
		} else {
			bm = MediaUtils.getDefaultArtwork(this, false);
			album.setImageBitmap(bm);	//显示专辑封面图片
		}
		//设置播放界面中的背景图片
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
			case R.id.back_btn:		//返回按钮
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
			case R.id.loop:		//循环模式按钮
				if (status==4){
					status=1;
					loop.setBackgroundResource(R.drawable.img_appwidget91_playmode_repeat_current);
					repeatOne();
					Toast.makeText(PlayActivity.this,"单曲播放", Toast.LENGTH_SHORT).show();
				}else if (status==1) {
					status=3;
					loop.setBackgroundResource(R.drawable.img_appwidget91_playmode_repeat_all);
					Toast.makeText(PlayActivity.this,"顺序循环", Toast.LENGTH_SHORT).show();
					repeatNone();				
				}else if (status==3) {
					status=4;
					loop.setBackgroundResource(R.drawable.img_appwidget91_playmode_shuffle);					
					Toast.makeText(PlayActivity.this,"随机播放", Toast.LENGTH_SHORT).show();									
					shuffleMusic();	
				}
				break;
			case R.id.alubnlayout:
				albumLayout.startAnimation(mHiddenAction);
				albumLayout.setVisibility(View.INVISIBLE);
				mLrcView.setVisibility(View.VISIBLE);
				break;
			case R.id.play_pre_btn:		//上一首
				previousMusic();
				break;
			case R.id.play_next_btn:		//下一首
				nextMusic();
				break;
			case R.id.play_play_btn:		//播放
				if (isFirstTime) {
					isFirstTime=false;
					if (isPlaying){
						playBtn.setBackgroundResource(R.drawable.img_lockscreen_play_normal);
						toSendingService(AppConstant.PlayerMsg.PAUSE_MSG);					
						Log.i("playactivity 正在播放", ""+isPlaying);
						isPlaying=false;
						playThread.setSuspend(true);

					}else {
						playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
						toSendingService(AppConstant.PlayerMsg.PLAY_MSG);
						isPlaying=true;
						Log.i("playactivity 暂停", ""+isPlaying);
						playThread.setSuspend(false);
					}
				} else {
					if (isPlaying){
						playBtn.setBackgroundResource(R.drawable.img_lockscreen_play_normal);
						toSendingService(AppConstant.PlayerMsg.PAUSE_MSG);					
						isPlaying = false;			
						Log.i("playactivity 正在播放", ""+isPlaying);
						playThread.setSuspend(true);

					}else {
						playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
						toSendingService(AppConstant.PlayerMsg.CONTINUE_MSG);
						isPlaying = true;
						Log.i("playactivity 暂停", ""+isPlaying);
						playThread.setSuspend(false);
					}
				}
				break;
			case R.id.lanmu:
				Intent intent=new Intent();
				Log.i("lanmu", "isplaying"+isPlaying);
				intent.putExtra("isPlaying", isPlaying);
				intent.putExtra("status", status);
				setResult(0, intent);//栏目按钮
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
			setResult(0, intent);//栏目按钮
		}
		return super.onKeyDown(keyCode, event);
	}
	private class SeekBarChangeListener implements OnSeekBarChangeListener {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			mLrcView.seekTo(progress, true,fromUser);
			if (fromUser){
				audioTrackChange(progress);		//用户控制进度的改变发送给服务器
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
	 *	用户手动改变进度条 
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
	 * 播放音乐
	 */
	public void play(){
		playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
		toSendingService(AppConstant.PlayerMsg.PLAY_MSG);
	}

	/**
	 * 随机播放
	 */
	public void shuffleMusic(){
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 4);
		sendBroadcast(intent);
	}

	/**
	 * 单曲循环
	 */
	public void repeatOne(){
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 1);
		sendBroadcast(intent);
	}

	/**
	 * 顺序播放
	 */
	public void repeatNone(){
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 3);
		sendBroadcast(intent);
	}

	/**
	 * 上一首
	 */
	public void previousMusic(){
		playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
		toSendingService(AppConstant.PlayerMsg.PRIVIOUS_MSG);
		isPlaying = true;
	}

	/**
	 * 下一首
	 */
	public void nextMusic(){
		playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
		toSendingService(AppConstant.PlayerMsg.NEXT_MSG);
		isPlaying = true;
	}
	/*
	 * 给服务器发送消息
	 */
	public void toSendingService( int msg) {

		Intent intent = new Intent();
		intent.setAction("com.example.music.MUSIC_SERVICE");
		intent.putExtra("listPosition", listPosition);
		Log.i("test", "给服务器发送的歌单"+listPosition);
		intent.putExtra("MSG", msg); 
		startService(intent);
	}
	/*
	 * 联网获取歌词www.geci.me
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
	 *		专辑封面 旋转线程
	 */
	class PlayThread extends Thread{

		private boolean suspend = false;  //默认运行

		private String control = ""; // 只是需要一个对象而已，这个对象没有实际意义  

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
	 * 文件下载
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
				while((len = is.read(bytes))!=-1)	// 从输入流中读取一定数量的字节，并将其存储在缓冲区数组 bytes 中。
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
	 * 得到文件的保存路径
	 * @return
	 * @throws IOException
	 */
	private String getPath(String title) {
		String path = FileUtil.setMkdir(this)+File.separator+title+".lrc";
		return path;
	}

	/**
	 * 解析和处理服务器返回歌词数据
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
	 * 获取歌词List集合
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
	 * 专辑封面动画
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
