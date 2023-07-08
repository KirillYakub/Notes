package com.example.notes

interface OnElementsClickListener {
    fun onClickListener(element: NotesListsData, position: Int)
    fun onLongClickListener(element: NotesListsData, position: Int)
    fun onNoteElementsClickListener(element: Any, position: Int)
}

interface OnTaskChangeListener {
    fun afterTextChangedListener(text: String, position: Int)
    fun isCompleteChangeListener(change: Boolean, position: Int)
}

interface OnDataSentListener {
    fun dataFromFragmentToActivity(data: NotesListsData)
}