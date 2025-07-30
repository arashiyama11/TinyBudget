package io.github.arashiyama11.tinybudget

import android.Manifest.permission.POST_NOTIFICATIONS
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import io.github.arashiyama11.tinybudget.ui.main.MainPresenter
import io.github.arashiyama11.tinybudget.ui.main.MainScreen
import io.github.arashiyama11.tinybudget.ui.main.MainUi
import io.github.arashiyama11.tinybudget.ui.theme.TinyBudgetTheme

class MainActivity : AppCompatActivity() {
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            startOverlayService()
        } else {
            Toast.makeText(this, "オーバーレイ権限が許可されませんでした", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            checkOverlayPermission()
        } else {
            Toast.makeText(this, "通知が許可されませんでした", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val circuit = Circuit.Builder()
            .addPresenter<MainScreen, MainScreen.State>(MainPresenter())
            .addUi<MainScreen, MainScreen.State> { uiState, modifier ->
                MainUi(uiState, modifier)
            }.build()

        setContent {
            TinyBudgetTheme {
                CircuitCompositionLocals(circuit) {
                    CircuitContent(MainScreen)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            checkOverlayPermission()
        } else {
            notificationPermissionLauncher.launch(POST_NOTIFICATIONS)
        }

        if (!accessibilityServiceEnabled(this)) {
            // アクセシビリティサービスが有効でない場合、設定画面へ誘導
            openAccessibilitySettings()
            return
        }
    }

    private fun checkOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            startOverlayService()
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        startForegroundService(intent)
        Toast.makeText(this, "サービスを開始します", Toast.LENGTH_SHORT).show()
    }

    fun accessibilityServiceEnabled(context: Context): Boolean {
        val target = AppLaunchAccessibilityService::class.java
        val am = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        // ユーザーが有効化している全サービスを取得
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { info ->
            val si = info.resolveInfo.serviceInfo
            si.packageName == context.packageName && si.name == target.name
        }
    }

    fun openAccessibilitySettings() {
        startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        )
    }
}
