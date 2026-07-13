package com.quickcleanpro.phonecleaner.feature.junkclean.model

/**
 * 閸ㄥ啫婧囬弬鍥︽娑撴艾濮熼崚鍡欒 *
 * 閻劋绨幍顐ｅ伎閵嗕胶绮ㄩ弸婊堫暕鐟欏牆鎷板〒鍛倞濮瑰洦鈧崵鐡戞稉缁樼閻炲棝鎽肩捄顖ょ礉
 * 閸ョ姵顒濋弨鎯ф躬 domain 鐏炲倷缍旀稉楦挎硶 data 閿?presentation 閻ㄥ嫮菙鐎规碍膩閸ㄥ鎷? */
enum class JunkCategory(
    val displayName: String,
    val description: String,
) {
    CACHE("Cache Files", "App and system cache"),
    TEMP_FILE("Temp Files", "Temporary files in temp directories"),
    RESIDUAL("Residual Files", "Leftover files from uninstalled apps"),
    APK("APK Files", "Downloaded APK installers"),
    DUPLICATE("Duplicate Files", "Files with duplicate content"),
    LARGE_FILE("Large Files", "Files taking up too much space"),
}
