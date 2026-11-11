package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.data.model.StockMovement
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonSearchBar
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderGlass
import com.example.ui.theme.BorderGlassWhite
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenDim
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.NeonVioletDim
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.viewmodel.AiAssistantViewModel
import com.example.ui.viewmodel.ClinicViewModel
import com.example.ui.viewmodel.SmartCaptureUiState
import com.example.util.BarcodeScannerHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    clinicViewModel: ClinicViewModel,
    aiViewModel: AiAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val products by clinicViewModel.filteredProducts.collectAsState()
    val allProducts by clinicViewModel.products.collectAsState()
    val settings by clinicViewModel.settings.collectAsState()
    val stockMovements by clinicViewModel.stockMovements.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showStockMovementsSheet by remember { mutableStateOf(false) }
    var showSmartCaptureDialog by remember { mutableStateOf(false) }
    var showBarcodeScanDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val scannedProduct by clinicViewModel.scannedProduct.collectAsState()
    val scannedBarcode by clinicViewModel.scannedBarcode.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddProductDialog = true
                },
                containerColor = NeonGreen,
                contentColor = Color(0xFF003822),
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة منتج")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة صنف", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header with Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "المخزون والأصناف البيطرية",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "إجمالي الأصناف: ${allProducts.size} | منخفض المخزون: ${allProducts.count { it.quantity <= it.minQuantity }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Smart Capture Button
                    IconButton(
                        onClick = { showSmartCaptureDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonVioletDim)
                            .testTag("smart_capture_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "التقاط ذكي", tint = NeonViolet)
                    }

                    // Barcode Scanner Button
                    IconButton(
                        onClick = { showBarcodeScanDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonGreenDim)
                            .testTag("barcode_scanner_button")
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "مسح باركود", tint = NeonGreen)
                    }

                    // Stock Movement History Button
                    IconButton(
                        onClick = { showStockMovementsSheet = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceElevated)
                            .testTag("stock_history_button")
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "سجل الحركات", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            NeonSearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    clinicViewModel.productSearchQuery.value = it
                },
                placeholder = "ابحث بالاسم أو الباركود أو المادة الفعالة..."
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips
            val categories = listOf("الكل", "مضادات حيوية", "مضادات طفيليات", "فيتامينات ومقويات", "لقاحات", "مطهرات ومستلزمات", "محاليل علاجية")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) NeonGreen else SurfaceElevated,
                        border = BorderStroke(1.dp, if (isSelected) NeonGreen else BorderGlass),
                        modifier = Modifier.clickable {
                            selectedCategory = cat
                            clinicViewModel.selectedCategory.value = cat
                        }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color(0xFF003822) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scanned Product Notification Card (if any barcode was just scanned)
            if (scannedProduct != null) {
                val prod = scannedProduct!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .border(BorderStroke(1.dp, NeonGreen), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = NeonGreenDim.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = NeonGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("نتيجة المسح: ${prod.name}", fontWeight = FontWeight.Bold, color = NeonGreen)
                            Text("الكمية: ${prod.quantity} | البيع: ${prod.sellPrice} ${settings.currencySymbol} | المادة: ${prod.activeIngredient}", fontSize = 12.sp)
                        }
                        IconButton(onClick = { clinicViewModel.clearScannedResult() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }
                }
            }

            // Products List
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "لا توجد أدوية بعد في المخزون" else "لا توجد أصناف تطابق بحثك",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            currencySymbol = settings.currencySymbol,
                            onEdit = {
                                editingProduct = product
                                showAddProductDialog = true
                            },
                            onDelete = { clinicViewModel.deleteProduct(product.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddProductDialog) {
        ProductFormDialog(
            initialProduct = editingProduct,
            currencySymbol = settings.currencySymbol,
            onDismiss = {
                showAddProductDialog = false
                editingProduct = null
            },
            onSave = { product ->
                clinicViewModel.saveProduct(product) {
                    showAddProductDialog = false
                    editingProduct = null
                }
            }
        )
    }

    // Smart Capture Dialog (AI-Powered)
    if (showSmartCaptureDialog) {
        SmartCaptureDialog(
            aiViewModel = aiViewModel,
            onDismiss = {
                showSmartCaptureDialog = false
                aiViewModel.resetSmartCapture()
            },
            onApply = { suggestion, scannedBarcodeStr ->
                editingProduct = Product(
                    name = suggestion.name,
                    category = suggestion.category,
                    activeIngredient = suggestion.activeIngredient,
                    dosageInstructions = suggestion.dosageInstructions,
                    costPrice = suggestion.suggestedCostPrice,
                    sellPrice = suggestion.suggestedSellPrice,
                    description = suggestion.description,
                    barcode = scannedBarcodeStr,
                    quantity = 10.0,
                    minQuantity = 3.0
                )
                showSmartCaptureDialog = false
                showAddProductDialog = true
            }
        )
    }

    // Barcode Scanner Dialog (Camera Photo or Manual input)
    if (showBarcodeScanDialog) {
        BarcodeScanDialog(
            onDismiss = { showBarcodeScanDialog = false },
            onBarcodeScanned = { barcode ->
                clinicViewModel.onBarcodeScanned(barcode)
                showBarcodeScanDialog = false
            }
        )
    }

    // Stock Movements Bottom Sheet
    if (showStockMovementsSheet) {
        StockMovementsBottomSheet(
            movements = stockMovements,
            onDismiss = { showStockMovementsSheet = false }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLowStock = product.quantity <= product.minQuantity

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    if (isLowStock) StatusWarning.copy(alpha = 0.6f) else BorderGlassWhite
                ),
                RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (product.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NeonViolet.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = product.category,
                                    fontSize = 11.sp,
                                    color = NeonViolet,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (product.barcode.isNotBlank()) {
                                Text(
                                    text = "باركود: ${product.barcode}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = NeonGreen, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (product.activeIngredient.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "المادة الفعالة: ${product.activeIngredient}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (product.dosageInstructions.isNotBlank()) {
                Text(
                    text = "الجرعة: ${product.dosageInstructions}",
                    fontSize = 11.sp,
                    color = NeonGreen.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderGlassWhite)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quantity with low-stock warning
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLowStock) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "تنبيه نقص المخزون",
                            tint = StatusWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "المتوفر: ${product.quantity}",
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) StatusWarning else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    if (isLowStock) {
                        Text(" (حد الطلب: ${product.minQuantity})", fontSize = 11.sp, color = StatusWarning)
                    }
                }

                // Prices
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "سعر البيع: %.2f %s".format(product.sellPrice, currencySymbol),
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "التكلفة: %.2f %s".format(product.costPrice, currencySymbol),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    initialProduct: Product?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "أدوية بيطرية") }
    var costPriceText by remember { mutableStateOf(initialProduct?.costPrice?.toString() ?: "") }
    var sellPriceText by remember { mutableStateOf(initialProduct?.sellPrice?.toString() ?: "") }
    var quantityText by remember { mutableStateOf(initialProduct?.quantity?.toString() ?: "10") }
    var minQuantityText by remember { mutableStateOf(initialProduct?.minQuantity?.toString() ?: "3") }
    var activeIngredient by remember { mutableStateOf(initialProduct?.activeIngredient ?: "") }
    var dosageInstructions by remember { mutableStateOf(initialProduct?.dosageInstructions ?: "") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = if (initialProduct == null) "إضافة صنف بيطري جديد" else "تعديل الصنف",
                fontWeight = FontWeight.Bold,
                color = NeonGreen
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الدواء / المنتج *") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("الباركود (Barcode)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("product_barcode_input")
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("التصنيف (مضاد، فيتامين، لقاح...)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it },
                        label = { Text("سعر التكلفة ($currencySymbol)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = sellPriceText,
                        onValueChange = { sellPriceText = it },
                        label = { Text("سعر البيع ($currencySymbol) *") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_sell_price_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("الكمية الحالية *") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_quantity_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = minQuantityText,
                        onValueChange = { minQuantityText = it },
                        label = { Text("حد التنبيه (الأدنى)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = activeIngredient,
                    onValueChange = { activeIngredient = it },
                    label = { Text("المادة الفعالة والتركيز") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dosageInstructions,
                    onValueChange = { dosageInstructions = it },
                    label = { Text("طريقة الاستخدام والجرعة") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("دواعي الاستعمال وملاحظات") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPriceText.toDoubleOrNull() ?: 0.0
                    val sell = sellPriceText.toDoubleOrNull() ?: 0.0
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    val minQty = minQuantityText.toDoubleOrNull() ?: 5.0

                    val product = (initialProduct ?: Product()).copy(
                        name = name.trim(),
                        barcode = barcode.trim(),
                        category = category.trim().ifBlank { "أدوية بيطرية" },
                        costPrice = cost,
                        sellPrice = sell,
                        quantity = qty,
                        minQuantity = minQty,
                        activeIngredient = activeIngredient.trim(),
                        dosageInstructions = dosageInstructions.trim(),
                        description = description.trim()
                    )
                    onSave(product)
                },
                enabled = name.isNotBlank() && sellPriceText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("حفظ الصنف", color = Color(0xFF003822), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun SmartCaptureDialog(
    aiViewModel: AiAssistantViewModel,
    onDismiss: () -> Unit,
    onApply: (com.example.data.repository.SmartProductSuggestion, String) -> Unit
) {
    val state by aiViewModel.smartCaptureState.collectAsState()
    var inputQuery by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    // Zero-permission Android Photo Picker for capturing/selecting drug image
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedBitmap = bitmap
            } catch (e: Exception) {
                // Error loading bitmap
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = NeonViolet)
                Spacer(modifier = Modifier.width(8.dp))
                Text("التقاط ذكي بالذكاء الاصطناعي", fontWeight = FontWeight.Bold, color = NeonViolet)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "التقط صورة علبة الدواء البيطري أو اكتب اسمه/الباركود وسيقوم نموذج Gemini باستخراج الاسم العلمي، المادة الفعالة، الجرعة، والتصنيف تلقائياً!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Photo preview or picker button
                if (selectedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, NeonViolet), RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "صورة الدواء",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { selectedBitmap = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إلغاء الصورة", tint = Color.White)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = NeonViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختر أو التقط صورة الدواء", color = NeonViolet)
                    }
                }

                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    label = { Text("أو اكتب اسم المستحضر / الباركود") },
                    placeholder = { Text("مثال: أوكسي تتراسيكلين، بنسلين...") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                when (state) {
                    is SmartCaptureUiState.Loading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = NeonViolet, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("جاري التحليل واستخراج البيانات البيطرية...", color = NeonViolet, fontSize = 12.sp)
                        }
                    }
                    is SmartCaptureUiState.Success -> {
                        val sug = (state as SmartCaptureUiState.Success).suggestion
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NeonVioletDim.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, NeonViolet),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("الاسم المقترح: ${sug.name}", fontWeight = FontWeight.Bold, color = NeonViolet)
                                Text("التصنيف: ${sug.category}", fontSize = 12.sp)
                                if (sug.activeIngredient.isNotBlank()) {
                                    Text("المادة الفعالة: ${sug.activeIngredient}", fontSize = 11.sp)
                                }
                                if (sug.dosageInstructions.isNotBlank()) {
                                    Text("الجرعة: ${sug.dosageInstructions}", fontSize = 11.sp, color = NeonGreen)
                                }
                                Text("سعر التكلفة المقترح: ${sug.suggestedCostPrice} | سعر البيع: ${sug.suggestedSellPrice}", fontSize = 11.sp)
                            }
                        }
                    }
                    is SmartCaptureUiState.Error -> {
                        Text(
                            text = (state as SmartCaptureUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (state is SmartCaptureUiState.Success) {
                Button(
                    onClick = {
                        val sug = (state as SmartCaptureUiState.Success).suggestion
                        onApply(sug, inputQuery)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("استخدام البيانات وتعبئة الصنف", color = Color(0xFF003822), fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        aiViewModel.analyzeProduct(selectedBitmap, inputQuery)
                    },
                    enabled = selectedBitmap != null || inputQuery.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
                ) {
                    Text("بدء التحليل بالذكاء الاصطناعي")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
fun BarcodeScanDialog(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    var manualBarcode by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    isScanning = true
                    scanError = null
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    selectedBitmap = bitmap
                    val result = BarcodeScannerHelper.scanBitmap(bitmap)
                    isScanning = false
                    if (result != null) {
                        onBarcodeScanned(result)
                    } else {
                        scanError = "لم يتم العثور على باركود في الصورة، يرجى إعادة المحاولة أو إدخاله يدوياً."
                    }
                } catch (e: Exception) {
                    isScanning = false
                    scanError = "حدث خطأ أثناء قراءة الصورة: ${e.message}"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = NeonGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("مسح أو إدخال الباركود", fontWeight = FontWeight.Bold, color = NeonGreen)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "امسح باركود المنتج بواسطة كاميرا الهاتف للبحث عنه في المخزن فوراً وعرض تفاصيله وسعره.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        photoLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreenDim),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("التقاط صورة الباركود بالكاميرا", color = NeonGreen)
                }

                if (isScanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري قراءة الباركود...", color = NeonGreen)
                    }
                }

                scanError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                HorizontalDivider(color = BorderGlass)

                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { manualBarcode = it },
                    label = { Text("أو اكتب رقم الباركود يدوياً") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (manualBarcode.isNotBlank()) {
                        onBarcodeScanned(manualBarcode.trim())
                    }
                },
                enabled = manualBarcode.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("بحث في المخزن", color = Color(0xFF003822), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMovementsBottomSheet(
    movements: List<StockMovement>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "سجل حركة المخزون (Audit Log)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated
                ) {
                    Text(
                        text = "${movements.size} حركة مسجلة",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (movements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد حركات مخزون مسجلة بعد",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(400.dp)
                ) {
                    items(movements) { mov ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mov.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    val typeArabic = when (mov.type) {
                                        "in" -> "توريد / شراء +"
                                        "out" -> "صرف / بيع -"
                                        else -> "تعديل رصيد"
                                    }
                                    val typeColor = when (mov.type) {
                                        "in" -> NeonGreen
                                        "out" -> StatusError
                                        else -> NeonViolet
                                    }
                                    Text(
                                        text = "$typeArabic | الكمية: ${mov.quantity}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = typeColor
                                    )
                                    if (mov.notes.isNotBlank()) {
                                        Text(mov.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    mov.createdAt?.let { ts ->
                                        val dateStr = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(ts.toDate())
                                        Text(dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("الرصيد الجديد: ${mov.newQuantity}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
