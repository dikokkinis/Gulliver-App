package com.example.gulliver

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.view.ViewTreeObserver
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.gulliver.databinding.ActivityMainBinding
import android.content.Intent
import android.graphics.Rect
import android.view.MotionEvent
import android.view.ViewGroup
import android.media.SoundPool
import android.media.AudioAttributes

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val cities = listOf("Barcelona", "Lisbon", "Porto", "Paris", "Amsterdam",
        "London", "Copenhagen", "Oslo", "Stockholm", "Berlin", "Prague", "Rome", "Naples")
    private var currentCityIndex = 0

    private lateinit var activeTextView: TextView
    private lateinit var inactiveTextView: TextView

    private var soundPool: SoundPool? = null
    private var arrowButtonClickSoundId: Int = 0 // Sound ID for arrow buttons
    private var transactionConfirmSoundId: Int = 0 // Sound ID for transaction confirm button
    private var transactionClearSoundId: Int = 0 // Sound ID for transaction confirm button


    private val cityCoordinates = mapOf(
        "Barcelona" to Pair(837f, 3140f), "Lisbon" to Pair(0f, 3140f),
        "Porto" to Pair(0f, 2700f), "Paris" to Pair(1150f, 2220f),
        "Amsterdam" to Pair(1400f, 1710f), "London" to Pair(850f, 1810f),
        "Copenhagen" to Pair(2020f, 1410f), "Oslo" to Pair(1850f, 710f),
        "Stockholm" to Pair(2400f, 810f), "Berlin" to Pair(2100f, 1700f),
        "Prague" to Pair(2250f, 2050f), "Rome" to Pair(2100f, 3140f),
        "Naples" to Pair(2250f, 3140f)
    )

    // Separate coordinates for positioning the MARKER on the map image
    private val markerCoordinates = mapOf(
        "Barcelona" to Pair(1487f, 4140f),
        "Lisbon" to Pair(140f, 4300f),
        "Porto" to Pair(300f, 4000f),
        "Paris" to Pair(1650f, 3220f),
        "Amsterdam" to Pair(1900f, 2610f),
        "London" to Pair(1450f, 2780f),
        "Copenhagen" to Pair(2560f, 2300f),
        "Oslo" to Pair(2410f, 1680f),
        "Stockholm" to Pair(2960f, 1750f),
        "Berlin" to Pair(2660f, 2670f),
        "Prague" to Pair(2810f, 3020f),
        "Rome" to Pair(2600f, 4110f),
        "Naples" to Pair(2870f, 4280f)
    )

    private val cityStyleMap = mapOf(
        "Barcelona" to Pair(Color.parseColor("#E3C396"), Color.parseColor("#5F2C2C")),
        "Lisbon" to Pair(Color.parseColor("#B16139"), Color.parseColor("#702D13")),
        "Porto" to Pair(Color.parseColor("#B16139"), Color.parseColor("#702D13")),
        "Paris" to Pair(Color.parseColor("#A263C7"), Color.parseColor("#1B3546")),
        "Amsterdam" to Pair(Color.parseColor("#D48B4C"), Color.parseColor("#2D585A")),
        "London" to Pair(Color.parseColor("#2E216D"), Color.parseColor("#851B1B")),
        "Copenhagen" to Pair(Color.parseColor("#701B1B"), Color.parseColor("#393744")),
        "Oslo" to Pair(Color.parseColor("#357A2E"), Color.parseColor("#671C44")),
        "Stockholm" to Pair(Color.parseColor("#2EB487"), Color.parseColor("#BD8C23")),
        "Berlin" to Pair(Color.parseColor("#181B46"), Color.parseColor("#000000")),
        "Prague" to Pair(Color.parseColor("#327577"), Color.parseColor("#4C5B2D")),
        "Rome" to Pair(Color.parseColor("#6AA22A"), Color.parseColor("#994747")),
        "Naples" to Pair(Color.parseColor("#6AA22A"), Color.parseColor("#994747"))
    )

    companion object {
        private const val DIRECTION_NEXT = 1
        private const val DIRECTION_PREVIOUS = -1
        private const val SLIDE_ANIMATION_DURATION = 700L
        private const val COLOR_ANIMATION_DURATION = 700L
        private const val TARGET_TEXT_SCALE = 0.5f
        private const val CLICK_DELAY = 1L
    }

    //Variables for the transaction menu visibility
    private var transactionMenuView: View? = null
    private var isTransactionMenuVisible = false

    //Variables for the main menu visibility
    private var MenuView: View? = null
    private var isMenuVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        arrowButtonClickSoundId = soundPool?.load(this, R.raw.click, 1) ?: 0
        transactionConfirmSoundId = soundPool?.load(this, R.raw.coins, 1) ?: 0
        transactionClearSoundId = soundPool?.load(this, R.raw.delete, 1) ?: 0

        binding.transaction.setOnClickListener {
            toggleTransactionMenu()
        }

        binding.topLeftIconButton.setOnClickListener {
            toggleMenu()
        }

        activeTextView = binding.cityTextView1
        inactiveTextView = binding.cityTextView2

        setupButtonClickListeners()
        setupMapObserver()
    }

    override fun onDestroy() {
        super.onDestroy()

        soundPool?.release()
        soundPool = null
    }

    private fun playArrowSound() {
        // Play the arrow button sound using SoundPool
        soundPool?.play(arrowButtonClickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    private fun playTransactionConfirmSound() {
        // Play the transaction confirm sound using SoundPool
        soundPool?.play(transactionConfirmSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    private fun playTransactionClearSound() {
        // Play the transaction confirm sound using SoundPool
        soundPool?.play(transactionClearSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }


    private fun setupMapObserver() {
        binding.mapImageView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.mapImageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val imageView = binding.mapImageView
                val drawable = imageView.drawable ?: return

                val drawableWidth = drawable.intrinsicWidth.toFloat()
                val drawableHeight = drawable.intrinsicHeight.toFloat()
                val imageViewWidth = imageView.width.toFloat()
                val imageViewHeight = imageView.height.toFloat()

                if (drawableWidth <= 0 || drawableHeight <= 0 || imageViewWidth <= 0 || imageViewHeight <= 0) {
                    return
                }

                val matrix = Matrix()
                val scaleX = imageViewWidth / drawableWidth
                val scaleY = imageViewHeight / drawableHeight
                matrix.postScale(scaleX, scaleY)

                val initialCityCoords = cityCoordinates["Barcelona"] ?: Pair(0f, 0f)
                val translateX = -initialCityCoords.first * scaleX
                val translateY = -initialCityCoords.second * scaleY
                matrix.postTranslate(translateX, translateY)

                imageView.imageMatrix = matrix
                imageView.invalidate()
                setupInitialCity()
            }
        })
    }

    //For the transaction menu visibility
    private fun toggleTransactionMenu() {
        if (isTransactionMenuVisible) {
            hideTransactionMenu()
        } else {
            showTransactionMenu()
        }
    }

    //For the main menu visibility
    private fun toggleMenu() {
        if (isMenuVisible) {
            hideMenu()
        } else {
            showMenu()
        }
    }

    //For the transaction menu
    private fun showTransactionMenu() {
        if (transactionMenuView == null) {
            // Inflate the menu for the first time
            val inflater = LayoutInflater.from(this)
            transactionMenuView = inflater.inflate(R.layout.transaction_menu, binding.menuContainer, false) // Inflate into a container


            binding.menuContainer.setOnTouchListener { v, event ->
                if (isTransactionMenuVisible && event.action == MotionEvent.ACTION_DOWN) {
                    val menuRect = Rect()
                    transactionMenuView?.getGlobalVisibleRect(menuRect)

                    if (!menuRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        hideTransactionMenu()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }


            setupTransactionMenuLogic(transactionMenuView!!)
        }

        if (transactionMenuView?.parent == null) {
            binding.menuContainer.addView(transactionMenuView)
        }
        transactionMenuView?.visibility = View.VISIBLE
        isTransactionMenuVisible = true

        binding.menuContainer.bringToFront()
        // Animate the menu appearing
        transactionMenuView?.alpha = 0f
        transactionMenuView?.animate()?.alpha(1f)?.setDuration(300)?.start()
    }

    //For the main menu
    private fun showMenu() {
        if (MenuView == null) {
            // Inflate the menu for the first time
            val inflater = LayoutInflater.from(this)
            MenuView = inflater.inflate(R.layout.menu, binding.mainMenuContainer, false)

            binding.mainMenuContainer.setOnTouchListener { v, event ->
                if (isMenuVisible && event.action == MotionEvent.ACTION_DOWN) {
                    val menuRect = Rect()
                    MenuView?.getGlobalVisibleRect(menuRect)

                    if (!menuRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        hideMenu()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        }

        if (MenuView?.parent == null) {
            binding.mainMenuContainer.addView(MenuView)
        }
        MenuView?.visibility = View.VISIBLE
        isMenuVisible = true

        binding.mainMenuContainer.bringToFront()

        MenuView?.alpha = 0f
        MenuView?.animate()?.alpha(1f)?.setDuration(300)?.start()
    }

    //For the transaction menu
    private fun hideTransactionMenu() {

        transactionMenuView?.animate()?.alpha(0f)?.setDuration(300)?.withEndAction {
            transactionMenuView?.visibility = View.GONE

        }?.start()

        if (transactionMenuView?.alpha == 1f && transactionMenuView?.animation == null) {
            transactionMenuView?.visibility = View.GONE
        }
        isTransactionMenuVisible = false
    }

    //For the main menu
    private fun hideMenu() {

        MenuView?.animate()?.alpha(0f)?.setDuration(300)?.withEndAction {
            MenuView?.visibility = View.GONE
        }?.start()

        if (MenuView?.alpha == 1f && MenuView?.animation == null) { // If no animation running
            MenuView?.visibility = View.GONE
        }
        isMenuVisible = false
    }


    private fun setupTransactionMenuLogic(menuView: View) {
        val tvDimitris = menuView.findViewById<TextView>(R.id.valueDimitris)
        val tvChristina = menuView.findViewById<TextView>(R.id.valueChristina)
        val btnChooseName = menuView.findViewById<ImageButton>(R.id.btnChooseName)
        val edtAmount = menuView.findViewById<EditText>(R.id.inputAmount)
        val btnClear = menuView.findViewById<ImageButton>(R.id.btnClear)
        val btnEnter = menuView.findViewById<ImageButton>(R.id.btnEnter)

        val defaultPersonButton = ContextCompat.getDrawable(this, R.drawable.person_button)
        val clickedPersonButton = ContextCompat.getDrawable(this, R.drawable.person_button_clicked)

        val prefs = getSharedPreferences("tx_prefs", Context.MODE_PRIVATE)
        fun loadValue(name: String): Float = prefs.getFloat(name, 0f)
        fun saveValue(name: String, v: Float) {
            prefs.edit().putFloat(name, v).apply()
        }

        tvDimitris.text = String.format("%.2f", loadValue("Δημήτρης"))
        tvChristina.text = String.format("%.2f", loadValue("Χριστίνα"))

        var chosenName: String? = null

        btnChooseName.setOnClickListener {
            // Change the image on click
            btnChooseName.setImageDrawable(clickedPersonButton)

            // Revert back to the default image after a short delay
            Handler().postDelayed({
                btnChooseName.setImageDrawable(defaultPersonButton)
            }, CLICK_DELAY)

            val nameOptions = arrayOf("Δημήτρης", "Χριστίνα")
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Διαλέξτε άτομο")
                .setItems(nameOptions) { _, which ->
                    chosenName = nameOptions[which]
                    edtAmount.visibility = View.VISIBLE
                    btnEnter.visibility = if (!edtAmount.text.isNullOrBlank()) View.VISIBLE else View.GONE
                }
                .show()
        }

        edtAmount.doAfterTextChanged { text ->
            btnEnter.visibility = if (!text.isNullOrBlank() && chosenName != null) View.VISIBLE else View.GONE
        }

        btnClear.setOnClickListener {
            playTransactionClearSound()
            saveValue("Δημήτρης", 0f)
            saveValue("Χριστίνα", 0f)
            tvDimitris.text = "0"
            tvChristina.text = "0"
            chosenName = null
            edtAmount.text?.clear()
            edtAmount.visibility = View.GONE
            btnEnter.visibility = View.GONE
            Toast.makeText(this, "Ποσό καθαρίστηκε", Toast.LENGTH_SHORT).show()
        }

        btnEnter.setOnClickListener {
            playTransactionConfirmSound() // Play sound for the Enter button
            val amount = edtAmount.text.toString().toFloatOrNull()
            if (chosenName == null || amount == null || amount <= 0) {
                Toast.makeText(this, "Διάλεξε άτομο και πρόσθεσε έγκυρο ποσό", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val otherName = if (chosenName == "Δημήτρης") "Χριστίνα" else "Δημήτρης"
            val half = amount / 2f
            val currentOtherOwes = loadValue(otherName)
            val newTotalForOther = currentOtherOwes + half
            saveValue(otherName, newTotalForOther)

            tvDimitris.text = String.format("%.2f", loadValue("Δημήτρης"))
            tvChristina.text = String.format("%.2f", loadValue("Χριστίνα"))

            Toast.makeText(this, "Συναλλαγή για $chosenName: $amount", Toast.LENGTH_LONG).show()

            // Reset for next entry
            chosenName = null
            edtAmount.text?.clear()
            edtAmount.visibility = View.GONE
            btnEnter.visibility = View.GONE
            // hideTransactionMenu() // Optionally close the menu after entry
        }
    }

    private fun animateMapToCoordinates(targetX: Float, targetY: Float) {
        val imageView = binding.mapImageView
        val drawable = imageView.drawable ?: return

        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        val imageViewWidth = imageView.width.toFloat()
        val imageViewHeight = imageView.height.toFloat()

        if (drawableWidth <= 0 || drawableHeight <= 0 || imageViewWidth <= 0 || imageViewHeight <= 0) return

        val scaleX = imageViewWidth / drawableWidth
        val scaleY = imageViewHeight / drawableHeight

        val currentMatrix = Matrix(imageView.imageMatrix)
        val currentValues = FloatArray(9)
        currentMatrix.getValues(currentValues)
        val currentTranslateX = currentValues[Matrix.MTRANS_X]
        val currentTranslateY = currentValues[Matrix.MTRANS_Y]

        val targetTranslateX = -targetX * scaleX
        val targetTranslateY = -targetY * scaleY

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = COLOR_ANIMATION_DURATION
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val newMatrix = Matrix()
            newMatrix.postScale(scaleX, scaleY)
            val interpolatedX = currentTranslateX + fraction * (targetTranslateX - currentTranslateX)
            val interpolatedY = currentTranslateY + fraction * (targetTranslateY - currentTranslateY)
            newMatrix.postTranslate(interpolatedX, interpolatedY)
            imageView.imageMatrix = newMatrix
        }
        animator.start()
    }

    private fun animateCityChange(newCity: String, direction: Int) {
        val backgroundLayout = binding.bottomNavLayout
        val cardView = binding.mapCardView

        val (newStartColor, newTextColor) = cityStyleMap[newCity]
            ?: Pair(Color.LTGRAY, Color.BLACK)

        val background = backgroundLayout.background.mutate()
        if (background is GradientDrawable) {
            val drawable: GradientDrawable = background
            val oldStartColor = drawable.colors?.getOrNull(0) ?: newStartColor
            val endColor = drawable.colors?.getOrNull(2) ?: Color.parseColor("#DACFCF")
            val middleColor = drawable.colors?.getOrNull(1) ?: Color.parseColor("#DACFCF")

            ValueAnimator.ofObject(ArgbEvaluator(), oldStartColor, newStartColor).apply {
                duration = COLOR_ANIMATION_DURATION
                addUpdateListener { animator ->
                    val color = animator.animatedValue as Int
                    drawable.colors = intArrayOf(color, middleColor, endColor)
                }
                start()
            }
        }

        val oldStrokeColor = cardView.strokeColor.takeIf { it != -1 } ?: newStartColor
        ValueAnimator.ofObject(ArgbEvaluator(), oldStrokeColor, newStartColor).apply {
            duration = COLOR_ANIMATION_DURATION
            addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                cardView.setStrokeColor(color)
            }
            start()
        }

        val slideDistance = backgroundLayout.width.toFloat()
        if (slideDistance <= 0f) {
            activeTextView.text = newCity
            activeTextView.setTextColor(newTextColor)
            activeTextView.scaleX = 1.0f
            activeTextView.scaleY = 1.0f
            activeTextView.translationX = 0f
            activeTextView.alpha = 1f
            inactiveTextView.alpha = 0f
            return
        }

        val slideOutX = -direction * slideDistance
        val slideInStartX = direction * slideDistance

        inactiveTextView.text = newCity
        inactiveTextView.setTextColor(newTextColor)
        inactiveTextView.translationX = slideInStartX
        inactiveTextView.alpha = 0f
        inactiveTextView.scaleX = TARGET_TEXT_SCALE
        inactiveTextView.scaleY = TARGET_TEXT_SCALE

        activeTextView.animate()
            .translationX(slideOutX)
            .alpha(0f)
            .scaleX(TARGET_TEXT_SCALE)
            .scaleY(TARGET_TEXT_SCALE)
            .setDuration(SLIDE_ANIMATION_DURATION)
            .start()

        inactiveTextView.animate()
            .translationX(0f)
            .alpha(1f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(SLIDE_ANIMATION_DURATION)
            .withEndAction {
                val temp = activeTextView
                activeTextView = inactiveTextView
                inactiveTextView = temp
                inactiveTextView.alpha = 0f
            }
            .start()
    }

    private fun setupInitialCity() {
        val marker = findViewById<ImageButton>(R.id.cityMarker)
        currentCityIndex = cities.indexOf("Barcelona").takeIf { it != -1 } ?: 0
        val initialCity = cities[currentCityIndex]
        val (startColor, textColor) = cityStyleMap[initialCity] ?: Pair(Color.LTGRAY, Color.BLACK)

        activeTextView.text = initialCity
        activeTextView.setTextColor(textColor)
        activeTextView.alpha = 1f
        activeTextView.scaleX = 1.0f
        activeTextView.scaleY = 1.0f
        activeTextView.translationX = 0f
        inactiveTextView.alpha = 0f

        val background = binding.bottomNavLayout.background.mutate()
        if (background is GradientDrawable) {
            val endColor = background.colors?.getOrNull(2) ?: Color.parseColor("#DACFCF")
            val middleColor = background.colors?.getOrNull(1) ?: Color.parseColor("#DACFCF")
            background.colors = intArrayOf(startColor, middleColor, endColor)
        }
        binding.mapCardView.setStrokeColor(startColor)

        Handler().postDelayed({
            marker.visibility = View.VISIBLE
            marker.setOnClickListener {
                openCityActivity(cities[currentCityIndex])
            }
        }, SLIDE_ANIMATION_DURATION)
    }

    private fun setupButtonClickListeners() {
        binding.leftArrowButton.setOnClickListener {
            playArrowSound() // Play sound when left arrow is clicked
            navigateToPreviousCity()
        }

        binding.rightArrowButton.setOnClickListener {
            playArrowSound() // Play sound when right arrow is clicked
            navigateToNextCity()
        }

        binding.bottomNavLayout.setOnClickListener {
            if (currentCityIndex in cities.indices) {
                Toast.makeText(this, "Current city: ${cities[currentCityIndex]}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToPreviousCity() {
        if (cities.isNotEmpty()) {
            currentCityIndex = (currentCityIndex - 1 + cities.size) % cities.size
            updateCityDisplay(DIRECTION_PREVIOUS)
        }
    }

    private fun navigateToNextCity() {
        if (cities.isNotEmpty()) {
            currentCityIndex = (currentCityIndex + 1) % cities.size
            updateCityDisplay(DIRECTION_NEXT)
        }
    }

    private fun updateCityDisplay(direction: Int) {
        val marker = findViewById<ImageButton>(R.id.cityMarker)
        marker.visibility = View.INVISIBLE

        if (currentCityIndex < 0 || currentCityIndex >= cities.size) {
            return
        }
        val city = cities[currentCityIndex]

        animateCityChange(city, direction)
        val (x, y) = cityCoordinates[city] ?: return
        animateMapToCoordinates(x, y)

        // Reappear only after transition
        Handler().postDelayed({
            marker.visibility = View.VISIBLE
            val (markerX, markerY) = markerCoordinates[city] ?: return@postDelayed
            positionMarker(markerX, markerY, binding.mapImageView.imageMatrix)
            marker.setOnClickListener {
                openCityActivity(cities[currentCityIndex])
            }
        }, SLIDE_ANIMATION_DURATION)
    }

    /**
     * Positions the marker on the screen based on its raw coordinates on the original map image
     * and the current transformation matrix of the map ImageView.
     *
     * @param mapX The X coordinate of the marker on the original map image.
     * @param mapY The Y coordinate of the marker on the original map image.
     * @param currentMapMatrix The current Matrix of the mapImageView.
     */
    private fun positionMarker(mapX: Float, mapY: Float, currentMapMatrix: Matrix) {
        val marker = binding.cityMarker

        val markerPoint = floatArrayOf(mapX, mapY)
        val transformedPoint = FloatArray(2)
        currentMapMatrix.mapPoints(transformedPoint, markerPoint)

        val finalMarkerX = transformedPoint[0]
        val finalMarkerY = transformedPoint[1]

        val markerWidth = if (marker.width > 0) marker.width else marker.drawable.intrinsicWidth
        val markerHeight = if (marker.height > 0) marker.height else marker.drawable.intrinsicHeight

        val markerHalfWidth = markerWidth / 2f
        val markerHalfHeight = markerHeight / 2f

        // Set the layout parameters for the marker
        val layoutParams = marker.layoutParams as? FrameLayout.LayoutParams
            ?: FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

        layoutParams.leftMargin = (finalMarkerX - markerHalfWidth).toInt()
        layoutParams.topMargin = (finalMarkerY - markerHalfHeight).toInt()
        marker.layoutParams = layoutParams
    }

    fun goToGallery(view: View) {
        val intent = Intent(this, GalleryActivity::class.java)
        startActivity(intent)
    }

    fun goToTickets(view: View) {
        val intent = Intent(this, TicketsActivity::class.java)
        startActivity(intent)
    }

    fun goToPacklist(view: View) {
        val intent = Intent(this, PacklistActivity::class.java)
        startActivity(intent)
    }

    private fun getImageViewScreenPosition(x: Float, y: Float): Pair<Int, Int> {
        val matrix = binding.mapImageView.imageMatrix
        val point = floatArrayOf(x, y)
        matrix.mapPoints(point)
        return Pair(point[0].toInt(), point[1].toInt())
    }


    private fun openCityActivity(city: String) {
        val intent = when (city) {
            "Barcelona" -> Intent(this, BarcelonaActivity::class.java)
            "Lisbon" -> Intent(this, LisbonActivity::class.java)
            "Porto" -> Intent(this, PortoActivity::class.java)
            "Paris" -> Intent(this, ParisActivity::class.java)
            "Amsterdam" -> Intent(this, AmsterdamActivity::class.java)
            "London" -> Intent(this, LondonActivity::class.java)
            "Copenhagen" -> Intent(this, CopenhagenActivity::class.java)
            "Oslo" -> Intent(this, OsloActivity::class.java)
            "Stockholm" -> Intent(this, StockholmActivity::class.java)
            "Berlin" -> Intent(this, BerlinActivity::class.java)
            "Prague" -> Intent(this, PragueActivity::class.java)
            "Rome" -> Intent(this, RomeActivity::class.java)
            "Naples" -> Intent(this, NaplesActivity::class.java)
            else -> null
        }
        intent?.let { startActivity(it) }
    }
}