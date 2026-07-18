package com.quickcleanpro.phonecleaner.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.quickcleanpro.phonecleaner.common.ads.AdRuntime
import com.quickcleanpro.phonecleaner.common.ads.ColdStartAdSession
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class OpenAdHostActivity : AppCompatActivity() {
    private val completed = AtomicBoolean(false)
    private var startupJob: Job? = null
    private var session: ColdStartAdSession? = null
    private val stateReceiver: ResultReceiver? by lazy {
        IntentCompat.getParcelableExtra(
            intent,
            EXTRA_OPEN_AD_STATE_RECEIVER,
            ResultReceiver::class.java,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        onBackPressedDispatcher.addCallback(this) {}
        startupJob =
            lifecycleScope.launch {
                try {
                    val app = application as? MyApp
                    if (app == null || !app.sdkInitialization.awaitAdvertiseReady()) {
                        complete()
                        return@launch
                    }
                    if (isFinishing || isDestroyed) return@launch
                    val createdSession =
                        AdRuntime(activityProvider = { this@OpenAdHostActivity }).runColdStart(
                            context = applicationContext,
                            onOpenAdStateChanged = ::dispatchOpenAdState,
                            onFinished = ::complete,
                        )
                    session = createdSession
                    if (completed.get()) createdSession.cancel()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    Log.e(TAG, "Cold start open ad host failed", error)
                    complete()
                }
            }
    }

    override fun onDestroy() {
        startupJob?.cancel()
        startupJob = null
        session?.cancel()
        session = null
        super.onDestroy()
    }

    private fun complete() {
        if (!completed.compareAndSet(false, true)) return
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun dispatchOpenAdState(active: Boolean) {
        stateReceiver?.send(
            OPEN_AD_STATE_CHANGED,
            Bundle().apply { putBoolean(OPEN_AD_ACTIVE, active) },
        )
    }

    private companion object {
        const val TAG = "OpenAdHostActivity"
    }
}

internal class OpenAdHostContract : ActivityResultContract<ResultReceiver, Unit>() {
    override fun createIntent(context: Context, input: ResultReceiver): Intent =
        Intent(context, OpenAdHostActivity::class.java)
            .putExtra(EXTRA_OPEN_AD_STATE_RECEIVER, input)

    override fun parseResult(resultCode: Int, intent: Intent?) = Unit
}

private const val EXTRA_OPEN_AD_STATE_RECEIVER = "open_ad_state_receiver"
internal const val OPEN_AD_STATE_CHANGED = 1
internal const val OPEN_AD_ACTIVE = "open_ad_active"
