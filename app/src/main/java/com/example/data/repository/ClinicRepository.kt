package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ClinicSettings
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.Product
import com.example.data.model.StockMovement
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ClinicRepository(val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب Google أولاً.")
    }

    // --- PRODUCTS ---

    fun observeProducts(): Flow<List<Product>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("products")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Product::class.java).sortedBy { it.name }
                }
        )
    }

    suspend fun saveProduct(product: Product) {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("products")
        if (product.id.isBlank()) {
            val doc = collection.document()
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "name" to product.name,
                "barcode" to product.barcode,
                "category" to product.category,
                "costPrice" to product.costPrice,
                "sellPrice" to product.sellPrice,
                "quantity" to product.quantity,
                "minQuantity" to product.minQuantity,
                "imageUrl" to product.imageUrl,
                "description" to product.description,
                "activeIngredient" to product.activeIngredient,
                "dosageInstructions" to product.dosageInstructions,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            doc.set(payload).await()

            // Record initial stock if > 0
            if (product.quantity > 0) {
                recordStockMovement(
                    productId = doc.id,
                    productName = product.name,
                    type = "in",
                    quantity = product.quantity,
                    previousQuantity = 0.0,
                    newQuantity = product.quantity,
                    notes = "رصيد افتتاحي للمنتج"
                )
            }
        } else {
            val doc = collection.document(product.id)
            val currentDoc = doc.get().await()
            val oldQty = (currentDoc.getDouble("quantity") ?: 0.0)
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "name" to product.name,
                "barcode" to product.barcode,
                "category" to product.category,
                "costPrice" to product.costPrice,
                "sellPrice" to product.sellPrice,
                "quantity" to product.quantity,
                "minQuantity" to product.minQuantity,
                "imageUrl" to product.imageUrl,
                "description" to product.description,
                "activeIngredient" to product.activeIngredient,
                "dosageInstructions" to product.dosageInstructions,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            doc.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()

            if (oldQty != product.quantity) {
                recordStockMovement(
                    productId = product.id,
                    productName = product.name,
                    type = "adjustment",
                    quantity = product.quantity - oldQty,
                    previousQuantity = oldQty,
                    newQuantity = product.quantity,
                    notes = "تعديل يدوي للمخزون"
                )
            }
        }
    }

    suspend fun deleteProduct(productId: String) {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("products").document(productId).delete().await()
    }

    suspend fun findProductByBarcode(barcode: String): Product? {
        val uid = requireUserId()
        if (barcode.isBlank()) return null
        val snapshot = db.collection("users").document(uid).collection("products")
            .whereEqualTo("barcode", barcode)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(Product::class.java)
    }

    // --- CUSTOMERS ---

    fun observeCustomers(): Flow<List<Customer>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("customers")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Customer::class.java).sortedBy { it.name }
                }
        )
    }

    suspend fun saveCustomer(customer: Customer) {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("customers")
        if (customer.id.isBlank()) {
            val doc = collection.document()
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "name" to customer.name,
                "phone" to customer.phone,
                "address" to customer.address,
                "taxNumber" to customer.taxNumber,
                "notes" to customer.notes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            doc.set(payload).await()
        } else {
            val doc = collection.document(customer.id)
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "name" to customer.name,
                "phone" to customer.phone,
                "address" to customer.address,
                "taxNumber" to customer.taxNumber,
                "notes" to customer.notes,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            doc.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    suspend fun deleteCustomer(customerId: String) {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("customers").document(customerId).delete().await()
    }

    // --- INVOICES & AUTOMATIC INVENTORY DEDUCTION ---

    fun observeInvoices(): Flow<List<Invoice>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("invoices")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Invoice::class.java).sortedByDescending { it.invoiceNumber }
                }
        )
    }

    suspend fun saveInvoice(invoice: Invoice, markAsPaid: Boolean = false): String {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("invoices")
        val doc = if (invoice.id.isBlank()) collection.document() else collection.document(invoice.id)
        val invoiceId = doc.id

        val status = if (markAsPaid) "paid" else invoice.status

        // Check stock availability if changing to paid
        if (status == "paid" && invoice.status != "paid") {
            validateAndDeductStock(uid, invoiceId, invoice)
        }

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "invoiceNumber" to invoice.invoiceNumber,
            "customerId" to invoice.customerId,
            "customerName" to invoice.customerName,
            "customerPhone" to invoice.customerPhone,
            "status" to status,
            "subtotal" to invoice.subtotal,
            "taxRate" to invoice.taxRate,
            "taxAmount" to invoice.taxAmount,
            "discount" to invoice.discount,
            "total" to invoice.total,
            "items" to invoice.items,
            "notes" to invoice.notes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (invoice.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            payload["issueDate"] = FieldValue.serverTimestamp()
        }

        doc.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        return invoiceId
    }

    suspend fun updateInvoiceStatus(invoice: Invoice, newStatus: String) {
        val uid = requireUserId()
        if (invoice.id.isBlank()) return

        if (newStatus == "paid" && invoice.status != "paid") {
            validateAndDeductStock(uid, invoice.id, invoice)
        }

        db.collection("users").document(uid).collection("invoices").document(invoice.id)
            .update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    private suspend fun validateAndDeductStock(uid: String, invoiceId: String, invoice: Invoice) {
        val items = invoice.parsedItems()
        val productsCol = db.collection("users").document(uid).collection("products")

        // First check stock availability
        for (item in items) {
            if (item.productId.isNotBlank()) {
                val doc = productsCol.document(item.productId).get().await()
                if (doc.exists()) {
                    val currentQty = doc.getDouble("quantity") ?: 0.0
                    if (currentQty < item.quantity) {
                        Log.w("ClinicRepo", "تنبيه: الكمية المتوفرة من ${item.productName} أقل من الكمية المطلوبة بالفاتورة")
                    }
                }
            }
        }

        // Deduct stock and record movements
        for (item in items) {
            if (item.productId.isNotBlank()) {
                val doc = productsCol.document(item.productId)
                val snapshot = doc.get().await()
                if (snapshot.exists()) {
                    val currentQty = snapshot.getDouble("quantity") ?: 0.0
                    val newQty = (currentQty - item.quantity).coerceAtLeast(0.0)
                    doc.update("quantity", newQty, "updatedAt", FieldValue.serverTimestamp()).await()

                    recordStockMovement(
                        productId = item.productId,
                        productName = item.productName,
                        type = "out",
                        quantity = item.quantity,
                        previousQuantity = currentQty,
                        newQuantity = newQty,
                        invoiceId = invoiceId,
                        notes = "فاتورة بيع رقم ${invoice.invoiceNumber}"
                    )
                }
            }
        }
    }

    suspend fun deleteInvoice(invoiceId: String) {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("invoices").document(invoiceId).delete().await()
    }

    // --- STOCK MOVEMENTS ---

    fun observeStockMovements(): Flow<List<StockMovement>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("stock_movements")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(StockMovement::class.java).sortedByDescending { it.createdAt }
                }
        )
    }

    suspend fun recordStockMovement(
        productId: String,
        productName: String,
        type: String,
        quantity: Double,
        previousQuantity: Double,
        newQuantity: Double,
        invoiceId: String = "",
        notes: String = ""
    ) {
        val uid = requireUserId()
        val doc = db.collection("users").document(uid).collection("stock_movements").document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "productId" to productId,
            "productName" to productName,
            "type" to type,
            "quantity" to quantity,
            "previousQuantity" to previousQuantity,
            "newQuantity" to newQuantity,
            "invoiceId" to invoiceId,
            "notes" to notes,
            "createdAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload).await()
    }

    // --- CLINIC SETTINGS ---

    fun observeSettings(): Flow<ClinicSettings> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("settings").document("default")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(ClinicSettings::class.java) ?: ClinicSettings(userId = uid)
                }
        )
    }

    suspend fun saveSettings(settings: ClinicSettings) {
        val uid = requireUserId()
        val doc = db.collection("users").document(uid).collection("settings").document("default")
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "clinicName" to settings.clinicName,
            "clinicPhone" to settings.clinicPhone,
            "clinicAddress" to settings.clinicAddress,
            "taxNumber" to settings.taxNumber,
            "currencySymbol" to settings.currencySymbol,
            "vatPercentage" to settings.vatPercentage,
            "invoiceHeader" to settings.invoiceHeader,
            "invoiceFooter" to settings.invoiceFooter,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    // Preload default sample clinic products if inventory is empty
    suspend fun checkAndSeedInitialData() {
        val uid = requireUserId()
        val existing = db.collection("users").document(uid).collection("products").limit(1).get().await()
        if (existing.isEmpty) {
            val defaultProducts = listOf(
                Product(
                    userId = uid,
                    name = "أوكسي تتراسيكلين 20% طويل المفعول",
                    barcode = "628100123001",
                    category = "مضادات حيوية",
                    costPrice = 3500.0,
                    sellPrice = 5000.0,
                    quantity = 25.0,
                    minQuantity = 5.0,
                    activeIngredient = "Oxytetracycline 200mg",
                    dosageInstructions = "1 مل لكل 10 كجم في العضل لمرة واحدة",
                    description = "مضاد حيوي واسع الطيف للماشية والأغنام والخيول"
                ),
                Product(
                    userId = uid,
                    name = "إيفرمكتين سوبر 1% حقن",
                    barcode = "628100123002",
                    category = "مضادات طفيليات",
                    costPrice = 4200.0,
                    sellPrice = 6000.0,
                    quantity = 18.0,
                    minQuantity = 4.0,
                    activeIngredient = "Ivermectin 10mg + Clorsulon",
                    dosageInstructions = "1 مل لكل 50 كجم تحت الجلد",
                    description = "علاج فعال للطفيليات الداخلية والخارجية والديدان الكبدية"
                ),
                Product(
                    userId = uid,
                    name = "بنسلين + ستربتومايسين فورت",
                    barcode = "628100123003",
                    category = "مضادات حيوية",
                    costPrice = 2800.0,
                    sellPrice = 4000.0,
                    quantity = 30.0,
                    minQuantity = 6.0,
                    activeIngredient = "Procaine Penicillin + Streptomycin",
                    dosageInstructions = "1 مل لكل 20 كجم في العضل لمدة 3 أيام",
                    description = "مضاد حيوي فعال لالتهابات الجهاز التنفسي والضرع"
                ),
                Product(
                    userId = uid,
                    name = "فيتامين هـ + سيلينيوم فائق",
                    barcode = "628100123004",
                    category = "فيتامينات ومقويات",
                    costPrice = 3000.0,
                    sellPrice = 4500.0,
                    quantity = 20.0,
                    minQuantity = 5.0,
                    activeIngredient = "Vitamin E + Sodium Selenite",
                    dosageInstructions = "عضل أو تحت الجلد 2-5 مل",
                    description = "مقوي للخصوبة والوقاية من مرض العضلات البيضاء في الحملان والعجول"
                ),
                Product(
                    userId = uid,
                    name = "بخاخ أزرق مطهر للجروح والعمليات",
                    barcode = "628100123005",
                    category = "مطهرات ومستلزمات",
                    costPrice = 1800.0,
                    sellPrice = 2800.0,
                    quantity = 40.0,
                    minQuantity = 10.0,
                    activeIngredient = "Oxytetracycline + Gentian Violet",
                    dosageInstructions = "رش موضعي على الجروح مرتين يومياً",
                    description = "مضاد للبكتيريا ومطهر سريع للجروح والتعفن والقروح"
                ),
                Product(
                    userId = uid,
                    name = "كالسيوم بوروجلوكونات 40%",
                    barcode = "628100123006",
                    category = "محاليل علاجية",
                    costPrice = 4000.0,
                    sellPrice = 6000.0,
                    quantity = 15.0,
                    minQuantity = 3.0,
                    activeIngredient = "Calcium Borogluconate 40%",
                    dosageInstructions = "حقن بطيء في الوريد أو تحت الجلد",
                    description = "علاج حمى الحليب ونقص الكالسيوم الحاد في الأبقار والأغنام"
                )
            )

            for (p in defaultProducts) {
                saveProduct(p)
            }

            val defaultCustomers = listOf(
                Customer(
                    userId = uid,
                    name = "مزرعة البركة للثروة الحيوانية",
                    phone = "771234567",
                    address = "صنعاء - بني حشيش",
                    notes = "مربي أبقار وأغنام معتمد"
                ),
                Customer(
                    userId = uid,
                    name = "الشيخ عبد الله القعافي",
                    phone = "777889900",
                    address = "الشارع الرئيسي",
                    notes = "عميل دائم"
                )
            )
            for (c in defaultCustomers) {
                saveCustomer(c)
            }

            saveSettings(ClinicSettings(userId = uid))
        }
    }
}
