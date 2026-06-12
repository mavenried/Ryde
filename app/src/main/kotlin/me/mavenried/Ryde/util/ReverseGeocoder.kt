package me.mavenried.Ryde.util

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object ReverseGeocoder {
    suspend fun getPlaceName(context: Context, lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Tiramisu has an async version, but since we are already in withContext(IO), 
                // we can use a callback or wait. For simplicity here, we'll use the blocking one
                // which is still supported.
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                addresses?.firstOrNull()?.let { 
                    it.locality ?: it.subAdminArea ?: it.adminArea 
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                addresses?.firstOrNull()?.let { 
                    it.locality ?: it.subAdminArea ?: it.adminArea 
                }
            }
        } catch (e: Exception) {
            FileLogger.logError(context, "Geocoding failed", e)
            null
        }
    }
}
