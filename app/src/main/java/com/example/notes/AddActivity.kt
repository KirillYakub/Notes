package com.example.notes

import android.Manifest
import android.R.attr.*
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notes.databinding.ActivityAddNoteBinding
import com.example.notes.databinding.NotificationDialogBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar


class AddActivity : AppCompatActivity(), OnElementsClickListener, OnTaskChangeListener
{
    private lateinit var binding: ActivityAddNoteBinding
    private lateinit var notificationBinding: NotificationDialogBinding
    private lateinit var photoAdapter: NotePhotoAdapter
    private lateinit var tasksAdapter: NoteTasksAdapter
    private lateinit var noteAfterEdit: NotesData
    private lateinit var photoPath: String
    private lateinit var notificationDialog: Dialog

    private var instanceStateObjectPositionTag = "INSTANCE_STATE_OBJECT_POSITION_TAG"
    private var instanceStateObjectTag = "INSTANCE_STATE_TAG"
    private var saveNewNoteAfterPause: Boolean = false
    private var cardStyle: Int = BaseNotesDataStyle.cardStyle
    private var listStyle: Int = BaseNotesDataStyle.listStyle
    private var cameraStyle: Int = BaseNotesDataStyle.cameraStyle
    private var notePosition: Int? = null
    private var noteBeforeEdit: NotesData? = null

    private lateinit var dateList: ArrayList<String>
    private lateinit var hoursList: ArrayList<String>
    private lateinit var minutesList: ArrayList<String>
    private var photoList = arrayListOf<String>()
    private var taskList = arrayListOf<Task>()
    private val appData = AppData(this)

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goBack()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if(this::noteAfterEdit.isInitialized) {
            when (noteBeforeEdit) {
                null -> outState.putInt(instanceStateObjectPositionTag, AppData.notesList.size - 1)
                else -> outState.putInt(instanceStateObjectPositionTag, notePosition!!)
            }
            outState.putParcelable(instanceStateObjectTag, noteAfterEdit)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        notificationBinding = NotificationDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, callback)
        noteForEditConfig(savedInstanceState)
    }

    override fun onStart()
    {
        super.onStart()

        binding.apply {
            back.setOnClickListener {
                goBack()
            }
            notification.setOnClickListener {
                createNotificationDialog()
            }
            camera.setOnClickListener {
                dispatchCaptureImageIntent()
            }
            list.setOnClickListener {
                addDataToTaskList()
            }
            gallery.setOnClickListener {
                dispatchSelectImageIntent()
            }
            palette.setOnClickListener {
                menu.visibility = View.GONE
                colors.visibility = View.VISIBLE
            }
            closePanel.setOnClickListener {
                menu.visibility = View.VISIBLE
                colors.visibility = View.GONE
            }
            orange.setOnClickListener {
                setOrangeTheme()
            }
            blue.setOnClickListener {
                setBlueTheme()
            }
            green.setOnClickListener {
                setGreenTheme()
            }
            yellow.setOnClickListener {
                setYellowTheme()
            }
            paper.setOnClickListener {
                setPaperTheme()
            }
        }
    }

    private fun goBack()
    {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume()
    {
        super.onResume()

        /*
        If we paused our activity and resumed it after that we need to check are we work with new
        note before it.
        */
        if(saveNewNoteAfterPause) {
            noteForEditConfig()
            saveNewNoteAfterPause = false
        }
        /*
        This is a save from display photos that was deleted by user not in program.
        If user paused app and delete photos in folders and after that returned to app
        we need to check that files exists yet
        */
        if(photoList.isNotEmpty()) {
            val deleteList = arrayListOf<String>()
            val deleteIndexList = arrayListOf<Int>()
            for(i in 0 until photoList.size) {
                try {
                    val fileName = File(photoList[i])
                    if (!fileName.exists()) {
                        deleteList.add(photoList[i])
                        deleteIndexList.add(i)
                    }
                }catch (_:IllegalArgumentException){
                }
            }
            photoList.removeAll(deleteList.toSet())
            when{
                !this::photoAdapter.isInitialized -> createPhotoAdapter()
                else -> if(deleteList.isNotEmpty())
                    photoAdapter.deleteItemsFromList(deleteList, deleteIndexList)
            }
        }
        /*
        User can not set notification for not created note. For it note must be added to main
        notifications list
        */
        if(noteBeforeEdit == null) binding.notification.visibility = View.GONE
    }

    override fun onPause()
    {
        super.onPause()

        val title: String = binding.title.text.toString()
        val text = binding.text.text.toString()
        if (title.isNotEmpty() || text.isNotEmpty() ||
            photoList.isNotEmpty() || taskList.isNotEmpty()) {
            var note = when {
                title.isNotEmpty() && text.isEmpty() -> NotesData(title = title)
                title.isEmpty() && text.isNotEmpty() -> NotesData(text = text)
                else -> NotesData(title, text)
            }

            note.cardBackStyle = cardStyle
            note.listStyle = listStyle
            note.imageStyle = cameraStyle
            note.photoList = photoList
            note.taskList = taskList

            /*
            When we create note or edit old - that is not important, we create a local note with input
            data and if we add new note we add this local note to our note list. If we edit note we
            change our old note to new with update data. Important to know that we need to save data
            to savedInstanceState if we change theme to night for example. For that we save note to
            noteAfterEdit field and use it in correct method.
            */
            if (noteBeforeEdit == null) {
                note.noteId = WorkWithSymbols.generateUniqueId()
                AppData.notesList.add(note)
                saveNewNoteAfterPause = true
            } else {
                note.noteId = noteBeforeEdit!!.noteId
                note.notificationId = noteBeforeEdit!!.notificationId
                note = NotesData(
                    note,
                    noteBeforeEdit!!.isImportant,
                    noteBeforeEdit!!.isRecent,
                    noteBeforeEdit!!.isDelete
                )
                editNoteAtLists(note, notePosition!!)
            }
            noteAfterEdit = note
        }
        else
            if(noteBeforeEdit != null) noteAfterEdit = noteBeforeEdit!!

        appData.saveListData()
    }

    private fun editNoteAtLists(note: NotesData, position: Int)
    {
        //When our note stored in different lists we need to edit it in every list if we have changes in note data
        if(note.isImportant)
            AppData.importantNotesList[
                    AppData.importantNotesList.indexOfFirst { it.noteId == note.noteId }] = note
        if(note.isRecent)
            AppData.resentsNotesList[
                    AppData.resentsNotesList.indexOfFirst { it.noteId == note.noteId}] = note
        when(AppData.activeList) {
            ActiveListForWork.allList -> AppData.notesList[position] = note
            else -> AppData.notesList[AppData.notesList.indexOfFirst { it.noteId == note.noteId}] = note
        }
    }

    @Throws(IOException::class)
    private fun dispatchCaptureImageIntent()
    {
        if (makePhotoCurrentVersionPermission()) {
            val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File.createTempFile(WorkWithSymbols.generateUniqueId(), ".jpg", storageDirectory)
            photoPath = imageFile.absolutePath
            val imageUri = FileProvider.getUriForFile(
                this,
                "$packageName.file-provider", imageFile
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            cameraActivityLauncher.launch(intent)
        }
    }

    private fun selectImageCurrentVersionPermission() : Boolean
    {
        return when {
            Build.VERSION.SDK_INT >= 33 -> {
                when (PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) -> true
                    else -> {
                        requestPermissions(arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES),
                            Permissions.requestGalleryPermission)
                        false
                    }
                }
            }
            else -> {
                when {
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> true
                    else -> {
                        requestPermissions(arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            Permissions.requestGalleryPermission)
                        false
                    }
                }
            }
        }
    }

    private fun makePhotoCurrentVersionPermission() : Boolean
    {
        return when {
            Build.VERSION.SDK_INT >= 33 -> {
                when  {
                    checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                            checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED -> true
                    else -> {
                        requestPermissions(arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_MEDIA_IMAGES),
                            Permissions.requestCameraPermission)
                        false
                    }
                }
            }
            else -> {
                when {
                    checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> true
                    else -> {
                        requestPermissions(arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            Permissions.requestCameraPermission)
                        false
                    }
                }
            }
        }
    }

    private fun dispatchSelectImageIntent()
    {
        if (selectImageCurrentVersionPermission()) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryActivityLauncher.launch(intent)
        }
    }

    private val cameraActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
            result -> if (result.resultCode == RESULT_OK)
                addImagesToPhotoList()
    }

    private val galleryActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
            result -> if (result.resultCode == RESULT_OK) {
                val data = result.data
                val uri = data!!.data
                if (uri != null)
                    copyFileByUri(uri)
            }
    }

    private fun copyFileByUri(uri: Uri)
    {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(WorkWithSymbols.generateUniqueId(), ".jpg", storageDirectory)

        val inputStream = contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(imageFile)
        val buffer = ByteArray(4 * 1024)
        var bytesRead: Int
        while (inputStream!!.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        outputStream.flush()
        inputStream.close()
        outputStream.close()

        //We need to get path to photo to local field if it was copied successfully
        if (imageFile.length() > 0) {
            photoPath = imageFile.absolutePath
            addImagesToPhotoList()
        } else {
            try {
                imageFile.delete()
                Toast.makeText(
                    this, "Sorry, something went wrong", Toast.LENGTH_SHORT
                ).show()
            } catch (_:IOException){
            }
        }
    }

    private fun createPhotoAdapter()
    {
        photoAdapter = NotePhotoAdapter(this)
        binding.photoView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.photoView.adapter = photoAdapter
        photoAdapter.addList(photoList)
    }

    private fun createTaskAdapter()
    {
        tasksAdapter = NoteTasksAdapter(this, this)
        binding.taskView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.taskView.adapter = tasksAdapter
        tasksAdapter.addList(taskList)
    }

    private fun addImagesToPhotoList()
    {
        //We can add photo to list only if photo was created, so we have a path to this photo
        if(photoPath.isNotEmpty()) {
            photoList.add(photoPath)
            when {
                //If out note has photoList we need to create adapter to display it
                !this::photoAdapter.isInitialized -> createPhotoAdapter()
                else -> photoAdapter.addPhoto(photoPath)
            }
        }
    }

    private fun addDataToTaskList()
    {
        val task = Task()
        taskList.add(task)
        when {
            //If out note has taskList we need to create adapter to display it
            !this::tasksAdapter.isInitialized -> createTaskAdapter()
            else -> tasksAdapter.addTask(task)
        }
    }

    private fun noteForEditConfig(savedInstanceState: Bundle? = null)
    {
        val noteFromFragment: NotesData? = when {
            /*
            When savedInstanceState is initialized we get data from it like notePosition from list
            to edit and note like object for edit
            */
            savedInstanceState != null -> {
                notePosition = savedInstanceState.getInt(instanceStateObjectPositionTag)
                savedInstanceState.getParcelable(instanceStateObjectTag)
            }
            /*
            When we pause our app note data will save to list and sharedPreferences...
            But if we reopen our activity and note is new in our list, that is we do not edit note,
            that note will have a duplicate in list. For that when note was created at first time
            we need to know that if page will be reopen we will work with this note like we want
            to edit it.
             */
            saveNewNoteAfterPause -> {
                notePosition = AppData.notesList.size - 1
                AppData.notesList[notePosition!!] as NotesData
            }
            //If we want to edit note we get data with intent from out fragment with list of notes
            else -> {
                notePosition = intent.getIntExtra(AppData.noteFromFragmentToActivityPosition, 0)
                when {
                    Build.VERSION.SDK_INT >= 33 -> intent.getParcelableExtra(
                        AppData.noteFromFragmentToActivity, NotesData::class.java
                    )
                    else -> intent.getParcelableExtra(AppData.noteFromFragmentToActivity)
                }
            }
        }

        /*
        We get data about our note for edit to
        our local activity fields because every note has it own style and lists like photo and tasks
        */
        if(noteFromFragment != null) {
            binding.apply {
                title.setText(noteFromFragment.title)
                text.setText(noteFromFragment.text)
                when(noteFromFragment.cardBackStyle) {
                    R.color.orange -> setOrangeTheme()
                    R.color.blue -> setBlueTheme()
                    R.color.green -> setGreenTheme()
                    R.color.yellow -> setYellowTheme()
                    R.color.paper -> setPaperTheme()
                }
            }

            WorkWithSymbols.addNoteToResents(noteFromFragment) //Add note to recent after open it
            if(noteFromFragment.photoList.isNotEmpty())
                photoList = noteFromFragment.photoList
            if(noteFromFragment.taskList.isNotEmpty()) {
                taskList = noteFromFragment.taskList
                createTaskAdapter()
            }
            noteBeforeEdit = noteFromFragment
        }
    }

    private fun setNoteTheme(colBack: Int, colNotific: Int, colCamera: Int, colList: Int, colGallery: Int,
                             colPalette: Int, colOfColMenu: Int, colOfMenu: Int, colOfMainPanel: Int)
    {
        cardStyle = colOfMainPanel
        listStyle = colList
        cameraStyle = colCamera

        binding.apply {
            back.setImageResource(colBack)
            notification.setImageResource(colNotific)
            camera.setImageResource(colCamera)
            list.setImageResource(colList)
            gallery.setImageResource(colGallery)
            palette.setImageResource(colPalette)
            colors.setBackgroundResource(colOfColMenu)
            menu.setBackgroundResource(colOfMenu)
            mainPanel.setBackgroundResource(colOfMainPanel)
        }
    }

    private fun setOrangeTheme()
    {
        setNoteTheme(R.drawable.orange_back, R.drawable.orange_notification, R.drawable.orange_camera
            , R.drawable.orange_list, R.drawable.orange_gallery, R.drawable.orange_palette
            , R.drawable.back_orange, R.drawable.back_orange, R.color.orange)
    }

    private fun setGreenTheme()
    {
        setNoteTheme(R.drawable.green_back, R.drawable.green_notification, R.drawable.green_camera
            , R.drawable.green_list, R.drawable.green_gallery, R.drawable.green_palette
            , R.drawable.back_green, R.drawable.back_green, R.color.green)
    }

    private fun setBlueTheme()
    {
        setNoteTheme(R.drawable.blue_back, R.drawable.blue_notification, R.drawable.blue_camera
            , R.drawable.blue_list, R.drawable.blue_gallery, R.drawable.blue_palette
            , R.drawable.back_blue, R.drawable.back_blue, R.color.blue)
    }

    private fun setYellowTheme()
    {
        setNoteTheme(R.drawable.yellow_back, R.drawable.yellow_notification, R.drawable.yellow_camera
            , R.drawable.yellow_list, R.drawable.yellow_gallery, R.drawable.yellow_palette
            , R.drawable.back_yellow, R.drawable.back_yellow, R.color.yellow)
    }

    private fun setPaperTheme()
    {
        setNoteTheme(R.drawable.paper_back, R.drawable.paper_notification, R.drawable.paper_camera
            , R.drawable.paper_list, R.drawable.paper_gallery, R.drawable.paper_palette
            , R.drawable.back_paper, R.drawable.back_paper, R.color.paper)
    }

    override fun onNoteElementsClickListener(element: Any, position: Int) {
        if(element is Task) {
            taskList.removeAt(position)
            tasksAdapter.removeTaskByPosition(position)
        }
        else if(element is String) {
            val intent = Intent(this, PhotoActivity::class.java)
            intent.putExtra(AppData.notePhotoToShow, element)
            intent.putExtra(AppData.noteStyleToShow, cardStyle)
            startActivity(intent)
        }
    }

    override fun onClickListener(element: NotesListsData, position: Int) {
    }

    override fun onLongClickListener(element: NotesListsData, position: Int) {
    }

    override fun afterTextChangedListener(text: String, position: Int) {
        taskList[position].task = text
    }

    override fun isCompleteChangeListener(change: Boolean, position: Int) {
        taskList[position].isComplete = change
    }

    private fun generateAllDatesOfYear()
    {
        dateList = arrayListOf()
        var currentDate = LocalDateTime.now()
        val nextYear = currentDate.plusYears(1)
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")
        while (currentDate.isBefore(nextYear)) {
            dateList.add(currentDate.format(formatter))
            currentDate = currentDate.plusDays(1)
        }
    }

    private fun generateMinuteArray()
    {
        minutesList = arrayListOf()
        for (minute in 0..59)
            minutesList.add(minute.toString().padStart(2, '0'))
    }

    private fun generateHoursArray()
    {
        hoursList = arrayListOf()
        for (hour in 0..23)
            hoursList.add(hour.toString().padStart(2, '0'))
    }

    private fun createNotificationDialog()
    {
        if(!this::notificationDialog.isInitialized)
        {
            notificationDialog = Dialog(this)
            notificationDialog.setContentView(notificationBinding.root)
            notificationDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            generateAllDatesOfYear()
            generateHoursArray()
            generateMinuteArray()
            setNotificationPickersValues()
        }
        openNotificationDialog()
    }

    private fun setNotificationPickersValues()
    {
        notificationBinding.apply {
            days.minValue = 0
            days.maxValue = dateList.size - 1
            days.displayedValues = dateList.toTypedArray()
            days.wrapSelectorWheel = false

            hours.minValue = 0
            hours.maxValue = hoursList.size - 1
            hours.displayedValues = hoursList.toTypedArray()
            hours.wrapSelectorWheel = false

            minutes.minValue = 0
            minutes.maxValue = minutesList.size - 1
            minutes.displayedValues = minutesList.toTypedArray()
            minutes.wrapSelectorWheel = false
        }
    }

    private fun openNotificationDialog()
    {
        if(setNotificationPermission()) {
            notificationBinding.apply {
                when (noteBeforeEdit!!.notificationId) {
                    0 -> notificationSwitcher.isChecked = false
                    else -> notificationSwitcher.isChecked = true
                }
                notificationSwitcher.setOnCheckedChangeListener { _, isChecked ->
                    when {
                        isChecked -> {
                            val date = dateList[days.value].split(".")
                            val numbers = date.map { it.toInt() }
                            val calendar = Calendar.getInstance()
                            calendar.set(
                                ("20" + numbers[2]).toInt(), numbers[1] - 1, numbers[0],
                                hoursList[hours.value].toInt(), minutesList[minutes.value].toInt(), 0
                            )
                            when (Calendar.getInstance().before(calendar)) {
                                true -> scheduleNotification(calendar.timeInMillis, true)
                                else -> notificationSwitcher.isChecked = false
                            }
                        }
                        else -> scheduleNotification(alarmOn = false)
                    }
                }
            }
            notificationDialog.show()
        }
    }

    private fun scheduleNotification(time: Long = 0, alarmOn: Boolean)
    {
        val notificationId = if(alarmOn) Notifications.generateUniqueId() else noteBeforeEdit!!.notificationId
        val intent = Intent(applicationContext, NotificationsReceiver::class.java)
        intent.putExtra(
            Notifications.notificationIdTag,
            notificationId)
        intent.putExtra(
            Notifications.notificationTitleTag,
            when {
                noteBeforeEdit!!.title.isNotEmpty() ||
                        noteBeforeEdit!!.text.isNotEmpty() -> getString(R.string.notification_name)
                noteBeforeEdit!!.taskList.isNotEmpty() -> getString(R.string.notification_task_name)
                else -> getString(R.string.notification_photo_name)
            })
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if(alarmOn) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                pendingIntent
            )
        }
        else alarmManager.cancel(pendingIntent)
        noteBeforeEdit!!.notificationId = if (alarmOn) notificationId else 0
    }

    private fun setNotificationPermission() : Boolean
    {
        return when {
            Build.VERSION.SDK_INT >= 33 -> {
                when (PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) -> true
                    else -> {
                        requestPermissions(arrayOf(
                            Manifest.permission.POST_NOTIFICATIONS),
                            Permissions.requestNotificationPermission)
                        false
                    }
                }
            }
            else -> true
        }
    }
}