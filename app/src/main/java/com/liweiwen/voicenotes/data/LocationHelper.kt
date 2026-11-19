package com.liweiwen.voicenotes.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

/**
 * 地理标记：优先读 GPS/网络最后一次定位，不主动耗电请求新定位。
 * 拿到后把经纬度格式化为短字符串（如 "31.23,121.47"）附在记录上。
 */
object LocationHelper {
    data class Place(val latitude: Double, val longitude: Double, val label: String)

    /** 取最后一次已知位置；没有定位权限或没有缓存时返回 null。 */
    fun lastPlace(context: Context): Place? {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        var best: Location? = null
        runCatching {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            for (p in providers) {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.time > best!!.time) best = loc
            }
        }
        val loc = best ?: return null
        val label = "%.2f, %.2f".format(loc.latitude, loc.longitude)
        return Place(loc.latitude, loc.longitude, label)
    }
}
