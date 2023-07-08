package com.example.notes

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.notes.databinding.AudioListItemBinding
import com.example.notes.databinding.AudioListItemLinearBinding
import com.example.notes.databinding.ListItemBinding
import com.example.notes.databinding.ListItemLinearBinding
import com.google.android.material.card.MaterialCardView

class NotesListAdapter(
    private val clickListener: OnElementsClickListener,
    private val context: Context)
    : RecyclerView.Adapter<NotesListAdapter.NotesListHolder>()
{
    private val notesList = arrayListOf<NotesListsData>()
    private val itemLayout = R.layout.list_item
    private val audioLayout = R.layout.audio_list_item
    private var itemLinearLayout = R.layout.list_item_linear
    private var audioLinearLayout = R.layout.audio_list_item_linear

    class NotesListHolder(item: View) : RecyclerView.ViewHolder(item)
    {
        private val symbolsCount = 25

        @SuppressLint("ResourceAsColor")
        fun bindNotes(note: NotesListsData, clickListener: OnElementsClickListener, context: Context)
        {
            when(note) {
                is NotesData -> {
                    val binding = when(AppData.gridType) {
                        true -> ListItemBinding.bind(itemView)
                        else -> ListItemLinearBinding.bind(itemView)
                    }
                    val noteCardView = when(binding) {
                        is ListItemBinding -> binding.mainPanel
                        is ListItemLinearBinding -> binding.mainPanel
                        else -> throw IllegalArgumentException("Unknown binding")
                    }
                    setElementData(note, binding)
                    onNotesPanelClick(noteCardView, clickListener, context, note)
                }
                is AudioRecorder -> {
                    val audioBinding: ViewBinding
                    when(AppData.gridType) {
                        true -> {
                            audioBinding = AudioListItemBinding.bind(itemView)
                            audioBinding.audioName.text =
                                WorkWithSymbols.firstStringSymbolsOutput(note.name, symbolsCount)
                            audioBinding.audioLength.text =
                                AudioTimerData.setTimerValue(note.length)
                        }
                        else -> {
                            audioBinding = AudioListItemLinearBinding.bind(itemView)
                            audioBinding.audioName.text =
                                WorkWithSymbols.firstStringSymbolsOutput(note.name, symbolsCount)
                            audioBinding.audioLength.text =
                                AudioTimerData.setTimerValue(note.length)
                        }
                    }
                    val audioCardView = when(audioBinding) {
                        is AudioListItemBinding -> audioBinding.audioPanel
                        is AudioListItemLinearBinding -> audioBinding.audioPanel
                        else -> throw IllegalArgumentException("Unknown binding")
                    }
                    onNotesPanelClick(audioCardView, clickListener, context, note)
                }
                else -> throw IllegalArgumentException("Illegal argument")
            }
        }

        private fun onNotesPanelClick(
            view: MaterialCardView, clickListener: OnElementsClickListener,
            context: Context, note: NotesListsData) {
            if(note is NotesData) {
                view.setCardBackgroundColor(
                    ContextCompat.getColor(context, note.cardBackStyle)
                )
            }
            view.setOnClickListener {
                clickListener.onClickListener(note, adapterPosition)
            }
            view.setOnLongClickListener {
                clickListener.onLongClickListener(note, adapterPosition)
                true
            }
        }

        private fun setElementData(note: NotesData, binding: ViewBinding)
        {
            when(binding) {
                is ListItemBinding -> {
                    binding.apply {
                        when {
                            note.title.isNotEmpty() -> {
                                title.text = WorkWithSymbols.firstStringSymbolsOutput(note.title, symbolsCount)
                                title.visibility = View.VISIBLE
                            }
                        }
                        when {
                            note.text.isNotEmpty() -> {
                                text.text = WorkWithSymbols.firstStringSymbolsOutput(note.text, symbolsCount)
                                text.visibility = View.VISIBLE
                            }
                        }
                        when {
                            note.taskList.isNotEmpty() -> {
                                list.visibility = View.VISIBLE
                                list.setImageResource(note.listStyle)
                            }
                        }
                        when {
                            note.photoList.isNotEmpty() -> {
                                camera.visibility = View.VISIBLE
                                camera.setImageResource(note.imageStyle)
                            }
                        }
                    }
                }
                is ListItemLinearBinding -> {
                    binding.apply {
                        when {
                            note.title.isNotEmpty() -> {
                                title.text = WorkWithSymbols.firstStringSymbolsOutput(note.title, symbolsCount)
                                title.visibility = View.VISIBLE
                            }
                        }
                        when {
                            note.text.isNotEmpty() -> {
                                text.text = WorkWithSymbols.firstStringSymbolsOutput(note.text, symbolsCount)
                                text.visibility = View.VISIBLE
                            }
                        }
                        when {
                            note.taskList.isNotEmpty() -> {
                                list.visibility = View.VISIBLE
                                list.setImageResource(note.listStyle)
                            }
                        }
                        when {
                            note.photoList.isNotEmpty() -> {
                                camera.visibility = View.VISIBLE
                                camera.setImageResource(note.imageStyle)
                            }
                        }
                    }
                }
                else -> throw IllegalArgumentException("Cant get access with this binding type")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesListHolder {
        val view = when(viewType) {
            itemLayout -> LayoutInflater
                .from(parent.context)
                .inflate(if(AppData.gridType) itemLayout else itemLinearLayout, parent, false)
            audioLayout -> LayoutInflater
                .from(parent.context)
                .inflate(if(AppData.gridType) audioLayout else audioLinearLayout, parent, false)
            else -> throw IllegalArgumentException("Illegal argument")
        }
        return NotesListHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return when(notesList[position]) {
            is NotesData -> itemLayout
            is AudioRecorder -> audioLayout
            else -> throw IllegalArgumentException("Illegal argument")
        }
    }

    override fun getItemCount(): Int {
        return notesList.size
    }

    override fun onBindViewHolder(holder: NotesListHolder, position: Int) {
        holder.bindNotes(notesList[position], clickListener, context)
    }

    fun changeNoteItem(position: Int, notes: NotesListsData)
    {
        notesList[position] = notes
        notifyItemChanged(position)
    }

    fun removeNoteByPosition(position: Int)
    {
        notesList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun addNote(note: NotesListsData)
    {
        notesList.add(note)
        notifyItemInserted(itemCount - 1)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addList(notesNewList: ArrayList<NotesListsData>)
    {
        notesList.clear()
        notesList.addAll(notesNewList)
        notifyDataSetChanged()
    }
}