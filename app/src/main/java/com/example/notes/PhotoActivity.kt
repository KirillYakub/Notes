package com.example.notes

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.notes.databinding.ActivityImageBinding
import java.io.FileNotFoundException

class PhotoActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityImageBinding

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setPhoto()
    }

    private fun setPhoto()
    {
        try {
            val pathToPhoto = intent.getStringExtra(AppData.notePhoto)
            binding.notePhoto.setImageBitmap(BitmapFactory.decodeFile(pathToPhoto))
        } catch (_:FileNotFoundException){
        } catch (_:NullPointerException){
        }
    }
}