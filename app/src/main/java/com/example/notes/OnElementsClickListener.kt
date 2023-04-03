package com.example.notes

interface OnElementsClickListener {
    fun onClickListener(element: Any, position: Int)
    fun onLongClickListener(element: Any, position: Int)
}

interface OnTaskChangeListener {
    fun afterTextChangedListener(text: String, position: Int)
    fun isCompleteChangeListener(change: Boolean, position: Int)
}