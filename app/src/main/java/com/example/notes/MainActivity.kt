package com.example.notes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.notes.databinding.ActivityMainMenuBinding

class MainActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityMainMenuBinding
    private lateinit var fragment: ListsFragment
    private val appData = AppData(this)

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityMainMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appData.loadListData()

        fragment = ListsFragment.newInstance(AppData.notesList)
        supportFragmentManager
            .beginTransaction()
            .add(binding.fragmentsView.id, fragment)
            .commit()
    }

    override fun onStart()
    {
        super.onStart()

        binding.add.setOnClickListener {
            val addIntent = Intent(this, AddActivity::class.java)
            startActivity(addIntent)
        }
        binding.all.setOnClickListener {
            setFragment(AppData.notesList)
            AppData.activeList = ActiveListForWork.allList
        }
        binding.important.setOnClickListener{
            setFragment(AppData.importantNotesList)
            AppData.activeList = ActiveListForWork.importantList
        }
        binding.recents.setOnClickListener{
            setFragment(AppData.resentsNotesList)
            AppData.activeList = ActiveListForWork.resentsList
        }
        binding.trash.setOnClickListener{
            setFragment(AppData.deleteNotesList)
            AppData.activeList = ActiveListForWork.deleteList
        }
    }

    override fun onPause() {
        super.onPause()
        appData.saveListData()
    }

    private fun setFragment(noteList: ArrayList<NotesData>)
    {
        fragment = ListsFragment.newInstance(noteList)
        supportFragmentManager
            .beginTransaction()
            .replace(binding.fragmentsView.id, fragment)
            .commit()
    }
}