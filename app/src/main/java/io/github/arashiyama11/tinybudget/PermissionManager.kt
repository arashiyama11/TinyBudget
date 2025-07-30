package io.github.arashiyama11.tinybudget

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.net.toUri
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class PermissionManager(
    private val activity: ComponentActivity
) {
    // --- 続きを保持するための Continuation ---
    private var notifCont: Continuation<Boolean>? = null
    private var overlayCont: Continuation<Boolean>? = null

    private var accessibilityCont: Continuation<Boolean>? = null
    private var waitingForAccessibility = false

    private val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private val notificationLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        notifCont?.resume(granted)
        notifCont = null
    }

    private val overlayLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 戻ってきたら再チェックして resume
        val granted = Settings.canDrawOverlays(activity)
        overlayCont?.resume(granted)
        overlayCont = null
    }

    // --- suspend 関数版：通知権限リクエスト ---
    suspend fun requestNotificationPermission(): Boolean =
        suspendCancellableCoroutine { cont ->
            // 既に許可済なら即座に true を返して終端
            val already = ActivityCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (already) {
                cont.resume(true)
                return@suspendCancellableCoroutine
            }

            // 続きを保存して Launcher を発動
            notifCont = cont
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

            // キャンセル時に Continuation をクリア
            cont.invokeOnCancellation { notifCont = null }
        }

    // --- suspend 関数版：オーバーレイ権限リクエスト ---
    suspend fun requestOverlayPermission(): Boolean =
        suspendCancellableCoroutine { cont ->
            val already = Settings.canDrawOverlays(activity)
            if (already) {
                cont.resume(true)
                return@suspendCancellableCoroutine
            }

            overlayCont = cont
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = "package:${activity.packageName}".toUri()
            }
            overlayLauncher.launch(intent)

            cont.invokeOnCancellation { overlayCont = null }
        }

    // ---（参考）アクセシビリティ設定の場合---
    // Settings画面には戻り値がないため、Activity の onResume イベントをキャッチして
    // チェックを行う方法が一般的です。
    suspend fun requestAccessibilityPermission(): Boolean = suspendCancellableCoroutine { cont ->
        // 1) 既に許可済みなら即時 true
        if (checkAccessibilityPermission()) {
            cont.resume(true)
            return@suspendCancellableCoroutine
        }

        // 2) 続きを保存してフラグ立て
        accessibilityCont = cont
        waitingForAccessibility = true

        // 3) 設定画面を開く
        activity.startActivity(settingsIntent)

        // 4) コルーチンキャンセル時のクリーンアップ
        cont.invokeOnCancellation {
            accessibilityCont = null
            waitingForAccessibility = false
        }
    }

    /**
     * MainActivity.onResume から呼ぶ
     */
    fun onActivityResumed() {
        if (!waitingForAccessibility) return

        // 5) 設定画面から戻ってきたと判断して resume
        waitingForAccessibility = false
        accessibilityCont?.resume(checkAccessibilityPermission())
        accessibilityCont = null
    }

    fun checkNotificationPermission(): Boolean {
        return checkSelfPermission(
            activity,
            POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(activity)
    }


    fun checkAccessibilityPermission(): Boolean {
        val am = activity.getSystemService(AccessibilityManager::class.java)
        return am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any {
            val si = it.resolveInfo.serviceInfo
            si.packageName == activity.packageName &&
                    si.name == AppLaunchAccessibilityService::class.java.name
        }
    }
}
