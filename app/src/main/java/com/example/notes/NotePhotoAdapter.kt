package com.example.notes

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notes.databinding.PhotoListTemBinding
import java.io.FileNotFoundException

class NotePhotoAdapter(private val onClickListener: OnElementsClickListener) : RecyclerView.Adapter<NotePhotoAdapter.NotesListHolder>()
{
    private val list = arrayListOf<String>()

    class NotesListHolder(item: View) : RecyclerView.ViewHolder(item)
    {
        private val binding = PhotoListTemBinding.bind(item)

        fun bindPhoto(notePhoto: String, onClickListener: OnElementsClickListener)
        {
            binding.apply {
                try {
                    val options = BitmapFactory.Options()
                    options.inSampleSize = 4
                    photo.setImageBitmap(BitmapFactory.decodeFile(notePhoto, options))
                } catch (_:FileNotFoundException) {
                } catch (_:NullPointerException){
                }
                photo.setOnClickListener {
                    onClickListener.onClickListener(notePhoto, adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesListHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.photo_list_tem, parent, false)
        return NotesListHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: NotesListHolder, position: Int) {
        holder.bindPhoto(list[position], onClickListener)
    }

    fun deleteItemsFromList(deleteList: ArrayList<String>, deleteIndexList: ArrayList<Int>)
    {
        list.removeAll(deleteList.toSet())
        for((removeCounter, i) in deleteIndexList.withIndex())
            notifyItemRemoved(i - removeCounter)
    }

    fun removePhotoByPosition(position: Int)
    {
        list.removeAt(position)
        notifyItemRemoved(position)
    }

    fun addPhoto(photoPath: String)
    {
        list.add(photoPath)
        notifyItemInserted(list.size - 1)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addList(newList: ArrayList<String>)
    {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}