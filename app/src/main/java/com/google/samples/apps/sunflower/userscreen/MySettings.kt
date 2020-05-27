package com.google.samples.apps.sunflower.userscreen

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.samples.apps.sunflower.R

class MySettings : PreferenceFragmentCompat() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        (requireActivity() as AppCompatActivity).apply {
            setupActionBarWithNavController(findNavController())
            (parentFragment?.parentFragment as User).toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val portNumberPreference: EditTextPreference? = findPreference(getString(R.string.pref_host_port))

        portNumberPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        val hostPreference: EditTextPreference? = findPreference(getString(R.string.pref_host))
        hostPreference?.setOnPreferenceChangeListener { _, newValue ->
            (newValue as? String)?.isNotBlank() ?: false
        }

        val aboutPreference: Preference? = findPreference(getString(R.string.pref_about))
        aboutPreference?.setOnPreferenceClickListener {
            // 1. Instantiate an AlertDialog.Builder with its constructor
            val builder: AlertDialog.Builder? = activity?.let {
                AlertDialog.Builder(it)
            }

            // 2. Chain together various setter methods to set the dialog characteristics
            builder?.setTitle(R.string.welcome)
                    ?.setItems(R.array.about_info, null)

            // 3. Get the AlertDialog from create()
            val dialog: AlertDialog? = builder?.create()
            dialog?.show()
            return@setOnPreferenceClickListener true
        }
    }
}