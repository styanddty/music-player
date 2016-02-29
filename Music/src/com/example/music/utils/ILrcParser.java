package com.example.music.utils;

import java.util.List;

import com.example.music.modle.LrcRow;

/**
 * 歌词列表
 */
public interface ILrcParser {

	List<LrcRow> getLrcRows(String str);
}
