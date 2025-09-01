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
import com.google.gson.Gson
import java.io.File
import android.media.SoundPool // Import SoundPool
import android.media.AudioAttributes // Import AudioAttributes for SoundPool.Builder

class CityTicketActivity : AppCompatActivity() {
    private lateinit var cityTicketsLayout: LinearLayout
    private val PREFS_NAME = "CityTicketsPrefs"
    private lateinit var cityName: String


    private var soundPool: SoundPool? = null
    private var deleteSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.city_tickets)


        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()
        deleteSoundId = soundPool?.load(this, R.raw.delete, 1) ?: 0

        cityTicketsLayout = findViewById(R.id.cityTicketsContainer)
        val addButton: ImageButton = findViewById(R.id.addCityTicketButton)

        cityName = intent.getStringExtra("cityName") ?: "default"

        loadTickets()

        addButton.setOnClickListener {
            val intent = Intent(this, AddCityTicketActivity::class.java)
            intent.putExtra("cityName", cityName)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }

    private fun playDeleteSound() {
        soundPool?.play(deleteSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    private fun loadTickets() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val key = "${cityName}_tickets"
        val ticketsJson = prefs.getString(key, null)

        cityTicketsLayout.removeAllViews()

        ticketsJson?.let {
            val tickets = Gson().fromJson(it, Array<CityTicket>::class.java)
            for (ticket in tickets) {
                val btn = createTicketButton(ticket)

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.setMargins(0, 0, 0, 32) // left, top, right, bottom in pixels

                btn.layoutParams = layoutParams
                cityTicketsLayout.addView(btn)
            }
        }
    }

    private fun createTicketButton(ticket: CityTicket): View {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.city_ticket_item, null)

        val activityText = view.findViewById<TextView>(R.id.activityText)
        val dateText = view.findViewById<TextView>(R.id.dateText)
        val arrivalTimeText = view.findViewById<TextView>(R.id.arrivalTimeText)

        val button = view.findViewById<ImageButton>(R.id.ticketButton)

        val deleteIcon = view.findViewById<ImageView>(R.id.deleteIcon)

        activityText.text = ticket.activity
        dateText.text = ticket.date
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

        button.setOnLongClickListener {
            deleteIcon.visibility = View.VISIBLE
            true
        }

        deleteIcon.setOnClickListener {
            playDeleteSound()
            deleteTicket(ticket)
        }
        return view
    }

    private fun deleteTicket(ticketToRemove: CityTicket) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val key = "${cityName}_tickets"
        val ticketsJson = prefs.getString(key, null)

        ticketsJson?.let {
            val tickets = Gson().fromJson(it, Array<CityTicket>::class.java).toMutableList()
            tickets.removeAll { it.pdfPath == ticketToRemove.pdfPath }

            val newJson = Gson().toJson(tickets)
            prefs.edit().putString(key, newJson).apply()

            loadTickets()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTickets()
    }
}
