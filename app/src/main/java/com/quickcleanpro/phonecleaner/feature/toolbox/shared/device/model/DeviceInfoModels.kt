package com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model

/**
 * 閻㈠灚鐫滈崗鍛杹閻㈢數濮搁幀浣告彥閻撗嶆嫹? *
 * Repository 鐏忓棛閮寸紒鐔风畭閹绢厺鑵戦惃鍕Ц閹胶鐖滄潪顒佸床閹存劙銆夐棃銏犲讲閻╁瓨甯存担璺ㄦ暏閻ㄥ嫭鏋冮張顒€鎷伴崗鍛暩閺嶅洩顔囬敓? * 闁灝鍘ゆい鐢告桨鐏炲倻鎮婇敓?Android 閻㈠灚鐫滈悩鑸碘偓浣哥埗闁插骏鎷? */
data class BatteryStatusInfo(
    val statusText: String,
    val isCharging: Boolean,
)

/**
 * 鐠佹儳顦涵顑挎娑撳骸鐫嗛獮鏇氫繆閹垰鎻╅悡褝鎷? */
data class DeviceHardwareInfo(
    val model: String,
    val androidVersion: String,
    val screenSize: String,
    val screenDensity: String,
    val multiTouchSupported: Boolean,
    val sensors: DeviceSensorInfo,
    val cpu: DeviceCpuInfo,
)

/**
 * 鐠佹儳顦导鐘冲妳閸ｃ劍鏁幐浣瑰剰閸愮鎷? *
 * 濮ｅ繋閲滅€涙顔岀€电懓绨叉い鐢告桨娑擃厼鐫嶇粈铏规畱娑撯偓妞ら€涚炊閹扮喎娅掗懗钘夊閿涘奔濞囬悽銊ョ鐏忔柨鈧棿绌堕敓?ViewModel 缂佺喍绔撮弰鐘茬殸閺傚洦顢嶉敓? */
data class DeviceSensorInfo(
    val accelerometer: Boolean,
    val magneticField: Boolean,
    val orientation: Boolean,
    val gyroscope: Boolean,
    val light: Boolean,
    val proximity: Boolean,
    val ambientTemperature: Boolean,
)

/**
 * CPU 閸╄櫣顢呮穱鈩冧紖閿? *
 * 妫版垹宸肩拠璇插絿閸欘垵鍏橀崶鐘侯啎婢跺洦娼堥梽鎰灗缁崵绮虹憗浣稿婢惰精瑙﹂敍灞姐亼鐠愩儲妞傞敓?data 鐏炲倹褰侀敓?Unknown 閺傚洦顢嶉敓? */
data class DeviceCpuInfo(
    val hardware: String,
    val model: String,
    val cores: Int,
    val maxFrequency: String,
)
