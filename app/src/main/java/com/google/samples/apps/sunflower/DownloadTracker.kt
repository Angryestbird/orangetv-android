/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.DialogInterface.OnDismissListener
import android.net.Uri
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.offline.DownloadHelper.Callback
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.upstream.DataSource.Factory
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet

/** Tracks media that has been downloaded.  */
class DownloadTracker(
        context: Context, dataSourceFactory: Factory, downloadManager: DownloadManager) {
    /** Listens for changes in the tracked downloads.  */
    interface Listener {
        /** Called when the tracked downloads changed.  */
        fun onDownloadsChanged()
    }

    private val context: Context
    private val dataSourceFactory: Factory
    private val listeners: CopyOnWriteArraySet<Listener>
    private val downloads: HashMap<Uri, Download>
    private val downloadIndex: DownloadIndex
    private var startDownloadDialogHelper: StartDownloadDialogHelper? = null
    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener?) {
        listeners.remove(listener)
    }

    fun isDownloaded(uri: Uri?): Boolean {
        val download = downloads[uri]
        return download != null && download.state != Download.STATE_FAILED
    }

    fun getDownloadRequest(uri: Uri?): DownloadRequest? {
        val download = downloads[uri]
        return if (download != null && download.state != Download.STATE_FAILED) download.request else null
    }

    fun toggleDownload(
            fragmentManager: FragmentManager?,
            name: String?,
            uri: Uri,
            extension: String,
            renderersFactory: RenderersFactory) {
        val download = downloads[uri]
        if (download != null) {
            DownloadService.sendRemoveDownload(
                    context, DemoDownloadService::class.java, download.request.id,  /* foreground= */false)
        } else {
            if (startDownloadDialogHelper != null) {
                startDownloadDialogHelper!!.release()
            }
            startDownloadDialogHelper = StartDownloadDialogHelper(
                    fragmentManager, getDownloadHelper(uri, extension, renderersFactory), name)
        }
    }

    private fun loadDownloads() {
        try {
            downloadIndex.getDownloads().use { loadedDownloads ->
                while (loadedDownloads.moveToNext()) {
                    val download = loadedDownloads.download
                    downloads[download.request.uri] = download
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to query downloads", e)
        }
    }

    private fun getDownloadHelper(
            uri: Uri, extension: String, renderersFactory: RenderersFactory): DownloadHelper {
        val type = Util.inferContentType(uri, extension)
        return when (type) {
            C.TYPE_DASH -> DownloadHelper.forDash(uri, dataSourceFactory, renderersFactory)
            C.TYPE_SS -> DownloadHelper.forSmoothStreaming(uri, dataSourceFactory, renderersFactory)
            C.TYPE_HLS -> DownloadHelper.forHls(uri, dataSourceFactory, renderersFactory)
            C.TYPE_OTHER -> DownloadHelper.forProgressive(uri)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    private inner class DownloadManagerListener : DownloadManager.Listener {
        override fun onDownloadChanged(downloadManager: DownloadManager, download: Download) {
            downloads[download.request.uri] = download
            for (listener in listeners) {
                listener.onDownloadsChanged()
            }
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            downloads.remove(download.request.uri)
            for (listener in listeners) {
                listener.onDownloadsChanged()
            }
        }
    }

    private inner class StartDownloadDialogHelper(
            private val fragmentManager: FragmentManager?, private val downloadHelper: DownloadHelper, private val name: String?) : Callback, OnClickListener, OnDismissListener {
        private var trackSelectionDialog: TrackSelectionDialog? = null
        private var mappedTrackInfo: MappedTrackInfo? = null
        fun release() {
            downloadHelper.release()
            trackSelectionDialog?.dismiss()
        }

        // DownloadHelper.Callback implementation.
        override fun onPrepared(helper: DownloadHelper) {
            if (helper.periodCount == 0) {
                Log.d(TAG, "No periods found. Downloading entire stream.")
                startDownload()
                downloadHelper.release()
                return
            }
            mappedTrackInfo = downloadHelper.getMappedTrackInfo( /* periodIndex= */0)
            if (!TrackSelectionDialog.willHaveContent(mappedTrackInfo)) {
                Log.d(TAG, "No dialog content. Downloading entire stream.")
                startDownload()
                downloadHelper.release()
                return
            }
            trackSelectionDialog = TrackSelectionDialog.createForMappedTrackInfoAndParameters( /* titleId= */
                    R.string.exo_download_description,
                    mappedTrackInfo,  /* initialParameters= */
                    DownloadHelper.DEFAULT_TRACK_SELECTOR_PARAMETERS,  /* allowAdaptiveSelections =*/
                    false,  /* allowMultipleOverrides= */
                    true,  /* onClickListener= */
                    this,  /* onDismissListener= */
                    this)
            trackSelectionDialog?.show(fragmentManager!!,  /* tag= */null)
        }

        override fun onPrepareError(helper: DownloadHelper, e: IOException) {
            Toast.makeText(
                    context.applicationContext, R.string.download_start_error, Toast.LENGTH_LONG)
                    .show()
            Log.e(TAG, "Failed to start download", e)
        }

        // DialogInterface.OnClickListener implementation.
        override fun onClick(dialog: DialogInterface, which: Int) {
            for (periodIndex in 0 until downloadHelper.periodCount) {
                downloadHelper.clearTrackSelections(periodIndex)
                for (i in 0 until mappedTrackInfo!!.rendererCount) {
                    if (!trackSelectionDialog!!.getIsDisabled( /* rendererIndex= */i)) {
                        downloadHelper.addTrackSelectionForSingleRenderer(
                                periodIndex,  /* rendererIndex= */
                                i,
                                DownloadHelper.DEFAULT_TRACK_SELECTOR_PARAMETERS,
                                trackSelectionDialog!!.getOverrides( /* rendererIndex= */i))
                    }
                }
            }
            val downloadRequest = buildDownloadRequest()
            if (downloadRequest.streamKeys.isEmpty()) { // All tracks were deselected in the dialog. Don't start the download.
                return
            }
            startDownload(downloadRequest)
        }

        // DialogInterface.OnDismissListener implementation.
        override fun onDismiss(dialogInterface: DialogInterface) {
            trackSelectionDialog = null
            downloadHelper.release()
        }

        // Internal methods.
        private fun startDownload(downloadRequest: DownloadRequest = buildDownloadRequest()) {
            DownloadService.sendAddDownload(
                    context, DemoDownloadService::class.java, downloadRequest,  /* foreground= */false)
        }

        private fun buildDownloadRequest(): DownloadRequest {
            return downloadHelper.getDownloadRequest(Util.getUtf8Bytes(name))
        }

        init {
            downloadHelper.prepare(this)
        }
    }

    companion object {
        private const val TAG = "DownloadTracker"
    }

    init {
        this.context = context.applicationContext
        this.dataSourceFactory = dataSourceFactory
        listeners = CopyOnWriteArraySet()
        downloads = HashMap()
        downloadIndex = downloadManager.downloadIndex
        downloadManager.addListener(DownloadManagerListener())
        loadDownloads()
    }
}