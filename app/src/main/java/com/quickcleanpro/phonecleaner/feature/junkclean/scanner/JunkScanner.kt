package com.quickcleanpro.phonecleaner.feature.junkclean.scanner

import com.quickcleanpro.phonecleaner.feature.junkclean.model.JunkCategory
import com.quickcleanpro.phonecleaner.feature.junkclean.model.JunkFile

/**
 * 闁搞劌鍟┃鍥棘閸ワ附顐介柟娈垮亝瀵潡宕抽妸锕€澶嶉敓?- 缂佹稒鐗滈弳鎰熼垾宕囩闁汇劌瀚悧瀹犵疀閸愨晛鈻曢敓?
 *
 * 婵絽绻嬬粩瀵哥矓瀹ュ懐鈧噣宕烽崜褑顫﹂柛銊ヮ儏椤曨喗鎯旈弬鍓ь伇濞戞搩浜滈崣鎸庢媴閹捐埖鐣遍柟娈垮亝瀵潡宕抽妸銉ф澖闁绘粌搴滅槐婵堚偓鍦仧楠炲洭寮甸浣稿閿?
 *
 * 闁绘粎澧楀﹢渚€骞嶉锝呬紟闁革絻鍔岄悿鍕偝鐢喚绐?
 * - CacheScanner闁挎稒纰嶆竟鍌炲箵韫囨挾瀹夐柣顫妼閹锋壆鍖栭懡銈囧煚缂傚倹鎸搁悺銊╂儎椤旇偐绉?
 * - TempFileScanner闁挎稒纰嶆竟鍌炲箵韫囧骸顦查柡鍐煐閺嬪啯绂?.tmp/.log閿?
 * - ApkScanner闁挎稒纰嶆竟鍌炲箵韫囨挸鍤掑☉鎾愁儓濞村洭鎯冮崙姗甂閻庣懓顦抽ˉ濠囧礌?
 * - DuplicateFileScanner闁挎稒宀搁埀顒佷亢缁诲啴宕崼婵堢憱闁稿﹤鍚嬮ˉ鍛圭€ｎ喖娅㈠璺虹У閺嬪啯绂?
 *
 * 闁规鍋呭鍨规担琛℃煠闁挎稒鐡揺pository闂侇剙绉村濠氬箥閳ь剟寮垫繅绫nner -> 婵絽绻嬮柌娓焎anner闁规鍋呭鍧楁偋閻熸壆鏆伴柣鈺婂枛閿?-> 閺夆晜鏌ㄥú鏈杣nkFile闁告帗顨夐敓?
 */
interface JunkScanner {
    val category: JunkCategory

    suspend fun scan(): List<JunkFile>

    fun getProgress(): Float
}
