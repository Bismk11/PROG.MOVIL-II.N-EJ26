package net.ivanvega.mitelefoniacompose

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Verificamos que la acción sea la de cambio de estado del teléfono
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

            // Si el teléfono está sonando
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                // Obtenemos el número entrante
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                if (incomingNumber != null) {
                    Log.d("PhoneStateReceiver", "Llamada entrante de: $incomingNumber")

                    // Leer los datos guardados en SharedPreferences
                    val sharedPreferences = context.getSharedPreferences("AutoReplyPrefs", Context.MODE_PRIVATE)
                    val targetNumber = sharedPreferences.getString("TARGET_NUMBER", "") ?: ""
                    val replyMessage = sharedPreferences.getString("REPLY_MESSAGE", "") ?: ""

                    // Si el número coincide y hay un mensaje configurado
                    // Nota: Se usa 'contains' o 'endsWith' para evitar problemas con prefijos de país (ej. +52)
                    if (targetNumber.isNotEmpty() && incomingNumber.endsWith(targetNumber) && replyMessage.isNotEmpty()) {
                        Log.d("PhoneStateReceiver", "¡Coincidencia! Enviando SMS...")
                        sendSms(targetNumber, replyMessage)
                    }
                }
            }
        }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            // Se utiliza SmsManager para enviar el mensaje de forma transparente
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("PhoneStateReceiver", "SMS enviado exitosamente a $phoneNumber")
        } catch (e: Exception) {
            Log.e("PhoneStateReceiver", "Error al enviar SMS: ${e.message}")
        }
    }
}