package com.example.notes

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.notes.databinding.ActivityImageBinding
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class PhotoActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityImageBinding
    private lateinit var pathToPhoto: String

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goBack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, callback)
        setStyle()
        setPhoto()
    }

    override fun onStart() {
        super.onStart()

        binding.apply {
            cancel.setOnClickListener {
                goBack()
            }
            delete.setOnClickListener {
                try {
                    val file = File(pathToPhoto)
                    if (file.exists())
                        file.delete()
                }catch (_:IOException){
                }catch (_:IllegalArgumentException){
                }
                finally {
                    goBack()
                }
            }
        }
    }

    private fun setPhoto()
    {
        try {
            pathToPhoto = intent.getStringExtra(AppData.notePhotoToShow).toString()
            binding.notePhoto.setImageBitmap(BitmapFactory.decodeFile(pathToPhoto))
        } catch (_:FileNotFoundException){
        } catch (_:NullPointerException){
        }
    }

    private fun setStyle()
    {
        binding.apply {
            val style = intent.getIntExtra(AppData.noteStyleToShow, R.color.paper)
            photoPanel.setBackgroundResource(style)
            when(style) {
                R.color.orange -> deletePanel.setBackgroundResource(R.drawable.photo_orange_back)
                R.color.blue -> deletePanel.setBackgroundResource(R.drawable.photo_blue_back)
                R.color.green -> deletePanel.setBackgroundResource(R.drawable.photo_green_back)
                R.color.yellow -> deletePanel.setBackgroundResource(R.drawable.photo_yellow_back)
                R.color.paper -> deletePanel.setBackgroundResource(R.drawable.photo_paper_back)
            }
        }
    }

    private fun goBack()
    {
        finish()
    }
}