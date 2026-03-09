package net.ivanvega.mitelefoniacompose

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.ivanvega.mitelefoniacompose.ui.theme.MiTelefoniaComposeTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Se requieren permisos para interceptar llamadas y enviar SMS.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.SEND_SMS
            )
        )

        setContent {
            MiTelefoniaComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AutoReplyScreen()
                }
            }
        }
    }
}

@Composable
fun SystemBroadcastReceiver(
    systemAction: String,
    onSystemEvent: (intent: Intent?) -> Unit
) {
    val context = LocalContext.current
    val currentOnSystemEvent by rememberUpdatedState(onSystemEvent)
    DisposableEffect(context, systemAction) {
        val intentFilter = IntentFilter(systemAction)
        val broadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                currentOnSystemEvent(intent)
            }
        }
        context.registerReceiver(broadcast, intentFilter)
        onDispose {
            context.unregisterReceiver(broadcast)
        }
    }
}

@Composable
fun AutoReplyScreen() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("AutoReplyPrefs", Context.MODE_PRIVATE) }

    var targetNumber by remember { mutableStateOf(sharedPreferences.getString("TARGET_NUMBER", "") ?: "") }
    var replyMessage by remember { mutableStateOf(sharedPreferences.getString("REPLY_MESSAGE", "") ?: "") }

    // Registro a nivel de contexto del receptor de llamadas
    SystemBroadcastReceiver(TelephonyManager.ACTION_PHONE_STATE_CHANGED) { intent ->
        val state = intent?.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            val incomingNumber = intent?.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            val savedNumber = sharedPreferences.getString("TARGET_NUMBER", "")
            val savedMessage = sharedPreferences.getString("REPLY_MESSAGE", "")

            if (!incomingNumber.isNullOrEmpty() && incomingNumber == savedNumber && !savedMessage.isNullOrEmpty()) {
                try {
                    val smsManager = SmsManager.getDefault()
                    smsManager.sendTextMessage(incomingNumber, null, savedMessage, null, null)
                    Toast.makeText(context, "Respuesta enviada a $incomingNumber", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("AutoReply", "Error enviando SMS", e)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Configuración de Auto-Respuesta", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = targetNumber,
            onValueChange = { targetNumber = it },
            label = { Text("Número a responder") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = replyMessage,
            onValueChange = { replyMessage = it },
            label = { Text("Mensaje automático") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                sharedPreferences.edit()
                    .putString("TARGET_NUMBER", targetNumber.trim())
                    .putString("REPLY_MESSAGE", replyMessage.trim())
                    .apply()
                Toast.makeText(context, "Guardado correctamente", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Configuración")
        }
    }
}
