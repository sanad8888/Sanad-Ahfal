package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ClinicSettings
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItemData
import com.example.data.model.Product
import com.example.data.model.StockMovement
import com.example.data.repository.ClinicRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "ClinicViewModel"

class ClinicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ClinicRepository(application)

    val products: StateFlow<List<Product>> = repository.observeProducts()
        .catch { e ->
            Log.e(TAG, "Error observing products", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customers: StateFlow<List<Customer>> = repository.observeCustomers()
        .catch { e ->
            Log.e(TAG, "Error observing customers", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val invoices: StateFlow<List<Invoice>> = repository.observeInvoices()
        .catch { e ->
            Log.e(TAG, "Error observing invoices", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val stockMovements: StateFlow<List<StockMovement>> = repository.observeStockMovements()
        .catch { e ->
            Log.e(TAG, "Error observing stock movements", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val settings: StateFlow<ClinicSettings> = repository.observeSettings()
        .catch { e ->
            Log.e(TAG, "Error observing settings", e)
            emit(ClinicSettings())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ClinicSettings())

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _scannedProduct = MutableStateFlow<Product?>(null)
    val scannedProduct: StateFlow<Product?> = _scannedProduct.asStateFlow()

    private val _scannedBarcode = MutableStateFlow<String?>(null)
    val scannedBarcode: StateFlow<String?> = _scannedBarcode.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.checkAndSeedInitialData()
            } catch (e: Exception) {
                Log.w(TAG, "Initial seed check: ${e.message}")
            }
        }
    }

    // --- Search & Filtering ---
    val productSearchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("الكل")

    val filteredProducts: StateFlow<List<Product>> = combine(
        products,
        productSearchQuery,
        selectedCategory
    ) { list, query, cat ->
        list.filter { product ->
            val matchQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.barcode.contains(query, ignoreCase = true) ||
                    product.activeIngredient.contains(query, ignoreCase = true)
            val matchCategory = cat == "الكل" || product.category == cat
            matchQuery && matchCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // --- Barcode lookup ---
    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            _scannedBarcode.value = barcode
            val found = repository.findProductByBarcode(barcode)
            _scannedProduct.value = found
            if (found != null) {
                _userMessage.emit("تم العثور على الصنف: ${found.name}")
            } else {
                _userMessage.emit("باركود جديد: $barcode (يمكنك إضافته عبر الالتقاط الذكي)")
            }
        }
    }

    fun clearScannedResult() {
        _scannedProduct.value = null
        _scannedBarcode.value = null
    }

    // --- Product Operations ---
    fun saveProduct(product: Product, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveProduct(product)
                _userMessage.emit("تم حفظ المنتج بنجاح")
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save product", e)
                _userMessage.emit("فشل حفظ المنتج: ${e.message}")
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(productId)
                _userMessage.emit("تم حذف المنتج")
            } catch (e: Exception) {
                _userMessage.emit("فشل الحذف: ${e.message}")
            }
        }
    }

    // --- Customer Operations ---
    fun saveCustomer(customer: Customer, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveCustomer(customer)
                _userMessage.emit("تم حفظ بيانات العميل")
                onComplete()
            } catch (e: Exception) {
                _userMessage.emit("فشل حفظ العميل: ${e.message}")
            }
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customerId)
                _userMessage.emit("تم حذف العميل")
            } catch (e: Exception) {
                _userMessage.emit("فشل الحذف: ${e.message}")
            }
        }
    }

    // --- Invoice Operations ---
    fun createInvoice(
        customer: Customer?,
        items: List<InvoiceItemData>,
        taxRate: Double,
        discount: Double,
        notes: String,
        markAsPaid: Boolean,
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val subtotal = items.sumOf { it.total }
                val taxAmount = subtotal * (taxRate / 100.0)
                val total = (subtotal + taxAmount - discount).coerceAtLeast(0.0)

                val count = invoices.value.size + 1
                val invoiceNumber = "INV-%04d".format(count)

                val invoice = Invoice(
                    invoiceNumber = invoiceNumber,
                    customerId = customer?.id ?: "",
                    customerName = customer?.name ?: "عميل نقدي",
                    customerPhone = customer?.phone ?: "",
                    status = if (markAsPaid) "paid" else "draft",
                    subtotal = subtotal,
                    taxRate = taxRate,
                    taxAmount = taxAmount,
                    discount = discount,
                    total = total,
                    items = items.map { it.toMap() },
                    notes = notes
                )

                val id = repository.saveInvoice(invoice, markAsPaid = markAsPaid)
                _userMessage.emit(
                    if (markAsPaid) "تم إصدار الفاتورة وتأكيد الدفع وخصم الكميات من المخزون بنجاح!"
                    else "تم حفظ الفاتورة كمسودة"
                )
                onComplete(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create invoice", e)
                _userMessage.emit("فشل إصدار الفاتورة: ${e.message}")
            }
        }
    }

    fun updateInvoiceStatus(invoice: Invoice, newStatus: String) {
        viewModelScope.launch {
            try {
                repository.updateInvoiceStatus(invoice, newStatus)
                _userMessage.emit(
                    if (newStatus == "paid") "تم تأكيد الدفع وخصم المخزون بنجاح!"
                    else "تم تحديث حالة الفاتورة إلى $newStatus"
                )
            } catch (e: Exception) {
                _userMessage.emit("فشل تحديث الفاتورة: ${e.message}")
            }
        }
    }

    fun deleteInvoice(invoiceId: String) {
        viewModelScope.launch {
            try {
                repository.deleteInvoice(invoiceId)
                _userMessage.emit("تم حذف الفاتورة")
            } catch (e: Exception) {
                _userMessage.emit("فشل الحذف: ${e.message}")
            }
        }
    }

    // --- Settings Operations ---
    fun saveSettings(newSettings: ClinicSettings) {
        viewModelScope.launch {
            try {
                repository.saveSettings(newSettings)
                _userMessage.emit("تم حفظ إعدادات العيادة بنجاح")
            } catch (e: Exception) {
                _userMessage.emit("فشل حفظ الإعدادات: ${e.message}")
            }
        }
    }

    // --- Reports and Analytics ---
    val totalRevenue: StateFlow<Double> = invoices.combine(settings) { invList, _ ->
        invList.filter { it.status == "paid" }.sumOf { it.total }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalCostOfSoldGoods: StateFlow<Double> = invoices.combine(settings) { invList, _ ->
        invList.filter { it.status == "paid" }.sumOf { inv ->
            inv.parsedItems().sumOf { it.costPrice * it.quantity }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val netProfit: StateFlow<Double> = combine(totalRevenue, totalCostOfSoldGoods) { rev, cost ->
        rev - cost
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val lowStockProducts: StateFlow<List<Product>> = products.combine(settings) { prodList, _ ->
        prodList.filter { it.quantity <= it.minQuantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val totalInventoryValue: StateFlow<Double> = products.combine(settings) { prodList, _ ->
        prodList.sumOf { it.costPrice * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)
}
