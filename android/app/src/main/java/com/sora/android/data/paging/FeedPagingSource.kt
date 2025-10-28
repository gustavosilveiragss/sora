package com.sora.android.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sora.android.data.remote.ApiService
import com.sora.android.domain.model.PostModel

class FeedPagingSource(
    private val apiService: ApiService
) : PagingSource<Int, PostModel>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PostModel> {
        val page = params.key ?: 0

        return try {
            val response = apiService.getFeed(page = page, size = params.loadSize)

            if (response.isSuccessful) {
                val pagedResponse = response.body()
                val posts = pagedResponse?.content ?: emptyList()
                val isLastPage = pagedResponse?.last ?: true

                LoadResult.Page(
                    data = posts,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (isLastPage) null else page + 1
                )
            } else {
                LoadResult.Error(Exception("Error loading feed: ${response.code()}"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PostModel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
