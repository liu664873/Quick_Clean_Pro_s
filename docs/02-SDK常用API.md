# SDK 常用 API

本文档整理宿主 App 最常用的 `AdvertiseSdk` API。示例代码都带中文注释，便于直接复制后按业务场景改造。

详细接入步骤请先看：

## 一、初始化 API

### `AdvertiseSdk.init`

用途：初始化广告 SDK、广告策略、Remote Config、打点、归因、通知等能力。

```kotlin
runBlocking {
    AdvertiseSdk.init(
        context = this@MyApp,
        isTest = BuildConfig.DEBUG,
        sdkConfig = AdvertiseConfigFactory.create(this@MyApp),
    )
}
```

注意：

1. `init` 是 `suspend` 方法，必须在协程或挂起环境中调用。
2. 建议 App 进程启动后只初始化一次。
3. 广告展示前应确保初始化流程已经启动。

## 二、开屏广告 API

### `showOpenAd`

用途：展示 App Open 开屏广告。常用于启动页 UMP 完成后、进入首页或引导页之前。

```kotlin
AdvertiseSdk.showOpenAd(
    activity = activity,
    // areaKey 必须存在于 ad_policy.json 或 Remote Config 广告策略中。
    areaKey = "openPageAdv",
    onCloseListener = AdvertiseSdk.OpenAdCloseListener {
        // 广告关闭、展示失败、无广告、策略不命中后都要继续启动流程。
        goNextPage()
    },
    onLoadedListener = AdvertiseSdk.OpenAdLoadedListener {
        // 广告加载成功后 SDK 会继续展示；这里通常只做日志或 UI 状态更新。
        Log.d("OpenAd", "open ad loaded")
    },
    onPaidListener = AdvertiseSdk.OpenAdPaidListener { valueMicros ->
        // 广告收入回调，单位为 micros；SDK 内部已做广告收入上报。
        Log.d("OpenAd", "open ad paid: $valueMicros")
    },
)
```

### 前后台开屏控制

```kotlin
// 是否允许 SDK 在 App 从后台回到前台时自动展示开屏广告。
AdvertiseSdk.isAppOpenAdEnabled = true

// 抑制下一次前后台开屏，常用于从系统设置、权限页、文件选择器返回前。
AdvertiseSdk.suppressNextAppOpenAd = true
```

## 三、插屏广告 API

### `showInterstitialAd`

用途：展示插屏广告。适合功能入口前、任务完成后、返回首页前等场景。

```kotlin
AdvertiseSdk.showInterstitialAd(
    activity = activity,
    // areaKey 对应一个插屏广告点，例如进入功能页、功能完成、返回首页。
    areaKey = "enterFeatureAdv",
) {
    // 插屏关闭、加载失败、超时、无广告、策略不命中后都会执行这里。
    // 业务后续动作必须放在回调里，避免广告还没关闭页面已经跳转。
    startActivity(Intent(activity, FeatureActivity::class.java))
}
```

推荐封装：

```kotlin
object AdvertisePageMediator {
    fun showEnterFeatureAd(activity: Activity, onContinue: () -> Unit) {
        AdvertiseSdk.showInterstitialAd(
            activity = activity,
            areaKey = "enterFeatureAdv",
        ) {
            // 对业务层只暴露“广告流程结束后继续”的语义。
            onContinue()
        }
    }
}
```

## 四、激励视频 API

### `showRewardedAd`

用途：展示激励视频广告。只有收到奖励回调后，宿主才应该发放业务奖励。

```kotlin
AdvertiseSdk.showRewardedAd(
    activity = activity,
    // 激励视频广告点，需要在 ad_policy 和广告位配置中存在。
    areaKey = "rewardedVideoAdv",
    onCloseListener = AdvertiseSdk.RewardedAdCloseListener {
        // 广告关闭或未展示后触发；不要在这里发奖励。
        Log.d("RewardedAd", "rewarded ad closed")
    },
    onLoadedListener = AdvertiseSdk.RewardedAdLoadedListener {
        // 激励视频已加载，SDK 会继续展示。
        Log.d("RewardedAd", "rewarded ad loaded")
    },
    onPaidListener = AdvertiseSdk.RewardedAdPaidListener { valueMicros ->
        // 广告收入回调；SDK 内部已做收入上报。
        Log.d("RewardedAd", "rewarded ad paid: $valueMicros")
    },
    onRewardListener = AdvertiseSdk.RewardedAdRewardListener { type, amount ->
        // 只有这里代表用户获得奖励，业务奖励必须在这里发放。
        grantReward(type = type, amount = amount)
    },
)
```

## 五、Banner 广告 API

### `getBannerAd`

用途：获取 Banner 广告 View。无广告、策略不命中、广告网络关闭或当前用户需要抑制广告时可能返回 `null`。

```kotlin
fun bindBanner(context: Context, container: FrameLayout) {
    // 绑定前清理旧 View，避免重复添加 Banner。
    container.removeAllViews()

    val banner = AdvertiseSdk.getBannerAd(
        context = context,
        areaKey = "homeBottomBannerAdv",
    )

    if (banner == null) {
        // 没有可展示广告时隐藏容器。
        container.visibility = View.GONE
        return
    }

    container.visibility = View.VISIBLE
    container.addView(
        banner,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ),
    )
}
```

### `destroyBannerAd`

用途：页面销毁或广告容器清理时销毁 Banner View。

```kotlin
fun clearBanner(container: FrameLayout) {
    for (index in 0 until container.childCount) {
        // Banner 底层是 AdView，需要主动 destroy，避免资源泄漏。
        AdvertiseSdk.destroyBannerAd(container.getChildAt(index) as? ViewGroup)
    }

    container.removeAllViews()
    container.visibility = View.GONE
}
```

## 六、Native 广告 API

### Compose：`rememberNativeAd`

用途：Compose 页面中获取并自动刷新 Native 广告。

```kotlin
@Composable
fun HomeNativeAd() {
    val nativeAdState = rememberNativeAd(
        areaKey = "homeNativeAdv",
        onAdClicked = {
            // 这是 AdMob 确认点击后的回调，可用于业务侧补充打点。
            AdvertiseSdk.logEvent(
                eventName = "native_ad_clicked",
                params = mapOf("areaKey" to "homeNativeAdv"),
            )
        },
    )

    nativeAdState.value?.let { nativeAd ->
        // 宿主需要使用 NativeAdView 正确绑定标题、素材、CTA、MediaView 等广告素材。
        DisplayNativeAd(nativeAd)
    }
}
```

### View 或非 Compose：`getNativeAd`

用途：从 Native 广告池取一组 Native 广告。

```kotlin
val adGroup = AdvertiseSdk.getNativeAd(
    context = context,
    areaKey = "homeNativeAdv",
    onAdGroupLoaded = {
        // 广告池异步加载完成后刷新 UI，再次尝试取广告。
        refreshNativeContainer()
    },
    onAdClickListener = AdvertiseSdk.NativeAdClickListener {
        // 仅在 AdMob 确认 Native 点击后触发。
        AdvertiseSdk.logEvent(
            eventName = "native_ad_clicked",
            params = mapOf("areaKey" to "homeNativeAdv"),
        )
    },
)

// 优先展示高价广告，其次中价，最后兜底广告。
val nativeAd = adGroup?.hAd ?: adGroup?.mAd ?: adGroup?.lAd
if (nativeAd != null) {
    showNativeAd(nativeAd)
}
```

注意：

1. Native 广告必须使用 Google Mobile Ads 的 `NativeAdView`。
2. 不要给 CTA 或素材 View 单独设置普通点击监听来替代广告 SDK 点击处理。
3. 页面离开时要销毁 Native 广告对象，Compose 版本的 `rememberNativeAd` 已自动处理。

## 七、广告预加载 API

### 预加载触发时机常量

| 常量 | 含义 |
| --- | --- |
| `LOAD_TIME_OPEN_APP` | 打开 App。 |
| `LOAD_TIME_PLAY_FINISH` | 广告播放结束或曝光后。 |
| `LOAD_TIME_ENTER_BACKGROUND` | App 进入后台。 |
| `LOAD_TIME_RECEIVE_NOTIFICATION` | 收到通知。 |
| `LOAD_TIME_ENTER_FEATURE` | 进入功能页。 |
| `LOAD_TIME_UNKNOWN` | 未指定来源。 |

### 开屏预加载

```kotlin
if (AdvertiseSdk.canPreloadOpen(AdvertiseSdk.LOAD_TIME_OPEN_APP)) {
    // 只有策略允许时才预加载，避免无条件请求广告。
    AdvertiseSdk.preloadOpen(
        context = context,
        loadTimeKey = AdvertiseSdk.LOAD_TIME_OPEN_APP,
    )
}
```

### 插屏预加载

```kotlin
if (AdvertiseSdk.canPreloadInterstitial(AdvertiseSdk.LOAD_TIME_ENTER_FEATURE)) {
    // 进入功能页时补充插屏缓存。
    AdvertiseSdk.preloadInterstitial(
        context = context,
        loadTimeKey = AdvertiseSdk.LOAD_TIME_ENTER_FEATURE,
    )
}
```

### Native 预加载

```kotlin
if (AdvertiseSdk.canPreloadNative(AdvertiseSdk.LOAD_TIME_OPEN_APP)) {
    AdvertiseSdk.preloadNative(
        context = context,
        loadTimeKey = AdvertiseSdk.LOAD_TIME_OPEN_APP,
    ) {
        // Native 广告组加载完成后，可以通知页面刷新广告容器。
        refreshNativeContainer()
    }
}
```

### 激励视频预加载

```kotlin
if (AdvertiseSdk.canPreloadRewarded(AdvertiseSdk.LOAD_TIME_ENTER_FEATURE)) {
    // 进入功能页时提前加载激励视频，减少用户点击后的等待。
    AdvertiseSdk.preloadRewarded(
        context = context,
        loadTimeKey = AdvertiseSdk.LOAD_TIME_ENTER_FEATURE,
    )
}
```

## 八、UMP 隐私同意 API

### `initConsent`

用途：提前初始化 Google UMP 同意状态。

```kotlin
val hasCachedConsentState = AdvertiseSdk.initConsent(activity) { success ->
    // UMP 状态请求完成；success 表示流程完成状态，不代表一定展示弹窗。
    Log.d("UMP", "init consent finished: $success")
}

if (hasCachedConsentState) {
    // 已有缓存状态时，可继续后续启动流程。
    continueStartup()
}
```

### `showSplashConsent`

用途：启动页展示 UMP 同意弹窗，弹窗结束后继续启动流程。

```kotlin
AdvertiseSdk.showSplashConsent(activity) {
    // UMP 弹窗关闭、无需展示或异常兜底后继续。
    showOpenAdOrContinue()
}
```

### `showPrivacyOptions`

用途：设置页提供隐私选项入口。

```kotlin
if (AdvertiseSdk.isPrivacyOptionsRequired) {
    // 只有 UMP 要求提供入口时再展示隐私选项。
    AdvertiseSdk.showPrivacyOptions(activity)
}
```

## 九、Remote Config API

### 初始化状态

```kotlin
val initialized = AdvertiseSdk.isRemoteConfigInitialized
val successful = AdvertiseSdk.isRemoteConfigInitSuccessful

AdvertiseSdk.getRemoteConfigInitStatus { isInitialized, isSuccessful, error ->
    if (isInitialized && isSuccessful) {
        // Remote Config 初始化成功，SDK 内部广告策略已经尝试应用。
    } else if (isInitialized) {
        // Remote Config 初始化结束但失败，SDK 会使用本地兜底配置。
        Log.w("RemoteConfig", "init failed", error)
    }
}
```

### 读取业务自定义参数

```kotlin
val featureEnabled = AdvertiseSdk.getRemoteConfigBoolean(
    // key 对应 Firebase Remote Config 顶层参数名。
    key = "feature_new_home_enabled",
    defaultValue = false,
)

val refreshSeconds = AdvertiseSdk.getRemoteConfigLong(
    key = "home_banner_refresh_seconds",
    defaultValue = 60L,
)

val homeTitle = AdvertiseSdk.getRemoteConfigString(
    key = "home_title",
    defaultValue = "",
)

val discount = AdvertiseSdk.getRemoteConfigDouble(
    key = "paywall_discount",
    defaultValue = 0.0,
)
```

## 十、打点 API

### `logEvent`

用途：上报宿主业务事件。

```kotlin
AdvertiseSdk.logEvent(
    eventName = "feature_clicked",
    params = mapOf(
        // 字段名和含义必须与数据后台约定一致。
        "feature" to "scan",
        "from" to "home",
    ),
)
```

### `setSuperProperties`

用途：设置公共事件属性，后续事件都会自动携带。

```kotlin
AdvertiseSdk.setSuperProperties(
    mapOf(
        "traffic_source" to "notification",
        "app_version" to BuildConfig.VERSION_NAME,
    )
)
```

### 用户属性

```kotlin
AdvertiseSdk.setUserOnceAttr(
    // 首次打开时间只写一次。
    AdvertiseSdk.ThinkingKeys.firstOpenTime,
    AdvertiseSdk.getFirstOpenTime(),
)

AdvertiseSdk.setUserAttr(
    // 最近打开时间允许后续覆盖。
    AdvertiseSdk.ThinkingKeys.latestOpenTime,
    AdvertiseSdk.getLatestOpenTime(),
)
```

### 设备和归因信息

```kotlin
val thinkingDeviceId = AdvertiseSdk.getThinkingDeviceId()
val isNature = AdvertiseSdk.isNature
val topic = AdvertiseSdk.topic
```

注意：广告加载、展示、点击、关闭、失败、超时、收入等广告生命周期事件由 SDK 内部统一上报，宿主不要重复手动上报。

## 十一、通知 API

### `ensurePersistentNotificationServiceRunning`

用途：确保常驻通知前台服务处于运行状态。

```kotlin
override fun onResume() {
    super.onResume()

    // 进入主页面后确保常驻通知服务运行。
    AdvertiseSdk.ensurePersistentNotificationServiceRunning(this)
}
```

### `sendDebugNotification`

用途：开发阶段发送调试通知，验证通知模板、路由和点击跳转。

```kotlin
AdvertiseSdk.sendDebugNotification(
    context = context,
    // 通知类型，需与通知配置中的类型一致。
    notificationType = "screen_unlock",
    // 配置名，需与通知配置中的配置项一致。
    configName = "screen_unlock",
)
```

### Push 点击字段

```kotlin
AdvertiseSdk.logEvent(
    eventName = AdvertiseSdk.PushLog.notificationClicked,
    params = mapOf(
        // Push 消息 ID。
        AdvertiseSdk.PushLog.msgId to msgId,
        // 目标用户 ID。
        AdvertiseSdk.PushLog.targetUserId to targetUserId,
    ),
)
```

## 十二、常用状态 API

```kotlin
// 隐私政策 URL。
val privacyUrl = AdvertiseSdk.privacyUrl

// 用户协议 URL。
val termsUrl = AdvertiseSdk.termsUrl

// 当前用户是否命中 Google IP。
val isGoogleIp = AdvertiseSdk.isGoogleIp

// 当前用户是否被标记为 paid_0。
val isPaidUser = AdvertiseSdk.isPaidUser

// 当前用户是否应隐藏广告，通常用于宿主 UI 判断。
val shouldSuppressAds = AdvertiseSdk.shouldSuppressAdsForCurrentUser

// 当前归因是否自然量。
val isNature = AdvertiseSdk.isNature

// 当前 FCM topic。
val topic = AdvertiseSdk.topic

// 引导页自动切换间隔，来自 NativeConfig 或 Remote Config。
val guideSwapTime = AdvertiseSdk.guidePageSwapTime
```

## 十三、Preference 调试 API

```kotlin
// 写入 SDK 偏好存储，通常只用于调试或少量跨模块状态。
AdvertiseSdk.putPreferenceString("debug_key", "debug_value")

// 读取 SDK 偏好存储，defaultValue 用于 key 不存在时兜底。
val value = AdvertiseSdk.getPreferenceString(
    key = "debug_key",
    defaultValue = "",
)
```

常见调试读取：

```kotlin
val routingSource = AdvertiseSdk.getPreferenceString("routing_source", "")
val adTag = AdvertiseSdk.getPreferenceString("adTag", "")
val notificationTag = AdvertiseSdk.getPreferenceString("notificationTag", "")
val preloadTag = AdvertiseSdk.getPreferenceString("preloadTag", "")
```

## 十四、API 使用原则

1. 初始化和配置集中在 `Application`、`SdkInitUtils`、`AdvertiseConfigFactory` 中处理。
2. 页面业务只调用语义化封装，例如 `AdvertisePageMediator.showEnterFeatureAd()`。
3. 开屏、插屏、激励视频的关闭回调必须能继续业务流程。
4. Banner 返回 `null` 时隐藏容器，不能强制占位。
5. Native 广告必须使用 `NativeAdView` 正确注册素材。
6. 激励视频只在 `onRewardListener` 中发放奖励。
7. 广告生命周期打点由 SDK 内部处理，宿主只打业务事件。
