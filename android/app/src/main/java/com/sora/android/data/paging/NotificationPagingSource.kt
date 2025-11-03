package com.sora.android.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sora.android.data.remote.ApiService
import com.sora.android.domain.model.NotificationModel

class NotificationPagingSource(
    private val apiService: ApiService,
    private val unreadOnly: Boolean,
    private val type: String?
) : PagingSource<Int, NotificationModel>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NotificationModel> {
        return try {
            val page = params.key ?: 0
            Log.d("SORA_NOTIFICATION_PAGING", "Loading page $page")

            val response = apiService.getNotifications(unreadOnly, type, page, params.loadSize)

            if (response.isSuccessful) {
                val notificationsResponse = response.body()
                val notifications = notificationsResponse?.notifications ?: emptyList()

                Log.d("SORA_NOTIFICATION_PAGING", "Loaded ${notifications.size} notifications for page $page")

                LoadResult.Page(
                    data = notifications,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (notifications.isEmpty()) null else page + 1
                )
            } else {
                Log.e("SORA_NOTIFICATION_PAGING", "Error loading notifications: ${response.code()}")
                LoadResult.Error(Exception("Error loading notifications: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("SORA_NOTIFICATION_PAGING", "Exception loading notifications", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, NotificationModel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
