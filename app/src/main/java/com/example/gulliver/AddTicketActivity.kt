package com.example.gulliver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import com.google.gson.Gson
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import android.media.SoundPool // Import SoundPool
import android.media.AudioAttributes // Import AudioAttributes for SoundPool.Builder

class AddTicketActivity : AppCompatActivity() {
    private lateinit var pdfUri: Uri
    private var pdfPath: String = ""
    private val PICK_PDF = 1001
    private val PREFS_NAME = "TicketsPrefs"


    private var soundPool: SoundPool? = null
    private var enterButtonClickSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_ticket)


        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        enterButtonClickSoundId = soundPool?.load(this, R.raw.fileupload, 1) ?: 0

        val addFileBtn = findViewById<ImageButton>(R.id.uploadPdfBtn)
        val enterBtn = findViewById<ImageButton>(R.id.submitTicketBtn)

        addFileBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf"
            startActivityForResult(intent, PICK_PDF)
        }

        enterBtn.setOnClickListener {
            playEnterSound()
            val from = findViewById<EditText>(R.id.editFrom).text.toString()
            val to = findViewById<EditText>(R.id.editTo).text.toString()
            val date = findViewById<EditText>(R.id.editDate).text.toString()
            val departure = findViewById<EditText>(R.id.editDeparture).text.toString()
            val arrival = findViewById<EditText>(R.id.editArrival).text.toString()

            if (pdfPath.isNotEmpty()) {
                val ticket = Ticket(from, to, date, departure, arrival, pdfPath)

                val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                val editor = prefs.edit()

                val existing = prefs.getString("tickets", null)
                val ticketList = if (existing != null) {
                    Gson().fromJson(existing, Array<Ticket>::class.java).toMutableList()
                } else mutableListOf()

                ticketList.add(ticket)
                editor.putString("tickets", Gson().toJson(ticketList))
                editor.apply()

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
