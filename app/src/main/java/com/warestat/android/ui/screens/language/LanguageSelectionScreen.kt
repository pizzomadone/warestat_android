package com.warestat.android.ui.screens.language

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warestat.android.i18n.EnglishStrings
import com.warestat.android.ui.theme.Primary

data class LanguageOption(val code: String, val flag: String, val nativeName: String, val englishName: String)

val languageOptions = listOf(
    LanguageOption("EN", "🇬🇧", "English", "English"),
    LanguageOption("ES", "🇪🇸", "Español", "Spanish"),
    LanguageOption("FR", "🇫🇷", "Français", "French"),
    LanguageOption("DE", "🇩🇪", "Deutsch", "German"),
    LanguageOption("IT", "🇮🇹", "Italiano", "Italian"),
)

@Composable
fun LanguageSelectionScreen(onLanguageSelected: (String) -> Unit) {
    val strings = EnglishStrings
    var selected by remember { mutableStateOf("EN") }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("WareStat", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Primary)
            Spacer(Modifier.height(8.dp))
            Text(strings.selectLanguage, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(strings.chooseLanguage, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(Modifier.height(32.dp))

            languageOptions.forEach { lang ->
                val isSelected = selected == lang.code
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Primary else Color.LightGray),
                    colors = CardDefaults.outlinedCardColors(containerColor = if (isSelected) Primary.copy(alpha = 0.08f) else Color.White),
                    onClick = { selected = lang.code }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(lang.flag, fontSize = 28.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(lang.nativeName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = if (isSelected) Primary else Color.Black)
                            if (lang.nativeName != lang.englishName) {
                                Text(lang.englishName, fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        if (isSelected) {
                            RadioButton(selected = true, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Primary))
                        } else {
                            RadioButton(selected = false, onClick = null)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onLanguageSelected(selected) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.continueBtn, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
