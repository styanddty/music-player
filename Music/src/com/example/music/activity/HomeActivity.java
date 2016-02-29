package com.example.music.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.example.music.R;
import com.example.music.adapter.MyBaseAdapter;
import com.example.music.modle.AppConstant;
import com.example.music.modle.Mp3Info;
import com.example.music.utils.MediaUtils;
import com.example.sortmusiclistview.CharacterParser;
import com.example.sortmusiclistview.ClearEditText;
import com.example.sortmusiclistview.PinyinComparator;
import com.example.sortmusiclistview.SideBar;
import com.example.sortmusiclistview.SideBar.OnTouchingLetterChangedListener;

public class HomeActivity extends Activity implements SectionIndexer{

	private ImageView album;
	private RelativeLayout homeRelativeLayout;
	private ListView mMusiclist;	//�����б�
	private List<Mp3Info> mp3Infos = null; 
	private TextView songTextView;		//��������
	private TextView songerTextView;	//��������
	private ImageView nextBtn;		//��һ��
	private ImageView preBtn;		//��һ��
	private ImageView playBtn;		//����(���š���ͣ)
//	private ImageView tmpMusicSelect;
	private boolean isFirstTime ;
	private boolean isPlaying;		//���ڲ���
	private int listPosition ;		//��ʶ�б�λ��
	private int status ;
	private HomeReceiver homeReceiver;
	private SharedPreferences pref;
	private SharedPreferences.Editor editor;
	private MyBaseAdapter baseAdapter;
	public static final String CTL_ACTION     = "com.music.action.CTL_ACTION";        //���ƶ���  
	public static final String UPDATE_MUSIC  = "com.music.action.UPDATE_MUSIC";
	/*
	 * ��������Ч�����start
	 */
	//	private ListView mMusiclist;
	private SideBar sideBar;
	private TextView dialog;
	//	private MyBaseAdapter baseAdapter;
	private ClearEditText mClearEditText;

	private LinearLayout titleLayout;
	private TextView title;
	private TextView tvNofriends;
	/**
	 * �ϴε�һ���ɼ�Ԫ�أ����ڹ���ʱ��¼��ʶ��
	 */
	private int lastFirstVisibleItem = -1;
	/**
	 * ����ת����ƴ������
	 */
	private CharacterParser characterParser;
	//	private List<Mp3Info> mp3Infos;

	/**
	 * ����ƴ��������ListView�����������
	 */
	private PinyinComparator pinyinComparator;
	/*
	 * ��������Ч�����end
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("test", "����home��create");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_home);
		initViews();
//		mMusiclist = (ListView) findViewById(R.id.music_list); 
//		mMusiclist = (ListView) findViewById(R.id.country_lvcountry);
//		mMusiclist.setOnItemClickListener(new MusicListItemClickListener()); 

//		mp3Infos = MediaUtils.getMp3Infos(getApplicationContext());  //��ȡ�������󼯺�  
//		mp3Infos =filledData(MediaUtils.getMp3Infos(getApplicationContext()));
//		baseAdapter = new MyBaseAdapter(this,mp3Infos);
//		mMusiclist.setAdapter(baseAdapter);    //��ʾ�����б�  

		finViewById();
		
		setViewOnclickListener();

		homeReceiver = new HomeReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(UPDATE_MUSIC);  
		registerReceiver(homeReceiver, filter);
		/*
		 * ������ʼ��
		 */
		pref = getSharedPreferences("data", MODE_PRIVATE);
		editor = pref.edit();
				editor.putBoolean("isPlaying", false);
				editor.commit();

		Log.i("service destroy", ""+listPosition);
		listPosition = pref.getInt("listPosition", 0);
		Log.i("home create", ""+listPosition);


		isFirstTime=true;
		isPlaying=pref.getBoolean("isPlaying", false);


	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(homeReceiver);
		editor.putInt("listPosition", listPosition);
		editor.putBoolean("isPlaying", isPlaying);
		editor.commit();
		super.onDestroy();
		Log.i("ִ��destroy", "----------��");
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode==0&&resultCode==0){
			isPlaying=data.getBooleanExtra("isPlaying",false);
			status=data.getIntExtra("status", 3);
			Log.i("status onactivity result", status+"");
		}
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", status);
		sendBroadcast(intent);

		editor.putInt("status", status);
		editor.commit();
		Log.i("onActivityResult", "����onActivityResult");
		Log.i("home onActivityResult isPlaying", ""+isPlaying);
	}

	@Override
	protected void onResume() {
		super.onResume();

		status= pref.getInt("status", 3);
		Log.i("AA", ""+status);
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", status);
		sendBroadcast(intent);
		Log.i("test", "����home��resume");
		//isPlaying  = pref.getBoolean("isPlaying", true);
		Log.i("home resume isPlaying", ""+isPlaying);
		changeHomeActivity();

	}
	@Override
	protected void onStop() {
		super.onStop();
		Log.i("stop", "-------->");
	}


	public void finViewById(){
		songTextView = (TextView) findViewById(R.id.song);
		songTextView.setSelected(true);
		songerTextView = (TextView) findViewById(R.id.songer);
		songerTextView.setSelected(true);
		nextBtn = (ImageView) findViewById(R.id.next_btn);
		preBtn = (ImageView) findViewById(R.id.pre_btn);
		playBtn = (ImageView) findViewById(R.id.play_btn);
		album = (ImageView) findViewById(R.id.album);
		homeRelativeLayout = (RelativeLayout) findViewById(R.id.home_relativeLayout);
	}

	/** 
	 * ��ÿһ����ť���ü����� 
	 */  
	private void setViewOnclickListener() {  
		ViewOnClickListener viewOnClickListener = new ViewOnClickListener();
		nextBtn.setOnClickListener(viewOnClickListener);
		preBtn.setOnClickListener(viewOnClickListener);
		playBtn.setOnClickListener(viewOnClickListener);
		album.setOnClickListener(viewOnClickListener);
		homeRelativeLayout.setOnClickListener(viewOnClickListener);

	}

	private class ViewOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.home_relativeLayout:
				playMusic(listPosition);
				break;
			case R.id.album:
				playMusic(listPosition);
				break;
			case R.id.play_btn:
				if (isFirstTime) {
					isFirstTime=false;
					if(isPlaying){
						playBtn.setBackgroundResource(R.drawable.img_lockscreen_play_normal);
						toSendingService(AppConstant.PlayerMsg.PAUSE_MSG);
						isPlaying = false;
					}else {
						playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
						toSendingService(AppConstant.PlayerMsg.PLAY_MSG);
						isPlaying = true;
					}
				}else {
					if(isPlaying){
						playBtn.setBackgroundResource(R.drawable.img_lockscreen_play_normal);
						toSendingService(AppConstant.PlayerMsg.PAUSE_MSG);
						isPlaying = false;
					}else {
						playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
						toSendingService(AppConstant.PlayerMsg.CONTINUE_MSG);
						isPlaying = true;
					}
				}
				break;
			case R.id.pre_btn:		//��һ��
				playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
				isPlaying = true;
				previous();
				break;
			case R.id.next_btn:
				playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
				isPlaying = true;
				next();
				break;
			}
		}  
	}


	/**
	 * ��ʾר������
	 */
	private void showArtwork(Mp3Info mp3Info) {
		Bitmap bm = MediaUtils.getArtwork(this, mp3Info.getId(), mp3Info.getAlbumId(), true, true);
		if(bm != null) {
			album.setImageBitmap(bm);	//��ʾר������ͼƬ
		} else {
			bm = MediaUtils.getDefaultArtwork(this, false);
			album.setImageBitmap(bm);	//��ʾר������ͼƬ
		}
	}


//	private class MusicListItemClickListener implements OnItemClickListener { 
//
//
//		@Override  
//		public void onItemClick(AdapterView<?> parent, View view, int position,  
//				long id) {  
//
//			listPosition = position; 
//			Log.i("position", Integer.toString(position));
//			isPlaying = true;
//			changeHomeActivity();
//			Log.i("test", "call changeHomeActibity");
//			play();          
//		}  
//
//	} 

	public void playMusic(int listPosition){
		if (mp3Infos != null){
			Intent intent = new Intent(HomeActivity.this,PlayActivity.class);
			intent.putExtra("listPosition", listPosition);
			intent.putExtra("isPlaying", isPlaying);
			intent.putExtra("isFirstTime" ,isFirstTime);
			intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
			intent.putExtra("status", status);
			startActivityForResult(intent, 0);
		}
	}


	/**
	 *��һ�׸��� 
	 */
	public void next(){
		toSendingService(AppConstant.PlayerMsg.NEXT_MSG); 
	}

	/**
	 *��һ�׸��� 
	 */
	public void previous(){
		toSendingService(AppConstant.PlayerMsg.PRIVIOUS_MSG); 
	}

	public void play(){
		toSendingService(AppConstant.PlayerMsg.PLAY_MSG);
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
	 * �ı����
	 */
	public void changeHomeActivity(){
		showArtwork(mp3Infos.get(listPosition));
//		for (int i=0;i<mp3Infos.size();i++){
//			mp3Infos.get(i).setState(false);
//		}
//		for (int i = 0; i < mMusiclist.getChildCount(); i++){
//			tmpMusicSelect = (ImageView) (mMusiclist.getChildAt(i).findViewById(R.id.music_select));
//			tmpMusicSelect.setVisibility(View.INVISIBLE);
//		}
//		mp3Infos.get(listPosition).setState(true);
		Log.i("test", Integer.toString(listPosition));
		Log.i("test", mp3Infos.get(listPosition).getTitle());
		Log.i("test", mp3Infos.get(listPosition).getArtist());
		baseAdapter.notifyDataSetChanged();
		Mp3Info mp3Info = mp3Infos.get(listPosition);
		songTextView.setText(mp3Info.getTitle());
		songerTextView.setText(mp3Info.getArtist());
		if (isPlaying) {
			playBtn.setBackgroundResource(R.drawable.img_lockscreen_pause_normal);
		} else {
			playBtn.setBackgroundResource(R.drawable.img_lockscreen_play_normal);;
		}
	}
	/**
	 *�Զ���㲥�����������ܷ��������صĹ㲥��Ϣ
	 */
	public class HomeReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(UPDATE_MUSIC)) {
				listPosition = intent.getIntExtra("current", -1);
				if (listPosition>=0){
					changeHomeActivity();
				}
			}
		}
	}
	/*
	 * �����������start
	 */
	private void initViews() {
		titleLayout = (LinearLayout) findViewById(R.id.title_layout);
		title = (TextView) this.findViewById(R.id.title_layout_catalog);
		tvNofriends = (TextView) this
				.findViewById(R.id.title_layout_no_friends);

		// ʵ��������תƴ����
		characterParser = CharacterParser.getInstance();
		pinyinComparator = new PinyinComparator();

		sideBar = (SideBar) findViewById(R.id.sidrbar);
		dialog = (TextView) findViewById(R.id.dialog);
		sideBar.setTextView(dialog);

		// �����Ҳഥ������
		sideBar.setOnTouchingLetterChangedListener(new OnTouchingLetterChangedListener() {

			@Override
			public void onTouchingLetterChanged(String s) {
				// ����ĸ�״γ��ֵ�λ��
				int position = baseAdapter.getPositionForSection(s.charAt(0));
				if (position != -1) {
					mMusiclist.setSelection(position);
				}

			}
		});

		mMusiclist = (ListView) findViewById(R.id.country_lvcountry);
		mMusiclist.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// ����Ҫ����adapter.getItem(position)����ȡ��ǰposition����Ӧ�Ķ���
				Toast.makeText(
						getApplication(),
						((Mp3Info) baseAdapter.getItem(position)).getTitle(),
						Toast.LENGTH_SHORT).show();
//				baseAdapter.updateListView(mp3Infos);
				listPosition = (int) baseAdapter.getItemId(position);
				Log.i("position", Integer.toString(position));
				isPlaying = true;
				
				changeHomeActivity();
				Log.i("test", "call changeHomeActibity");
				play();     
//				for (int i = 0; i < mp3Infos.size(); i++) {
//					mp3Infos.get(i).setState(false);
//				}
//				mp3Infos.get(position).setState(true);
	
			}
		});

		//		mp3Infos = filledData(getResources().getStringArray(R.array.date));
		mp3Infos =filledData(MediaUtils.getMp3Infos(getApplicationContext()));
		// ����a-z��������Դ����

		Collections.sort(mp3Infos, pinyinComparator);
		baseAdapter = new MyBaseAdapter(this, mp3Infos);
		mMusiclist.setAdapter(baseAdapter);
		mMusiclist.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				int section = getSectionForPosition(firstVisibleItem);
				int nextSection = getSectionForPosition(firstVisibleItem + 1);
				int nextSecPosition = getPositionForSection(+nextSection);
				if (firstVisibleItem != lastFirstVisibleItem) {
					MarginLayoutParams params = (MarginLayoutParams) titleLayout
							.getLayoutParams();
					params.topMargin = 0;
					titleLayout.setLayoutParams(params);
					title.setText(
							mp3Infos.get(getPositionForSection(section)).getSortLetters());
				}
				if (nextSecPosition == firstVisibleItem + 1) {
					View childView = view.getChildAt(0);
					if (childView != null) {
						int titleHeight = titleLayout.getHeight();
						int bottom = childView.getBottom();
						MarginLayoutParams params = (MarginLayoutParams) titleLayout
								.getLayoutParams();
						if (bottom < titleHeight) {
							float pushedDistance = bottom - titleHeight;
							params.topMargin = (int) pushedDistance;
							titleLayout.setLayoutParams(params);
						} else {
							if (params.topMargin != 0) {
								params.topMargin = 0;
								titleLayout.setLayoutParams(params);
							}
						}
					}
				}
				lastFirstVisibleItem = firstVisibleItem;
			}
		});
		mClearEditText = (ClearEditText) findViewById(R.id.filter_edit);

		// �������������ֵ�ĸı�����������
		mClearEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// ���ʱ����Ҫ��ѹЧ�� �Ͱ������ص�
				titleLayout.setVisibility(View.GONE);
				// ������������ֵΪ�գ�����Ϊԭ�����б�����Ϊ���������б�
				filterData(s.toString());
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
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
	 * ����������е�ֵ���������ݲ�����ListView
	 * 
	 * @param filterStr
	 */
	private void filterData(String filterStr) {
		List<Mp3Info> filterDateList = new ArrayList<Mp3Info>();

		if (TextUtils.isEmpty(filterStr)) {
			filterDateList = mp3Infos;
			tvNofriends.setVisibility(View.GONE);
		} else {
			filterDateList.clear();
			for (Mp3Info mp3Info : mp3Infos) {
				String name = mp3Info.getTitle();
				if (name.indexOf(filterStr.toString()) != -1
						|| characterParser.getSelling(name).startsWith(
								filterStr.toString())) {
					filterDateList.add(mp3Info);
				}
			}
		}

		// ����a-z��������
		Collections.sort(filterDateList, pinyinComparator);
		baseAdapter.updateListView(filterDateList);
		if (filterDateList.size() == 0) {
			tvNofriends.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public Object[] getSections() {
		return null;
	}

	/**
	 * ����ListView�ĵ�ǰλ�û�ȡ���������ĸ��Char asciiֵ
	 */
	public int getSectionForPosition(int position) {
		return mp3Infos.get(position).getSortLetters().charAt(0);
	}

	/**
	 * ���ݷ��������ĸ��Char asciiֵ��ȡ���һ�γ��ָ�����ĸ��λ��
	 */
	public int getPositionForSection(int section) {
		for (int i = 0; i < mp3Infos.size(); i++) {
			String sortStr = mp3Infos.get(i).getSortLetters();
			char firstChar = sortStr.toUpperCase().charAt(0);
			if (firstChar == section) {
				return i;
			}
		}
		return -1;
	}


/*
 * �����������end
 */

}
