package com.warestat.android.ui.screens.scanner

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.warestat.android.util.BarcodeScannerUtil

@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember { BarcodeScannerUtil(context) }
    var lastScanned by remember { mutableStateOf("") }
    var flashEnabled by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { scanner.stopScanning() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    scanner.startScanning(lifecycleOwner, previewView) { barcode ->
                        if (barcode != lastScanned) {
                            lastScanned = barcode
                            onBarcodeScanned(barcode)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay UI
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Scansiona codice", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row {
                    IconButton(onClick = { flashEnabled = !flashEnabled }) {
                        Icon(Icons.Default.FlashOn, "Flash", tint = if (flashEnabled) Color.Yellow else Color.White)
                    }
                    IconButton(onClick = { scanner.stopScanning(); onDismiss() }) {
                        Icon(Icons.Default.Close, "Chiudi", tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Scan frame
            Box(
                modifier = Modifier.size(250.dp).background(Color.Transparent)
            ) {
                // Corner decorations
                val cornerSize = 30.dp
                val cornerThickness = 3.dp
                val cornerColor = Color.White

                // TL corner
                Box(Modifier.size(cornerSize).align(Alignment.TopStart).background(cornerColor, RoundedCornerShape(topStart = 4.dp)).let { it })
                Box(Modifier.width(cornerThickness).height(cornerSize).align(Alignment.TopStart).background(cornerColor))
                Box(Modifier.height(cornerThickness).width(cornerSize).align(Alignment.TopStart).background(cornerColor))

                // TR corner
                Box(Modifier.width(cornerThickness).height(cornerSize).align(Alignment.TopEnd).background(cornerColor))
                Box(Modifier.height(cornerThickness).width(cornerSize).align(Alignment.TopEnd).background(cornerColor))

                // BL corner
                Box(Modifier.width(cornerThickness).height(cornerSize).align(Alignment.BottomStart).background(cornerColor))
                Box(Modifier.height(cornerThickness).width(cornerSize).align(Alignment.BottomStart).background(cornerColor))

                // BR corner
                Box(Modifier.width(cornerThickness).height(cornerSize).align(Alignment.BottomEnd).background(cornerColor))
                Box(Modifier.height(cornerThickness).width(cornerSize).align(Alignment.BottomEnd).background(cornerColor))
            }

            Spacer(Modifier.weight(1f))

            // Bottom hint
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Inquadra il codice a barre o QR code", color = Color.White, fontSize = 14.sp)
                    if (lastScanned.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Scansionato: $lastScanned", color = Color.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Semi-transparent overlay outside scan area
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp).align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp).align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
        )
    }
}
