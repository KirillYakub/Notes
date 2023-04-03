package com.example.notes

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NotesData(
    title: String = String(), text: String = String(), noteId: String = String()) : Parcelable
{
    var title: String
    var text: String
    var noteId: String
    var isImportant: Boolean = false
    var isRecent: Boolean = false
    var isDelete: Boolean = false

    var cardBackStyle: Int = BaseNotesDataStyle.cardStyle
    var listStyle: Int = BaseNotesDataStyle.listStyle
    var imageStyle: Int = BaseNotesDataStyle.cameraStyle

    var photoList = arrayListOf<String>()
    var taskList = arrayListOf<Task>()

    constructor(notesData: NotesData, important: Boolean, recent: Boolean, delete: Boolean) :
            this(notesData.title, notesData.text, notesData.noteId)
    {
        this.cardBackStyle = notesData.cardBackStyle
        this.listStyle = notesData.listStyle
        this.imageStyle = notesData.imageStyle
        this.photoList = notesData.photoList
        this.taskList = notesData.taskList
        this.isImportant = important
        this.isRecent = recent
        this.isDelete = delete
    }

    constructor(parcel: Parcel) : this()
    {
        title = parcel.readString().toString()
        text = parcel.readString().toString()
        noteId = parcel.readString().toString()
        isImportant = intToBoolean(parcel.readInt())
        isRecent = intToBoolean(parcel.readInt())
        isDelete = intToBoolean(parcel.readInt())
        cardBackStyle = parcel.readInt()
        listStyle = parcel.readInt()
        imageStyle = parcel.readInt()
        parcel.readStringList(photoList)
        parcel.readTypedList(taskList, Task.CREATOR)
    }

    init {
        this.title = title
        this.text = text
        this.noteId = noteId
    }

    private fun booleanToInt(bullValue: Boolean) : Int {
        return if(bullValue) 1 else 0
    }
    private fun intToBoolean(intValue: Int) : Boolean {
        return intValue == 1
    }

    fun firstStringSymbolsOutput(str: String): String
    {
        val size = 25
        val symbolId: Int
        var subStr: String

        return if (str.length > size) {
            subStr = str.substring(0, size)
            symbolId = subStr.lastIndexOf(' ')
            if (symbolId != -1)
                subStr = subStr.substring(0, symbolId)
            "$subStr ..."
        } else str
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(text)
        parcel.writeString(noteId)
        parcel.writeInt(booleanToInt(isImportant))
        parcel.writeInt(booleanToInt(isRecent))
        parcel.writeInt(booleanToInt(isDelete))
        parcel.writeInt(cardBackStyle)
        parcel.writeInt(listStyle)
        parcel.writeInt(imageStyle)
        parcel.writeStringList(photoList)
        parcel.writeTypedList(taskList)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NotesData> {
        override fun createFromParcel(parcel: Parcel): NotesData {
            return NotesData(parcel)
        }

        override fun newArray(size: Int): Array<NotesData?> {
            return arrayOfNulls(size)
        }
    }
}

class Task(task: String = String(), isComplete: Boolean = false) : Parcelable
{
    var task:String
    var isComplete: Boolean

    constructor(parcel: Parcel) : this() {
        task = parcel.readString().toString()
        isComplete = parcel.readByte() != 0.toByte()
    }

    init {
        this.task = task
        this.isComplete = isComplete
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(task)
        parcel.writeByte(if (isComplete) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Task> {
        override fun createFromParcel(parcel: Parcel): Task {
            return Task(parcel)
        }

        override fun newArray(size: Int): Array<Task?> {
            return arrayOfNulls(size)
        }
    }
}

class AppData(private var context: Context)
{
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var json: String? = null
    private val type = object : TypeToken<ArrayList<NotesData>>(){}.type
    private val gson = Gson()

    fun saveListData() {
        sharedPreferences = context.getSharedPreferences(listsSharedPrefs, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        putDataToEditorForSave(notesList, notesListTag)
        putDataToEditorForSave(importantNotesList, importantNotesListTag)
        putDataToEditorForSave(resentsNotesList, resentsNotesListTag)
        putDataToEditorForSave(deleteNotesList, deleteNotesListTag)
        editor.apply()
    }

    fun loadListData() {
        sharedPreferences = context.getSharedPreferences(listsSharedPrefs, Context.MODE_PRIVATE)
        notesList = loadDataFromJson(notesListTag)
        importantNotesList = loadDataFromJson(importantNotesListTag)
        resentsNotesList = loadDataFromJson(resentsNotesListTag)
        deleteNotesList = loadDataFromJson(deleteNotesListTag)
    }

    private fun putDataToEditorForSave(list: ArrayList<NotesData>, tag: String) {
        json = gson.toJson(list)
        editor.putString(tag, json)
    }

    private fun loadDataFromJson(tag: String) : ArrayList<NotesData> {
        json = sharedPreferences.getString(tag, null)
        return gson.fromJson(json, type) ?: arrayListOf()
    }

    companion object {
        lateinit var notesList: ArrayList<NotesData>
        lateinit var importantNotesList: ArrayList<NotesData>
        lateinit var resentsNotesList: ArrayList<NotesData>
        lateinit var deleteNotesList: ArrayList<NotesData>

        var activeList = ActiveListForWork.allList
        var notePositionForEditWhenFragmentUpdate: Int? = null

        const val noteFromFragmentToActivity = "NOTE_FROM_FRAGMENT_T0_ACTIVITY"
        const val notePosition = "NOTE_POSITION"
        const val notePhoto = "NOTE_PHOTO"
        const val listsSharedPrefs = "LISTS_SHARED_PREFS"
        const val notesListTag = "NOTES_LIST"
        const val importantNotesListTag = "IMPORTANT_LIST"
        const val resentsNotesListTag = "RESENTS_LIST"
        const val deleteNotesListTag = "DELETE_LIST"
        const val requestCameraPermission = 100
        const val requestGalleryPermission = 101
        const val maxResentsNotes = 16
    }
}

object BaseNotesDataStyle {
    const val cardStyle = R.color.paper
    const val listStyle = R.drawable.paper_list
    const val cameraStyle = R.drawable.paper_camera
}

object ActiveListForWork {
    const val allList = "ALL_LIST"
    const val importantList = "IMPORTANT_LIST"
    const val resentsList = "RESENTS_LIST"
    const val deleteList = "DELETE_LIST"
}