package com.example.notes

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notes.databinding.TaskListItemBinding

class NoteTasksAdapter(
    private val clickListener: OnElementsClickListener,
    private val taskChangeListener: OnTaskChangeListener)
    : RecyclerView.Adapter<NoteTasksAdapter.NotesListHolder>()
{
    private val list = arrayListOf<Task>()

    class NotesListHolder(item: View) : RecyclerView.ViewHolder(item)
    {
        private val binding = TaskListItemBinding.bind(item)

        fun bind(task: Task,
                 clickListener: OnElementsClickListener, taskChangeListener: OnTaskChangeListener)
        {
            binding.apply {
                taskText.setText(task.task)
                completedTask.isChecked = task.isComplete

                completedTask.setOnClickListener{
                    taskChangeListener.isCompleteChangeListener(completedTask.isChecked, adapterPosition)
                }
                deleteTask.setOnClickListener{
                    taskText.clearFocus()
                    clickListener.onNoteElementsClickListener(task, adapterPosition)
                }
                taskText.addTextChangedListener(object : TextWatcher{
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun afterTextChanged(text: Editable?) {
                        taskChangeListener.afterTextChangedListener(text.toString(), adapterPosition)
                    }
                })
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesListHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_list_item, parent, false)
        return NotesListHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: NotesListHolder, position: Int) {
        holder.bind(list[position], clickListener, taskChangeListener)
    }

    fun removeTaskByPosition(position: Int)
    {
        list.removeAt(position)
        notifyItemRemoved(position)
    }

    fun addTask(task: Task)
    {
        list.add(task)
        notifyItemInserted(list.size - 1)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addList(newList: ArrayList<Task>)
    {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}