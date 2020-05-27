package com.google.samples.apps.sunflower.utilities

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.samples.apps.sunflower.R

internal fun getHostUrl(context: Context): String {

    val host = PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.pref_host), "10.0.2.2")
    val hostPort = PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.pref_host_port), "8080")
    val protocol = PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.pref_host_protocol), "http")
    return "${protocol}://${host}:${hostPort}/"
}