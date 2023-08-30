package com.example.notes

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList

class AppData(private var context: Context)
{
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var json: String? = null
    private val gson = Gson()

    fun saveListData()
    {
        sharedPreferences = context.getSharedPreferences(listsSharedPrefs, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        editor.putInt(currentNotificationId, notificationId)
        editor.putBoolean(notificationChannelCreated, notificationChannelIsCreated)
        editor.putBoolean(layoutTypeToDisplay, gridType)
        editor.putBoolean(priorityDisplay, priorityDisplayText)
        putDataToEditorForSave(
            notesList, list,
            audioList, allNotesListTag,
            allAudioListTag)
        putDataToEditorForSave(
            deleteNotesList, deleteList,
            audioDeleteList, deleteNotesListTag,
            deleteAudioListTag)
        editor.apply()
    }

    private fun putDataToEditorForSave(
        list: ArrayList<NotesListsData>,
        listForNotes: ArrayList<NotesData>,
        listForAudio: ArrayList<AudioRecorder>,
        noteTag: String, audioTag: String) {
        for(i in 0 until list.size) {
            when(list[i]) {
                is NotesData -> listForNotes.add(list[i] as NotesData)
                is AudioRecorder -> listForAudio.add(list[i] as AudioRecorder)
                else -> throw IllegalArgumentException("Unknown data type")
            }
        }
        json = gson.toJson(listForNotes)
        editor.putString(noteTag, json)
        json = gson.toJson(listForAudio)
        editor.putString(audioTag, json)

        listForNotes.clear()
        listForAudio.clear()
    }

    fun loadListData() {
        sharedPreferences = context.getSharedPreferences(listsSharedPrefs, Context.MODE_PRIVATE)
        notificationId = sharedPreferences.getInt(currentNotificationId, 1)
        notificationChannelIsCreated = sharedPreferences.getBoolean(notificationChannelCreated, false)
        gridType = sharedPreferences.getBoolean(layoutTypeToDisplay, true)
        priorityDisplayText = sharedPreferences.getBoolean(priorityDisplay, true)
        val onlyNotesList = loadDataFromJson(allNotesListTag, object : TypeToken<ArrayList<NotesData>>(){}.type)
        val onlyAudioList = loadDataFromJson(allAudioListTag, object : TypeToken<ArrayList<AudioRecorder>>(){}.type)
        val onlyNotesDeleteList = loadDataFromJson(deleteNotesListTag, object : TypeToken<ArrayList<NotesData>>(){}.type)
        val onlyAudioDeleteList = loadDataFromJson(deleteAudioListTag, object : TypeToken<ArrayList<AudioRecorder>>(){}.type)
        when (priorityDisplayText) {
            true -> {
                notesList = (onlyNotesList + onlyAudioList) as ArrayList<NotesListsData>
                deleteNotesList = (onlyNotesDeleteList + onlyAudioDeleteList) as ArrayList<NotesListsData>
            }
            else -> {
                notesList = (onlyAudioList + onlyNotesList) as ArrayList<NotesListsData>
                deleteNotesList = (onlyAudioDeleteList + onlyNotesDeleteList) as ArrayList<NotesListsData>
            }
        }
    }

    private fun loadDataFromJson(tag: String, type: Type): ArrayList<NotesListsData> {
        json = sharedPreferences.getString(tag, null)
        val list = gson.fromJson<ArrayList<NotesListsData>>(json, type) ?: ArrayList()
        when {
            list.isNotEmpty() -> {
                for(i in 0 until list.size) {
                    when {
                        list[i].isDelete -> {
                            deleteNotesList.add(list[i])
                            continue
                        }
                    }
                    when { list[i].isRecent -> resentsNotesList.add(list[i]) }
                    when { list[i].isImportant -> importantNotesList.add(list[i]) }
                }
            }
        }
        return list
    }

    companion object {
        //All lists of notes, like text and audio notes
        var notesList = arrayListOf<NotesListsData>()
        var deleteNotesList = arrayListOf<NotesListsData>()
        var importantNotesList = arrayListOf<NotesListsData>()
        var resentsNotesList = arrayListOf<NotesListsData>()
        var audioList = arrayListOf<AudioRecorder>()
        var audioDeleteList = arrayListOf<AudioRecorder>()
        var list = arrayListOf<NotesData>()
        var deleteList = arrayListOf<NotesData>()

        var activeList = ActiveListForWork.allList
        var firstInput = false
        var gridType = true
        var priorityDisplayText = true
        var notificationChannelIsCreated = false
        var notificationId = -1

        //Tags to current work with note data: edit current note, show current note photo, else
        const val noteFromFragmentToActivity = "NOTE_FROM_FRAGMENT_T0_ACTIVITY"
        const val noteFromFragmentToActivityPosition = "NOTE_POSITION"
        const val notePhotoToShow = "NOTE_PHOTO"
        const val noteStyleToShow = "NOTES_STYLE"
        const val layoutTypeToDisplay = "LAYOUT_TYPE"
        const val priorityDisplay = "PRIORITY_DISPLAY"
        const val currentNotificationId = "CURRENT_NOTIFICATION_ID"
        const val notificationChannelCreated = "NOTIFICATION_CHANNEL"

        //Tags to save and load notes with sharedPreferences
        const val listsSharedPrefs = "LISTS_SHARED_PREFS"
        const val deleteAudioListTag = "DELETE_AUDIO_LIST"
        const val allAudioListTag = "ALL_AUDIO_LIST"
        const val deleteNotesListTag = "DELETE_NOTES_LIST"
        const val allNotesListTag = "ALL_NOTES_LIST"
    }
}

@SuppressLint("NonConstantResourceId")
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

object Permissions {
    const val requestCameraPermission = 100
    const val requestGalleryPermission = 101
    const val requestAudioPermission = 103
    const val requestNotificationPermission = 104
}

object Notifications {
    const val notificationChannelName = "NOTES_NOTIFICATIONS"
    const val notificationChannelId = "NOTES_NOTIFICATION_CHANNEL_ID"
    const val notificationIdTag = "NOTE_NOTIFICATION_ID"
    const val notificationTitleTag = "NOTIFICATION_TITLE_TAG"
    fun generateUniqueId() : Int
    {
        when (AppData.notificationId) {
            Int.MAX_VALUE -> AppData.notificationId = 1
            else -> AppData.notificationId++
        }
        return if (AppData.notesList.any { it is NotesData && it.notificationId == AppData.notificationId })
            generateUniqueId()
        else
            AppData.notificationId
    }
}

object AudioTimerData
{
    //Micro timer settings and current time display function
    private const val millsInSecond = 1000
    fun setTimerValue(time: Long): String
    {
        val hours = (time / millsInSecond / 3600).toInt()
        val minutes = ((time / millsInSecond % 3600) / 60).toInt()
        val seconds = (time / millsInSecond % 60).toInt()

        return when(hours) {
            0 -> String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            else -> String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
}