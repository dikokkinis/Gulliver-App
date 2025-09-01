package com.example.gulliver

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.media.SoundPool // Import SoundPool
import android.media.AudioAttributes // Import AudioAttributes for SoundPool.Builder

class AddShopActivity : AppCompatActivity() {

    private var imageUri: Uri? = null

    private var soundPool: SoundPool? = null
    private var enterButtonClickSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_shop)


        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        enterButtonClickSoundId = soundPool?.load(this, R.raw.fileupload, 1) ?: 0

        val selectImageButton = findViewById<ImageButton>(R.id.buttonSelectImage)
        val saveShopButton = findViewById<ImageButton>(R.id.buttonSaveShop)

        selectImageButton.setOnClickListener {

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, 100)
        }

        saveShopButton.setOnClickListener {
            playEnterSound()
            val shopName = findViewById<EditText>(R.id.editShopName).text.toString()
            val shopLocation = findViewById<EditText>(R.id.editShopLocation).text.toString()
            val shopRating = findViewById<EditText>(R.id.editShopRating).text.toString()

            val resultIntent = Intent()
            resultIntent.putExtra("shopName", shopName)
            resultIntent.putExtra("shopLocation", shopLocation)
            resultIntent.putExtra("shopRating", shopRating)

            imageUri?.let {
                resultIntent.putExtra("imageUri", it.toString())
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
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
        if (resultCode == Activity.RESULT_OK && requestCode == 100) {

            data?.data?.let { uri ->
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, takeFlags)

                    imageUri = uri
                    findViewById<ImageView>(R.id.imagePreview).setImageURI(imageUri)
                } catch (e: SecurityException) {
                    // case where permission could not be taken
                    Toast.makeText(this, "Could not get permission for image.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}