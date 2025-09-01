package com.example.gulliver

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.SharedPreferences
import android.widget.ImageView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID // For generating Item IDs


class PacklistActivity : AppCompatActivity() {

    private lateinit var editTextItem: EditText
    private lateinit var recyclerViewItems: RecyclerView
    private lateinit var packlistItemAdapter: PacklistItemAdapter
    private val itemsList = mutableListOf<Item>()

    //For the persistence
    private lateinit var sharedPrefs: SharedPreferences
    private val gson = Gson()
    private val PREFS_KEY = "PACKLIST_PREFS"
    private val ITEMS_KEY = "ITEMS_LIST"

    //For the deletion
    private var selectedItemForDeletion: Item? = null
    private lateinit var imageViewDelete: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.packlist_layout)

        editTextItem = findViewById(R.id.editTextItem)
        recyclerViewItems = findViewById(R.id.recyclerView)

        setupRecyclerView()
        setupEditTextListener()

        sharedPrefs = getSharedPreferences(PREFS_KEY, MODE_PRIVATE)
        loadItemsFromPrefs()

        updateRecyclerViewData()

        imageViewDelete = findViewById(R.id.imageViewDelete)

        imageViewDelete.setOnClickListener {
            selectedItemForDeletion?.let { item ->
                itemsList.removeAll { it.id == item.id }
                selectedItemForDeletion = null
                imageViewDelete.visibility = View.GONE
                updateRecyclerViewData()
            }
        }
    }

    private fun setupRecyclerView() {
        packlistItemAdapter = PacklistItemAdapter(
            this,
            onItemToggled = { item -> toggleItemPackedState(item) },
            onItemLongPressed = { item -> showDeleteIcon(item) }
        )

        recyclerViewItems.apply {
            layoutManager = LinearLayoutManager(this@PacklistActivity)
            adapter = packlistItemAdapter

        }
    }

    private fun showDeleteIcon(item: Item) {
        selectedItemForDeletion = item
        imageViewDelete.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        if (imageViewDelete.visibility == View.VISIBLE) {
            imageViewDelete.visibility = View.GONE
            selectedItemForDeletion = null
        } else {
            super.onBackPressed()
        }
    }

    private fun setupEditTextListener() {
        editTextItem.setOnEditorActionListener { textView, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                val itemText = textView.text.toString().trim()
                if (itemText.isNotEmpty()) {
                    addNewItem(itemText)
                    textView.text = "" // Clear the EditText after adding
                    hideKeyboard(textView)
                }
                true
            } else {
                false
            }
        }
    }

    private fun addNewItem(text: String) {
        val newItem = Item(text = text)
        itemsList.add(0, newItem)
        updateRecyclerViewData()
    }

    private fun toggleItemPackedState(itemToToggle: Item) {
        val itemIndex = itemsList.indexOfFirst { it.id == itemToToggle.id }
        if (itemIndex != -1) {

            val currentItem = itemsList[itemIndex]
            val updatedItem = currentItem.copy(isPacked = !currentItem.isPacked)
            itemsList[itemIndex] = updatedItem
            updateRecyclerViewData()
        }
    }

    private fun updateRecyclerViewData() {
        packlistItemAdapter.submitList(itemsList.toList())
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onPause() {
        super.onPause()
        saveItemsToPrefs()
    }

    private fun saveItemsToPrefs() {
        val json = gson.toJson(itemsList)
        sharedPrefs.edit().putString(ITEMS_KEY, json).apply()
    }

    private fun loadItemsFromPrefs() {
        val json = sharedPrefs.getString(ITEMS_KEY, null)
        if (!json.isNullOrEmpty()) {
            val itemType = object : TypeToken<MutableList<Item>>() {}.type
            val loadedItems: MutableList<Item> = gson.fromJson(json, itemType)
            itemsList.clear()
            itemsList.addAll(loadedItems)
        }
    }

}