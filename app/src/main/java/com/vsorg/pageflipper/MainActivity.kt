package com.vsorg.pageflipper

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

//Reference : https://github.com/moritz-wundke/android-page-curl/tree/master

class MainActivity : AppCompatActivity() {

    private lateinit var listOfPages: ArrayList<Bitmap>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listOfPages = arrayListOf()
        listOfPages.add(BitmapFactory.decodeResource(resources, R.drawable.blank))
        listOfPages.add(BitmapFactory.decodeResource(resources, R.drawable.page1))
        listOfPages.add(BitmapFactory.decodeResource(resources, R.drawable.page2))
        listOfPages.add(BitmapFactory.decodeResource(resources, R.drawable.page3))
        listOfPages.add(BitmapFactory.decodeResource(resources, R.drawable.page4))
        listOfPages.add(BitmapFactory.decodeResource(resources, R.drawable.page5))
        listOfPages.add(BitmapFactory.decodeResource(resources, R.drawable.page6))
        listOfPages.add(BitmapFactory.decodeResource(resources, R.drawable.page7))
        listOfPages.add(BitmapFactory.decodeResource(resources, R.drawable.page8))

        findViewById<PageCurlView>(R.id.pc_view).setImageResources(listOfPages)
    }
}