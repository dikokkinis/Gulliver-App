package com.example.gulliver

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.File
import android.media.SoundPool // Import SoundPool
import android.media.AudioAttributes // Import AudioAttributes for SoundPool.Builder

class AddCityTicketActivity : AppCompatActivity() {
    private lateinit var pdfUri: Uri
    private var pdfPath: String = ""
    private val PICK_PDF = 1001

    // SoundPool instance and sound IDs
    private var soundPool: SoundPool? = null
    private var enterButtonClickSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_cityticket)

        // Initialize SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // For UI sounds
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2) // Allows two sounds to play concurrently
            .setAudioAttributes(audioAttributes)
            .build()

        enterButtonClickSoundId = soundPool?.load(this, R.raw.fileupload, 1) ?: 0


        val cityName = intent.getStringExtra("cityName")
        val isCityTicket = cityName != null
        val prefsName = if (isCityTicket) "CityTicketsPrefs" else "TicketsPrefs"
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)

        val addFileBtn = findViewById<ImageButton>(R.id.uploadPdfBtn)
        val enterBtn = findViewById<ImageButton>(R.id.submitTicketBtn)

        addFileBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf"
            startActivityForResult(intent, PICK_PDF)
        }

        enterBtn.setOnClickListener {
            playEnterSound()
            val activity = findViewById<EditText>(R.id.editActivity).text.toString()
            val date = findViewById<EditText>(R.id.editDate).text.toString()
            val arrival = findViewById<EditText>(R.id.editArrival).text.toString()

            if (pdfPath.isNotEmpty()) {
                val ticket = CityTicket(activity, date, arrival, pdfPath)
                val key = if (isCityTicket) "${cityName}_tickets" else "tickets"

                val existing = prefs.getString(key, null)
                val ticketList = if (existing != null) {
                    Gson().fromJson(existing, Array<CityTicket>::class.java).toMutableList()
                } else mutableListOf()

                ticketList.add(ticket)
                prefs.edit().putString(key, Gson().toJson(ticketList)).apply()

                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }

    private fun playEnterSound() {
        // Play the arrow button sound
        soundPool?.play(enterButtonClickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PDF && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                pdfUri = uri
                val file = File(cacheDir, "ticket_${System.currentTimeMillis()}.pdf")
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                pdfPath = file.absolutePath
            }
        }
    }
}
