package com.shijian.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** 高德地图 App 跳转（Q2）；所有 Intent 带 FLAG_ACTIVITY_NEW_TASK，可在非 Activity context 调用。 */
object AmapIntents {

    private const val AMAP_URI = "amapuri://route/plan/?sid=BGVIS1&dlat=%f&dlon=%f&dname=%s&dev=0&t=0"
    private const val AMAP_URI_ALT = "androidamap://navi?sourceApplication=shijian&lat=%f&lon=%f&poiname=%s"
    private const val AMAP_WEB = "https://uri.amap.com/navigation?to=%f,%f,%s"

    /**
     * 打开高德地图查看/导航该位置
     * 优先 amapuri → androidamap → 网页降级；任何异常不抛。
     */
    fun openNavigation(context: Context, lat: Double, lng: Double, name: String) {
        runCatching {
            val encoded = Uri.encode(name)
            val uris = listOf(
                String.format(AMAP_URI, lat, lng, encoded),
                String.format(AMAP_URI_ALT, lat, lng, encoded),
                String.format(AMAP_WEB, lng, lat, encoded)
            )
            val pm = context.packageManager
            for (u in uris) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(u))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    if (intent.resolveActivity(pm) != null) {
                        context.startActivity(intent)
                        return
                    }
                } catch (_: Exception) {
                }
            }
            // 最后的兜底：即使用系统浏览器打开网页版
            val last = uris.lastOrNull() ?: return
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(last))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) { }
        }
    }
}
