package com.ammase.googleplace.picker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.view.View
import com.ammase.googleplace.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_location_picker.*
import com.google.android.gms.maps.GoogleMap as GoogleMap1

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mResultData : Bundle? = null
    private var isCameraMove = false
    private val mHandler = Handler()
    private lateinit var mMap : GoogleMap1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        toolbar.setNavigationOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                asyncMap()
            }else{
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),101)
            }
        }else{
            asyncMap()
        }



        actionConfirm.setOnClickListener {
            mResultData?.also {
                setResult(Activity.RESULT_OK,Intent().apply { putExtras(it) })
            } ?: setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 101 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            asyncMap()
        }
    }

    private fun asyncMap(){
        supportFragmentManager.findFragmentById(R.id.mapFragment)?.also {
            (it as SupportMapFragment).getMapAsync(this)
        }
    }


    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap1) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isZoomGesturesEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.isMyLocationEnabled = true

        mMap.setMinZoomPreference(10f)
        mMap.setMaxZoomPreference(25f)
        val labelLoading = resources.getString(R.string.label_loading)
        mMap.setOnCameraMoveListener {
            isCameraMove = true
            if(!textView.text.toString().equals(labelLoading)) textView.text = labelLoading
            if(actionConfirm.isEnabled) actionConfirm.isEnabled = false
        }

        mMap.setOnCameraIdleListener {
            isCameraMove = false
            mHandler.postDelayed({
                if(isCameraMove) return@postDelayed

                if(mResultData == null) mResultData = Bundle().apply {
                    putParcelable(DATA_LATLNG,mMap.cameraPosition?.target)
                }
                startService(
                    Intent(
                        this@LocationPickerActivity,
                        AddressFinderService::class.java).apply {
                        putExtra("receiver",mReceiver)
                        putExtra("location",mMap.cameraPosition?.target)
                    })
            }, 350L)
        }
        initFirsLocation()
    }

    private val mReceiver = object : ResultReceiver(mHandler){

        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            super.onReceiveResult(resultCode, resultData)
            if(resultCode == AddressFinderService.RESULT_SUCCESS){
                resultData?.getString(AddressFinderService.RESULT_DATA_KEY)?.let {
                    mResultData?.putString(DATA_ADDRESS,it)
                    textView.text = it
                }
            }
            if(!actionConfirm.isEnabled) actionConfirm.isEnabled = true
        }
    }


    @SuppressLint("MissingPermission")
    private fun initFirsLocation(){
        LocationServices.getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { location ->
                location?.also {
                    LatLng(it.latitude, it.longitude).also { latlng ->
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng))
                    }
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15f))
                }
            }
    }

    companion object{
        const val DATA_LATLNG = "latlng"
        const val DATA_ADDRESS = "address"
    }
}
