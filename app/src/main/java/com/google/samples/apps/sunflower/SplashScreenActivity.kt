/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.sunflower

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.samples.apps.sunflower.utilities.hideSupportActionbar

/**
 * a splash screen delays 3s
 */
class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSupportActionbar()
        setContentView(R.layout.activity_splash_screen)
        val task = splashScreenTast()
        task.execute()
    }

    inner class splashScreenTast : AsyncTask<Unit, Int, Unit>() {

        override fun doInBackground(vararg params: Unit?) {
            for (i in 3 downTo 1) {
                publishProgress(i)
                Thread.sleep(1000)
            }
        }

        override fun onProgressUpdate(vararg values: Int?) {
            Toast.makeText(this@SplashScreenActivity, "remaining ${values[0]}s", Toast.LENGTH_SHORT).show()
        }

        override fun onPostExecute(result: Unit?) {

            Intent(this@SplashScreenActivity, MainActivity::class.java).apply {
                startActivity(this)
            }
        }
    }
}
