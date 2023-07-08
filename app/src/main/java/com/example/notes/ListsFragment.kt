package com.example.notes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.notes.databinding.FragmentListsBinding
import java.io.File
import java.io.IOException

class ListsFragment : Fragment(), OnElementsClickListener
{
    private var onDataSentListener: OnDataSentListener? = null

    private lateinit var binding: FragmentListsBinding
    private lateinit var adapter: NotesListAdapter
    private lateinit var context: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        when(context) {
            is OnDataSentListener -> onDataSentListener = context
            else -> throw IllegalArgumentException("Activity must implement OnDataSentListener")
        }
    }

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

        /*
        We need to check that our notes adapter was initialized in out onCreateView method.
        After that we set current layout manager.
        */
        if(this::adapter.isInitialized) {
            binding.notesView.layoutManager = when(AppData.gridType) {
                true -> StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                else -> LinearLayoutManager(context)
            }
            binding.notesView.adapter = adapter
            try {
                if (Build.VERSION.SDK_INT >= 33)
                    requireArguments().getParcelableArrayList(
                        CURRENT_LIST,
                        NotesListsData::class.java
                    )?.let {
                        adapter.addList(
                            it
                        )
                    }
                else {
                    requireArguments().getParcelableArrayList<NotesListsData>(CURRENT_LIST)?.let {
                        adapter.addList(
                            it
                        )
                    }
                }
            } catch (_: IllegalStateException)
            { }
        }
    }

    override fun onClickListener(element: NotesListsData, position: Int) {
        /*
        We can not tab on notes in delete list, so when we tab on note in any other list we need to check
        if it is text note - we will open addActivity page to edit it. If it is audio note we need to give this
        audio to our MainActivity.
        */
        if(AppData.activeList != ActiveListForWork.deleteList) {
            when (element) {
                is NotesData -> {
                    val intent = Intent(context, AddActivity::class.java)
                    intent.putExtra(AppData.noteFromFragmentToActivity, element)
                    intent.putExtra(AppData.noteFromFragmentToActivityPosition, position)
                    startActivity(intent)
                    requireActivity().finish()
                }
                is AudioRecorder -> {
                    when(File(element.filePath).exists()) {
                        true -> onDataSentListener!!.dataFromFragmentToActivity(element)
                        else -> {
                            if(element.isDelete) AppData.deleteNotesList.removeAt(position)
                            itemsAfterDeleteConnectionBetweenListsConfig(element, position)
                        }
                    }
                }
                else -> throw IllegalArgumentException("Illegal argument")
            }
        }
    }

    override fun onLongClickListener(element: NotesListsData, position: Int)
    {
        var newElement: NotesListsData
        binding.apply {
            popUpMenuVisibilityConfig()
            important.setOnClickListener {
                /*
                If we want to edit information about note importance we need to update this info in every list
                where this note exists. But if we tab on "important" button in important list we also
                need to remove note from this list
                */
                newElement = itemsAfterEditImportantConfigBetweenListsConfig(element)
                when(element.isImportant) {
                    true -> {
                        when(AppData.activeList) {
                            ActiveListForWork.importantList -> adapter.removeNoteByPosition(position)
                            else -> adapter.changeNoteItem(position, newElement)
                        }
                    }
                    else -> adapter.changeNoteItem(position, newElement)
                }
                itemPopUpMenu.visibility = View.GONE
            }
            delete.setOnClickListener {
                /*
                When we want to delete note in delete list note just will be deleted and it is all.
                But if we want to delete note and we are in any other list we need to delete this note from all lists
                and add it to delete list
                */
                when(element.isDelete) {
                    true -> {
                        deleteNotesPhotoAfterFullNoteDelete(element)
                        AppData.deleteNotesList.removeAt(position)
                        adapter.removeNoteByPosition(position)
                    }
                    else -> {
                        newElement = createNewElement(element,
                            important = false, recent = false, delete = true)
                        itemsAfterDeleteConnectionBetweenListsConfig(element, position)
                        AppData.deleteNotesList.add(newElement)
                        sentDataToActivity()
                    }
                }
                itemPopUpMenu.visibility = View.GONE
            }
            deleteNow.setOnClickListener {
                //It will delete note in an instant without adding it to delete list
                deleteNotesPhotoAfterFullNoteDelete(element)
                itemsAfterDeleteConnectionBetweenListsConfig(element, position)
                sentDataToActivity()
                itemPopUpMenu.visibility = View.GONE
            }
            returnItem.setOnClickListener {
                newElement = createNewElement(element,
                    important = false, recent = false, delete = false)
                AppData.deleteNotesList.removeAt(position)
                adapter.removeNoteByPosition(position)
                AppData.notesList.add(newElement)
                sentDataToActivity()
                itemPopUpMenu.visibility = View.GONE
            }
        }
    }

    override fun onNoteElementsClickListener(element: Any, position: Int) {
    }

    private fun createNewElement(
        element: NotesListsData, important: Boolean, recent: Boolean, delete: Boolean) : NotesListsData
    {
        val newElement: NotesListsData = when (element) {
            is NotesData -> NotesData(element, important, recent, delete)
            is AudioRecorder -> AudioRecorder(element, important, recent, delete)
            else -> throw IllegalArgumentException("Illegal argument")
        }
        return newElement
    }

    private fun itemsAfterEditImportantConfigBetweenListsConfig(element: NotesListsData) : NotesListsData
    {
        /*
        When we want to change note data in all list we need to create new note on old note data and
        change some of it data. After that we put new note to old note place in every list.
        */
        val newElement = createNewElement(element, !element.isImportant, element.isRecent, false)
        when {
            newElement.isImportant -> AppData.importantNotesList.add(newElement)
            else -> AppData.importantNotesList.removeAt(
                AppData.importantNotesList.indexOfFirst { (it.noteId == newElement.noteId)})
        }
        when {
            newElement.isRecent -> AppData.resentsNotesList[
                    AppData.resentsNotesList.indexOfFirst { it.noteId == newElement.noteId }] = newElement
        }
        AppData.notesList[
                AppData.notesList.indexOfFirst { it.noteId == newElement.noteId }] = newElement
        return newElement
    }

    private fun itemsAfterDeleteConnectionBetweenListsConfig(element: NotesListsData, position: Int)
    {
        when{
            element.isImportant -> AppData.importantNotesList.removeAt(
                AppData.importantNotesList.indexOfFirst { it.noteId == element.noteId })
        }
        when{
            element.isRecent -> AppData.resentsNotesList.removeAt(
                AppData.resentsNotesList.indexOfFirst { it.noteId == element.noteId })
        }
        AppData.notesList.removeAt(AppData.notesList.indexOfFirst { it.noteId == element.noteId })
        adapter.removeNoteByPosition(position)
    }

    private fun popUpMenuVisibilityConfig()
    {
        binding.apply {
            when (AppData.activeList) {
                ActiveListForWork.deleteList -> {
                    deleteNow.visibility = View.GONE
                    important.visibility = View.GONE
                }
                else -> returnItem.visibility = View.GONE
            }
            itemPopUpMenu.visibility = View.VISIBLE
        }
    }

    private fun deleteNotesPhotoAfterFullNoteDelete(element: NotesListsData)
    {
        //When we delete note we need also delete all it photos
        when(element) {
            is NotesData -> {
                for (i in 0 until element.photoList.size)
                    deleteOperation(element.photoList[i])
            }
            is AudioRecorder -> deleteOperation(element.filePath)
            else -> throw IllegalArgumentException("Illegal argument")
        }
    }

    fun sentDataToFragment(element: NotesListsData)
    {
        if(this::adapter.isInitialized)
            adapter.addNote(element)
    }

    private fun deleteOperation(path: String)
    {
        try {
            val file = File(path)
            if (file.exists())
                file.delete()
        } catch (_: IOException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun sentDataToActivity()
    {
        when(requireActivity()) {
            is MainActivity -> {
                val fragmentActivity = requireActivity() as MainActivity
                fragmentActivity.quantityOfElementsUIUpdate(AppData.notesList.size)
            }
            else -> throw IllegalArgumentException("Illegal activity of fragment")
        }
    }

    companion object
    {
        @JvmStatic
        private val CURRENT_LIST = "NOTES_CURRENT_LIST"

        @JvmStatic
        fun newInstance(notesList: ArrayList<NotesListsData>) : ListsFragment {
            val args = Bundle().apply {
                putParcelableArrayList(CURRENT_LIST, notesList)
            }
            val fragment = ListsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}