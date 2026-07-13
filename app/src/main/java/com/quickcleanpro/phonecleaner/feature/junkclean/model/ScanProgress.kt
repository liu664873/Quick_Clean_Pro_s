package com.quickcleanpro.phonecleaner.feature.junkclean.model

/**
 * 閸ㄥ啫婧囬幍顐ｅ伎鏉╂稑瀹抽悩鑸碘偓?*
 * Repository 闁俺绻冪拠銉δ侀崹瀣倻閹殿偅寮挎い鐢告桨閹镐胶鐢诲Ч鍥ㄥГ鏉╂稑瀹?ViewModel 閸愬秷娴嗛幑顫礋妞ょ敻娼扮仦鏇犮仛閻樿埖鈧?*/
data class ScanProgress(
    val percent: Float = 0f,
    val currentCategory: JunkCategory? = null,
    val foundCount: Int = 0,
    val foundSize: Long = 0,
) {
    companion object {
        /** 鐏忔碍婀鈧慨瀣閹诲繑妞傞惃鍕敄鏉╂稑瀹*/
        val IDLE = ScanProgress(0f)

        /** 閹殿偅寮跨€瑰本鍨氶崜宥囨暏娴滃骸鎮庨獮鑸垫付缂佸牏绮ㄩ弸婊呮畱鏉╂稑瀹抽崜宥囩磻*/
        val COMPLETE_PREFIX = ScanProgress(99f)
    }
}
