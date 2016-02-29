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

	private MediaPlayer mediaPlayer;		//ý�岥��������
	private int msg;		
	private boolean isPause;		//��ͣ״̬
	private int current ;		//��¼��ǰ���ڲ��ŵ�����
	private List<Mp3Info> mp3Infos;		//���Mp3Info����ļ���
	private int status;			//����״̬��Ĭ��Ϊ˳�򲥷�
	private MyReceiver myReceiver;		//�Զ���㲥������
	private int currentTime;		//��ǰ���Ž���
	private int duration;		//���Ž���

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
	public static final String MUSIC_CURRENT = "com.music.action.MUSIC_CURRENT";  //�������ֲ���ʱ��
	public static final String MUSIC_DURATION = "com.music.action.MUSIC_DURATION";  //�����ֳ��ȸ���
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
					handler.sendEmptyMessageDelayed(1, 1000);		//����ѭ�����1s����һ��
				}
			}
		};
	};
	
	public void onCreate() {
		super.onCreate();
		initBroadcast();
		Log.i("test", "�����Ѿ�����");
		mediaPlayer = new MediaPlayer();
//		mp3Infos =(MediaUtils.getMp3Infos(getApplicationContext()));
		mp3Infos =filledData(MediaUtils.getMp3Infos(getApplicationContext()));
		PinyinComparator pinyinComparator = new PinyinComparator();
		Collections.sort(mp3Infos, pinyinComparator);
		/**
		 * �������ֲ������ʱ�ļ�����
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
		 * ������ʼ��
		 */
		status=3;
		
	}

	/**
	 * ��ȡ���λ��
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
		int tcurrent = intent.getIntExtra("listPosition", -1);		//��ǰ���Ÿ�����mp3Infos��λ��
		Log.i("test", "action music-service");
		Log.i("test", "���������ŵĸ���"+Integer.toString(tcurrent));
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
		if (msg == AppConstant.PlayerMsg.PLAY_MSG){		//��������
			Log.i("���ַ����е�msg","PlayerMsg.PLAY_MSG");
			play(0);

		}else if (msg == AppConstant.PlayerMsg.PAUSE_MSG) {		//��ͣ
			Log.i("���ַ����е�msg","PlayerMsg.PAUSE_MSG");
			pause();

		}else if (msg == AppConstant.PlayerMsg.STOP_MSG) {		//ֹͣ
			Log.i("���ַ����е�msg","PlayerMsg.STOP_MSG");
			stop();

		}else if (msg == AppConstant.PlayerMsg.CONTINUE_MSG) {		
			Log.i("���ַ����е�msg","PlayerMsg.CONTINUE_MSG");
			resume();
		}else if (msg == AppConstant.PlayerMsg.PRIVIOUS_MSG ) {
			Log.i("���ַ����е�msg","PlayerMsg.PRIVIOUS_MSG");
			previous();
		}else if (msg == AppConstant.PlayerMsg.NEXT_MSG) {
			Log.i("���ַ����е�msg","PlayerMsg.NEXT_MSG");
			next();
		}else if (msg == AppConstant.PlayerMsg.PROGRESS_CHANGE) {
			Log.i("���ַ����е�msg","PlayerMsg.PROGRESS_CHANGE");
			currentTime = intent.getIntExtra("progress", -1);
			play(currentTime);
		}else if (msg == AppConstant.PlayerMsg.PLAYING_MSG) {
			handler.sendEmptyMessage(1);
		}
		Log.i("test", "onStart ------->end");
	}

	/**
	 *�������� 
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
	 * ��ͣ����
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
	 * ��һ��
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
	 * ��һ��
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
	 * ���Ͳ��Ÿ����ı�㲥
	 */
	void toSendUpdateMusic(){
		Intent sendIntent = new Intent(UPDATE_MUSIC);
		sendIntent.putExtra("current", current);
		sendBroadcast(sendIntent);
	}


	/**
	 * ֹͣ����
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
	 * ʵ��һ��OnpreparLister�ӿڣ�������׼���õ�ʱ��ʼ����
	 */
	private final class PreparedListener implements OnPreparedListener{

		private int currentTime;

		public PreparedListener(int currentTime){
			this.currentTime = currentTime;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			mediaPlayer.start();
			if (currentTime >0){	//������ֲ��Ǵ�ͷ����
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
	 * ΪListView�������
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
	public class MyReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i("test", "onreceive");
			String action=intent.getAction();
			if (action.equals(PlayActivity.CTL_ACTION)) {
				int control = intent.getIntExtra("control",-1);
				switch (control) {
				case 1:
					status = 1;		//����״̬����Ϊ1������ѭ��
					break;
				case 3:
					status = 3;		//����״̬����Ϊ3��˳�򲥷�
					break;
				case 4:
					status = 4;		//����״̬����Ϊ4���������
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
//						//���͹㲥������Activity����е�BroadcastReceiver����
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
			notification.tickerText = "��ʼ���ţ�" + songName;
			notification.icon = R.drawable.music;
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			notification.flags |= Notification.FLAG_NO_CLEAR;
			RemoteViews remoteViews = new RemoteViews(getPackageName(),
					R.layout.notification);

			// ���ø�ı���
			remoteViews.setTextViewText(R.id.notiname, songName);
			if (musicImage != null) {
				remoteViews.setImageViewBitmap(R.id.notiicon, musicImage);
			}
			// ����֪ͨ��һ������¼�
			PendingIntent prePIntent = PendingIntent.getBroadcast(
					getBaseContext(), 0, new Intent(
							PlayerService.ACTION_PREVIOUS),
					PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.notipre, prePIntent);
			// ����֪ͨ���ŵ���¼�
			PendingIntent playPIntent = PendingIntent.getBroadcast(
					getBaseContext(), 0, new Intent(
							PlayerService.ACTION_PALY_PAUSE),
					PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.notiplay, playPIntent);
			// ����֪ͨ��һ������¼�
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
