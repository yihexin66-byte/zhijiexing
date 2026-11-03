package com.example.zhijiexing

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.zhijiexing.data.SessionStore
import com.example.zhijiexing.ui.theme.ZhijiexingTheme

class MainActivity : ComponentActivity() {

    private val localNetworkPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 拒绝时登录页会提示无法连接服务器，可稍后在系统设置中开启 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestLocalNetworkPermissionIfNeeded()
        setContent {
            ZhijiexingTheme {
                ZhijiexingApp()
            }
        }
    }

    /** 默认连接 uniCloud，不打扰用户；只有手动改为本机/局域网后端时才申请 Android 17 的本地网络权限。 */
    private fun requestLocalNetworkPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 37) return
        if (!SessionStore(this).usesLocalServer()) return
        val granted = ContextCompat.checkSelfPermission(
            this, "android.permission.ACCESS_LOCAL_NETWORK"
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            localNetworkPermission.launch("android.permission.ACCESS_LOCAL_NETWORK")
        }
    }
}
