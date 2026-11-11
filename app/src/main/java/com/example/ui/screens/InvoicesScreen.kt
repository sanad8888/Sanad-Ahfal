package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItemData
import com.example.data.model.Product
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonSearchBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderGlass
import com.example.ui.theme.BorderGlassWhite
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenDim
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.viewmodel.ClinicViewModel
import com.example.util.InvoicePdfHelper
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    viewModel: ClinicViewModel,
    modifier: Modifier = Modifier
) {
    val invoices by viewModel.invoices.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }

    var showNewInvoiceDialog by remember { mutableStateOf(false) }
    var selectedInvoiceForDetails by remember { mutableStateOf<Invoice?>(null) }

    val context = LocalContext.current

    val filteredInvoices = remember(invoices, searchQuery, selectedFilter) {
        invoices.filter { inv ->
            val matchQuery = searchQuery.isBlank() ||
                    inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.customerName.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (selectedFilter) {
                "مدفوعة" -> inv.status == "paid"
                "مسودة" -> inv.status == "draft"
                "ملغاة" -> inv.status == "cancelled"
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewInvoiceDialog = true },
                containerColor = NeonGreen,
                contentColor = Color(0xFF003822),
                modifier = Modifier.testTag("new_invoice_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "فاتورة جديدة")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فاتورة جديدة", fontWeight = FontWeight.Bold)
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "الفواتير والمبيعات",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "إجمالي الفواتير: ${invoices.size} (المدفوعة: ${invoices.count { it.status == "paid" }})",
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
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = NeonGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            NeonSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "ابحث برقم الفاتورة أو اسم العميل..."
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf("الكل", "مدفوعة", "مسودة", "ملغاة")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) NeonGreen else SurfaceElevated,
                        border = BorderStroke(1.dp, if (isSelected) NeonGreen else BorderGlass),
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color(0xFF003822) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Invoices List
            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "لا توجد فواتير بعد، اضغط على زر 'فاتورة جديدة' للبدء" else "لا توجد فواتير تطابق بحثك",
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
                    items(filteredInvoices, key = { it.id }) { invoice ->
                        InvoiceCard(
                            invoice = invoice,
                            currencySymbol = settings.currencySymbol,
                            onClick = { selectedInvoiceForDetails = invoice }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // New Invoice Dialog
    if (showNewInvoiceDialog) {
        NewInvoiceDialog(
            customers = customers,
            products = products,
            defaultTaxRate = settings.vatPercentage,
            currencySymbol = settings.currencySymbol,
            onDismiss = { showNewInvoiceDialog = false },
            onCreate = { customer, items, taxRate, discount, notes, markAsPaid ->
                viewModel.createInvoice(
                    customer = customer,
                    items = items,
                    taxRate = taxRate,
                    discount = discount,
                    notes = notes,
                    markAsPaid = markAsPaid
                )
                showNewInvoiceDialog = false
            }
        )
    }

    // Invoice Details BottomSheet
    selectedInvoiceForDetails?.let { inv ->
        val currentInvoice = invoices.find { it.id == inv.id } ?: inv
        InvoiceDetailsBottomSheet(
            invoice = currentInvoice,
            settings = settings,
            onDismiss = { selectedInvoiceForDetails = null },
            onMarkAsPaid = {
                viewModel.updateInvoiceStatus(currentInvoice, "paid")
            },
            onDelete = {
                viewModel.deleteInvoice(currentInvoice.id)
                selectedInvoiceForDetails = null
            },
            onShareWhatsApp = {
                InvoicePdfHelper.shareInvoiceViaWhatsApp(context, currentInvoice, settings)
            }
        )
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(BorderStroke(1.dp, BorderGlassWhite), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                StatusBadge(status = invoice.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "العميل: ${invoice.customerName.ifBlank { "عميل نقدي" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            invoice.issueDate?.let { ts ->
                val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(ts.toDate())
                Text(
                    text = "التاريخ: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Text(
                    text = "${invoice.items.size} أصناف",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "%.2f %s".format(invoice.total, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailsBottomSheet(
    invoice: Invoice,
    settings: com.example.data.model.ClinicSettings,
    onDismiss: () -> Unit,
    onMarkAsPaid: () -> Unit,
    onDelete: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Receipt Header Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceElevated)
                    .border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = settings.clinicName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = settings.invoiceHeader,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderGlass)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "فاتورة: ${invoice.invoiceNumber}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        StatusBadge(status = invoice.status)
                    }

                    if (invoice.customerName.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("العميل:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(invoice.customerName, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Items table
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        invoice.parsedItems().forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.productName} × ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "%.2f %s".format(item.total, settings.currencySymbol),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderGlass)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المجموع الفرعي:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("%.2f %s".format(invoice.subtotal, settings.currencySymbol))
                    }
                    if (invoice.taxAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الضريبة (%.0f%%):".format(invoice.taxRate), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("%.2f %s".format(invoice.taxAmount, settings.currencySymbol))
                        }
                    }
                    if (invoice.discount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الخصم:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("-%.2f %s".format(invoice.discount, settings.currencySymbol), color = NeonViolet)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الإجمالي الكلي:", fontWeight = FontWeight.Bold, color = NeonGreen)
                        Text(
                            "%.2f %s".format(invoice.total, settings.currencySymbol),
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            if (invoice.status != "paid") {
                Button(
                    onClick = onMarkAsPaid,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("mark_invoice_paid_button")
                ) {
                    Icon(imageVector = Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("سداد الفاتورة وخصم الكميات من المخزون تلقائياً", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onShareWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_whatsapp_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة واتساب + PDF", color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun NewInvoiceDialog(
    customers: List<Customer>,
    products: List<Product>,
    defaultTaxRate: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onCreate: (Customer?, List<InvoiceItemData>, Double, Double, String, Boolean) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customCustomerName by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<InvoiceItemData>() }

    var taxRate by remember { mutableDoubleStateOf(defaultTaxRate) }
    var discountText by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    var showProductPicker by remember { mutableStateOf(false) }

    val subtotal = items.sumOf { it.total }
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val taxAmount = subtotal * (taxRate / 100.0)
    val total = (subtotal + taxAmount - discount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "إصدار فاتورة بيطرية جديدة",
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
                // Customer Section
                Text("العميل / المربي:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = customCustomerName,
                    onValueChange = { customCustomerName = it },
                    placeholder = { Text("اسم العميل (أو نقدي)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Customer Selector
                if (customers.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(customers) { c ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedCustomer?.id == c.id) NeonGreen else SurfaceElevated,
                                modifier = Modifier.clickable {
                                    selectedCustomer = c
                                    customCustomerName = c.name
                                }
                            ) {
                                Text(
                                    text = c.name,
                                    fontSize = 11.sp,
                                    color = if (selectedCustomer?.id == c.id) Color(0xFF003822) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderGlass)

                // Items Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("أصناف الفاتورة (${items.size}):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Button(
                        onClick = { showProductPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreenDim),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة صنف", color = NeonGreen, fontSize = 12.sp)
                    }
                }

                if (items.isEmpty()) {
                    Text(
                        text = "لم تتم إضافة أي صنف بعد، اضغط 'إضافة صنف'",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                } else {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceElevated, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${item.quantity} × ${item.unitPrice} = ${item.total} $currencySymbol", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(
                                onClick = { items.removeAt(index) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderGlass)

                // Tax & Discount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = "%.0f".format(taxRate),
                        onValueChange = { taxRate = it.toDoubleOrNull() ?: 0.0 },
                        label = { Text("ضريبة %") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("الخصم") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Total display
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المجموع الفرعي:", fontSize = 12.sp)
                            Text("%.2f %s".format(subtotal, currencySymbol), fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الضريبة:", fontSize = 12.sp)
                            Text("%.2f %s".format(taxAmount, currencySymbol), fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الإجمالي الكلي:", fontWeight = FontWeight.Bold, color = NeonGreen)
                            Text("%.2f %s".format(total, currencySymbol), fontWeight = FontWeight.Bold, color = NeonGreen, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        val customer = selectedCustomer ?: if (customCustomerName.isNotBlank()) Customer(name = customCustomerName) else null
                        onCreate(customer, items.toList(), taxRate, discount, notes, true)
                    },
                    enabled = items.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.fillMaxWidth().testTag("save_and_pay_invoice_button")
                ) {
                    Text("حفظ وسداد الآن (خصم المخزون)", color = Color(0xFF003822), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val customer = selectedCustomer ?: if (customCustomerName.isNotBlank()) Customer(name = customCustomerName) else null
                        onCreate(customer, items.toList(), taxRate, discount, notes, false)
                    },
                    enabled = items.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().testTag("save_draft_invoice_button")
                ) {
                    Text("حفظ كمسودة فقط")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )

    // Product Picker Dialog
    if (showProductPicker) {
        ProductPickerDialog(
            products = products,
            currencySymbol = currencySymbol,
            onDismiss = { showProductPicker = false },
            onProductSelected = { prod, qty ->
                val unitPrice = prod.sellPrice
                val totalItem = unitPrice * qty
                items.add(
                    InvoiceItemData(
                        productId = prod.id,
                        productName = prod.name,
                        barcode = prod.barcode,
                        quantity = qty,
                        unitPrice = unitPrice,
                        costPrice = prod.costPrice,
                        total = totalItem
                    )
                )
                showProductPicker = false
            }
        )
    }
}

@Composable
fun ProductPickerDialog(
    products: List<Product>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onProductSelected: (Product, Double) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantityText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("اختر الدواء أو الصنف", fontWeight = FontWeight.Bold, color = NeonGreen) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                NeonSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "ابحث عن الصنف..."
                )
                Spacer(modifier = Modifier.height(10.dp))

                val filtered = products.filter {
                    searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(filtered) { p ->
                        val isSelected = selectedProduct?.id == p.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) NeonGreenDim else SurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) NeonGreen else BorderGlass),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedProduct = p }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("المتوفر بالمخزن: ${p.quantity} | السعر: ${p.sellPrice} $currencySymbol", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                selectedProduct?.let { p ->
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("الكمية المطلوبة (المتوفر: ${p.quantity})") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedProduct?.let { p ->
                        val qty = quantityText.toDoubleOrNull() ?: 1.0
                        onProductSelected(p, qty)
                    }
                },
                enabled = selectedProduct != null,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("إضافة للفاتورة", color = Color(0xFF003822), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
