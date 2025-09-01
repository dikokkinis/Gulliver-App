package com.example.gulliver

import android.content.Context
import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton

class StockholmActivity : CityActivity() {

    override val cityPrefsName: String = "stockholm_prefs"

    private val scheduleCards = listOf(
        Pair("day1", R.id.day1Card),
        Pair("day2", R.id.day2Card),
        Pair("day3", R.id.day3Card),
        Pair("day4", R.id.day4Card)
    )

    private lateinit var budgetEditText: EditText

    private val PREFS_NAME = "stockholm_prefs"
    private val BUDGET_KEY = "budget_value"
    private val HISTORY_KEY = "history_stockholm"
    private val NOTES_KEY = "notes_stockholm"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //For the transparent action bar
        window.apply {
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            statusBarColor = android.graphics.Color.TRANSPARENT
        }
        setContentView(R.layout.activity_stockholm)

        budgetEditText = findViewById(R.id.budgetEditText)
        val historyButton = findViewById<ImageButton>(R.id.historyButton)
        val notesButton = findViewById<ImageButton>(R.id.notesButton)

        historyButton.setOnClickListener {
            showPopupDialog(HISTORY_KEY, R.layout.dialogue_history)
        }

        notesButton.setOnClickListener {
            showPopupDialog(NOTES_KEY, R.layout.dialogue_notes)
        }

        val cityTicketButton = findViewById<ImageButton>(R.id.cityticketButton)

        cityTicketButton.setOnClickListener {
            val intent = Intent(this, CityTicketActivity::class.java)
            intent.putExtra("cityName", "Stockholm")
            startActivity(intent)
        }

        // Load saved budget
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedBudget = prefs.getInt(BUDGET_KEY, 0)
        if (savedBudget > 0) {
            budgetEditText.setText(savedBudget.toString())
        }

        budgetEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toIntOrNull() ?: 0
                prefs.edit().putInt(BUDGET_KEY, value).apply()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        scheduleCards.forEach { (dayKey, cardId) ->
            val card = findViewById<LinearLayout>(cardId)

            card.setOnClickListener {
                showScheduleDialog(dayKey)
            }
        }

        setupShopSection()
    }

    override fun showScheduleDialog(dayKey: String) {
        super.showScheduleDialog(dayKey)
    }

    private fun showPopupDialog(key: String, layoutId: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedText = prefs.getString(key, "")

        val dialogView = LayoutInflater.from(this).inflate(layoutId, null)
        val editText = dialogView.findViewById<EditText>(R.id.dialogEditText)

        editText.setText(savedText)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val newText = editText.text.toString()
                prefs.edit().putString(key, newText).apply()
            }
            .setNegativeButton("Ακύρωση", null)
            .create()

        dialog.show()
    }
}