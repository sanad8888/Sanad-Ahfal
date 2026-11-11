package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.StatCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderGlass
import com.example.ui.theme.BorderGlassWhite
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.viewmodel.ClinicViewModel

@Composable
fun ReportsScreen(
    viewModel: ClinicViewModel,
    modifier: Modifier = Modifier
) {
    val totalRevenue by viewModel.totalRevenue.collectAsState()
    val totalCost by viewModel.totalCostOfSoldGoods.collectAsState()
    val netProfit by viewModel.netProfit.collectAsState()
    val inventoryValue by viewModel.totalInventoryValue.collectAsState()
    val lowStockList by viewModel.lowStockProducts.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val currency = settings.currencySymbol

    // Calculate top-selling products
    val topSelling = remember(invoices) {
        val countMap = mutableMapOf<String, Pair<String, Double>>()
        invoices.filter { it.status == "paid" }.forEach { inv ->
            inv.parsedItems().forEach { item ->
                val prev = countMap[item.productId]
                val prevQty = prev?.second ?: 0.0
                val name = if (item.productName.isNotBlank()) item.productName else (prev?.first ?: "صنف")
                countMap[item.productId] = Pair(name, prevQty + item.quantity)
            }
        }
        countMap.values.sortedByDescending { it.second }.take(5)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "التقارير والمؤشرات المالية",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "بيانات فعلية من واقع الفواتير المدفوعة والمخزون",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = NeonGreen)
            }
        }

        // Financial KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "إجمالي المبيعات",
                value = "%.1f %s".format(totalRevenue, currency),
                subtitle = "من الفواتير المدفوعة",
                icon = Icons.Default.PointOfSale,
                iconTint = NeonGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "صافي الربح الفعلي",
                value = "%.1f %s".format(netProfit, currency),
                subtitle = "الأرباح بعد خصم التكاليف",
                icon = Icons.Default.TrendingUp,
                iconTint = NeonViolet,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "تكلفة البضاعة المباعة",
                value = "%.1f %s".format(totalCost, currency),
                icon = Icons.Default.AttachMoney,
                iconTint = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "قيمة المخزون الحالي",
                value = "%.1f %s".format(inventoryValue, currency),
                icon = Icons.Default.Inventory,
                iconTint = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }

        // Custom Visual Bar Chart for Revenue vs Cost vs Profit
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = BorderGlass
        ) {
            Column {
                Text(
                    text = "مقارنة المبيعات والتكاليف والأرباح",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                Spacer(modifier = Modifier.height(14.dp))

                val maxVal = maxOf(totalRevenue, totalCost, netProfit, 100.0)

                // Canvas chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val barWidth = 36.dp.toPx()
                    val spacing = (size.width - (barWidth * 3)) / 4

                    val h1 = ((totalRevenue / maxVal) * (size.height - 20)).toFloat().coerceAtLeast(10f)
                    val h2 = ((totalCost / maxVal) * (size.height - 20)).toFloat().coerceAtLeast(10f)
                    val h3 = ((netProfit.coerceAtLeast(0.0) / maxVal) * (size.height - 20)).toFloat().coerceAtLeast(10f)

                    // Bar 1: Revenue
                    val x1 = spacing
                    drawRoundRect(
                        color = Color(0xFF00FFA3),
                        topLeft = Offset(x1, size.height - h1),
                        size = Size(barWidth, h1),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Bar 2: Cost
                    val x2 = spacing * 2 + barWidth
                    drawRoundRect(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(x2, size.height - h2),
                        size = Size(barWidth, h2),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Bar 3: Net Profit
                    val x3 = spacing * 3 + barWidth * 2
                    drawRoundRect(
                        color = Color(0xFFA855F7),
                        topLeft = Offset(x3, size.height - h3),
                        size = Size(barWidth, h3),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ChartLegend(color = NeonGreen, label = "المبيعات")
                    ChartLegend(color = Color(0xFF38BDF8), label = "التكاليف")
                    ChartLegend(color = NeonViolet, label = "صافي الربح")
                }
            }
        }

        // Top Selling Products
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = BorderGlassWhite
        ) {
            Column {
                Text(
                    text = "الأصناف الأكثر طلباً ومبيعاً",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (topSelling.isEmpty()) {
                    Text(
                        text = "لا توجد مبيعات مسجلة بعد لتحديد الأصناف الأكثر مبيعاً",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    topSelling.forEachIndexed { index, pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (index == 0) NeonGreen else SurfaceElevated
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index == 0) Color(0xFF003822) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = pair.first,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "بيعت ${pair.second.toInt()} وحدة",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        if (index < topSelling.lastIndex) {
                            HorizontalDivider(color = BorderGlassWhite.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // Low Stock Warnings
        if (lowStockList.isNotEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = StatusWarning.copy(alpha = 0.5f)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusWarning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تنبيهات نواقص المخزون (${lowStockList.size} أصناف)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusWarning
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    lowStockList.take(5).forEach { prod ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(prod.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "المتبقي: ${prod.quantity} (حد الطلب: ${prod.minQuantity})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusWarning
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
