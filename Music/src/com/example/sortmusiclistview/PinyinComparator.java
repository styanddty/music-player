package com.example.sortmusiclistview;

import java.util.Comparator;

import com.example.music.modle.Mp3Info;

/**
 * 
 * @author xiaanming
 *
 */
public class PinyinComparator implements Comparator<Mp3Info> {

	public int compare(Mp3Info o1, Mp3Info o2) {
		if (o1.getSortLetters().equals("@")
				|| o2.getSortLetters().equals("#")) {
			return -1;
		} else if (o1.getSortLetters().equals("#")
				|| o2.getSortLetters().equals("@")) {
			return 1;
		} else {
			return o1.getSortLetters().compareTo(o2.getSortLetters());
		}
	}

}
