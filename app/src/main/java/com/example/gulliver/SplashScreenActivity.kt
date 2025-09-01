package com.example.gulliver

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashScreenActivity : AppCompatActivity() {

    private val SPLASH_DISPLAY_LENGTH = 2000L
    private val ANIMATION_DELAY = 100L

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Handle the splash screen transition.

        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen_layout)

        val planeIcon: ImageView = findViewById(R.id.iv_plane_icon)

        splashScreen.setKeepOnScreenCondition { false }

        // Post a delayed action to start the animation
        Handler(Looper.getMainLooper()).postDelayed({
            val slideInAnimation = AnimationUtils.loadAnimation(this, R.anim.plane_slide_in)
            planeIcon.startAnimation(slideInAnimation)
        }, ANIMATION_DELAY) // Start animation after a short delay

        Handler(Looper.getMainLooper()).postDelayed({

            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
            finish() // Close the splash activity so the user can't go back to it
        }, SPLASH_DISPLAY_LENGTH)
    }
}