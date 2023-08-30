package com.example.notes

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.notes.databinding.ActivityOptionsBinding

class OptionsActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityOptionsBinding
    private val appData = AppData(this)

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goBack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onStart()
    {
        super.onStart()

        binding.apply {
            when(AppData.gridType) {
                true -> grid.isChecked = true
                else -> list.isChecked = true
            }
            when(AppData.priorityDisplayText) {
                true -> textNote.isChecked = true
                else -> audioNote.isChecked = true
            }
            list.setOnClickListener { AppData.gridType = false; }
            grid.setOnClickListener { AppData.gridType = true }
            textNote.setOnClickListener {
                AppData.priorityDisplayText = true
                AppData.firstInput = false
            }
            audioNote.setOnClickListener {
                AppData.priorityDisplayText = false
                AppData.firstInput = false
            }
            back.setOnClickListener { goBack() }
        }
    }

    override fun onPause() {
        super.onPause()
        appData.saveListData()
    }

    private fun goBack()
    {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}