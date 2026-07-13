package com.quickcleanpro.phonecleaner.feature.antivirus

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanEngine
import com.quickcleanpro.phonecleaner.feature.antivirus.AntivirusPreferences
import com.quickcleanpro.phonecleaner.feature.antivirus.TrustlookConfigurationException
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanEngineListener
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanErrorKind
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanItem
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanMode
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanUiState
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusSecurityRepository
import com.quickcleanpro.phonecleaner.feature.antivirus.getAppLabelAndIcon
import com.quickcleanpro.phonecleaner.feature.antivirus.getProtectionIcon
import com.quickcleanpro.phonecleaner.feature.antivirus.logScanError
import com.quickcleanpro.phonecleaner.feature.antivirus.scanErrorMessage
import com.quickcleanpro.phonecleaner.feature.antivirus.scanStartErrorMessage
import com.quickcleanpro.phonecleaner.feature.antivirus.toThreat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "VirusScan"
private const val PROGRESS_TICK_MILLIS = 40L
private const val INSTALLED_APPS_DIALOG_INTERACTION_THRESHOLD_MILLIS = 1_500L

class VirusScanViewModel constructor(
    application: Application,
    private val scanEngine: VirusScanEngine,
    private val securityRepository: VirusSecurityRepository,
    private val preferences: AntivirusPreferences,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VirusScanUiState())
    val uiState: StateFlow<VirusScanUiState> = _uiState.asStateFlow()
    private val _homeState = MutableStateFlow(VirusHomeUiState())
    val homeState: StateFlow<VirusHomeUiState> = _homeState.asStateFlow()
    private val _homeEffects = MutableSharedFlow<VirusHomeEffect>(extraBufferCapacity = 1)
    val homeEffects: SharedFlow<VirusHomeEffect> = _homeEffects.asSharedFlow()

    private var completionJob: Job? = null
    private var progressJob: Job? = null
    private var appDisplayJob: Job? = null
    private var pathDisplayJob: Job? = null
    private var appDisplayChannel = Channel<String>(Channel.UNLIMITED)
    private var pathDisplayChannel = Channel<String>(Channel.UNLIMITED)
    private var scanStartedAt: Long = 0L
    private var scanGeneration = 0
    private var isAppDisplayStarted = false
    private var isAppDisplayStopped = false
    private var isPathDisplayStarted = false
    private val processedPackageNames = mutableSetOf<String>()
    private val threatIds = mutableSetOf<String>()
    private val pendingThreatJobs = ConcurrentHashMap.newKeySet<Job>()

    fun onHomeAction(action: VirusHomeAction) {
        when (action) {
            VirusHomeAction.Entered -> {
                if (!preferences.isScanNoticeAccepted()) {
                    _homeState.update { it.copy(showScanNotice = true) }
                }
            }
            VirusHomeAction.Back -> _homeEffects.tryEmit(VirusHomeEffect.Exit)
            is VirusHomeAction.ScanRequested -> requestHomeScan(action.mode)
            VirusHomeAction.ScanNoticeAccepted -> {
                preferences.setScanNoticeAccepted()
                val mode = _homeState.value.pendingScanMode
                _homeState.update { it.copy(showScanNotice = false) }
                if (mode != null) checkInstalledAppsAccess(mode, returningFromSettings = false)
            }
            VirusHomeAction.ScanNoticeDeclined -> {
                _homeState.update { it.copy(showScanNotice = false) }
                _homeEffects.tryEmit(VirusHomeEffect.Exit)
            }
            VirusHomeAction.InstalledAppsSettingsRequested -> {
                _homeState.update {
                    it.copy(
                        showInstalledAppsPermissionDialog = false,
                        waitingForSettingsReturn = true,
                    )
                }
                _homeEffects.tryEmit(VirusHomeEffect.OpenInstalledAppsSettings)
            }
            VirusHomeAction.InstalledAppsDialogDismissed -> {
                _homeState.update {
                    it.copy(
                        showInstalledAppsPermissionDialog = false,
                        scanRequestPending = false,
                    )
                }
            }
            VirusHomeAction.Resumed -> {
                val state = _homeState.value
                val mode = state.pendingScanMode
                if (state.waitingForSettingsReturn && mode != null) {
                    _homeState.update { it.copy(waitingForSettingsReturn = false) }
                    checkInstalledAppsAccess(mode, returningFromSettings = true)
                }
            }
            VirusHomeAction.SettingsLaunchFailed -> {
                _homeState.update {
                    it.copy(
                        waitingForSettingsReturn = false,
                        scanRequestPending = false,
                        showInstalledAppsPermissionDialog = true,
                    )
                }
            }
            is VirusHomeAction.DeepScanPermissionResult -> {
                val mode = _homeState.value.pendingScanMode ?: VirusScanMode.Deep
                _homeState.update { it.copy(scanRequestPending = false) }
                if (action.granted) {
                    _homeEffects.tryEmit(VirusHomeEffect.NavigateToScan(mode))
                }
            }
        }
    }

    private fun requestHomeScan(mode: VirusScanMode) {
        if (_homeState.value.scanRequestPending) return
        _homeState.update { it.copy(pendingScanMode = mode) }
        if (!preferences.isScanNoticeAccepted()) {
            _homeState.update { it.copy(showScanNotice = true) }
            return
        }
        checkInstalledAppsAccess(mode, returningFromSettings = false)
    }

    private fun checkInstalledAppsAccess(
        mode: VirusScanMode,
        returningFromSettings: Boolean,
    ) {
        if (_homeState.value.scanRequestPending) return
        _homeState.update { it.copy(scanRequestPending = true, pendingScanMode = mode) }
        viewModelScope.launch {
            val hadPreviousFailure = preferences.hasInstalledAppsAccessFailedBefore()
            val startedAt = System.currentTimeMillis()
            val hasAccess = withContext(Dispatchers.IO) { securityRepository.hasInstalledAppsAccess() }
            val elapsed = System.currentTimeMillis() - startedAt
            if (hasAccess) {
                preferences.setInstalledAppsAccessFailed(false)
                _homeState.update {
                    it.copy(
                        showInstalledAppsPermissionDialog = false,
                        waitingForSettingsReturn = false,
                    )
                }
                if (mode == VirusScanMode.Deep) {
                    _homeEffects.emit(VirusHomeEffect.RequestDeepScanPermission(mode))
                } else {
                    _homeState.update { it.copy(scanRequestPending = false) }
                    _homeEffects.emit(VirusHomeEffect.NavigateToScan(mode))
                }
            } else {
                preferences.setInstalledAppsAccessFailed(true)
                _homeState.update {
                    it.copy(
                        scanRequestPending = false,
                        waitingForSettingsReturn = false,
                        showInstalledAppsPermissionDialog =
                            !returningFromSettings && hadPreviousFailure &&
                                elapsed < INSTALLED_APPS_DIALOG_INTERACTION_THRESHOLD_MILLIS,
                    )
                }
            }
        }
    }

    fun isScanNoticeAccepted(): Boolean = preferences.isScanNoticeAccepted()

    fun acceptScanNotice() = preferences.setScanNoticeAccepted()

    fun hasInstalledAppsAccessFailedBefore(): Boolean = preferences.hasInstalledAppsAccessFailedBefore()

    fun setInstalledAppsAccessFailed(failed: Boolean) = preferences.setInstalledAppsAccessFailed(failed)

    fun startScan(mode: VirusScanMode) {
        resetScanState()
        val generation = scanGeneration
        completionJob = null

        _uiState.value = VirusScanUiState(
            mode = mode,
            isScanning = false,
            currentIcon = getApplication<Application>().getProtectionIcon()
        )

        refreshAdbRisk()

        if (!securityRepository.trustlookApiKeyState().isConfigured) {
            handleStartFailure(TrustlookConfigurationException())
            return
        }

        runCatching {
            val listener = createScanListener(mode, generation)
            scanEngine.startScan(mode, listener)
        }.onFailure { error ->
            handleStartFailure(error)
        }
    }

    fun resetScanState() {
        scanGeneration++
        completionJob?.cancel()
        completionJob = null
        stopTrackProgress()
        resetDisplayQueues()
        pendingThreatJobs.forEach { job -> job.cancel() }
        pendingThreatJobs.clear()
        processedPackageNames.clear()
        threatIds.clear()
        scanEngine.cancelScan()
        scanStartedAt = 0L
        _uiState.value = VirusScanUiState()
    }

    fun refreshAdbRisk() {
        viewModelScope.launch(Dispatchers.Default) {
            val hasRisk = securityRepository.hasAdbRisk()
            _uiState.update { state -> state.copy(hasAdbRisk = hasRisk) }
        }
    }

    fun hasInstalledAppsAccess(): Boolean = securityRepository.hasInstalledAppsAccess()

    fun clearError() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }

    fun cancelScan() {
        scanGeneration++
        completionJob?.cancel()
        completionJob = null
        stopTrackProgress()
        stopDisplayQueues()
        scanEngine.cancelScan()
        scanStartedAt = 0L
        _uiState.update { state ->
            state.copy(isScanning = false, scanCompleted = false)
        }
    }

    fun removeThreatByPackage(packageName: String) {
        _uiState.update { state ->
            val nextThreats = state.threats.filterNot { it.packageName == packageName }
            state.copy(
                threats = nextThreats,
                appThreatCount = nextThreats.count { !it.isFile },
                fileThreatCount = nextThreats.count { it.isFile }
            )
        }
        threatIds.removeAll { it == packageName || it.endsWith(":$packageName") }
    }

    fun removeThreatByFilePath(path: String) {
        _uiState.update { state ->
            val nextThreats = state.threats.filterNot { it.apkPath == path }
            state.copy(
                threats = nextThreats,
                appThreatCount = nextThreats.count { !it.isFile },
                fileThreatCount = nextThreats.count { it.isFile }
            )
        }
        threatIds.remove("file:$path")
    }

    private fun createScanListener(mode: VirusScanMode, generation: Int): VirusScanEngineListener {
        return object : VirusScanEngineListener {
            override fun onScanStarted() {
                handleScanStarted(mode, generation)
            }

            override fun onScanProgress(item: VirusScanItem?) {
                item ?: return
                if (generation == scanGeneration && !_uiState.value.scanCompleted) {
                    if (scanStartedAt == 0L || !_uiState.value.isScanning) {
                        handleScanStarted(mode, generation)
                    }
                    handleScanProgress(mode, item)
                }
            }

            override fun onScanError(
                code: Int,
                message: String?,
                kind: VirusScanErrorKind,
            ) {
                if (generation != scanGeneration) return
                scanGeneration++
                stopTrackProgress()
                stopDisplayQueues()
                logScanError(code, message, securityRepository.trustlookApiKeyState().logState)
                _uiState.update { state ->
                    state.copy(
                        isScanning = false,
                        errorMessage = scanErrorMessage(getApplication(), kind)
                    )
                }
            }

            override fun onScanCanceled() {
                if (generation != scanGeneration) return
                scanGeneration++
                stopTrackProgress()
                stopDisplayQueues()
                _uiState.update { state -> state.copy(isScanning = false) }
            }

            override fun onScanInterrupt() {
                if (generation != scanGeneration) return
                scanGeneration++
                stopTrackProgress()
                stopDisplayQueues()
                _uiState.update { state -> state.copy(isScanning = false) }
            }

            override fun onScanFinished() {
                if (generation == scanGeneration) {
                    if (scanStartedAt == 0L) {
                        handleScanStarted(mode, generation)
                    }
                    finishWhenUiIsReady(mode, generation)
                }
            }
        }
    }

    private fun handleScanStarted(mode: VirusScanMode, generation: Int) {
        if (generation != scanGeneration) return
        if (_uiState.value.scanCompleted) return
        if (scanStartedAt == 0L) {
            scanStartedAt = System.currentTimeMillis()
        }
        _uiState.update { state ->
            if (generation == scanGeneration && !state.scanCompleted) {
                state.copy(
                    mode = mode,
                    isScanning = true,
                    currentIcon = state.currentIcon ?: getApplication<Application>().getProtectionIcon()
                )
            } else {
                state
            }
        }
        startTrackProgress(mode, generation)
    }

    private fun handleScanProgress(mode: VirusScanMode, item: VirusScanItem) {
        val packageName = item.packageName?.takeIf { it.isNotBlank() }
        if (packageName != null) {
            if (item.score >= 6) addThreat(item, isFile = false)
            appDisplayChannel.trySend(packageName)
            return
        }

        if (mode == VirusScanMode.Deep) {
            val path = item.apkPath?.takeIf { it.isNotBlank() }
            if (item.score >= 6) addThreat(item, isFile = true)
            if (path != null) pathDisplayChannel.trySend(path)
        }
    }

    private fun addThreat(item: VirusScanItem, isFile: Boolean) {
        val job = viewModelScope.launch {
            val threat = withContext(Dispatchers.IO) {
                item.toThreat(getApplication(), isFile)
            }
            if (!threatIds.add(threat.id)) return@launch

            _uiState.update { state ->
                val nextThreats = state.threats + threat
                state.copy(
                    threats = nextThreats,
                    appThreatCount = nextThreats.count { !it.isFile },
                    fileThreatCount = nextThreats.count { it.isFile }
                )
            }
        }
        pendingThreatJobs.add(job)
        job.invokeOnCompletion { pendingThreatJobs.remove(job) }
    }

    private fun finishWhenUiIsReady(mode: VirusScanMode, generation: Int) {
        completionJob?.cancel()
        completionJob = viewModelScope.launch {
            val startedAt = scanStartedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
            val elapsed = System.currentTimeMillis() - startedAt
            val remaining = (mode.minDurationMillis - elapsed).coerceAtLeast(0L)
            if (remaining > 0L) delay(remaining)
            pendingThreatJobs.toList().joinAll()
            if (generation != scanGeneration) return@launch
            stopTrackProgress()
            stopDisplayQueues()
            _uiState.update { state ->
                state.copy(
                    isScanning = false,
                    scanCompleted = true,
                    progressFraction = 1f
                )
            }
        }
    }

    private fun startTrackProgress(mode: VirusScanMode, generation: Int) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                if (generation != scanGeneration) break
                val elapsed = System.currentTimeMillis() - scanStartedAt
                val fraction = (elapsed.toFloat() / mode.minDurationMillis).coerceIn(0f, 1f)
                _uiState.update { state ->
                    if (state.isScanning) {
                        state.copy(progressFraction = maxOf(state.progressFraction, fraction))
                    } else {
                        state
                    }
                }
                handleProgressStageTriggers(mode, fraction, generation)
                if (fraction >= 1f) break
                delay(PROGRESS_TICK_MILLIS)
            }
        }
    }

    private fun stopTrackProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun handleProgressStageTriggers(mode: VirusScanMode, progress: Float, generation: Int) {
        if (progress >= mode.circleStartThreshold(circleIndex = 1)) {
            startAppDisplayConsumer(mode, generation)
        }
        if (mode == VirusScanMode.Quick && progress >= mode.circleFillThreshold(circleIndex = 1)) {
            stopAppDisplayConsumer()
        }
        if (mode == VirusScanMode.Deep && progress >= mode.circleStartThreshold(circleIndex = 2)) {
            stopAppDisplayConsumer()
            enterPathDisplayStage(generation)
            startPathDisplayConsumer(mode, generation)
        }
    }

    private fun handleStartFailure(error: Throwable) {
        if (scanGeneration < 0) return
        stopTrackProgress()
        stopDisplayQueues()
        completionJob?.cancel()
        completionJob = null
        scanStartedAt = 0L
        Log.w(TAG, "Trustlook scan start failed apiKey=${securityRepository.trustlookApiKeyState().logState}", error)
        _uiState.update { state ->
            state.copy(
                isScanning = false,
                errorMessage = scanStartErrorMessage(getApplication(), error)
            )
        }
    }

    private fun startAppDisplayConsumer(mode: VirusScanMode, generation: Int) {
        if (isAppDisplayStarted || isAppDisplayStopped || generation != scanGeneration) return
        isAppDisplayStarted = true
        appDisplayJob = viewModelScope.launch(Dispatchers.Default) {
            var lastUpdateTime = 0L
            for (packageName in appDisplayChannel) {
                if (generation != scanGeneration) break
                if (!processedPackageNames.add(packageName)) continue

                val appLabelAndIcon = withContext(Dispatchers.IO) {
                    getAppLabelAndIcon(getApplication(), packageName)
                }
                delayUntilNextDisplay(lastUpdateTime, mode.displayUpdateIntervalMillis)
                if (generation != scanGeneration) break

                _uiState.update { state ->
                    if (state.isScanning && !state.isPathMode) {
                        state.copy(
                            currentLabel = appLabelAndIcon.first,
                            currentIcon = appLabelAndIcon.second
                        )
                    } else {
                        state
                    }
                }
                lastUpdateTime = System.currentTimeMillis()
            }
        }
    }

    private fun stopAppDisplayConsumer() {
        if (isAppDisplayStopped) return
        isAppDisplayStopped = true
        appDisplayJob?.cancel()
        appDisplayJob = null
        appDisplayChannel.close()
    }

    private fun enterPathDisplayStage(generation: Int) {
        if (isPathDisplayStarted || generation != scanGeneration) return
        _uiState.update { state ->
            if (state.isScanning) {
                state.copy(
                    isPathMode = true,
                    currentLabel = "",
                    currentIcon = getApplication<Application>().getProtectionIcon()
                )
            } else {
                state
            }
        }
    }

    private fun startPathDisplayConsumer(mode: VirusScanMode, generation: Int) {
        if (isPathDisplayStarted || generation != scanGeneration) return
        isPathDisplayStarted = true
        pathDisplayJob = viewModelScope.launch(Dispatchers.Default) {
            var lastUpdateTime = 0L
            for (path in pathDisplayChannel) {
                if (generation != scanGeneration) break
                delayUntilNextDisplay(lastUpdateTime, mode.displayUpdateIntervalMillis)
                if (generation != scanGeneration) break

                _uiState.update { state ->
                    if (state.isScanning && state.isPathMode) {
                        state.copy(currentLabel = path)
                    } else {
                        state
                    }
                }
                lastUpdateTime = System.currentTimeMillis()
            }
        }
    }

    private suspend fun delayUntilNextDisplay(lastUpdateTime: Long, intervalMillis: Long) {
        if (lastUpdateTime == 0L) return
        val elapsed = System.currentTimeMillis() - lastUpdateTime
        if (elapsed < intervalMillis) delay(intervalMillis - elapsed)
    }

    private fun resetDisplayQueues() {
        stopDisplayQueues()
        appDisplayChannel.cancel()
        pathDisplayChannel.cancel()
        appDisplayChannel = Channel(Channel.UNLIMITED)
        pathDisplayChannel = Channel(Channel.UNLIMITED)
        isAppDisplayStarted = false
        isAppDisplayStopped = false
        isPathDisplayStarted = false
    }

    private fun stopDisplayQueues() {
        appDisplayJob?.cancel()
        pathDisplayJob?.cancel()
        appDisplayJob = null
        pathDisplayJob = null
        closeDisplayQueues()
    }

    private fun closeDisplayQueues() {
        appDisplayChannel.close()
        pathDisplayChannel.close()
    }

    override fun onCleared() {
        cancelScan()
        super.onCleared()
    }
}
