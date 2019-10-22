package com.ammase.googleplace.picker

import android.app.IntentService
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.io.IOException

class AddressFinderService : IntentService("") {
    private var receiver : ResultReceiver? = null

    override fun onHandleIntent(intent: Intent?) {
        intent?.also {
            receiver = it.getParcelableExtra("receiver")
            val location = it.getParcelableExtra<LatLng>("location")
            if(location == null || receiver == null) return

            var addresses: List<Address> = emptyList()
            var errorMessage : String? = null
            Geocoder(this).also {
                try {
                    addresses = it.getFromLocation(location.latitude, location.longitude, 1)
                } catch (ioException: IOException) {
                    Log.e("TEST", ioException.message)
                    errorMessage = ioException.message ?: "Error"
                } catch (illegalArgumentException: IllegalArgumentException) {
                    Log.e("TEST", illegalArgumentException.message)
                    errorMessage = illegalArgumentException.message ?: "Error"
                }
            }

            if (addresses.isEmpty()) {
                if (errorMessage.isNullOrEmpty()) {
                    errorMessage = "Address not found"
                }
                receiveResult(AddressFinderService.RESULT_FAIL, errorMessage!!)
            } else {
                val address = addresses[0]

                val addressFragments = with(address){
                    (0..address.maxAddressLineIndex).map { getAddressLine(it) }
                }

                var detailedLocation = addressFragments.joinToString( "\n")

                if (detailedLocation.isEmpty())
                    detailedLocation = "Pinned location"

                receiveResult(AddressFinderService.RESULT_SUCCESS, detailedLocation)
            }
        }
    }

    private fun receiveResult(resultCode: Int, message: String) {
        Bundle().also {
            it.putString(RESULT_DATA_KEY, message)
            receiver?.send(resultCode, it)
        }
    }

    companion object{
        const val RESULT_DATA_KEY = "data"
        const val RESULT_SUCCESS = 1
        const val RESULT_FAIL = 0
    }
}
