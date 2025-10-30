package com.sora.android.data.repository

import android.util.Log
import androidx.paging.PagingData
import com.sora.android.core.network.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

suspend fun <T : Any> offlineFirstPaging(
    tag: String,
    networkMonitor: NetworkMonitor,
    getCached: suspend () -> List<T>,
    fetchFromApi: suspend () -> List<T>?
): Flow<PagingData<T>> = flow {
    Log.d(tag, "AJUDANTE: offlineFirstPaging INICIADO")

    val cached = getCached()
    Log.d(tag, "AJUDANTE: getCached() retornou ${cached.size} items")

    val isOffline = !networkMonitor.isCurrentlyConnected()
    Log.d(tag, "AJUDANTE: Verificação de rede - isOffline=$isOffline")

    if (isOffline) {
        Log.d(tag, "AJUDANTE: OFFLINE - emitindo ${cached.size} cached items")
        emit(PagingData.from(cached))
        Log.d(tag, "AJUDANTE: OFFLINE - emitir COMPLETO, returning")
        return@flow
    }

    if (cached.isNotEmpty()) {
        Log.d(tag, "AJUDANTE: ONLINE + cache - emitindo ${cached.size} items first")
        emit(PagingData.from(cached))
        Log.d(tag, "AJUDANTE: Cache emitir COMPLETO")
    } else {
        Log.d(tag, "AJUDANTE: ONLINE + NO cache - waiting for API")
    }

    try {
        Log.d(tag, "AJUDANTE: Calling fetchFromApi()")
        val apiResult = fetchFromApi()
        Log.d(tag, "AJUDANTE: fetchFromApi() retornou ${apiResult?.size ?: "null"} items")

        if (apiResult != null) {
            Log.d(tag, "AJUDANTE: API success - emitindo ${apiResult.size} items")
            emit(PagingData.from(apiResult))
            Log.d(tag, "AJUDANTE: API emitir COMPLETO")
        } else if (cached.isEmpty()) {
            Log.d(tag, "AJUDANTE: API null + no cache - emitindo empty")
            emit(PagingData.empty())
            Log.d(tag, "AJUDANTE: Empty emitir COMPLETO")
        } else {
            Log.d(tag, "AJUDANTE: API null but cache alpronto emitted - done")
        }
    } catch (e: Exception) {
        Log.e(tag, "AJUDANTE: Exception - ${e.message}", e)
        if (cached.isEmpty()) {
            Log.d(tag, "AJUDANTE: Exception + no cache - emitindo empty")
            emit(PagingData.empty())
        }
    }

    Log.d(tag, "AJUDANTE: offlineFirstPaging FINISHED")
}

suspend fun <T> offlineFirstData(
    tag: String,
    networkMonitor: NetworkMonitor,
    getCached: suspend () -> T,
    fetchFromApi: suspend () -> T?
): Flow<T> = flow {
    val cached = getCached()
    Log.d(tag, "Cache loaded")

    if (!networkMonitor.isCurrentlyConnected()) {
        Log.d(tag, "Offline: Emit cache and stop")
        emit(cached)
        return@flow
    }

    emit(cached)

    try {
        val apiResult = fetchFromApi()
        if (apiResult != null) {
            Log.d(tag, "API success")
            emit(apiResult)
        }
    } catch (e: Exception) {
        Log.d(tag, "Exceção: ${e.message}")
    }
}
