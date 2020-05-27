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

package com.google.samples.apps.sunflower.data

import androidx.annotation.MainThread
import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import com.google.samples.apps.sunflower.api.Api
import com.google.samples.apps.sunflower.utilities.createStatusLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor

/**
 * This boundary callback gets notified when user reaches to the edges of the list such that the
 * database cannot provide any more data.
 * <p>
 * The boundary callback might be called multiple times for the same direction so it does its own
 * rate limiting using the PagingRequestHelper class.
 */
class MovieBoundaryCallback(
        private val subredditName: String,
        private val webservice: Api,
        private val handleResponse: (String, List<Movie>?) -> Unit,
        private val ioExecutor: Executor,
        private val db: AppDatabase)
    : PagedList.BoundaryCallback<Movie>() {

    val helper = PagingRequestHelper(ioExecutor)
    val networkState = helper.createStatusLiveData()

    companion object {
        const val NETWORK_PAGE_SIZE = 2
    }

    /**
     * Database returned 0 items. We should query the backend for more items.
     */
    @MainThread
    override fun onZeroItemsLoaded() {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            webservice.getTop().enqueue(createWebserviceCallback(it))
        }
    }

    /**
     * User reached to the end of the list.
     */
    @MainThread
    override fun onItemAtEndLoaded(itemAtEnd: Movie) {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            ioExecutor.execute {
                val nextLoadPage = db.movieDao().getPlantsCount() / NETWORK_PAGE_SIZE
                if (nextLoadPage < 3) {
                    webservice.getTopAfter(nextLoadPage).enqueue(createWebserviceCallback(it))
                }
            }
        }
    }

    /**
     * every time it gets new items, boundary callback simply inserts them into the database and
     * paging library takes care of refreshing the list if necessary.
     */
    private fun insertItemsIntoDb(
            response: Response<List<Movie>>,
            it: PagingRequestHelper.Request.Callback) {
        ioExecutor.execute {
            handleResponse(subredditName, response.body())
            it.recordSuccess()
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: Movie) {
        // ignored, since we only ever append to what's in the DB
    }

    private fun createWebserviceCallback(it: PagingRequestHelper.Request.Callback)
            : Callback<List<Movie>> {
        return object : Callback<List<Movie>> {
            override fun onFailure(
                    call: Call<List<Movie>>,
                    t: Throwable) {
                it.recordFailure(t)
            }

            override fun onResponse(
                    call: Call<List<Movie>>,
                    response: Response<List<Movie>>) {
                insertItemsIntoDb(response, it)
            }
        }
    }
}