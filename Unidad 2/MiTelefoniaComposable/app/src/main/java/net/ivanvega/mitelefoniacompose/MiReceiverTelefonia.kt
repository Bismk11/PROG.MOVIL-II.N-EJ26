package net.ivanvega.mitelefoniacompose

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log

class MiReceiverTelefonia : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d("AutoReply", "Llamada entrante detectada desde: $incomingNumber")

                val sharedPreferences = context.getSharedPreferences("AutoReplyPrefs", Context.MODE_PRIVATE)
                val savedNumber = sharedPreferences.getString("TARGET_NUMBER", "")
                val savedMessage = sharedPreferences.getString("REPLY_MESSAGE", "")

                if (!incomingNumber.isNullOrEmpty() && incomingNumber == savedNumber && !savedMessage.isNullOrEmpty()) {
                    try {
                        val smsManager: SmsManager = context.getSystemService(SmsManager::class.java)
                        smsManager.sendTextMessage(incomingNumber, null, savedMessage, null, null)
                        Log.d("AutoReply", "Respuesta automática enviada correctamente a $incomingNumber")
                    } catch (e: Exception) {
                        Log.e("AutoReply", "Error al enviar el SMS automático", e)
                    }
                }
            }
        }
    }
}
