package com.shijian.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** 定位工具：最后一次已知位置优先，失败则请求单次定位（仅美食搜索使用） */
object LocationUtils {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return@withContext null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        // 1. 最后已知位置
        for (p in providers) {
            runCatching { lm.getLastKnownLocation(p) }.getOrNull()?.let { return@withContext it }
        }

        // 2. 请求单次更新（5 秒超时）
        val done = CompletableDeferred<Location?>()
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!done.isCompleted) done.complete(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        for (p in providers) {
            runCatching { lm.requestSingleUpdate(p, listener, Looper.getMainLooper()) }
        }
        withTimeoutOrNull(5000) { done.await() } ?: run {
            if (!done.isCompleted) done.complete(null)
            null
        }
    }
}
