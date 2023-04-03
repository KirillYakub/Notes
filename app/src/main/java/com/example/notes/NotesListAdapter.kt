package com.example.notes

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.notes.databinding.ListItemBinding

class NotesListAdapter(
    private val clickListener: OnElementsClickListener,
    private val context: Context)
    : RecyclerView.Adapter<NotesListAdapter.NotesListHolder>()
{
    private val notesList = arrayListOf<NotesData>()

    class NotesListHolder(item: View) : RecyclerView.ViewHolder(item)
    {
        private val binding = ListItemBinding.bind(item)

        @SuppressLint("ResourceAsColor")
        fun bindNotes(note: NotesData, clickListener: OnElementsClickListener,  context: Context)
        {
            binding.apply {
                //brush.setImageResource(note.brushStyle)
                setCurrentElementsVisibility(note)
                mainPanel.setCardBackgroundColor(
                    ContextCompat.getColor(context, note.cardBackStyle))

                mainPanel.setOnClickListener {
                    clickListener.onClickListener(note, adapterPosition)
                }
                mainPanel.setOnLongClickListener{
                    clickListener.onLongClickListener(note, adapterPosition)
                    true
                }
            }
        }

        private fun setCurrentElementsVisibility(note: NotesData)
        {
            binding.apply {
                if(note.title.isNotEmpty() && note.text.isNotEmpty()) {
                    title.text = note.firstStringSymbolsOutput(note.title)
                    text.text = note.firstStringSymbolsOutput(note.text)
                }
                else if(note.title.isNotEmpty()) {
                    title.text = note.firstStringSymbolsOutput(note.title)
                    text.visibility = View.GONE
                }
                else if(note.text.isNotEmpty()) {
                    text.text = note.firstStringSymbolsOutput(note.text)
                    title.visibility = View.GONE
                }
                else
                    textPanel.visibility = View.GONE

                if(note.taskList.isNotEmpty() || note.photoList.isNotEmpty()) {
                    if (note.taskList.isNotEmpty()) {
                        list.visibility = View.VISIBLE
                        list.setImageResource(note.listStyle)
                    }
                    if (note.photoList.isNotEmpty()) {
                        camera.visibility = View.VISIBLE
                        camera.setImageResource(note.imageStyle)
                    }
                }
                else
                    noteDataPanel.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesListHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return NotesListHolder(view)
    }

    override fun getItemCount(): Int {
        return notesList.size
    }

    override fun onBindViewHolder(holder: NotesListHolder, position: Int) {
        holder.bindNotes(notesList[position], clickListener, context)
    }

    fun changeNoteItem(position: Int, notes: NotesData)
    {
        notesList[position] = notes
        notifyItemChanged(position)
    }

    fun removeNoteByPosition(position: Int)
    {
        notesList.removeAt(position)
        notifyItemRemoved(position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addList(notesNewList: ArrayList<NotesData>)
    {
        notesList.clear()
        notesList.addAll(notesNewList)
        notifyDataSetChanged()
    }
}