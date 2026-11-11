package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.AlQaafiClinicTheme
import com.example.ui.theme.BackgroundDark
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            AlQaafiClinicTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = BackgroundDark
                    ) {
                        AppRoot(authViewModel = authViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AppRoot(authViewModel: AuthViewModel) {
    val authState by authViewModel.uiState.collectAsState()

    when (val state = authState) {
        is AuthUiState.Authenticated -> {
            val uid = state.user.uid
            key(uid) {
                MainScreen(
                    currentUserId = uid,
                    authViewModel = authViewModel
                )
            }
        }
        else -> {
            AuthScreen(authViewModel = authViewModel)
        }
    }
}
