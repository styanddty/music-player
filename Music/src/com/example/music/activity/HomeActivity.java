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
	private ListView mMusiclist;	//音乐列表
	private List<Mp3Info> mp3Infos = null; 
	private TextView songTextView;		//歌曲标题
	private TextView songerTextView;	//歌手名称
	private ImageView nextBtn;		//下一首
	private ImageView preBtn;		//上一首
	private ImageView playBtn;		//播放(播放、暂停)
//	private ImageView tmpMusicSelect;
	private boolean isFirstTime ;
	private boolean isPlaying;		//正在播放
	private int listPosition ;		//标识列表位置
	private int status ;
	private HomeReceiver homeReceiver;
	private SharedPreferences pref;
	private SharedPreferences.Editor editor;
	private MyBaseAdapter baseAdapter;
	public static final String CTL_ACTION     = "com.music.action.CTL_ACTION";        //控制动作  
	public static final String UPDATE_MUSIC  = "com.music.action.UPDATE_MUSIC";
	/*
	 * 歌曲搜索效果添加start
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
	 * 上次第一个可见元素，用于滚动时记录标识。
	 */
	private int lastFirstVisibleItem = -1;
	/**
	 * 汉字转换成拼音的类
	 */
	private CharacterParser characterParser;
	//	private List<Mp3Info> mp3Infos;

	/**
	 * 根据拼音来排列ListView里面的数据类
	 */
	private PinyinComparator pinyinComparator;
	/*
	 * 歌曲搜索效果添加end
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("test", "调用home中create");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_home);
		initViews();
//		mMusiclist = (ListView) findViewById(R.id.music_list); 
//		mMusiclist = (ListView) findViewById(R.id.country_lvcountry);
//		mMusiclist.setOnItemClickListener(new MusicListItemClickListener()); 

//		mp3Infos = MediaUtils.getMp3Infos(getApplicationContext());  //获取歌曲对象集合  
//		mp3Infos =filledData(MediaUtils.getMp3Infos(getApplicationContext()));
//		baseAdapter = new MyBaseAdapter(this,mp3Infos);
//		mMusiclist.setAdapter(baseAdapter);    //显示歌曲列表  

		finViewById();
		
		setViewOnclickListener();

		homeReceiver = new HomeReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(UPDATE_MUSIC);  
		registerReceiver(homeReceiver, filter);
		/*
		 * 参数初始化
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
		Log.i("执行destroy", "----------》");
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
		Log.i("onActivityResult", "调用onActivityResult");
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
		Log.i("test", "调用home中resume");
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
	 * 给每一个按钮设置监听器 
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
			case R.id.pre_btn:		//上一首
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
	 * 显示专辑封面
	 */
	private void showArtwork(Mp3Info mp3Info) {
		Bitmap bm = MediaUtils.getArtwork(this, mp3Info.getId(), mp3Info.getAlbumId(), true, true);
		if(bm != null) {
			album.setImageBitmap(bm);	//显示专辑封面图片
		} else {
			bm = MediaUtils.getDefaultArtwork(this, false);
			album.setImageBitmap(bm);	//显示专辑封面图片
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
	 *下一首歌曲 
	 */
	public void next(){
		toSendingService(AppConstant.PlayerMsg.NEXT_MSG); 
	}

	/**
	 *上一首歌曲 
	 */
	public void previous(){
		toSendingService(AppConstant.PlayerMsg.PRIVIOUS_MSG); 
	}

	public void play(){
		toSendingService(AppConstant.PlayerMsg.PLAY_MSG);
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
	 * 改变界面
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
	 *自定义广播接收器，接受服务器返回的广播信息
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
	 * 歌词搜索部分start
	 */
	private void initViews() {
		titleLayout = (LinearLayout) findViewById(R.id.title_layout);
		title = (TextView) this.findViewById(R.id.title_layout_catalog);
		tvNofriends = (TextView) this
				.findViewById(R.id.title_layout_no_friends);

		// 实例化汉字转拼音类
		characterParser = CharacterParser.getInstance();
		pinyinComparator = new PinyinComparator();

		sideBar = (SideBar) findViewById(R.id.sidrbar);
		dialog = (TextView) findViewById(R.id.dialog);
		sideBar.setTextView(dialog);

		// 设置右侧触摸监听
		sideBar.setOnTouchingLetterChangedListener(new OnTouchingLetterChangedListener() {

			@Override
			public void onTouchingLetterChanged(String s) {
				// 该字母首次出现的位置
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
				// 这里要利用adapter.getItem(position)来获取当前position所对应的对象
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
		// 根据a-z进行排序源数据

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

		// 根据输入框输入值的改变来过滤搜索
		mClearEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// 这个时候不需要挤压效果 就把他隐藏掉
				titleLayout.setVisibility(View.GONE);
				// 当输入框里面的值为空，更新为原来的列表，否则为过滤数据列表
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
	 * 为ListView填充数据
	 * 
	 * @param date
	 * @return
	 */
	@SuppressLint("DefaultLocale")
	private List<Mp3Info> filledData(List<Mp3Info> tMp3Infos) {
		Mp3Info tmp3Info=null;
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
	 * 根据输入框中的值来过滤数据并更新ListView
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

		// 根据a-z进行排序
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
	 * 根据ListView的当前位置获取分类的首字母的Char ascii值
	 */
	public int getSectionForPosition(int position) {
		return mp3Infos.get(position).getSortLetters().charAt(0);
	}

	/**
	 * 根据分类的首字母的Char ascii值获取其第一次出现该首字母的位置
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
 * 歌词搜索部分end
 */

}
