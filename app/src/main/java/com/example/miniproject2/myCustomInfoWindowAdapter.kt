package com.example.miniproject2

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.koushikdutta.ion.Ion
import com.squareup.picasso.Picasso



// Sources
// https://gist.github.com/ccjeng/ff8ca25e0e92302639dadbe4a8533279
// Ion docs: https://github.com/koush/ion#load-an-image-into-an-imageview

class myCustomInfoWindowAdapter(mapsActivity: MapsActivity) : GoogleMap.InfoWindowAdapter {

    private var context: Activity? = mapsActivity

//    fun myCustomInfoWindowAdapter(context: Activity?) {
//        this.context = context
//    }

    override fun getInfoWindow(p0: Marker): View? {
        return null
    }


    override fun getInfoContents(marker: Marker): View? {
        val thatPoiInfo: ArrayList<String> = marker.tag as ArrayList<String>

        val view: View = context!!.layoutInflater.inflate(R.layout.my_custom_info_window_adapter, null)
        val poiTitle = view.findViewById<View>(R.id.poiTitle) as TextView
        val poiDescription = view.findViewById<View>(R.id.poiDescription) as TextView

        val poiThumbnail = view.findViewById<View>(R.id.poiThumbnail) as ImageView
        Log.i("TAG1", thatPoiInfo[2] )

        poiTitle.text = marker.title
        poiDescription.text = thatPoiInfo[1]
        if (thatPoiInfo[2] == "NoPicture") {
            poiThumbnail.setImageResource(android.R.drawable.ic_menu_report_image)
        }else{
            Picasso.get()               // Picasso works fine, but only displays some of the images.
                .load(thatPoiInfo[2])
                .placeholder(android.R.drawable.ic_popup_sync)
                .into(poiThumbnail)
//            Ion.with(poiThumbnail)        // Seems that Ion does not work here
//                .placeholder(android.R.drawable.ic_popup_sync)
//                .error(android.R.drawable.presence_offline)
//                .load(thatPoiInfo[2])
//            Ion.with(context)             // And here
//                .load(thatPoiInfo[2])
//                .withBitmap()
//                .placeholder(android.R.drawable.ic_popup_sync)
//                .error(android.R.drawable.presence_offline)
//                .intoImageView(poiThumbnail)
        }

        return view
    }

}