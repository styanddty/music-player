package com.example.music.service;

import java.util.Collections;
import java.util.List;













import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.music.R;
import com.example.music.activity.HomeActivity;
import com.example.music.activity.PlayActivity;
import com.example.music.modle.AppConstant;
import com.example.music.modle.Mp3Info;
import com.example.music.utils.MediaUtils;
import com.example.sortmusiclistview.CharacterParser;
import com.example.sortmusiclistview.PinyinComparator;

public class PlayerService extends Service {

	private MediaPlayer mediaPlayer;		//媒体播放器对象
	private int msg;		
	private boolean isPause;		//暂停状态
	private int current ;		//记录当前正在播放的音乐
	private List<Mp3Info> mp3Infos;		//存放Mp3Info对象的集合
	private int status;			//播放状态，默认为顺序播放
	private MyReceiver myReceiver;		//自定义广播接收器
	private int currentTime;		//当前播放进度
	private int duration;		//播放进度

	//noti
		private Notification notification;
		public final static String KEY_PLAYID = "playID";
			
		//noti
		public final static String ACTION_PALY = "com.music.PlayerService.Play";
		public final static String ACTION_PALY_PAUSE = "com.music.PlayerService.Play_PAUSE";
		public final static String ACTION_STOP = "com.music.PlayerService.STOP";
		public final static String ACTION_NEXT = "com.music.PlayerService.NEXT";
		public final static String ACTION_PREVIOUS = "com.music.PlayerService.PREVIOUS";
		private static final int NOTIFICATION_ID = 1000;

	public static final String UPDATE_MUSIC = "com.music.action.UPDATE_MUSIC";
	public static final String CTL_ACTION = "com.music.action.CTL_ACTION";  
	public static final String MUSIC_CURRENT = "com.music.action.MUSIC_CURRENT";  //更新音乐播放时间
	public static final String MUSIC_DURATION = "com.music.action.MUSIC_DURATION";  //新音乐长度更新
	public static final String MUSIC_SERVICE = "com.example.music.MUSIC_SERVICE"; 

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 1){
				if (mediaPlayer !=null){
					currentTime = mediaPlayer.getCurrentPosition();
					Intent intent = new Intent();
					intent.setAction(MUSIC_CURRENT);
					intent.putExtra("currentTime", currentTime);
					sendBroadcast(intent);
					handler.sendEmptyMessageDelayed(1, 1000);		//自身循环间隔1s发送一次
				}
			}
		};
	};
	
	public void onCreate() {
		super.onCreate();
		initBroadcast();
		Log.i("test", "服务已经启动");
		mediaPlayer = new MediaPlayer();
//		mp3Infos =(MediaUtils.getMp3Infos(getApplicationContext()));
		mp3Infos =filledData(MediaUtils.getMp3Infos(getApplicationContext()));
		PinyinComparator pinyinComparator = new PinyinComparator();
		Collections.sort(mp3Infos, pinyinComparator);
		/**
		 * 设置音乐播放完成时的监听器
		 */
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				next();
			}
		});

		myReceiver = new MyReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(PlayActivity.CTL_ACTION);
		registerReceiver(myReceiver, filter);
		/*
		 * 参数初始化
		 */
		status=3;
		
	}

	/**
	 * 获取随机位置
	 * @param end
	 * @return
	 */
	protected int getRandomIndex(int end){
		int index = (int)(Math.random()*end);
		return index;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	@Deprecated
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.i("test", "onStart");
		//		String action=intent.getAction();
		int tcurrent = intent.getIntExtra("listPosition", -1);		//当前播放歌曲在mp3Infos的位置
		Log.i("test", "action music-service");
		Log.i("test", "服务器播放的歌曲"+Integer.toString(tcurrent));
		Log.i("test", mp3Infos.get(tcurrent).getTitle());
		Log.i("test", "status="+Integer.toString(status));
		if (status==3) {
			current=tcurrent;
			Log.i("current1", "current="+Integer.toString(current));
		}
		if (status==1) {
			Log.i("current2", "current="+Integer.toString(current));
			current=tcurrent;
		}
		if (status==4) {
			current=getRandomIndex(mp3Infos.size() -1);
			Log.i("current3", "current="+Integer.toString(current));
		}	
		msg = intent.getIntExtra("MSG", 0);
		if (msg == AppConstant.PlayerMsg.PLAY_MSG){		//播放音乐
			Log.i("音乐服务中的msg","PlayerMsg.PLAY_MSG");
			play(0);

		}else if (msg == AppConstant.PlayerMsg.PAUSE_MSG) {		//暂停
			Log.i("音乐服务中的msg","PlayerMsg.PAUSE_MSG");
			pause();

		}else if (msg == AppConstant.PlayerMsg.STOP_MSG) {		//停止
			Log.i("音乐服务中的msg","PlayerMsg.STOP_MSG");
			stop();

		}else if (msg == AppConstant.PlayerMsg.CONTINUE_MSG) {		
			Log.i("音乐服务中的msg","PlayerMsg.CONTINUE_MSG");
			resume();
		}else if (msg == AppConstant.PlayerMsg.PRIVIOUS_MSG ) {
			Log.i("音乐服务中的msg","PlayerMsg.PRIVIOUS_MSG");
			previous();
		}else if (msg == AppConstant.PlayerMsg.NEXT_MSG) {
			Log.i("音乐服务中的msg","PlayerMsg.NEXT_MSG");
			next();
		}else if (msg == AppConstant.PlayerMsg.PROGRESS_CHANGE) {
			Log.i("音乐服务中的msg","PlayerMsg.PROGRESS_CHANGE");
			currentTime = intent.getIntExtra("progress", -1);
			play(currentTime);
		}else if (msg == AppConstant.PlayerMsg.PLAYING_MSG) {
			handler.sendEmptyMessage(1);
		}
		Log.i("test", "onStart ------->end");
	}

	/**
	 *播放音乐 
	 */
	private void play(int currentTime){
		try {
			mediaPlayer.reset();
			Mp3Info mp3Info = mp3Infos.get(current);
			String url=mp3Info.getUrl();
			mediaPlayer.setDataSource(url);
			mediaPlayer.prepareAsync();
			mediaPlayer.setOnPreparedListener(new PreparedListener(currentTime));
			handler.sendEmptyMessage(1);
			noti(mp3Info.getTitle(), mp3Info.getArtist(), MediaUtils.getArtwork(this, mp3Info.getId(), mp3Info.getAlbumId(), 
					true, true));

		} catch (Exception e) {
		}
	}

	/**
	 * 暂停音乐
	 */
	private void pause(){
		if (mediaPlayer !=null && mediaPlayer.isPlaying()){
			mediaPlayer.pause();
			isPause = true;
		}
	}

	private void resume(){
		if (isPause){
			mediaPlayer.start();
			isPause = false;
		}
	}

	/**
	 * 上一首
	 */
	private void previous(){
		if (status==1) {

		}
		if (status==3) {
			current--;
			if (current==-1) {
				current=mp3Infos.size()-1;
			}
		}
		if (status==4) {
			current=getRandomIndex(mp3Infos.size()-1);
		}
		Mp3Info mp3Info = mp3Infos.get(current);
		if (mp3Info != null) {
			try {
				
				noti(mp3Info.getTitle(), mp3Info.getArtist(), MediaUtils.getArtwork(this, mp3Info.getId(), mp3Info.getAlbumId
(), true, true));
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		toSendUpdateMusic();
		play(0);
	}

	/**
	 * 下一首
	 */
	private void next(){
		if (status==1) {
		}
		if (status==3) {
			current++;
			if (current==mp3Infos.size()) {
				current=0;
			}

		}
		if (status==4) {
			current=getRandomIndex(mp3Infos.size()-1);
		}
		Mp3Info mp3Info = mp3Infos.get(current);
		if (mp3Info != null) {
			try {
				
				noti(mp3Info.getTitle(), mp3Info.getArtist(), MediaUtils.getArtwork(this, mp3Info.getId(), mp3Info.getAlbumId
(), true, true));
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		toSendUpdateMusic();
		play(0);
	}
	/*
	 * 发送播放歌曲改变广播
	 */
	void toSendUpdateMusic(){
		Intent sendIntent = new Intent(UPDATE_MUSIC);
		sendIntent.putExtra("current", current);
		sendBroadcast(sendIntent);
	}


	/**
	 * 停止音乐
	 */
	private void stop(){
		if (mediaPlayer != null){
			mediaPlayer.stop();
			try {
				mediaPlayer.prepare();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onDestroy() {
		Log.i("service destroy", ""+current);
		if (mediaPlayer != null){
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

	/**
	 * 实现一个OnpreparLister接口，当音乐准备好的时候开始播放
	 */
	private final class PreparedListener implements OnPreparedListener{

		private int currentTime;

		public PreparedListener(int currentTime){
			this.currentTime = currentTime;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			mediaPlayer.start();
			if (currentTime >0){	//如果音乐不是从头播放
				mediaPlayer.seekTo(currentTime);
			}
			Intent intent = new Intent();
			intent.setAction(MUSIC_DURATION);
			duration = mediaPlayer.getDuration();
			intent.putExtra("duration", duration);
			sendBroadcast(intent);
		}
	}
	/**
	 * 为ListView填充数据
	 * 
	 * @param date
	 * @return
	 */
	@SuppressLint("DefaultLocale")
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
	public class MyReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i("test", "onreceive");
			String action=intent.getAction();
			if (action.equals(PlayActivity.CTL_ACTION)) {
				int control = intent.getIntExtra("control",-1);
				switch (control) {
				case 1:
					status = 1;		//播放状态设置为1：单曲循环
					break;
				case 3:
					status = 3;		//播放状态设置为3：顺序播放
					break;
				case 4:
					status = 4;		//播放状态设置为4：随机播放
					break;
				}
			}
		}

	}
	
	private void initBroadcast() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_PALY);
		filter.addAction(ACTION_PALY_PAUSE);
		filter.addAction(ACTION_NEXT);
		filter.addAction(ACTION_PREVIOUS);
		filter.addAction(ACTION_STOP);
		registerReceiver(receiver, filter);
	}
				//noti
			private void notiprevious(){
				
				toSendUpdateMusic();
				
				Mp3Info mp3Info = mp3Infos.get(current != 0 ? --current : (current = mp3Infos.size()-1));
				if (mp3Info != null) {
					try {
						String path1 = mp3Infos.get(current).getUrl();
						mediaPlayer.setDataSource(path1);
						mediaPlayer.prepareAsync();
						noti(mp3Info.getTitle(), mp3Info.getArtist(), MediaUtils.getArtwork(this, mp3Info.getId(), mp3Info.
getAlbumId(), true, true));
						
//						Intent sendIntent = new Intent(UPDATE_ACTION);
//						sendIntent.putExtra("current", current);
//						//发送广播，将被Activity组件中的BroadcastReceiver接受
//						sendBroadcast(sendIntent);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				play(0);
			}
			
			
			//noti
	
			private void notiplay(int currentTime){
				
				if(mediaPlayer.isPlaying())
				{
					mediaPlayer.pause();
				}else {
					mediaPlayer.start();
				}
						
			}
		//noti
			private void notinext(){
				
				toSendUpdateMusic();
				Mp3Info mp3Info = mp3Infos.get(current != mp3Infos.size() -1 ? ++current : (current = 0));
				if (mp3Info != null) {
					try {
						String path1 = mp3Infos.get(current).getUrl();
						mediaPlayer.setDataSource(path1);
						mediaPlayer.prepareAsync();
						noti(mp3Info.getTitle(), mp3Info.getArtist(), MediaUtils.getArtwork(this, mp3Info.getId(), mp3Info.
getAlbumId(), true, true));
						
//						Intent sendIntent = new Intent(UPDATE_ACTION);
//						sendIntent.putExtra("current", current);
//						sendBroadcast(sendIntent);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				
				play(0);
			}
	
	private void noti(String songName, String singerName, Bitmap musicImage) {
		// assign the song name to songName
		// PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),
		// 0, new Intent(getApplicationContext(), MainACtivity.class),
		// PendingIntent.FLAG_UPDATE_CURRENT);
		if (notification == null) {
			notification = new Notification();
			notification.tickerText = "开始播放：" + songName;
			notification.icon = R.drawable.music;
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			notification.flags |= Notification.FLAG_NO_CLEAR;
			RemoteViews remoteViews = new RemoteViews(getPackageName(),
					R.layout.notification);

			// 设置歌的标题
			remoteViews.setTextViewText(R.id.notiname, songName);
			if (musicImage != null) {
				remoteViews.setImageViewBitmap(R.id.notiicon, musicImage);
			}
			// 设置通知上一曲点击事件
			PendingIntent prePIntent = PendingIntent.getBroadcast(
					getBaseContext(), 0, new Intent(
							PlayerService.ACTION_PREVIOUS),
					PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.notipre, prePIntent);
			// 设置通知播放点击事件
			PendingIntent playPIntent = PendingIntent.getBroadcast(
					getBaseContext(), 0, new Intent(
							PlayerService.ACTION_PALY_PAUSE),
					PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.notiplay, playPIntent);
			// 设置通知下一曲点击事件
			PendingIntent nextPIntent = PendingIntent.getBroadcast(
					getBaseContext(), 0, new Intent(
							PlayerService.ACTION_NEXT),
					PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.notinext, nextPIntent);

			notification.contentView = remoteViews;
		}else {
			notification.contentView.setTextViewText(R.id.notiname, songName);
			if (musicImage != null) {
				notification.contentView.setImageViewBitmap(R.id.notiicon, musicImage);
			}
		}
		// notification.setLatestEventInfo(getApplicationContext(),
		// "MusicPlayerSample", "Playing: " + songName, pi);
		startForeground(NOTIFICATION_ID, notification);
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_PALY.equals(action)) {
				int musicId = intent.getIntExtra(KEY_PLAYID, 0);
				if (musicId != 0) {
					play(musicId);
				}

			} else if (ACTION_PALY_PAUSE.equals(action)) {
				notiplay(0);
			} else if (ACTION_STOP.equals(action)) {
				stop();
			} else if (ACTION_PREVIOUS.equals(action)) {
				notiprevious();
			} else if (ACTION_NEXT.equals(action)) {
				notinext();
			}

		}
	};

}
