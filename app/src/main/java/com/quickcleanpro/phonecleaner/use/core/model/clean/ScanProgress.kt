package com.quickcleanpro.phonecleaner.use.core.model.clean

/**
 * 垃圾扫描进度状态 *
 * Repository 通过该模型向扫描页面持续汇报进度 ViewModel 再转换为页面展示状态 */
data class ScanProgress(
    val percent: Float = 0f,
    val currentCategory: JunkCategory? = null,
    val foundCount: Int = 0,
    val foundSize: Long = 0,
) {
    companion object {
        /** 尚未开始扫描时的空进度*/
        val IDLE = ScanProgress(0f)

        /** 扫描完成前用于合并最终结果的进度前缀*/
        val COMPLETE_PREFIX = ScanProgress(99f)
    }
}
