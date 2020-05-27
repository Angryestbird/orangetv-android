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

import android.app.Notification
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.PlatformScheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import com.google.android.exoplayer2.util.NotificationUtil
import com.google.android.exoplayer2.util.Util

/** A service for downloading media.  */
class DemoDownloadService : DownloadService(
        FOREGROUND_NOTIFICATION_ID,
        DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
        CHANNEL_ID,
        R.string.exo_download_notification_channel_name,  /* channelDescriptionResourceId= */
        0) {
    private var notificationHelper: DownloadNotificationHelper? = null
    override fun onCreate() {
        super.onCreate()
        notificationHelper = DownloadNotificationHelper(this, CHANNEL_ID)
    }

    override fun getDownloadManager(): DownloadManager {
        return (application as OrangeTvApplication).getDownloadManager()!!
    }

    override fun getScheduler(): PlatformScheduler? {
        return if (Util.SDK_INT >= 21) PlatformScheduler(this, JOB_ID) else null
    }

    override fun getForegroundNotification(downloads: List<Download>): Notification {
        return notificationHelper!!.buildProgressNotification(
                R.drawable.ic_download,  /* contentIntent= */null,  /* message= */null, downloads)
    }

    override fun onDownloadChanged(download: Download) {
        val notification: Notification
        notification = if (download.state == Download.STATE_COMPLETED) {
            notificationHelper!!.buildDownloadCompletedNotification(
                    R.drawable.ic_download_done,  /* contentIntent= */
                    null,
                    Util.fromUtf8Bytes(download.request.data))
        } else if (download.state == Download.STATE_FAILED) {
            notificationHelper!!.buildDownloadFailedNotification(
                    R.drawable.ic_download_done,  /* contentIntent= */
                    null,
                    Util.fromUtf8Bytes(download.request.data))
        } else {
            return
        }
        NotificationUtil.setNotification(this, nextNotificationId++, notification)
    }

    companion object {
        private const val CHANNEL_ID = "download_channel"
        private const val JOB_ID = 1
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private var nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1
    }

    init {
        nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1
    }
}