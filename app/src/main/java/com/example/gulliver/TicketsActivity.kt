package com.example.gulliver

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import com.google.gson.Gson
import android.media.SoundPool // Import SoundPool
import android.media.AudioAttributes // Import AudioAttributes for SoundPool.Builder


class TicketsActivity : AppCompatActivity() {
    private lateinit var ticketsLayout: LinearLayout
    private val PREFS_NAME = "TicketsPrefs"

    private var soundPool: SoundPool? = null
    private var deleteSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tickets_layout)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()
        deleteSoundId = soundPool?.load(this, R.raw.delete, 1) ?: 0

        ticketsLayout = findViewById(R.id.ticketsContainer)
        val addButton: ImageButton = findViewById(R.id.addTicketButton)

        loadTickets()

        addButton.setOnClickListener {
            val intent = Intent(this, AddTicketActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }

    private fun playDeleteSound() {
        // Play the transaction confirm sound
        soundPool?.play(deleteSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    private fun loadTickets() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val ticketsJson = prefs.getString("tickets", null)

        ticketsLayout.removeAllViews()

        ticketsJson?.let {
            val tickets = Gson().fromJson(it, Array<Ticket>::class.java)
            for (ticket in tickets) {
                val btn = createTicketButton(ticket)

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.setMargins(0, 0, 0, 32)

                btn.layoutParams = layoutParams
                ticketsLayout.addView(btn)
            }
        }
    }

    private fun createTicketButton(ticket: Ticket): View {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.ticket_item, null)

        val fromText = view.findViewById<TextView>(R.id.fromText)
        val toText = view.findViewById<TextView>(R.id.toText)
        val dateText = view.findViewById<TextView>(R.id.dateText)
        val departureTimeText = view.findViewById<TextView>(R.id.departureTimeText)
        val arrivalTimeText = view.findViewById<TextView>(R.id.arrivalTimeText)

        val button = view.findViewById<ImageButton>(R.id.ticketButton)

        val deleteIcon = view.findViewById<ImageView>(R.id.deleteIcon)

        fromText.text = ticket.from
        toText.text = ticket.to
        dateText.text = ticket.date
        departureTimeText.text = ticket.departureTime
        arrivalTimeText.text = ticket.arrivalTime

        button.setOnClickListener {
            val file = File(ticket.pdfPath)
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        }

        // Long press to show delete icon
        button.setOnLongClickListener {
            deleteIcon.visibility = View.VISIBLE
            true
        }
        // Tap delete icon to remove ticket
        deleteIcon.setOnClickListener {
            playDeleteSound() //Play the delete sound when the delete icon is pressed
            deleteTicket(ticket)
        }
        return view
    }

    private fun deleteTicket(ticketToRemove: Ticket) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val ticketsJson = prefs.getString("tickets", null)

        ticketsJson?.let {
            val tickets = Gson().fromJson(it, Array<Ticket>::class.java).toMutableList()
            tickets.removeAll { it.pdfPath == ticketToRemove.pdfPath }

            val newJson = Gson().toJson(tickets)
            prefs.edit().putString("tickets", newJson).apply()

            loadTickets()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTickets()
    }
}
