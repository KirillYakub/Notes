package com.example.notes

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.notes.databinding.FragmentListsBinding
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

class ListsFragment : Fragment(), OnElementsClickListener
{
    private lateinit var binding: FragmentListsBinding
    private lateinit var adapter: NotesListAdapter
    private lateinit var context: Context

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (container != null) {
            context = container.context
            adapter = NotesListAdapter(this, context)
        }
        binding = FragmentListsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(this::adapter.isInitialized) {
            binding.notesView.layoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            binding.notesView.adapter = adapter
            try {
                if (Build.VERSION.SDK_INT >= 33)
                    adapter.addList(
                        requireArguments().getParcelableArrayList(
                            CURRENT_LIST,
                            NotesData::class.java
                        ) as ArrayList<NotesData>
                    )
                else
                    adapter.addList(
                        requireArguments().getParcelableArrayList<NotesData>(
                            CURRENT_LIST
                        ) as ArrayList<NotesData>
                    )
            } catch (_: IllegalStateException)
            { }
        }
    }

    override fun onStart() {
        super.onStart()

        if(AppData.notePositionForEditWhenFragmentUpdate != null) {
            val pos = AppData.notePositionForEditWhenFragmentUpdate
            when(AppData.activeList) {
                ActiveListForWork.allList -> adapter.changeNoteItem(pos!!, AppData.notesList[pos])
                ActiveListForWork.importantList ->
                    adapter.changeNoteItem(pos!!, AppData.importantNotesList[pos])
                ActiveListForWork.resentsList ->
                    adapter.changeNoteItem(pos!!, AppData.resentsNotesList[pos])
                ActiveListForWork.deleteList ->
                    adapter.changeNoteItem(pos!!, AppData.deleteNotesList[pos])
            }
            AppData.notePositionForEditWhenFragmentUpdate = null
        }
    }

    override fun onClickListener(element: Any, position: Int) {
        if(AppData.activeList != ActiveListForWork.deleteList) {
            val intent = Intent(context, AddActivity::class.java)
            intent.putExtra(AppData.noteFromFragmentToActivity, element as NotesData)
            intent.putExtra(AppData.notePosition, position)
            startActivity(intent)
        }
    }

    override fun onLongClickListener(element: Any, position: Int)
    {
        var newNoteElement: NotesData
        binding.apply {
            popUpMenuVisibilityConfig()
            important.setOnClickListener{
                if((element as NotesData).isImportant) {
                    newNoteElement = itemsAfterEditImportantConfigBetweenListsConfig(element)
                    if(AppData.activeList == ActiveListForWork.importantList)
                        adapter.removeNoteByPosition(position)
                    else
                        adapter.changeNoteItem(position, newNoteElement)
                } else {
                    newNoteElement = itemsAfterEditImportantConfigBetweenListsConfig(element)
                    adapter.changeNoteItem(position, newNoteElement)
                }
                itemPopUpMenu.visibility = View.GONE
            }
            delete.setOnClickListener{
                if((element as NotesData).isDelete) {
                    deleteNotesPhotoAfterFullNoteDelete(element)
                    AppData.deleteNotesList.removeAt(position)
                    adapter.removeNoteByPosition(position)
                } else {
                    newNoteElement = NotesData(element, important = false, recent = false, delete = true)
                    itemsAfterDeleteConnectionBetweenListsConfig(element, position)
                    AppData.deleteNotesList.add(newNoteElement)
                }
                itemPopUpMenu.visibility = View.GONE
            }
            deleteNow.setOnClickListener{
                deleteNotesPhotoAfterFullNoteDelete(element as NotesData)
                itemsAfterDeleteConnectionBetweenListsConfig(element, position)
                itemPopUpMenu.visibility = View.GONE
            }
            returnItem.setOnClickListener {
                newNoteElement = NotesData(element as NotesData, important = false, recent = false, delete = false)
                AppData.deleteNotesList.removeAt(position)
                adapter.removeNoteByPosition(position)
                AppData.notesList.add(newNoteElement)
                itemPopUpMenu.visibility = View.GONE
            }
        }
    }

    private fun itemsAfterEditImportantConfigBetweenListsConfig(notes: NotesData) : NotesData
    {
        val newNote = NotesData(notes, !notes.isImportant, notes.isRecent, false)
        if(newNote.isImportant)
            AppData.importantNotesList.add(newNote)
        else
            AppData.importantNotesList.removeAt(
                AppData.importantNotesList.indexOfFirst { it.noteId == newNote.noteId })
        if(newNote.isRecent)
            AppData.resentsNotesList[
                    AppData.resentsNotesList.indexOfFirst { it.noteId == newNote.noteId}] = newNote
        AppData.notesList[AppData.notesList.indexOfFirst { it.noteId == newNote.noteId}] = newNote
        return newNote
    }

    private fun itemsAfterDeleteConnectionBetweenListsConfig(notes: NotesData, position: Int)
    {
        if(notes.isImportant)
            AppData.importantNotesList.removeAt(
                AppData.importantNotesList.indexOfFirst { it.noteId == notes.noteId})
        if(notes.isRecent)
            AppData.resentsNotesList.removeAt(
                AppData.resentsNotesList.indexOfFirst { it.noteId == notes.noteId})
        AppData.notesList.removeAt(AppData.notesList.indexOfFirst { it.noteId == notes.noteId})
        adapter.removeNoteByPosition(position)
    }

    private fun popUpMenuVisibilityConfig()
    {
        binding.apply {
            if(AppData.activeList == ActiveListForWork.deleteList) {
                deleteNow.visibility = View.GONE
                important.visibility = View.GONE
            }
            else
                returnItem.visibility = View.GONE
            itemPopUpMenu.visibility = View.VISIBLE
        }
    }

    private fun deleteNotesPhotoAfterFullNoteDelete(note: NotesData)
    {
        for(i in 0 until note.photoList.size) {
            try {
                val file = File(note.photoList[i])
                if (file.exists())
                    file.delete()
            }catch (_:IOException){
            }catch (_:IllegalArgumentException){
            }
        }
    }

    companion object
    {
        @JvmStatic
        private val CURRENT_LIST = "NOTES_CURRENT_LIST"

        @JvmStatic
        fun newInstance(notesList: ArrayList<NotesData>) : ListsFragment {
            val args = Bundle().apply {
                putParcelableArrayList(CURRENT_LIST, notesList)
            }
            val fragment = ListsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}