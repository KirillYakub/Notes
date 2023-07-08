package com.example.notes

import android.os.Parcel
import android.os.Parcelable
import kotlin.random.Random

open class NotesListsData(
    @JvmField var noteId: String = String(),
    @JvmField var isImportant: Boolean = false,
    @JvmField var isRecent: Boolean = false,
    @JvmField var isDelete: Boolean = false) : Parcelable
{
    protected fun readFromParcel(parcel: Parcel) {
        noteId = parcel.readString().toString()
        isImportant = parcel.readBooleanCompat()
        isRecent = parcel.readBooleanCompat()
        isDelete = parcel.readBooleanCompat()
    }

    private fun Parcel.readBooleanCompat(): Boolean {
        return readByte().toInt() != 0
    }

    protected constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(noteId)
        parcel.writeByte(if (isImportant) 1 else 0)
        parcel.writeByte(if (isRecent) 1 else 0)
        parcel.writeByte(if (isDelete) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<NotesListsData> {
        override fun createFromParcel(parcel: Parcel): NotesListsData {
            return NotesListsData(parcel)
        }

        override fun newArray(size: Int): Array<NotesListsData?> {
            return arrayOfNulls(size)
        }
    }
}

class NotesData(
    title: String = String(), text: String = String(), noteId: String = String(),
    important: Boolean = false, recent: Boolean = false, delete: Boolean = false)
    : NotesListsData(noteId, important, recent, delete)
{
    var title: String
    var text: String
    var notificationId: Int = 0
    var cardBackStyle: Int = BaseNotesDataStyle.cardStyle
    var listStyle: Int = BaseNotesDataStyle.listStyle
    var imageStyle: Int = BaseNotesDataStyle.cameraStyle

    var photoList = arrayListOf<String>()
    var taskList = arrayListOf<Task>()

    constructor(notesData: NotesData, important: Boolean, recent: Boolean, delete: Boolean) :
            this(notesData.title, notesData.text, notesData.noteId, important, recent, delete)
    {
        this.notificationId = notesData.notificationId
        this.cardBackStyle = notesData.cardBackStyle
        this.listStyle = notesData.listStyle
        this.imageStyle = notesData.imageStyle
        this.photoList = notesData.photoList
        this.taskList = notesData.taskList
    }

    private constructor(parcel: Parcel) : this()
    {
        readFromParcel(parcel)
        title = parcel.readString().toString()
        text = parcel.readString().toString()
        notificationId = parcel.readInt()
        cardBackStyle = parcel.readInt()
        listStyle = parcel.readInt()
        imageStyle = parcel.readInt()
        parcel.readStringList(photoList)
        parcel.readTypedList(taskList, Task.CREATOR)
    }

    init {
        this.title = title
        this.text = text
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeString(title)
        parcel.writeString(text)
        parcel.writeInt(notificationId)
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

    private constructor(parcel: Parcel) : this() {
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

class AudioRecorder(
    length: Long = 0, name: String = "Your audio", noteId:String = String(),
    filePath: String = String(), important: Boolean = false,
    recent: Boolean = false, delete: Boolean = false) :
    NotesListsData(noteId, important, recent, delete)
{
    var length: Long
    var name: String
    var filePath: String

    private constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
        length = parcel.readLong()
        name = parcel.readString().toString()
        filePath = parcel.readString().toString()
    }

    init {
        this.length = length
        this.name = name
        this.filePath = filePath
    }

    constructor(
        audio: AudioRecorder, important: Boolean, recent: Boolean, delete: Boolean) :
            this(audio.length, audio.name, audio.noteId, audio.filePath, important, recent, delete)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeLong(length)
        parcel.writeString(name)
        parcel.writeString(filePath)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AudioRecorder> {
        override fun createFromParcel(parcel: Parcel): AudioRecorder {
            return AudioRecorder(parcel)
        }

        override fun newArray(size: Int): Array<AudioRecorder?> {
            return arrayOfNulls(size)
        }
    }
}

object WorkWithSymbols
{
    fun generateUniqueId(): String
    {
        val size = 25
        val characterSet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_"
        val random = Random(System.nanoTime())
        val id = StringBuilder()

        for (i in 0 until size) {
            val rIndex = random.nextInt(characterSet.length)
            id.append(characterSet[rIndex])
        }
        return id.toString()
    }

    fun firstStringSymbolsOutput(str: String, countOfSymbols: Int): String
    {
        val symbolId: Int
        var subStr: String

        return if (str.length > countOfSymbols) {
            subStr = str.substring(0, countOfSymbols)
            symbolId = subStr.lastIndexOf(' ')
            if (symbolId != -1)
                subStr = subStr.substring(0, symbolId)
            "$subStr ..."
        } else str
    }

    fun addNoteToResents(note: NotesListsData)
    {
        if(!note.isRecent) {
            note.isRecent = true
            AppData.resentsNotesList.add(note)
        }
    }
}