package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClinicSettings
import com.example.ui.components.GlassCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderGlass
import com.example.ui.theme.BorderGlassWhite
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenDim
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.NeonVioletDim
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.viewmodel.AiAssistantViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ClinicViewModel
import com.example.ui.viewmodel.MessageSender

@Composable
fun AiAndSettingsScreen(
    aiViewModel: AiAssistantViewModel,
    clinicViewModel: ClinicViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = NeonGreen,
            divider = { HorizontalDivider(color = BorderGlassWhite) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = if (selectedTab == 0) NeonViolet else Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مساعد Gemini الذكي", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = if (selectedTab == 1) NeonGreen else Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعدادات المنشأة والضريبة", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
        }

        if (selectedTab == 0) {
            AiChatSection(aiViewModel = aiViewModel)
        } else {
            ClinicSettingsSection(
                clinicViewModel = clinicViewModel,
                authViewModel = authViewModel
            )
        }
    }
}

@Composable
fun AiChatSection(
    aiViewModel: AiAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val messages by aiViewModel.chatMessages.collectAsState()
    val isLoading by aiViewModel.isChatLoading.collectAsState()
    var inputMessage by remember { mutableStateOf("") }

    val quickPrompts = listOf(
        "جرعة أوكسي تتراسيكلين لعجل وزنه 100 كجم",
        "فترة سحب الإيفرمكتين في اللحوم والحليب",
        "علاج حمى الحليب ونقص الكالسيوم في الأبقار",
        "أهم المضادات الحيوية واسعة الطيف للأغنام",
        "كيفية تنظيم المخزون وتقليل هدر الأدوية"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Chat Header
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = BorderGlass
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NeonVioletDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "مساعد عيادة القعافي البيطرية الذكي",
                        fontWeight = FontWeight.Bold,
                        color = NeonViolet,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "مدعوم بـ Gemini 3.8 Flash لحساب الجرعات والاستشارات البيطرية",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Suggestions
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(quickPrompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderGlassWhite),
                    modifier = Modifier.clickable {
                        aiViewModel.sendMessage(prompt)
                    }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat Messages Thread
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == MessageSender.USER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = if (isUser) 14.dp else 2.dp,
                                    bottomEnd = if (isUser) 2.dp else 14.dp
                                )
                            )
                            .background(if (isUser) NeonGreenDim else SurfaceElevated)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isUser) NeonGreen.copy(alpha = 0.5f) else NeonViolet.copy(alpha = 0.3f)
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isUser) "أنت" else "مساعد عيادة القعافي 🩺",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isUser) NeonGreen else NeonViolet
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (msg.text.isBlank() && msg.isStreaming) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = NeonViolet, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("جاري التفكير وصياغة الإجابة...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Text(
                                    text = msg.text,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                placeholder = { Text("اسأل عن دواء، جرعة لحيوان، أو استشارة...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonViolet,
                    unfocusedBorderColor = BorderGlassWhite,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputMessage.isNotBlank()) {
                        val text = inputMessage
                        inputMessage = ""
                        aiViewModel.sendMessage(text)
                    }
                },
                enabled = inputMessage.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputMessage.isNotBlank()) NeonViolet else SurfaceElevated)
                    .testTag("ai_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "إرسال",
                    tint = if (inputMessage.isNotBlank()) Color.White else Color.Gray
                )
            }
        }
    }
}

@Composable
fun ClinicSettingsSection(
    clinicViewModel: ClinicViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val currentSettings by clinicViewModel.settings.collectAsState()
    val context = LocalContext.current

    var clinicName by remember(currentSettings) { mutableStateOf(currentSettings.clinicName) }
    var clinicPhone by remember(currentSettings) { mutableStateOf(currentSettings.clinicPhone) }
    var clinicAddress by remember(currentSettings) { mutableStateOf(currentSettings.clinicAddress) }
    var taxNumber by remember(currentSettings) { mutableStateOf(currentSettings.taxNumber) }
    var currencySymbol by remember(currentSettings) { mutableStateOf(currentSettings.currencySymbol) }
    var vatPercentageText by remember(currentSettings) { mutableStateOf(currentSettings.vatPercentage.toString()) }
    var invoiceHeader by remember(currentSettings) { mutableStateOf(currentSettings.invoiceHeader) }
    var invoiceFooter by remember(currentSettings) { mutableStateOf(currentSettings.invoiceFooter) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Clinic Info Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = BorderGlass
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("معلومات المنشأة والعيادة", fontWeight = FontWeight.Bold, color = NeonGreen)
                }

                OutlinedTextField(
                    value = clinicName,
                    onValueChange = { clinicName = it },
                    label = { Text("اسم العيادة / المنشأة") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("clinic_name_input")
                )

                OutlinedTextField(
                    value = clinicPhone,
                    onValueChange = { clinicPhone = it },
                    label = { Text("رقم الهاتف للتواصل والفواتير") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = clinicAddress,
                    onValueChange = { clinicAddress = it },
                    label = { Text("عنوان العيادة") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = taxNumber,
                    onValueChange = { taxNumber = it },
                    label = { Text("الرقم الضريبي للمنشأة") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Currency and Tax Settings Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = BorderGlassWhite
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("إعدادات العملة والضريبة المضافة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                // Quick Currency Selector
                Text("رمز العملة المعتمد:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val currencies = listOf("ر.ي", "ر.س", "$", "د.إ", "ج.م")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    currencies.forEach { cur ->
                        val isSelected = currencySymbol == cur
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) NeonGreen else SurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) NeonGreen else BorderGlassWhite),
                            modifier = Modifier.clickable { currencySymbol = cur }
                        ) {
                            Text(
                                text = cur,
                                color = if (isSelected) Color(0xFF003822) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = vatPercentageText,
                    onValueChange = { vatPercentageText = it },
                    label = { Text("نسبة ضريبة القيمة المضافة (%)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("vat_percentage_input")
                )

                OutlinedTextField(
                    value = invoiceHeader,
                    onValueChange = { invoiceHeader = it },
                    label = { Text("ترويسة الفاتورة المطبوعة") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = invoiceFooter,
                    onValueChange = { invoiceFooter = it },
                    label = { Text("تذييل الفاتورة المطبوعة") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Save Button
        Button(
            onClick = {
                val vat = vatPercentageText.toDoubleOrNull() ?: 15.0
                clinicViewModel.saveSettings(
                    currentSettings.copy(
                        clinicName = clinicName.trim(),
                        clinicPhone = clinicPhone.trim(),
                        clinicAddress = clinicAddress.trim(),
                        taxNumber = taxNumber.trim(),
                        currencySymbol = currencySymbol.trim(),
                        vatPercentage = vat,
                        invoiceHeader = invoiceHeader.trim(),
                        invoiceFooter = invoiceFooter.trim()
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_settings_button")
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color(0xFF003822))
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ جميع الإعدادات", color = Color(0xFF003822), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sign Out Button
        OutlinedButton(
            onClick = { authViewModel.signOut(context) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sign_out_button")
        ) {
            Icon(imageVector = Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تسجيل الخروج من الحساب", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
