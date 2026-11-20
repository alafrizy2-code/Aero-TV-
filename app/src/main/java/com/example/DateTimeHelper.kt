package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeHelper {

    private val idLocale = Locale("id", "ID")
    // Format tanggal DD-MM-YYYY (contoh: 31-10-2026)
    private val dateFormatter = SimpleDateFormat("dd-MM-yyyy", idLocale)

    /**
     * Mendapatkan tanggal sistem saat ini dalam format DD-MM-YYYY (contoh: "31-10-2026").
     */
    fun getFormattedDate(): String {
        return dateFormatter.format(Date())
    }

    /**
     * Memperbarui TextView tanggal dengan tanggal sistem saat ini.
     */
    fun updateDateView(textView: TextView) {
        textView.text = getFormattedDate()
    }

    /**
     * Mendaftarkan BroadcastReceiver untuk memantau perubahan tanggal / waktu sistem secara otomatis.
     */
    fun registerTimeTickReceiver(context: Context, onTick: () -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                onTick()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        return receiver
    }
}
