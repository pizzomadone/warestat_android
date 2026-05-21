package com.warestat.android.ui.screens.eula

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warestat.android.i18n.LocalStrings
import com.warestat.android.ui.theme.Primary

@Composable
fun EulaScreen(onAccept: () -> Unit) {
    val strings = LocalStrings.current
    var accepted by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val reachedBottom by remember { derivedStateOf { scrollState.value >= scrollState.maxValue - 50 } }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text(strings.eulaTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Primary)
            Text(strings.eulaVersion, fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.weight(1f), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EulaSection(strings.eulaSec1Title, strings.eulaSec1Body)
                    EulaSection(strings.eulaSec2Title, strings.eulaSec2Body)
                    EulaSection(strings.eulaSec3Title, strings.eulaSec3Body)
                    EulaSection(strings.eulaSec4Title, strings.eulaSec4Body)
                    EulaSection(strings.eulaSec5Title, strings.eulaSec5Body)
                    EulaSection(strings.eulaSec6Title, strings.eulaSec6Body)
                    EulaSection(strings.eulaSec7Title, strings.eulaSec7Body)
                    EulaSection(strings.eulaSec8Title, strings.eulaSec8Body)
                    EulaSection(strings.eulaSec9Title, strings.eulaSec9Body)

                    if (!reachedBottom) {
                        Text(strings.eulaScrollHint, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = accepted, onCheckedChange = { if (reachedBottom) accepted = it }, enabled = reachedBottom)
                Spacer(Modifier.width(8.dp))
                Text(strings.eulaCheckboxLabel, fontSize = 13.sp, color = if (reachedBottom) Color.Unspecified else Color.Gray)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = accepted
            ) {
                Text(strings.eulaAcceptBtn, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun EulaSection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Primary)
        Text(content, fontSize = 13.sp, color = Color.DarkGray)
    }
}
