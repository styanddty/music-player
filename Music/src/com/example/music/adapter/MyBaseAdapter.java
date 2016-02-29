package com.example.music.adapter;

import java.util.List;




import com.example.music.R;
import com.example.music.modle.Mp3Info;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;


public class MyBaseAdapter extends BaseAdapter  implements SectionIndexer{
	
	private List<Mp3Info> mp3Infos;
	
	private Context mContext;

	public MyBaseAdapter(Context mContext, List<Mp3Info> mp3Infos) {
		this.mContext = mContext;
		this.mp3Infos = mp3Infos;
	}


	@Override
	public int getCount() {
		return mp3Infos.size();
	}

	/**
	 * 当ListView数据发生变化时,调用此方法来更新ListView
	 * 
	 * @param list
	 */
	public void updateListView(List<Mp3Info> mp3Infos) {
		this.mp3Infos = mp3Infos;
		notifyDataSetChanged();
	}


	public Object getItem(int position) {
		return mp3Infos.get(position);
	}

	public long getItemId(int position) {
		return position;
	}


	@SuppressLint("InflateParams")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder 	holder = new ViewHolder();
		if (convertView ==null){
			convertView = LayoutInflater.from(mContext).inflate(R.layout.music_list_item, null);	
			holder.title = (TextView) convertView.findViewById(R.id.music_title);
			holder.artist = (TextView) convertView.findViewById(R.id.music_Artist);
			holder.letter = (TextView) convertView.findViewById(R.id.catalog);
			convertView.setTag(holder);
		}else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		// 根据position获取分类的首字母的Char ascii值
		int section = getSectionForPosition(position);

		// 如果当前位置等于该分类首字母的Char的位置 ，则认为是第一次出现
		if (position == getPositionForSection(section)) {
			holder.letter.setVisibility(View.VISIBLE);
		    holder.letter.setText(mp3Infos.get(position).getSortLetters());
		} else {
			holder.letter.setVisibility(View.GONE);
		}
		holder.title.setText(mp3Infos.get(position).getTitle());
		holder.artist.setText(mp3Infos.get(position).getArtist());

		return convertView;
	}
	
    public final class ViewHolder{
        public TextView title;
        public TextView artist;
        public TextView letter;
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
	@SuppressLint("DefaultLocale")
	@Override
	public int getPositionForSection(int section) {
		for (int i = 0; i < getCount(); i++) {
			String sortStr = mp3Infos.get(i).getSortLetters();
			char firstChar = sortStr.toUpperCase().charAt(0);
			if (firstChar == section) {
				return i;
			}
		}

		return -1;
	}

	@Override
	public Object[] getSections() {
		return null;
	}
    
}
