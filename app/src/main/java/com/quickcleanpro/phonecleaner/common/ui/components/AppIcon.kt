package com.quickcleanpro.phonecleaner.common.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val packageIconBitmapCache = ConcurrentHashMap<String, ImageBitmap>()

private fun loadPackageIconBitmap(context: Context, packageName: String): ImageBitmap? {
    packageIconBitmapCache[packageName]?.let { return it }

    val packageManager = context.packageManager
    val icon = runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
        ?: runCatching {
            packageManager.getLaunchIntentForPackage(packageName)
                ?.resolveActivity(packageManager)
                ?.let { componentName ->
                    packageManager.getActivityInfo(componentName, 0).loadIcon(packageManager)
                }
        }.getOrNull()
        ?: runCatching {
            packageManager.getApplicationInfo(packageName, 0).loadIcon(packageManager)
        }.getOrNull()

    return icon?.toBitmap()?.asImageBitmap()?.also {
        packageIconBitmapCache[packageName] = it
    }
}

/**
 * 閿?Drawable 鏉烆剚宕查敓?Bitmap
 */
private fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable && bitmap != null) return bitmap
    val width = intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

/**
 * 缁绢垱鏋冪€涙娴橀弽鍥风礄閺冪姴瀵橀崥宥呭鏉炴枻绱氶敍宀€鏁ゆ禍搴☆槵闁鍨ㄩ懕姘値閿?
 * @param text 閺勫墽銇氶惃鍕瀮鐎涙绱欓柅姘埗娑撴椽顩荤€涙鐦濋敓?
 * @param color 閼冲本娅欐０婊嗗
 * @param modifier 娣囶噣銈扮粭锔肩礄閸欘垱甯堕崚璺烘槀鐎靛摜鐡戦敓?
 */
@Composable
fun TextIcon(
    text: String,
    color: Color,
    modifier: Modifier = Modifier.size(44.dp)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun rememberPackageIconBitmap(packageName: String?): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val cached = packageName?.let(packageIconBitmapCache::get)
    val icon by produceState<ImageBitmap?>(initialValue = cached, packageName) {
        if (packageName != null && value == null) {
            value = withContext(Dispatchers.IO) { loadPackageIconBitmap(context, packageName) }
        }
    }
    return icon
}

@Composable
fun CircularAppLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(id = R.drawable.app_logo),
        contentDescription = contentDescription,
        modifier = modifier.clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}

/**
 * 闁氨鏁ゆ惔鏃傛暏閸ョ偓鐖ｇ紒鍕閿涘奔绱崗鍫滃▏閻劎婀＄€圭偛绨查悽銊ユ禈閺嶅浄绱濋崥锕€鍨弰鍓с仛婢跺洭鈧宕伴敓?
 * @param packageName 鎼存梻鏁ら崠鍛倳閿涘牆褰叉稉铏光敄閿涘瞼鈹栭弮鍓佹纯閹恒儲妯夌粈鍝勫窗娴ｅ稄绱?
 * @param fallbackText 婢跺洭鈧鏋冪€涙绱欓柅姘埗娑撴椽顩荤€涙鐦濋敍澶涚礉瑜版挻妫ゅ▔鏇″箯閸欐牕娴橀弽鍥ㄦ閺勫墽銇?
 * @param color 婢跺洭鈧鏋冪€涙娈戦懗灞炬珯閼硅绱欐禒鍛秼閺勫墽銇氶弬鍥х摟閺冩湹濞囬悽顭掔礆
 * @param isAggregate 閺勵垰鎯佹稉楦夸粵閸氬牓銆嶉敍鍫濐洤閳ユ粎閮寸紒鐔剁瑢閺堫亞鐓″ù渚€鍣洪垾婵撶礆閿涘本顒濋弮鍓佹纯閹恒儲妯夌粈鍝勵槵闁鏋冮敓?
 * @param modifier 婢舵牠鍎存穱顕€銈扮粭锔肩礉閸欘垵鐨熼弫鏉戞槀鐎靛摜鐡?
 */
@Composable
fun PackageAppIcon(
    packageName: String?,
    fallbackText: String,
    color: Color = Color(0xFF000000),
    isAggregate: Boolean = false,
    modifier: Modifier = Modifier.size(44.dp)
) {
    if (isAggregate) {
        TextIcon(text = fallbackText, color = color, modifier = modifier)
        return
    }

    val appIcon = rememberPackageIconBitmap(packageName)

    Box(
        modifier = modifier.size(44.dp).clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (appIcon != null) {
            Image(
                bitmap = appIcon,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // 婢跺洭鈧绱伴弰鍓с仛闁氨鏁ら崡鐘辩秴閸ョ偓鐖?
            CircularAppLogo(
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 濞撳懐鎮婇崶鐐垼缂傛挸鐡ㄩ敍鍫濆讲闁绱濋崷銊ョ安閻劌宓忔潪鑺ュ灗闂団偓鐟曚礁鍩涢弬鐗堟鐠嬪啰鏁ら敓?
 */
fun clearPackageIconCache() {
    packageIconBitmapCache.clear()
}
