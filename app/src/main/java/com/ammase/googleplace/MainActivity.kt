package com.ammase.googleplace

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ammase.googleplace.picker.LocationPickerActivity
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        actionPickLocation.setOnClickListener {
            startActivityForResult(Intent(this,LocationPickerActivity::class.java),101)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 101 && resultCode == Activity.RESULT_OK){
            data?.also {
                var locationMessage = ""
                it.getParcelableExtra<LatLng>(LocationPickerActivity.DATA_LATLNG)?.let {
                    locationMessage += "\nLat : ${it.latitude}, Lng : ${it.longitude}"
                }
                it.getStringExtra(LocationPickerActivity.DATA_ADDRESS)?.let {
                    locationMessage += "\n${it}"
                }
                textLocation.text = resources.getString(R.string.your_location,locationMessage)
            }
        }
    }
}
