package com.shijian.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** 定位工具：最后一次已知位置优先，失败则请求单次定位（仅美食搜索使用）；全部异常兜底。 */
object LocationUtils {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        runCatching {
            val fine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!fine && !coarse) return@runCatching null

            val ctx = context.applicationContext
            val lm = (ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
                ?: return@runCatching null

            val providers = buildList {
                if (fine) add(LocationManager.GPS_PROVIDER)
                if (coarse) add(LocationManager.NETWORK_PROVIDER)
                add(LocationManager.PASSIVE_PROVIDER)
            }.filter { p ->
                runCatching { lm.isProviderEnabled(p) }.getOrDefault(true)
            }

            // 1. 最后已知位置
            for (p in providers) {
                val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull()
                if (loc != null) return@runCatching loc
            }

            // 2. 请求单次更新（5 秒超时）
            val done = CompletableDeferred<Location?>()
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { if (!done.isCompleted) done.complete(location) }
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            for (p in providers.take(2)) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val executor = { r: Runnable? -> r?.run() }
                        lm.getCurrentLocation(p, null, { executor(it) }, { l -> listener.onLocationChanged(l) })
                    } else {
                        @Suppress("DEPRECATION")
                        lm.requestSingleUpdate(p, listener, Looper.getMainLooper())
                    }
                }
            }

            val result = withTimeoutOrNull(5000) { done.await() }
            runCatching { lm.removeUpdates(listener) }
            result
        }.getOrNull()
    }
}
