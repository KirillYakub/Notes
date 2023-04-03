package com.example.notes

import android.Manifest
import android.R.attr.*
import android.content.Intent
import android.content.pm.PackageManager
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random


class AddActivity : AppCompatActivity(), OnElementsClickListener, OnTaskChangeListener
{
    private lateinit var binding: ActivityAddNoteBinding
    private lateinit var photoAdapter: NotePhotoAdapter
    private lateinit var tasksAdapter: NoteTasksAdapter
    private lateinit var photoPath: String

    private var cardStyle: Int = BaseNotesDataStyle.cardStyle
    private var listStyle: Int = BaseNotesDataStyle.listStyle
    private var cameraStyle: Int = BaseNotesDataStyle.cameraStyle
    private var notePosition: Int? = null
    private var noteForEdit: NotesData? = null
    private var isOnBackPressed: Boolean = false

    private var photosForDeleteWhenActivityDestroy = arrayListOf<String>()
    private var photoList = arrayListOf<String>()
    private var taskList = arrayListOf<Task>()

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goBack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, callback)
        noteForEditConfig()
    }

    override fun onStart()
    {
        super.onStart()

        binding.apply {
            back.setOnClickListener {
                goBack()
            }
            notification.setOnClickListener {

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
        AppData.notePositionForEditWhenFragmentUpdate = notePosition
        isOnBackPressed = true
        finish()
    }

    override fun onResume()
    {
        super.onResume()

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
            if(!this::photoAdapter.isInitialized)
                createPhotoAdapter()
            else
                if(deleteList.isNotEmpty())
                    photoAdapter.deleteItemsFromList(deleteList, deleteIndexList)
        }
    }

    override fun onPause()
    {
        super.onPause()

        if(isOnBackPressed) {
            val title: String = binding.title.text.toString()
            val text = binding.text.text.toString()
            if (title.isNotEmpty() || text.isNotEmpty() ||
                photoList.isNotEmpty() || taskList.isNotEmpty()) {
                var note = if (title.isNotEmpty() && text.isEmpty())
                    NotesData(title = title)
                else if (title.isEmpty() && text.isNotEmpty())
                    NotesData(text = text)
                else
                    NotesData(title, text)

                note.cardBackStyle = cardStyle
                note.listStyle = listStyle
                note.imageStyle = cameraStyle
                note.photoList = photoList
                note.taskList = taskList

                if (noteForEdit == null) {
                    note.noteId = generateUniqueId()
                    AppData.notesList.add(note)
                } else {
                    note.noteId = noteForEdit!!.noteId
                    note = NotesData(
                        note,
                        noteForEdit!!.isImportant,
                        noteForEdit!!.isRecent,
                        noteForEdit!!.isDelete
                    )
                    editNoteAtLists(note, notePosition!!)
                }
            }
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()

        if(!isOnBackPressed && photosForDeleteWhenActivityDestroy.isNotEmpty()) {
            for(i in 0 until photosForDeleteWhenActivityDestroy.size)
                deleteFileByPath(photosForDeleteWhenActivityDestroy[i])
        }
    }

    private fun editNoteAtLists(note: NotesData, position: Int)
    {
        if(note.isImportant) {
            AppData.importantNotesList[
                    AppData.importantNotesList.indexOfFirst { it.noteId == note.noteId }] = note
        }
        if(note.isRecent)
            AppData.resentsNotesList[
                    AppData.resentsNotesList.indexOfFirst { it.noteId == note.noteId}] = note

        if(AppData.activeList == ActiveListForWork.allList)
            AppData.notesList[position] = note
        else
            AppData.notesList[AppData.notesList.indexOfFirst { it.noteId == note.noteId}] = note
    }

    private fun generateUniqueId(): String
    {
        val size = 25
        val characterSet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_"
        val random = Random(System.nanoTime())
        val id = StringBuilder()

        for (i in 0 until size)
        {
            val rIndex = random.nextInt(characterSet.length)
            id.append(characterSet[rIndex])
        }
        return id.toString()
    }

    @Throws(IOException::class)
    private fun dispatchCaptureImageIntent()
    {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File.createTempFile(generateUniqueId(), ".jpg", storageDirectory)
            photoPath = imageFile.absolutePath
            val imageUri = FileProvider.getUriForFile(
                this,
                "$packageName.file-provider", imageFile
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            cameraActivityLauncher.launch(intent)
        } else requestPermissions(
            arrayOf(Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE),
            AppData.requestCameraPermission
        )
    }

    private fun dispatchSelectImageIntent()
    {
        if (checkSelfPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryActivityLauncher.launch(intent)
        } else requestPermissions(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE),
            AppData.requestGalleryPermission
        )
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
        val imageFile = File.createTempFile(generateUniqueId(), ".jpg", storageDirectory)

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
        if(this::photoPath.isInitialized && photoPath.isNotEmpty()) {
            photoList.add(photoPath)
            if (photoList.size == 1) {
                createPhotoAdapter()
                photosForDeleteWhenActivityDestroy = photoList
            } else {
                photoAdapter.addPhoto(photoPath)
                photosForDeleteWhenActivityDestroy.add(photoPath)
            }
        }
    }

    private fun addDataToTaskList()
    {
        val task = Task()
        taskList.add(task)
        if(taskList.size == 1)
            createTaskAdapter()
        else
            tasksAdapter.addTask(task)
    }

    private fun noteForEditConfig()
    {
        val noteFromFragment = if(Build.VERSION.SDK_INT >= 33)
            intent.getParcelableExtra(
                AppData.noteFromFragmentToActivity, NotesData::class.java)
        else
            intent.getParcelableExtra(
                AppData.noteFromFragmentToActivity)

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
            notePosition = intent.getIntExtra(AppData.notePosition, 0)

            if(AppData.resentsNotesList.indexOfFirst { it.noteId == noteFromFragment.noteId} == -1) {
                noteFromFragment.isRecent = true
                AppData.resentsNotesList.add(noteFromFragment)
                if(AppData.resentsNotesList.size > AppData.maxResentsNotes)
                    AppData.resentsNotesList.removeAt(0)
            }
            if(noteFromFragment.photoList.isNotEmpty())
                photoList = noteFromFragment.photoList
            if(noteFromFragment.taskList.isNotEmpty()) {
                taskList = noteFromFragment.taskList
                createTaskAdapter()
            }
            noteForEdit = noteFromFragment
        }
    }

    private fun deleteFileByPath(path: String)
    {
        try {
            val file = File(path)
            if (file.exists())
                file.delete()
        }catch (_:IOException){
        }catch (_:IllegalArgumentException){
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

    override fun onClickListener(element: Any, position: Int) {
        if(element is Task) {
            taskList.removeAt(position)
            tasksAdapter.removeTaskByPosition(position)
        }
        else if(element is String) {
            val intent = Intent(this, PhotoActivity::class.java)
            intent.putExtra(AppData.notePhoto, element)
            startActivity(intent)
        }
    }

    override fun onLongClickListener(element: Any, position: Int) {
    }

    override fun afterTextChangedListener(text: String, position: Int) {
        taskList[position].task = text
    }

    override fun isCompleteChangeListener(change: Boolean, position: Int) {
        taskList[position].isComplete = change
    }
}