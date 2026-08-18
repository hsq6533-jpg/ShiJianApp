package com.shijian.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.shijian.app.data.prefs.DarkMode
import com.shijian.app.ui.navigation.AppNavHost
import com.shijian.app.ui.navigation.Routes
import com.shijian.app.ui.screens.profile.UpdateDialog
import com.shijian.app.ui.theme.ShiJianTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ShiJianApp).container

        // 通知点击：shijian://app/news → 落到新闻 Tab
        val startTab = intent?.data?.path?.trim('/')
            ?.takeIf { it in Routes.TABS } ?: Routes.HOME

        enableEdgeToEdge()

        setContent {
            val settings by container.settingsRepo.settings.collectAsState()
            val darkTheme = when (settings.darkMode) {
                DarkMode.SYSTEM -> isSystemInDarkTheme()
                DarkMode.LIGHT -> false
                DarkMode.DARK -> true
            }
            ShiJianTheme(darkTheme = darkTheme) {
                var showUpdate by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    val current = BuildConfig.VERSION_NAME
                    if (container.settingsRepo.lastSeenVersion() != current) {
                        container.settingsRepo.markVersionSeen(current)
                        showUpdate = true
                    }
                }
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize()
                ) {
                    AppNavHost(container = container, startTab = startTab)
                    if (showUpdate) {
                        UpdateDialog(onDismiss = { showUpdate = false })
                    }
                }
            }
        }
    }
}
