package com.example.gulliver

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.AnimatorInflater
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.media.SoundPool
import android.media.AudioAttributes
import com.bumptech.glide.Glide

abstract class CityActivity : AppCompatActivity() {

    abstract val cityPrefsName: String

    private lateinit var allCategoriesContainer: LinearLayout
    private val categoryList = mutableListOf<ShopCategory>()
    private val gson = Gson()
    private lateinit var animOut: AnimatorSet
    private lateinit var animIn: AnimatorSet
    private val CATEGORY_LIST_KEY by lazy { "${cityPrefsName}_category_list" }

    private var soundPool: SoundPool? = null
    private var deleteSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()
        deleteSoundId = soundPool?.load(this, R.raw.delete, 1) ?: 0
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }

    private fun playDeleteSound() {
        soundPool?.play(deleteSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    protected fun setupShopSection() {
        allCategoriesContainer = findViewById(R.id.allCategoriesContainer)
        val addNewCategoryButton = findViewById<ImageButton>(R.id.addNewCategoryButton)

        animOut = AnimatorInflater.loadAnimator(applicationContext, R.animator.card_flip_out) as AnimatorSet
        animIn = AnimatorInflater.loadAnimator(applicationContext, R.animator.card_flip_in) as AnimatorSet

        loadData()

        addNewCategoryButton.setOnClickListener {
            addNewCategoryView()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode >= 200) {
            val categoryIndex = requestCode - 200
            val shopName = data?.getStringExtra("shopName")
            val shopLocation = data?.getStringExtra("shopLocation")
            val shopRating = data?.getStringExtra("shopRating")
            val imageUriString = data?.getStringExtra("imageUri")
            val newShop = Shop(shopName, shopLocation, shopRating, imageUriString)

            categoryList.getOrNull(categoryIndex)?.shops?.add(newShop)
            saveData()
            loadData()
        }
    }

    private fun saveData() {
        val prefs = getSharedPreferences(cityPrefsName, Context.MODE_PRIVATE)
        val jsonString = gson.toJson(categoryList)
        prefs.edit().putString(CATEGORY_LIST_KEY, jsonString).apply()
    }

    private fun loadData() {
        allCategoriesContainer.removeAllViews()
        categoryList.clear()

        val prefs = getSharedPreferences(cityPrefsName, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(CATEGORY_LIST_KEY, null)

        if (jsonString != null) {
            val type = object : TypeToken<MutableList<ShopCategory>>() {}.type
            try {
                val savedCategories: MutableList<ShopCategory> = gson.fromJson(jsonString, type)
                categoryList.addAll(savedCategories)
            } catch (e: Exception) {
                Log.e("CityActivity", "Error parsing categories from JSON", e)
            }
        }

        if (categoryList.isEmpty()) {
            addNewCategoryView()
        } else {
            categoryList.forEachIndexed { index, category ->
                addCategoryViewFromData(category, index)
            }
        }
    }

    private fun addNewCategoryView() {
        val inflater = LayoutInflater.from(this)
        val categoryView = inflater.inflate(R.layout.shop_category_layout, allCategoriesContainer, false)

        val titleEditText = categoryView.findViewById<EditText>(R.id.shopCategoryEditText)
        val titleTextView = categoryView.findViewById<TextView>(R.id.shopCategoryTitle)
        val addShopButton = categoryView.findViewById<ImageButton>(R.id.addShopButton)
        val deleteCategoryButton = categoryView.findViewById<ImageButton>(R.id.deleteCategoryButton)

        titleEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val title = titleEditText.text.toString()
                if (title.isNotEmpty()) {
                    titleEditText.visibility = View.GONE
                    titleTextView.text = title
                    titleTextView.visibility = View.VISIBLE
                    addShopButton.visibility = View.VISIBLE

                    val newCategory = ShopCategory(title = title)
                    categoryList.add(newCategory)
                    saveData()

                    val categoryIndex = categoryList.size - 1
                    addShopButton.setOnClickListener { launchAddShopActivity(categoryIndex) }

                    titleTextView.setOnLongClickListener {
                        deleteCategoryButton.visibility = View.VISIBLE
                        true
                    }
                    deleteCategoryButton.setOnClickListener {
                        playDeleteSound()
                        showDeleteConfirmationDialog(categoryIndex)
                    }
                }
                true
            } else { false }
        }
        allCategoriesContainer.addView(categoryView)
    }

    private fun addCategoryViewFromData(category: ShopCategory, index: Int) {
        val inflater = LayoutInflater.from(this)
        val categoryView = inflater.inflate(R.layout.shop_category_layout, allCategoriesContainer, false)

        val titleEditText = categoryView.findViewById<EditText>(R.id.shopCategoryEditText)
        val titleTextView = categoryView.findViewById<TextView>(R.id.shopCategoryTitle)
        val shopsHorizontalContainer = categoryView.findViewById<LinearLayout>(R.id.shopsHorizontalContainer)
        val addShopButton = categoryView.findViewById<ImageButton>(R.id.addShopButton)
        val deleteCategoryButton = categoryView.findViewById<ImageButton>(R.id.deleteCategoryButton)

        titleEditText.visibility = View.GONE
        titleTextView.text = category.title
        titleTextView.visibility = View.VISIBLE
        addShopButton.visibility = View.VISIBLE

        addShopButton.setOnClickListener { launchAddShopActivity(index) }

        titleTextView.setOnLongClickListener {
            deleteCategoryButton.visibility = View.VISIBLE
            true
        }
        deleteCategoryButton.setOnClickListener {
            playDeleteSound()
            showDeleteConfirmationDialog(index)
        }

        category.shops.forEachIndexed { shopIndex, shop ->
            val shopCard = createShopCard(shop, index, shopIndex)
            shopsHorizontalContainer.addView(shopCard, shopsHorizontalContainer.childCount - 1)
        }

        allCategoriesContainer.addView(categoryView)
    }

    private fun launchAddShopActivity(categoryIndex: Int) {
        val intent = Intent(this, AddShopActivity::class.java)
        startActivityForResult(intent, 200 + categoryIndex)
    }

    private fun showDeleteConfirmationDialog(categoryIndex: Int) {
        AlertDialog.Builder(this)
            .setTitle("Διαγραφή Κατηγορίας")
            .setMessage("Θέλετε να διαγράψετε αυτή τη κατηγορία και όλα τα μαγαζιά της?")
            .setPositiveButton("Διαγραφή") { _, _ ->
                categoryList.removeAt(categoryIndex)
                saveData()
                loadData()
            }
            .setNegativeButton("Άκυρο", null)
            .show()
    }


    protected open fun showScheduleDialog(dayKey: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialogue_schedule, null)
        val editText = view.findViewById<EditText>(R.id.scheduleEditText)

        val prefs = getSharedPreferences(cityPrefsName, Context.MODE_PRIVATE)
        editText.setText(prefs.getString("${dayKey}_schedule", ""))

        AlertDialog.Builder(this)
            .setTitle("Πρόγραμμα Ημέρας")
            .setView(view) //
            .setPositiveButton("Αποθήκευση") { _, _ ->
                val text = editText.text.toString()
                prefs.edit().putString("${dayKey}_schedule", text).apply()
            }
            .setNegativeButton("Ακύρωση", null)
            .show()
    }

    private fun createShopCard(shop: Shop, categoryIndex: Int, shopIndex: Int): View {
        val inflater = LayoutInflater.from(this)
        val shopCardView = inflater.inflate(R.layout.shop_card_layout, null, false)

        val flipperContainer = shopCardView.findViewById<View>(R.id.flipper_container)
        val cardFront = shopCardView.findViewById<View>(R.id.card_front)
        val cardBack = shopCardView.findViewById<View>(R.id.card_back)
        flipperContainer.tag = true

        val shopImageView = cardFront.findViewById<ImageView>(R.id.shopImageView)
        val deleteShopButton = cardFront.findViewById<ImageButton>(R.id.deleteShopButton)
        val shopLocationTextView = cardBack.findViewById<TextView>(R.id.shopLocationTextView)

        val shopNameTextView = shopCardView.findViewById<TextView>(R.id.shopNameTextView)
        val shopRatingTextView = shopCardView.findViewById<TextView>(R.id.shopRatingTextView)

        shopNameTextView.text = shop.name
        shopRatingTextView.text = shop.rating
        shopLocationTextView.text = shop.location

        try {
            shop.imageUri?.let { uriString ->
                val imageUri = Uri.parse(uriString)
                Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(shopImageView)
            } ?: shopImageView.setImageResource(R.drawable.placeholder_image)
        } catch (e: SecurityException) {
            shopImageView.setImageResource(R.drawable.placeholder_image)
            Log.e("CityActivity", "Failed to load image URI: ${shop.imageUri}", e)
        }

        val scale = resources.displayMetrics.density
        cardFront.cameraDistance = 8000 * scale
        cardBack.cameraDistance = 8000 * scale

        shopLocationTextView.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Shop Location", shopLocationTextView.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Η διεύθυνση αντιγράφηκε", Toast.LENGTH_SHORT).show()
            true
        }

        shopLocationTextView.setOnClickListener {
            val location = shop.location
            if (!location.isNullOrEmpty()) {
                try {
                    // Create a URI for Google Maps. 'q=' is for search query.
                    val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location))
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps") // Specify Google Maps app

                    if (mapIntent.resolveActivity(packageManager) != null) {
                        startActivity(mapIntent)
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://maps.google.com/?q=" + Uri.encode(location)))
                        startActivity(webIntent)
                        Toast.makeText(this, "Opening in browser maps.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("CityActivity", "Error opening map for location: $location", e)
                    Toast.makeText(this, "Could not open map for this location.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Location not available.", Toast.LENGTH_SHORT).show()
            }
        }

        flipperContainer.setOnLongClickListener {
            deleteShopButton.visibility = View.VISIBLE
            true
        }
        deleteShopButton.setOnClickListener {
            playDeleteSound()
            categoryList.getOrNull(categoryIndex)?.shops?.removeAt(shopIndex)
            saveData()
            loadData()
        }

        flipperContainer.setOnClickListener { view ->
            if (deleteShopButton.visibility == View.VISIBLE) {
                deleteShopButton.visibility = View.GONE
                return@setOnClickListener
            }

            val isFrontShowing = view.tag as? Boolean ?: true
            val outAnim = animOut.clone()
            val inAnim = animIn.clone()

            if (isFrontShowing) {
                outAnim.setTarget(cardFront)
                inAnim.setTarget(cardBack)
                outAnim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        cardFront.visibility = View.GONE
                    }
                })
                cardBack.visibility = View.VISIBLE
                outAnim.start()
                inAnim.start()
            } else {
                outAnim.setTarget(cardBack)
                inAnim.setTarget(cardFront)
                outAnim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        cardBack.visibility = View.GONE
                    }
                })
                cardFront.visibility = View.VISIBLE
                outAnim.start()
                inAnim.start()
            }
            view.tag = !isFrontShowing
        }
        return shopCardView
    }
}
