package com.example.notes

import android.Manifest
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.notes.databinding.ActivityMainMenuBinding
import com.example.notes.databinding.AudioItemDialogBinding
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), OnDataSentListener
{
    private lateinit var binding: ActivityMainMenuBinding
    private lateinit var audioBinding: AudioItemDialogBinding
    private lateinit var fragment: ListsFragment
    private lateinit var audioDialog: Dialog
    private lateinit var audioPath: String

    private val appData = AppData(this)
    private var isRecordingPaused: Boolean = false
    private var isPlayerPlaying: Boolean = false
    private var savePanelIsVisible: Boolean = false
    private var audioNote: AudioRecorder? = null
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var playerPos: Int? = null
    private var timerValueInMills: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        audioBinding = AudioItemDialogBinding.inflate(layoutInflater)
        binding = ActivityMainMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppData.activeList = ActiveListForWork.allList
        if(!AppData.firstInput) {
            AppData.firstInput = true
            appData.loadListData()
        }
        if(savedInstanceState == null && !this::fragment.isInitialized) {
            fragment = ListsFragment.newInstance(AppData.notesList)
            supportFragmentManager
                .beginTransaction()
                .add(binding.fragmentsView.id, fragment)
                .commit()
        }
        createNotificationChannel()
        quantityOfElementsUIUpdate(AppData.notesList.size)
    }

    override fun onStart()
    {
        super.onStart()
        val context = this

        binding.apply {
            add.setOnClickListener {
                val addIntent = Intent(context, AddActivity::class.java)
                startActivity(addIntent)
                finish()
            }
            addAudio.setOnClickListener {
                openAudioDialog()
            }
            all.setOnClickListener { setMainFragment() }
            important.setOnClickListener{
                setFragment(AppData.importantNotesList)
                AppData.activeList = ActiveListForWork.importantList
            }
            recents.setOnClickListener{
                setFragment(AppData.resentsNotesList)
                AppData.activeList = ActiveListForWork.resentsList
            }
            trash.setOnClickListener{
                setFragment(AppData.deleteNotesList)
                AppData.activeList = ActiveListForWork.deleteList
            }
            menu.setOnClickListener {
                val intent = Intent(context, OptionsActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onPause()
    {
        super.onPause()

        when {
            recorder != null || savePanelIsVisible -> {
                Thread { stopRecording() }.start()
                stopMicroInterface()
                saveNewAudio()
                cancelDialog()
            }
            player != null -> {
                Thread { stopPlaying() }.start()
                stopMicroInterface()
                cancelDialog()
            }
        }
        appData.saveListData()
    }

    private fun setMainFragment()
    {
        setFragment(AppData.notesList)
        AppData.activeList = ActiveListForWork.allList
    }

    private fun setFragment(noteList: ArrayList<NotesListsData>)
    {
        fragment = ListsFragment.newInstance(noteList)
        supportFragmentManager
            .beginTransaction()
            .replace(binding.fragmentsView.id, fragment)
            .commit()
    }

    private fun recordAudioCurrentVersionPermission() : Boolean
    {
        return when {
            Build.VERSION.SDK_INT >= 33 -> {
                when {
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            && checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED -> true
                    else -> {
                        requestPermissions(arrayOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_MEDIA_AUDIO),
                            Permissions.requestAudioPermission)
                        false
                    }
                }
            }
            else -> {
                when {
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> true
                    else -> {
                        requestPermissions(
                            arrayOf(Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            Permissions.requestAudioPermission)
                        false
                    }
                }
            }
        }
    }

    private fun openAudioDialog()
    {
        if(recordAudioCurrentVersionPermission()) {
            if(!this::audioDialog.isInitialized) {
                audioDialog = Dialog(this)
                audioDialog.setCanceledOnTouchOutside(false)
                audioDialog.setCancelable(false)
                audioDialog.setContentView(audioBinding.root)
                audioDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
            openAudioDialogView()
        }
    }

    private fun openAudioDialogView()
    {
        audioBinding.apply {
            if(audioNote != null) {
                WorkWithSymbols.addNoteToResents(audioNote!!)
                startStop.visibility = View.GONE
            }
            startStop.setOnClickListener {
                Thread {
                    when (recorder) {
                        null -> {
                            startRecording()
                            runOnUiThread {
                                startMicroInterface()
                                startStop.setImageResource(R.drawable.audio_stop)
                                exit.visibility = View.INVISIBLE
                            }
                        }
                        else -> {
                            stopRecording()
                            runOnUiThread {
                                stopMicroInterface()
                                startStop.setImageResource(R.drawable.audio_start)
                                recorderPanel.visibility = View.GONE
                                savePanel.visibility = View.VISIBLE
                            }
                            savePanelIsVisible = true
                        }
                    }
                }.start()
            }
            playPause.setOnClickListener {
                Thread {
                    when (audioNote) {
                        null -> {
                            if (recorder != null) {
                                when (isRecordingPaused) {
                                    true -> {
                                        recorder!!.resume()
                                        isRecordingPaused = false
                                        runOnUiThread {
                                            startMicroInterface()
                                            playPause.setImageResource(R.drawable.audio_pause)
                                        }
                                    }
                                    else -> {
                                        recorder!!.pause()
                                        isRecordingPaused = true
                                        runOnUiThread {
                                            stopMicroInterface()
                                            playPause.setImageResource(R.drawable.audio_play)
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            when (player) {
                                null -> {
                                    startPlaying()
                                    runOnUiThread {
                                        startMicroInterface()
                                        playPause.setImageResource(R.drawable.audio_pause)
                                    }
                                    player!!.setOnCompletionListener {
                                        Thread { stopPlaying() }.start()
                                        stopMicroInterface()
                                        timerValueInMills = 0L
                                    }
                                }
                                else -> {
                                    when (isPlayerPlaying) {
                                        true -> {
                                            playerPos = player!!.currentPosition
                                            player!!.stop()
                                            runOnUiThread {
                                                stopMicroInterface()
                                                playPause.setImageResource(R.drawable.audio_play)
                                            }
                                            isPlayerPlaying = false
                                        }
                                        else -> {
                                            player!!.prepare()
                                            player!!.seekTo(playerPos!!)
                                            player!!.start()
                                            runOnUiThread {
                                                startMicroInterface()
                                                playPause.setImageResource(R.drawable.audio_pause)
                                            }
                                            isPlayerPlaying = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.start()
            }
            save.setOnClickListener {
                saveNewAudio()
                cancelDialog()
            }
            cancel.setOnClickListener {
                try {
                    val file = File(audioPath)
                    if(file.exists())
                        file.delete()
                } catch (_: IOException) {
                } catch (_: IllegalArgumentException) {
                } finally {
                    cancelDialog()
                }
            }
            exit.setOnClickListener {
                if(player != null) {
                    Thread { stopPlaying() }.start()
                    stopMicroInterface()
                }
                cancelDialog()
            }
        }
        audioDialog.show()
    }

    private fun startRecording()
    {
        try {
            recorder = when {
                Build.VERSION.SDK_INT >= 31 -> MediaRecorder(this)
                else -> MediaRecorder()
            }
            recorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            val audio = File.createTempFile(WorkWithSymbols.generateUniqueId(), ".mp3", storageDirectory)
            audioPath = audio.absolutePath
            recorder!!.setOutputFile(audioPath)
            recorder!!.prepare()
            recorder!!.start()
        } catch (_: Exception) {
        }
    }

    private fun stopRecording()
    {
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            isRecordingPaused = false
        } catch (_: Exception) {
        }
    }

    private fun stopPlaying()
    {
        try {
            if(isPlayerPlaying) {
                player!!.stop()
                player!!.release()
            }
            player = null
            isPlayerPlaying = false
        } catch (_: Exception) {
        }
    }

    private fun startPlaying()
    {
        if(audioNote!= null) {
            try {
                player = MediaPlayer()
                val audio = File(audioNote!!.filePath)
                player!!.setDataSource(audio.absolutePath)
                player!!.prepare()
                player!!.start()
                isPlayerPlaying = true
            } catch (_: Exception) {
            }
        }
    }

    private fun startStopwatch()
    {
        audioBinding.apply {
            when(timerValueInMills) {
                0L -> chronometer.base = SystemClock.elapsedRealtime()
                else -> chronometer.base = SystemClock.elapsedRealtime() - timerValueInMills
            }
            chronometer.start()
        }
    }

    private fun stopStopwatch()
    {
        audioBinding.apply {
            chronometer.stop()
            timerValueInMills = SystemClock.elapsedRealtime() - chronometer.base
        }
    }

    private fun cancelDialog()
    {
        if(this::audioDialog.isInitialized)
        {
            audioDialog.cancel()
            audioBinding.apply {
                playPause.setImageResource(R.drawable.audio_play)
                startStop.visibility = View.VISIBLE
                recorderPanel.visibility = View.VISIBLE
                exit.visibility = View.VISIBLE
                savePanel.visibility = View.GONE
            }
            audioNote = null
            playerPos = null
            timerValueInMills = 0L
            savePanelIsVisible = false
        }
    }

    private fun startMicroInterface()
    {
        startStopwatch()
        audioBinding.audioAnim.playAnimation()
    }

    private fun stopMicroInterface()
    {
        stopStopwatch()
        audioBinding.audioAnim.cancelAnimation()
    }

    override fun dataFromFragmentToActivity(data: NotesListsData)
    {
        when(data) {
            is AudioRecorder -> {
                audioNote = data
                openAudioDialog()
            }
            else -> throw IllegalArgumentException("Data must have AudioRecorder type")
        }
    }

    private fun saveNewAudio()
    {
        val text = audioBinding.audioName.text.toString()
        val newAudio = when {
            text.isEmpty() -> AudioRecorder(length = timerValueInMills,
                noteId = WorkWithSymbols.generateUniqueId(), filePath = audioPath)
            else -> AudioRecorder(
                timerValueInMills, text, WorkWithSymbols.generateUniqueId(), audioPath)
        }
        AppData.notesList.add(newAudio)
        if(AppData.activeList == ActiveListForWork.allList)
            fragment.sentDataToFragment(newAudio)
    }

    private fun createNotificationChannel()
    {
        if(!AppData.notificationChannelIsCreated)
        {
            AppData.notificationChannelIsCreated = true
            val channel = NotificationChannel(Notifications.notificationChannelId,
                Notifications.notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun quantityOfElementsUIUpdate(quantity: Int)
    {
        if(!isDestroyed)
            binding.elementsQuantity.text = quantity.toString()
    }
}