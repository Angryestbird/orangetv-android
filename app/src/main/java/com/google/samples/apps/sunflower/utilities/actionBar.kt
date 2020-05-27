package com.google.samples.apps.sunflower.utilities

import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

internal fun AppCompatActivity.hideSupportActionbar() {
    // If the Android version is lower than Jellybean, use this call to hide
    // the status bar.
    if (Build.VERSION.SDK_INT < 16) {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
    } else {
        // Hide the status bar.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        supportActionBar?.hide()
    }
}

internal fun AppCompatActivity.restoreSystemUI() {
    window?.decorView?.apply {
        // Calling setSystemUiVisibility() with a value of 0 clears
        // all flags.
        systemUiVisibility = 0
    }
}
