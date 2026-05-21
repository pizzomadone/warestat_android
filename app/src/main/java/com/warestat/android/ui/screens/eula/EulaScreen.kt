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
import com.warestat.android.ui.theme.Primary

@Composable
fun EulaScreen(onAccept: () -> Unit) {
    var accepted by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val reachedBottom by remember { derivedStateOf { scrollState.value >= scrollState.maxValue - 50 } }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Contratto di Licenza Utente Finale", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Primary)
            Text("WareStat Android — v1.0", fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.weight(1f), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EulaSection("1. Accettazione delle condizioni",
                        "Utilizzando questa applicazione si accettano i termini e le condizioni di questo Contratto di Licenza Utente Finale (EULA). Se non si accettano questi termini, non è possibile utilizzare l'applicazione.")
                    EulaSection("2. Concessione di licenza",
                        "L'applicazione è concessa in licenza, non venduta. È concessa una licenza limitata, non esclusiva e non trasferibile per utilizzare l'applicazione su dispositivi di proprietà dell'utente.")
                    EulaSection("3. Restrizioni",
                        "È vietato: copiare, modificare o distribuire l'applicazione; decompilare o tentare di estrarre il codice sorgente; utilizzare l'applicazione per scopi illegali; rimuovere o alterare avvisi di copyright.")
                    EulaSection("4. Dati e privacy",
                        "Tutti i dati vengono memorizzati localmente sul dispositivo. L'applicazione non invia dati a server esterni. L'utente è responsabile della sicurezza e del backup dei propri dati.")
                    EulaSection("5. Garanzia e responsabilità",
                        "L'applicazione è fornita 'così com'è' senza garanzie di alcun tipo. In nessun caso l'autore sarà responsabile per danni diretti, indiretti o consequenziali derivanti dall'uso dell'applicazione.")
                    EulaSection("6. Backup e perdita di dati",
                        "Si raccomanda vivamente di eseguire backup regolari dei dati. L'autore non è responsabile per la perdita di dati causata da guasti hardware, malfunzionamenti software o uso improprio.")
                    EulaSection("7. Aggiornamenti",
                        "L'autore si riserva il diritto di aggiornare l'applicazione e questo contratto in qualsiasi momento. L'uso continuato dell'applicazione dopo gli aggiornamenti costituisce accettazione delle modifiche.")
                    EulaSection("8. Risoluzione",
                        "La licenza è effettiva fino alla risoluzione. Si risolve automaticamente in caso di violazione di qualsiasi termine. Alla risoluzione è necessario cessare l'uso e disinstallare l'applicazione.")
                    EulaSection("9. Legge applicabile",
                        "Questo contratto è regolato dalla legge italiana. Eventuali controversie saranno risolte dai tribunali italiani competenti.")

                    if (!reachedBottom) {
                        Text("↓ Scorri fino in fondo per abilitare l'accettazione", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = accepted, onCheckedChange = { if (reachedBottom) accepted = it }, enabled = reachedBottom)
                Spacer(Modifier.width(8.dp))
                Text("Ho letto e accetto i termini del contratto di licenza", fontSize = 13.sp, color = if (reachedBottom) Color.Unspecified else Color.Gray)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = accepted
            ) {
                Text("Accetta e continua", fontSize = 16.sp)
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
