package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val barcode: String = "",
    val category: String = "أدوية بيطرية",
    val costPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val quantity: Double = 0.0,
    val minQuantity: Double = 5.0,
    val imageUrl: String = "",
    val description: String = "",
    val activeIngredient: String = "",
    val dosageInstructions: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val taxNumber: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InvoiceItemData(
    val productId: String = "",
    val productName: String = "",
    val barcode: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val total: Double = 0.0
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "productId" to productId,
            "productName" to productName,
            "barcode" to barcode,
            "quantity" to quantity,
            "unitPrice" to unitPrice,
            "costPrice" to costPrice,
            "total" to total
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): InvoiceItemData {
            return InvoiceItemData(
                productId = map["productId"] as? String ?: "",
                productName = map["productName"] as? String ?: "",
                barcode = map["barcode"] as? String ?: "",
                quantity = (map["quantity"] as? Number)?.toDouble() ?: 1.0,
                unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                costPrice = (map["costPrice"] as? Number)?.toDouble() ?: 0.0,
                total = (map["total"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

@IgnoreExtraProperties
data class Invoice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val status: String = "draft", // "draft", "paid", "cancelled"
    val subtotal: Double = 0.0,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val items: List<Map<String, Any>> = emptyList(),
    val notes: String = "",
    @ServerTimestamp val issueDate: Timestamp? = null,
    @ServerTimestamp val dueDate: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedItems(): List<InvoiceItemData> {
        return items.map { InvoiceItemData.fromMap(it) }
    }
}

@IgnoreExtraProperties
data class StockMovement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val type: String = "in", // "in", "out", "adjustment"
    val quantity: Double = 0.0,
    val previousQuantity: Double = 0.0,
    val newQuantity: Double = 0.0,
    val invoiceId: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ClinicSettings(
    @DocumentId val id: String = "default",
    val userId: String = "",
    val clinicName: String = "عيادة القعافي البيطرية",
    val clinicPhone: String = "+967 777 123 456",
    val clinicAddress: String = "صنعاء، اليمن - الشارع العام",
    val taxNumber: String = "300123456700003",
    val currencySymbol: String = "ر.ي",
    val vatPercentage: Double = 15.0,
    val invoiceHeader: String = "عيادة القعافي للطب والجراحة البيطرية",
    val invoiceFooter: String = "شكراً لثقتكم بعيادة القعافي البيطرية - نتمنى لحيواناتكم دوام الصحة والعافية",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
