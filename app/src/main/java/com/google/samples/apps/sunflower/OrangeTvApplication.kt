/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.sunflower

import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.DefaultRenderersFactory.ExtensionRendererMode
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.upstream.DataSource.Factory
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.*
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import com.google.samples.apps.sunflower.di.DaggerApplicationComponent
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import java.io.File
import java.io.IOException

/**
 * Placeholder application to facilitate overriding Application methods for debugging and testing.
 */
class OrangeTvApplication : DaggerApplication() {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.factory().create(applicationContext)
    }

    private lateinit var userAgent: String

    private val databaseProvider: DatabaseProvider by lazy { ExoDatabaseProvider(this) }

    private val downloadDirectory: File by lazy { getExternalFilesDir(null) ?: filesDir }

    private val downloadCache: Cache by lazy {
        val downloadContentDirectory = File(downloadDirectory, DOWNLOAD_CONTENT_DIRECTORY)
        SimpleCache(downloadContentDirectory, NoOpCacheEvictor(), databaseProvider)
    }

    private lateinit var downloadManager: DownloadManager
    private lateinit var downloadTracker: DownloadTracker

    override fun onCreate() {
        super.onCreate()
        userAgent = Util.getUserAgent(this, "ExoPlayerDemo")
    }

    /** Returns a [DataSource.Factory].  */
    fun buildDataSourceFactory(): Factory {
        val upstreamFactory = DefaultDataSourceFactory(this, buildHttpDataSourceFactory())
        return buildReadOnlyCacheDataSource(upstreamFactory, downloadCache)
    }

    /** Returns a [HttpDataSource.Factory].  */
    fun buildHttpDataSourceFactory(): HttpDataSource.Factory {
        return DefaultHttpDataSourceFactory(userAgent)
    }

    /** Returns whether extension renderers should be used.  */
    fun useExtensionRenderers(): Boolean {
        return "withExtensions" == BuildConfig.FLAVOR
    }

    fun buildRenderersFactory(preferExtensionRenderer: Boolean): RenderersFactory {
        @ExtensionRendererMode val extensionRendererMode = if (useExtensionRenderers()) if (preferExtensionRenderer) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        return DefaultRenderersFactory( /* context= */this)
                .setExtensionRendererMode(extensionRendererMode)
    }

    fun getDownloadManager(): DownloadManager? {
        initDownloadManager()
        return downloadManager
    }

    fun getDownloadTracker(): DownloadTracker? {
        initDownloadManager()
        return downloadTracker
    }

    @Synchronized
    private fun initDownloadManager() {
        if (downloadManager == null) {
            val downloadIndex = DefaultDownloadIndex(databaseProvider)
            upgradeActionFile(
                    DOWNLOAD_ACTION_FILE, downloadIndex,  /* addNewDownloadsAsCompleted= */false)
            upgradeActionFile(
                    DOWNLOAD_TRACKER_ACTION_FILE, downloadIndex,  /* addNewDownloadsAsCompleted= */true)
            val downloaderConstructorHelper = DownloaderConstructorHelper(downloadCache, buildHttpDataSourceFactory())
            downloadManager = DownloadManager(
                    this, downloadIndex, DefaultDownloaderFactory(downloaderConstructorHelper))
            downloadTracker = DownloadTracker( /* context= */this, buildDataSourceFactory(), downloadManager!!)
        }
    }

    private fun upgradeActionFile(
            fileName: String, downloadIndex: DefaultDownloadIndex, addNewDownloadsAsCompleted: Boolean) {
        try {
            ActionFileUpgradeUtil.upgradeAndDelete(
                    File(downloadDirectory, fileName),  /* downloadIdProvider= */
                    null,
                    downloadIndex,  /* deleteOnFailure= */
                    true,
                    addNewDownloadsAsCompleted)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to upgrade action file: $fileName", e)
        }
    }

    companion object {
        private const val TAG = "DemoApplication"
        private const val DOWNLOAD_ACTION_FILE = "actions"
        private const val DOWNLOAD_TRACKER_ACTION_FILE = "tracked_actions"
        private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"
        protected fun buildReadOnlyCacheDataSource(
                upstreamFactory: Factory?, cache: Cache?): CacheDataSourceFactory {
            return CacheDataSourceFactory(
                    cache,
                    upstreamFactory,
                    FileDataSourceFactory(),  /* cacheWriteDataSinkFactory= */
                    null,
                    CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,  /* eventListener= */
                    null)
        }
    }
}