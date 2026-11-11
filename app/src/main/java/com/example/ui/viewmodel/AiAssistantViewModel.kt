package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SmartProductSuggestion
import com.example.data.repository.VeterinaryAiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "AiAssistantViewModel"

data class ChatMessage(
    val sender: MessageSender,
    val text: String,
    val isStreaming: Boolean = false
)

enum class MessageSender {
    USER, AI
}

sealed interface SmartCaptureUiState {
    data object Idle : SmartCaptureUiState
    data object Loading : SmartCaptureUiState
    data class Success(val suggestion: SmartProductSuggestion) : SmartCaptureUiState
    data class Error(val message: String) : SmartCaptureUiState
}

class AiAssistantViewModel : ViewModel() {

    private val repository = VeterinaryAiRepository()

    private val _smartCaptureState = MutableStateFlow<SmartCaptureUiState>(SmartCaptureUiState.Idle)
    val smartCaptureState: StateFlow<SmartCaptureUiState> = _smartCaptureState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "مرحباً بك في عيادة القعافي البيطرية! 🩺 أنا مساعدك الذكي المتخصص في الطب والصيدلة البيطرية وإدارة العيادة. كيف أساعدك اليوم؟"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun analyzeProduct(bitmap: Bitmap?, barcodeOrText: String) {
        viewModelScope.launch {
            _smartCaptureState.value = SmartCaptureUiState.Loading
            try {
                val suggestion = repository.analyzeProduct(bitmap, barcodeOrText)
                _smartCaptureState.value = SmartCaptureUiState.Success(suggestion)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Smart capture timed out", e)
                _smartCaptureState.value = SmartCaptureUiState.Error("استغرق التحليل وقتاً أطول من المتوقع، يرجى إعادة المحاولة.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Smart capture failed", e)
                _smartCaptureState.value = SmartCaptureUiState.Error(
                    e.localizedMessage ?: "فشل تحليل المنتج بواسطة الذكاء الاصطناعي."
                )
            }
        }
    }

    fun resetSmartCapture() {
        _smartCaptureState.value = SmartCaptureUiState.Idle
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isChatLoading.value) return

        val userMessage = ChatMessage(sender = MessageSender.USER, text = userText.trim())
        _chatMessages.value = _chatMessages.value + userMessage
        _isChatLoading.value = true

        val aiPlaceholder = ChatMessage(sender = MessageSender.AI, text = "", isStreaming = true)
        _chatMessages.value = _chatMessages.value + aiPlaceholder

        viewModelScope.launch {
            val responseBuffer = StringBuilder()
            try {
                repository.sendChatMessage(userText).collect { chunk ->
                    responseBuffer.append(chunk)
                    val currentList = _chatMessages.value.toMutableList()
                    if (currentList.isNotEmpty() && currentList.last().isStreaming) {
                        currentList[currentList.lastIndex] = ChatMessage(
                            sender = MessageSender.AI,
                            text = responseBuffer.toString(),
                            isStreaming = true
                        )
                        _chatMessages.value = currentList
                    }
                }

                // Finalize streaming
                val currentList = _chatMessages.value.toMutableList()
                if (currentList.isNotEmpty()) {
                    currentList[currentList.lastIndex] = ChatMessage(
                        sender = MessageSender.AI,
                        text = responseBuffer.toString().ifBlank { "تمت معالجة الطلب." },
                        isStreaming = false
                    )
                    _chatMessages.value = currentList
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat response timed out", e)
                val currentList = _chatMessages.value.toMutableList()
                if (currentList.isNotEmpty()) {
                    currentList[currentList.lastIndex] = ChatMessage(
                        sender = MessageSender.AI,
                        text = "عذراً، استغرق الرد وقتاً طويلاً. يرجى إعادة إرسال السؤال.",
                        isStreaming = false
                    )
                    _chatMessages.value = currentList
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat streaming failed", e)
                val currentList = _chatMessages.value.toMutableList()
                if (currentList.isNotEmpty()) {
                    currentList[currentList.lastIndex] = ChatMessage(
                        sender = MessageSender.AI,
                        text = "تعذر الحصول على إجابة: ${e.localizedMessage}",
                        isStreaming = false
                    )
                    _chatMessages.value = currentList
                }
            } finally {
                _isChatLoading.value = false
            }
        }
    }
}
