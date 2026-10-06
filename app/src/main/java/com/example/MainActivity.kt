package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.core.session.SessionManager
import com.example.core.theme.ThemeMode
import com.example.core.theme.UnimaidTheme
import com.example.data.repository.AuthRepositoryImpl
import com.example.navigation.AppNavigation

class MainActivity : ComponentActivity() {

    private var currentActionMode: android.view.ActionMode? = null

    override fun onActionModeStarted(mode: android.view.ActionMode?) {
        super.onActionModeStarted(mode)
        currentActionMode = mode
    }

    override fun onActionModeFinished(mode: android.view.ActionMode?) {
        super.onActionModeFinished(mode)
        if (currentActionMode == mode) {
            currentActionMode = null
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            try {
                currentActionMode?.finish()
                currentActionMode = null
            } catch (_: Exception) {}
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            currentActionMode?.finish()
            currentActionMode = null
        } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionManager = SessionManager.getInstance(applicationContext)
        val authRepository = AuthRepositoryImpl(sessionManager)

        setContent {
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
            val navController = rememberNavController()

            LaunchedEffect(Unit) {
                authRepository.validateAndRestoreSession()
            }

            UnimaidTheme(themeMode = themeMode) {
                AppNavigation(
                    navController = navController,
                    authRepository = authRepository,
                    themeMode = themeMode,
                    onToggleTheme = {
                        themeMode = when (themeMode) {
                            ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.LIGHT
                            ThemeMode.SYSTEM -> ThemeMode.DARK
                        }
                    },
                    onSetThemeMode = { newMode ->
                        themeMode = newMode
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

