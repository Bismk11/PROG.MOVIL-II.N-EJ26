package net.ivanvega.mitelefoniacompose

import android.telephony.SmsManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ScreenViewModel: ViewModel() {

    var phoneNumber by mutableStateOf("")
    var message by mutableStateOf("")

    fun onPhoneNumberChange(newValue: String) {
        phoneNumber = newValue
    }

    fun onMessageChange(newValue: String) {
        message = newValue
    }

    fun sendSMS(){
        if (phoneNumber.isNotBlank() && message.isNotBlank()) {
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
