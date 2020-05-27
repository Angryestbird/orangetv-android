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

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.C.ContentType
import com.google.android.exoplayer2.drm.*
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.source.ads.AdsLoader
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.source.ads.AdsMediaSource.MediaSourceFactory
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.Parameters
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.ui.DebugTextViewHelper
import com.google.android.exoplayer2.ui.PlayerControlView.VisibilityListener
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.spherical.SphericalSurfaceView
import com.google.android.exoplayer2.upstream.DataSource.Factory
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.*

/** An activity that plays media using [SimpleExoPlayer].  */
class PlayerActivity : AppCompatActivity(), OnClickListener, PlaybackPreparer, VisibilityListener {
    companion object {
        const val DRM_SCHEME_EXTRA = "drm_scheme"
        const val DRM_LICENSE_URL_EXTRA = "drm_license_url"
        const val DRM_KEY_REQUEST_PROPERTIES_EXTRA = "drm_key_request_properties"
        const val DRM_MULTI_SESSION_EXTRA = "drm_multi_session"
        const val PREFER_EXTENSION_DECODERS_EXTRA = "prefer_extension_decoders"
        const val ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW"
        const val EXTENSION_EXTRA = "extension"
        const val ACTION_VIEW_LIST = "com.google.android.exoplayer.demo.action.VIEW_LIST"
        const val URI_LIST_EXTRA = "uri_list"
        const val EXTENSION_LIST_EXTRA = "extension_list"
        const val AD_TAG_URI_EXTRA = "ad_tag_uri"
        const val ABR_ALGORITHM_EXTRA = "abr_algorithm"
        const val ABR_ALGORITHM_DEFAULT = "default"
        const val ABR_ALGORITHM_RANDOM = "random"
        const val SPHERICAL_STEREO_MODE_EXTRA = "spherical_stereo_mode"
        const val SPHERICAL_STEREO_MODE_MONO = "mono"
        const val SPHERICAL_STEREO_MODE_TOP_BOTTOM = "top_bottom"
        const val SPHERICAL_STEREO_MODE_LEFT_RIGHT = "left_right"
        // For backwards compatibility only.
        private const val DRM_SCHEME_UUID_EXTRA = "drm_scheme_uuid"
        // Saved instance state keys.
        private const val KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters"
        private const val KEY_WINDOW = "window"
        private const val KEY_POSITION = "position"
        private const val KEY_AUTO_PLAY = "auto_play"
        private var DEFAULT_COOKIE_MANAGER: CookieManager? = null
        private fun isBehindLiveWindow(e: ExoPlaybackException): Boolean {
            if (e.type != ExoPlaybackException.TYPE_SOURCE) {
                return false
            }
            var cause: Throwable? = e.sourceException
            while (cause != null) {
                if (cause is BehindLiveWindowException) {
                    return true
                }
                cause = cause.cause
            }
            return false
        }

        init {
            DEFAULT_COOKIE_MANAGER = CookieManager()
            DEFAULT_COOKIE_MANAGER?.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        }
    }

    private var playerView: PlayerView? = null
    private var debugRootView: LinearLayout? = null
    private var selectTracksButton: Button? = null
    private var debugTextView: TextView? = null
    private var isShowingTrackSelectionDialog = false
    private var dataSourceFactory: Factory? = null
    private var player: SimpleExoPlayer? = null
    private var mediaDrm: FrameworkMediaDrm? = null
    private var mediaSource: MediaSource? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var trackSelectorParameters: Parameters? = null
    private var debugViewHelper: DebugTextViewHelper? = null
    private var lastSeenTrackGroupArray: TrackGroupArray? = null
    private var startAutoPlay = false
    private var startWindow = 0
    private var startPosition: Long = 0
    // Fields used only for ad playback. The ads loader is loaded via reflection.
    private var adsLoader: AdsLoader? = null
    private var loadedAdTagUri: Uri? = null
    // Activity lifecycle
    public override fun onCreate(savedInstanceState: Bundle?) {
        val sphericalStereoMode = intent.getStringExtra(SPHERICAL_STEREO_MODE_EXTRA)
        if (sphericalStereoMode != null) {
            setTheme(R.style.PlayerTheme_Spherical)
        }
        super.onCreate(savedInstanceState)
        dataSourceFactory = buildDataSourceFactory()
        if (CookieHandler.getDefault() !== DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER)
        }
        setContentView(R.layout.player_activity)
        debugRootView = findViewById(R.id.controls_root)
        debugTextView = findViewById(R.id.debug_text_view)
        selectTracksButton = findViewById(R.id.select_tracks_button)
        selectTracksButton?.setOnClickListener(this)
        playerView = findViewById(R.id.player_view)
        playerView!!.setControllerVisibilityListener(this)
        playerView!!.setErrorMessageProvider(PlayerErrorMessageProvider())
        playerView!!.requestFocus()
        if (sphericalStereoMode != null) {
            val stereoMode: Int
            stereoMode = if (SPHERICAL_STEREO_MODE_MONO == sphericalStereoMode) {
                C.STEREO_MODE_MONO
            } else if (SPHERICAL_STEREO_MODE_TOP_BOTTOM == sphericalStereoMode) {
                C.STEREO_MODE_TOP_BOTTOM
            } else if (SPHERICAL_STEREO_MODE_LEFT_RIGHT == sphericalStereoMode) {
                C.STEREO_MODE_LEFT_RIGHT
            } else {
                showToast(R.string.error_unrecognized_stereo_mode)
                finish()
                return
            }
            (playerView?.getVideoSurfaceView() as SphericalSurfaceView).setDefaultStereoMode(stereoMode)
        }
        if (savedInstanceState != null) {
            trackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS)
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY)
            startWindow = savedInstanceState.getInt(KEY_WINDOW)
            startPosition = savedInstanceState.getLong(KEY_POSITION)
        } else {
            trackSelectorParameters = ParametersBuilder().build()
            clearStartPosition()
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        releasePlayer()
        releaseAdsLoader()
        clearStartPosition()
        setIntent(intent)
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
            if (playerView != null) {
                playerView!!.onResume()
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer()
            if (playerView != null) {
                playerView!!.onResume()
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            if (playerView != null) {
                playerView!!.onPause()
            }
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            if (playerView != null) {
                playerView!!.onPause()
            }
            releasePlayer()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        releaseAdsLoader()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (grantResults.size == 0) { // Empty results are triggered if a permission is requested while another request was already
// pending and can be safely ignored in this case.
            return
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializePlayer()
        } else {
            showToast(R.string.storage_permission_denied)
            finish()
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateTrackSelectorParameters()
        updateStartPosition()
        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters)
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay)
        outState.putInt(KEY_WINDOW, startWindow)
        outState.putLong(KEY_POSITION, startPosition)
    }

    // Activity input
    override fun dispatchKeyEvent(event: KeyEvent): Boolean { // See whether the player view wants to handle media or DPAD keys events.
        return playerView!!.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
    }

    // OnClickListener methods
    override fun onClick(view: View) {
        if (view === selectTracksButton && !isShowingTrackSelectionDialog
                && TrackSelectionDialog.willHaveContent(trackSelector)) {
            isShowingTrackSelectionDialog = true
            val trackSelectionDialog = TrackSelectionDialog.createForTrackSelector(
                    trackSelector  /* onDismissListener= */
            ) { dismissedDialog: DialogInterface? -> isShowingTrackSelectionDialog = false }
            trackSelectionDialog.show(supportFragmentManager,  /* tag= */null)
        }
    }

    // PlaybackControlView.PlaybackPreparer implementation
    override fun preparePlayback() {
        player!!.retry()
    }

    // PlaybackControlView.VisibilityListener implementation
    override fun onVisibilityChange(visibility: Int) {
        debugRootView!!.visibility = visibility
    }

    // Internal methods
    private fun initializePlayer() {
        if (player == null) {
            val intent = intent
            val action = intent.action
            val uris: Array<Uri?>
            var extensions: Array<String?>?
            if (ACTION_VIEW == action) {
                uris = arrayOf(intent.data)
                extensions = arrayOf(intent.getStringExtra(EXTENSION_EXTRA))
            } else if (ACTION_VIEW_LIST == action) {
                val uriStrings = intent.getStringArrayExtra(URI_LIST_EXTRA)
                uris = arrayOfNulls(uriStrings.size)
                for (i in uriStrings.indices) {
                    uris[i] = Uri.parse(uriStrings[i])
                }
                extensions = intent.getStringArrayExtra(EXTENSION_LIST_EXTRA)
                if (extensions == null) {
                    extensions = arrayOfNulls(uriStrings.size)
                }
            } else {
                showToast(getString(R.string.unexpected_intent_action, action))
                finish()
                return
            }
            if (!Util.checkCleartextTrafficPermitted(*uris)) {
                showToast(R.string.error_cleartext_not_permitted)
                return
            }
            if (Util.maybeRequestReadExternalStoragePermission( /* activity= */this, *uris)) { // The player will be reinitialized if the permission is granted.
                return
            }
            var drmSessionManager: DefaultDrmSessionManager<FrameworkMediaCrypto?>? = null
            if (intent.hasExtra(DRM_SCHEME_EXTRA) || intent.hasExtra(DRM_SCHEME_UUID_EXTRA)) {
                val drmLicenseUrl = intent.getStringExtra(DRM_LICENSE_URL_EXTRA)
                val keyRequestPropertiesArray = intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES_EXTRA)
                val multiSession = intent.getBooleanExtra(DRM_MULTI_SESSION_EXTRA, false)
                var errorStringId = R.string.error_drm_unknown
                if (Util.SDK_INT < 18) {
                    errorStringId = R.string.error_drm_not_supported
                } else {
                    try {
                        val drmSchemeExtra = if (intent.hasExtra(DRM_SCHEME_EXTRA)) DRM_SCHEME_EXTRA else DRM_SCHEME_UUID_EXTRA
                        val drmSchemeUuid = Util.getDrmUuid(intent.getStringExtra(drmSchemeExtra))
                        if (drmSchemeUuid == null) {
                            errorStringId = R.string.error_drm_unsupported_scheme
                        } else {
                            drmSessionManager = buildDrmSessionManagerV18(
                                    drmSchemeUuid, drmLicenseUrl, keyRequestPropertiesArray, multiSession)
                        }
                    } catch (e: UnsupportedDrmException) {
                        errorStringId = if (e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME) R.string.error_drm_unsupported_scheme else R.string.error_drm_unknown
                    }
                }
                if (drmSessionManager == null) {
                    showToast(errorStringId)
                    finish()
                    return
                }
            }
            val trackSelectionFactory: TrackSelection.Factory
            val abrAlgorithm = intent.getStringExtra(ABR_ALGORITHM_EXTRA)
            trackSelectionFactory = if (abrAlgorithm == null || ABR_ALGORITHM_DEFAULT == abrAlgorithm) {
                AdaptiveTrackSelection.Factory()
            } else if (ABR_ALGORITHM_RANDOM == abrAlgorithm) {
                RandomTrackSelection.Factory()
            } else {
                showToast(R.string.error_unrecognized_abr_algorithm)
                finish()
                return
            }
            val preferExtensionDecoders = intent.getBooleanExtra(PREFER_EXTENSION_DECODERS_EXTRA, false)
            val renderersFactory = (application as OrangeTvApplication).buildRenderersFactory(preferExtensionDecoders)
            trackSelector = DefaultTrackSelector(trackSelectionFactory)
            trackSelector!!.parameters = trackSelectorParameters
            lastSeenTrackGroupArray = null
            player = ExoPlayerFactory.newSimpleInstance( /* context= */
                    this, renderersFactory, trackSelector, drmSessionManager)
            player!!.addListener(PlayerEventListener())
            player!!.setPlayWhenReady(startAutoPlay)
            player!!.addAnalyticsListener(EventLogger(trackSelector))
            playerView!!.setPlayer(player)
            playerView!!.setPlaybackPreparer(this)
            debugViewHelper = DebugTextViewHelper(player, debugTextView)
            debugViewHelper!!.start()
            val mediaSources = arrayOfNulls<MediaSource>(uris.size)
            for (i in uris.indices) {
                mediaSources[i] = buildMediaSource(uris[i], extensions[i])
            }
            mediaSource = if (mediaSources.size == 1) mediaSources[0] else ConcatenatingMediaSource(*mediaSources)
            val adTagUriString = intent.getStringExtra(AD_TAG_URI_EXTRA)
            if (adTagUriString != null) {
                val adTagUri = Uri.parse(adTagUriString)
                if (adTagUri != loadedAdTagUri) {
                    releaseAdsLoader()
                    loadedAdTagUri = adTagUri
                }
                val adsMediaSource = createAdsMediaSource(mediaSource, Uri.parse(adTagUriString))
                if (adsMediaSource != null) {
                    mediaSource = adsMediaSource
                } else {
                    showToast(R.string.ima_not_loaded)
                }
            } else {
                releaseAdsLoader()
            }
        }
        val haveStartPosition = startWindow != C.INDEX_UNSET
        if (haveStartPosition) {
            player!!.seekTo(startWindow, startPosition)
        }
        player!!.prepare(mediaSource, !haveStartPosition, false)
        updateButtonVisibility()
    }

    private fun buildMediaSource(uri: Uri?, overrideExtension: String? = null): MediaSource {
        val downloadRequest = (application as OrangeTvApplication).getDownloadTracker()!!.getDownloadRequest(uri)
        if (downloadRequest != null) {
            return DownloadHelper.createMediaSource(downloadRequest, dataSourceFactory)
        }
        @ContentType val type = Util.inferContentType(uri, overrideExtension)
        return when (type) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    @Throws(UnsupportedDrmException::class)
    private fun buildDrmSessionManagerV18(
            uuid: UUID, licenseUrl: String, keyRequestPropertiesArray: Array<String>?, multiSession: Boolean): DefaultDrmSessionManager<FrameworkMediaCrypto?> {
        val licenseDataSourceFactory = (application as OrangeTvApplication).buildHttpDataSourceFactory()
        val drmCallback = HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory)
        if (keyRequestPropertiesArray != null) {
            var i = 0
            while (i < keyRequestPropertiesArray.size - 1) {
                drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
                        keyRequestPropertiesArray[i + 1])
                i += 2
            }
        }
        releaseMediaDrm()
        mediaDrm = FrameworkMediaDrm.newInstance(uuid)
        return DefaultDrmSessionManager(uuid, mediaDrm, drmCallback, null, multiSession)
    }

    private fun releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters()
            updateStartPosition()
            debugViewHelper!!.stop()
            debugViewHelper = null
            player!!.release()
            player = null
            mediaSource = null
            trackSelector = null
        }
        if (adsLoader != null) {
            adsLoader!!.setPlayer(null)
        }
        releaseMediaDrm()
    }

    private fun releaseMediaDrm() {
        if (mediaDrm != null) {
            mediaDrm!!.release()
            mediaDrm = null
        }
    }

    private fun releaseAdsLoader() {
        if (adsLoader != null) {
            adsLoader!!.release()
            adsLoader = null
            loadedAdTagUri = null
            playerView!!.overlayFrameLayout!!.removeAllViews()
        }
    }

    private fun updateTrackSelectorParameters() {
        if (trackSelector != null) {
            trackSelectorParameters = trackSelector!!.parameters
        }
    }

    private fun updateStartPosition() {
        if (player != null) {
            startAutoPlay = player!!.playWhenReady
            startWindow = player!!.currentWindowIndex
            startPosition = Math.max(0, player!!.contentPosition)
        }
    }

    private fun clearStartPosition() {
        startAutoPlay = true
        startWindow = C.INDEX_UNSET
        startPosition = C.TIME_UNSET
    }

    /** Returns a new DataSource factory.  */
    private fun buildDataSourceFactory(): Factory {
        return (application as OrangeTvApplication).buildDataSourceFactory()
    }

    /** Returns an ads media source, reusing the ads loader if one exists.  */
    private fun createAdsMediaSource(mediaSource: MediaSource?, adTagUri: Uri): MediaSource? { // Load the extension source using reflection so the demo app doesn't have to depend on it.
// The ads loader is reused for multiple playbacks, so that ad playback can resume.
        return try {
            val loaderClass = Class.forName("com.google.android.exoplayer2.ext.ima.ImaAdsLoader")
            if (adsLoader == null) { // Full class names used so the LINT.IfChange rule triggers should any of the classes move.
// LINT.IfChange
                val loaderConstructor = loaderClass
                        .asSubclass(AdsLoader::class.java)
                        .getConstructor(Context::class.java, Uri::class.java)
                // LINT.ThenChange(../../../../../../../../proguard-rules.txt)
                adsLoader = loaderConstructor.newInstance(this, adTagUri)
            }
            adsLoader!!.setPlayer(player)
            val adMediaSourceFactory: MediaSourceFactory = object : MediaSourceFactory {
                override fun createMediaSource(uri: Uri): MediaSource {
                    return buildMediaSource(uri)
                }

                override fun getSupportedTypes(): IntArray {
                    return intArrayOf(C.TYPE_DASH, C.TYPE_SS, C.TYPE_HLS, C.TYPE_OTHER)
                }
            }
            AdsMediaSource(mediaSource, adMediaSourceFactory, adsLoader, playerView)
        } catch (e: ClassNotFoundException) { // IMA extension not loaded.
            null
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    // User controls
    private fun updateButtonVisibility() {
        selectTracksButton!!.isEnabled = player != null && TrackSelectionDialog.willHaveContent(trackSelector)
    }

    private fun showControls() {
        debugRootView!!.visibility = View.VISIBLE
    }

    private fun showToast(messageId: Int) {
        showToast(getString(messageId))
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                showControls()
            }
            updateButtonVisibility()
        }

        override fun onPlayerError(e: ExoPlaybackException) {
            if (isBehindLiveWindow(e)) {
                clearStartPosition()
                initializePlayer()
            } else {
                updateButtonVisibility()
                showControls()
            }
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
            updateButtonVisibility()
            if (trackGroups !== lastSeenTrackGroupArray) {
                val mappedTrackInfo = trackSelector!!.currentMappedTrackInfo
                if (mappedTrackInfo != null) {
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_video)
                    }
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_audio)
                    }
                }
                lastSeenTrackGroupArray = trackGroups
            }
        }
    }

    private inner class PlayerErrorMessageProvider : ErrorMessageProvider<ExoPlaybackException> {
        override fun getErrorMessage(e: ExoPlaybackException): android.util.Pair<Int, String>? {
            var errorString = getString(R.string.error_generic)
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                val cause = e.rendererException
                if (cause is DecoderInitializationException) { // Special case for decoder initialization failures.
                    val decoderInitializationException = cause
                    errorString = if (decoderInitializationException.decoderName == null) {
                        if (decoderInitializationException.cause is DecoderQueryException) {
                            getString(R.string.error_querying_decoders)
                        } else if (decoderInitializationException.secureDecoderRequired) {
                            getString(
                                    R.string.error_no_secure_decoder, decoderInitializationException.mimeType)
                        } else {
                            getString(R.string.error_no_decoder, decoderInitializationException.mimeType)
                        }
                    } else {
                        getString(
                                R.string.error_instantiating_decoder,
                                decoderInitializationException.decoderName)
                    }
                }
            }
            return android.util.Pair.create<Int, String>(0, errorString)
        }
    }
}