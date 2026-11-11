package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.viewmodel.AiAssistantViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ClinicViewModel
import kotlinx.coroutines.flow.collectLatest

enum class MainTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    INVOICES("الفواتير", Icons.Default.ReceiptLong),
    INVENTORY("المخزون", Icons.Default.Inventory2),
    CUSTOMERS("العملاء", Icons.Default.People),
    REPORTS("التقارير", Icons.Default.BarChart),
    AI_SETTINGS("المساعد الذكي", Icons.Default.AutoAwesome)
}

@Composable
fun MainScreen(
    currentUserId: String,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    clinicViewModel: ClinicViewModel = viewModel(key = currentUserId),
    aiViewModel: AiAssistantViewModel = viewModel(key = currentUserId)
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentUserId) {
        clinicViewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Handle Back Press to return to Home (Invoices) tab first
    BackHandler(enabled = selectedTabIndex != 0) {
        selectedTabIndex = 0
    }

    // Force RTL for Arabic Clinic Experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = BackgroundDark,
            contentWindowInsets = WindowInsets.navigationBars,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    MainTab.values().forEachIndexed { index, tab ->
                        val isSelected = selectedTabIndex == index
                        val activeColor = if (tab == MainTab.AI_SETTINGS) NeonViolet else NeonGreen

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeColor,
                                selectedTextColor = activeColor,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = activeColor.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTabIndex) {
                    0 -> InvoicesScreen(viewModel = clinicViewModel)
                    1 -> InventoryScreen(clinicViewModel = clinicViewModel, aiViewModel = aiViewModel)
                    2 -> CustomersScreen(viewModel = clinicViewModel)
                    3 -> ReportsScreen(viewModel = clinicViewModel)
                    4 -> AiAndSettingsScreen(
                        aiViewModel = aiViewModel,
                        clinicViewModel = clinicViewModel,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}
