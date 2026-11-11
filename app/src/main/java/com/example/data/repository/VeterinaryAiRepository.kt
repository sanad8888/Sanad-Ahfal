package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.Part
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "VetAiRepo"

data class SmartProductSuggestion(
    val name: String,
    val category: String,
    val activeIngredient: String,
    val dosageInstructions: String,
    val suggestedCostPrice: Double,
    val suggestedSellPrice: Double,
    val description: String
)

class VeterinaryAiRepository {

    private val smartScanModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.2f
            },
            systemInstruction = Content(
                role = "system",
                parts = listOf(
                    TextPart(
                        """
                        أنت خبير صيدلاني بيطري لعيادة القعافي البيطرية.
                        وظيفتك تحليل أدوية ومستلزمات بيطرية وإرجاع تفاصيل الصنف بدقة كـ JSON بالحقول التالية فقط:
                        {
                          "name": "اسم الدواء أو المنتج البيطري بالعربية",
                          "category": "مضادات حيوية أو فيتامينات أو لقاحات...",
                          "activeIngredient": "المادة الفعالة والتركيز",
                          "dosageInstructions": "الجرعة الموصى بها وطريقة الإعطاء",
                          "suggestedCostPrice": 2500.0,
                          "suggestedSellPrice": 3500.0,
                          "description": "دواعي الاستعمال وفترة التحريم إن وجدت"
                        }
                        """.trimIndent()
                    )
                )
            )
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = Content(
                role = "system",
                parts = listOf(
                    TextPart(
                        """
                        أنت "المساعد الذكي لعيادة القعافي البيطرية" (Al-Qaafi Veterinary Clinic Smart Assistant).
                        مهامك:
                        1. تقديم المشورة البيطرية السريعة للأطباء ومربي الحيوانات (الأبقار، الأغنام، الماعز، الخيول، الإبل، الدواجن، والحيوانات الأليفة).
                        2. حساب جرعات الأدوية بدقة بناءً على وزن الحيوان والمادة الفعالة.
                        3. التنبيه إلى فترات سحب الدواء (Withdrawal Period) من الحليب واللحوم للحفاظ على صحة المستهلك.
                        4. مساعدة إدارة العيادة في تنظيم المخزون وحساب الأرباح واقتراح بدائل دوائية عند نقص صنف معين.
                        كن لطيفاً، علمياً، مباشراً واستخدم اللغة العربية الفصحى الواضحة مع لمسة ترحيبية باسم "عيادة القعافي البيطرية".
                        """.trimIndent()
                    )
                )
            )
        )
    }

    private var activeChat: Chat? = null

    suspend fun analyzeProduct(imageBitmap: Bitmap?, barcodeOrText: String): SmartProductSuggestion {
        return withTimeout(60_000L) {
            try {
                val promptText = if (barcodeOrText.isNotBlank()) {
                    "حلل هذا المستحضر البيطري: '$barcodeOrText'. استخرج اسمه التجاري، تصنيفه، مادته الفعالة، الجرعة، دواعي الاستعمال والأسعار التقديرية بالريال بتنسيق JSON."
                } else {
                    "حلل صورة علبة الدواء أو المستحضر البيطري المرفقة واستخرج بيانات الصنف بدقة بتنسيق JSON."
                }

                val parts = mutableListOf<Part>()
                if (imageBitmap != null) {
                    parts.add(ImagePart(imageBitmap))
                }
                parts.add(TextPart(promptText))

                val requestContent = Content(role = "user", parts = parts)
                val response: GenerateContentResponse = smartScanModel.generateContent(requestContent)

                val jsonText = response.text ?: throw IllegalStateException("لم يتم استلام رد من النموذج")
                val json = JSONObject(jsonText)

                SmartProductSuggestion(
                    name = json.optString("name", barcodeOrText.ifBlank { "مستحضر بيطري جديد" }),
                    category = json.optString("category", "أدوية بيطرية"),
                    activeIngredient = json.optString("activeIngredient", ""),
                    dosageInstructions = json.optString("dosageInstructions", ""),
                    suggestedCostPrice = json.optDouble("suggestedCostPrice", 2500.0),
                    suggestedSellPrice = json.optDouble("suggestedSellPrice", 3500.0),
                    description = json.optString("description", "")
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "فشل التحليل الذكي للمنتج", e)
                throw e
            }
        }
    }

    fun getOrStartChat(): Chat {
        if (activeChat == null) {
            val userGreeting = Content(role = "user", parts = listOf(TextPart("مرحباً")))
            val modelGreeting = Content(
                role = "model",
                parts = listOf(
                    TextPart("أهلاً بك في عيادة القعافي البيطرية! أنا مساعدك الذكي، كيف يمكنني مساعدتك اليوم في إدارة العيادة، حساب الجرعات، أو الاستشارات البيطرية؟")
                )
            )

            activeChat = chatModel.startChat(
                history = listOf(userGreeting, modelGreeting)
            )
        }
        return activeChat!!
    }

    fun sendChatMessage(userMessage: String): Flow<String> = flow {
        val chat = getOrStartChat()
        withTimeout(60_000L) {
            chat.sendMessageStream(userMessage).collect { chunk ->
                val chunkText = chunk.text
                if (!chunkText.isNullOrEmpty()) {
                    emit(chunkText)
                }
            }
        }
    }
}
