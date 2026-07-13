package com.quickcleanpro.phonecleaner.feature.junkclean.model

/**
 * 娑撶粯绔婚悶鍡涙懠鐠侯垯鑵戦惃鍕€崷鐐瀮娴犺埖膩閸?*
 * 鐠囥儲膩閸ㄥ銆冪粈鍝勫嚒缂佸繗顫﹂幍顐ｅ伎閸ｃ劏鐦戦崚顐㈠毉閻ㄥ嫬褰插〒鍛倞閺傚洣娆?* 娴兼艾婀禒鎾崇氨閵嗕胶鏁ゆ笟瀣ㄢ偓浣稿彙娴滎偆濮搁幀浣告嫲缂佹挻鐏夋い鍏哥闂傜繝绱堕柅?*/
data class JunkFile(
    val id: String,
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val category: JunkCategory,
    val lastModified: Long = System.currentTimeMillis(),
) {
    /** 閺嶇厧绱￠崠鏍ф倵閻ㄥ嫭鏋冩禒璺恒亣鐏忓繑鏋冨*/
    val formattedSize: String
        get() = formatFileSize(fileSize)

    companion object {
        /**
         * 鐏忓棗鐡ч懞鍌涙殶閺嶇厧绱￠崠鏍﹁礋闂堛垹鎮?UI 閻ㄥ嫮鐓弬鍥ㄦ拱         */
        fun formatFileSize(bytes: Long): String =
            when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
                bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
                else -> "${"%.1f".format(bytes / (1024.0 * 1024 * 1024))} GB"
            }
    }
}
