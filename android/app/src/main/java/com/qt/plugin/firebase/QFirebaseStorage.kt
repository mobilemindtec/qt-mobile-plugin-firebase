package com.qt.plugin.firebase

import android.net.Uri
import android.util.Log
import com.qt.plugin.core.QtMobilePluginChannel
import com.qt.plugin.core.QtMobilePluginChannelMessage
import com.google.firebase.firestore.ListenerRegistration
import com.qt.plugin.core.QtMobilePluginDispatcher
import com.qt.plugin.core.QtMobilePluginChannelException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.File
import java.lang.Exception
import java.util.HashMap

class QFirebaseStorage : QtMobilePluginChannel() {

    private var currentMessage: QtMobilePluginChannelMessage? = null
    private var initOk = false
    private val listeners: Map<String, ListenerRegistration> = HashMap()
    override fun getChannelName(): String {
        return CHANNEL_NAME
    }

    override fun getCurrentMessage(): QtMobilePluginChannelMessage {
        return currentMessage!!
    }

    override fun register() {
        QtMobilePluginDispatcher.channelRegister(CHANNEL_NAME, this)
    }

    override fun callMethod(message: QtMobilePluginChannelMessage) {
        Log.i(TAG, "callMethod " + message.methodName)
        currentMessage = message
        val bucketName = message.firstArg<String>()
        when (message.methodName) {
            METHOD_INIT -> init(bucketName)
            else -> throw QtMobilePluginChannelException(
                String.format(
                    "method %s not available",
                    message.methodName
                )
            )
        }
    }

    fun init(bucketName: String) {
        if (STORAGE == null) {
            try {
                Log.i(TAG, "Storage init bucket $bucketName")
                STORAGE = FirebaseStorage.getInstance(String.format("gs://%s", bucketName))
                Log.i(TAG, "Storage OK!")
                initOk = true
            } catch (e: Exception) {
                Log.w(TAG, "Storage init error:", e)
                throw QtMobilePluginChannelException(
                    String.format(
                        "Storage init error: \$s",
                        e.message
                    ), e
                )
            }
        }
    }

    fun upload(filePath: String, key: String) {
        if (STORAGE == null) {
            throw QtMobilePluginChannelException("storage not initialized")
        }
        val storageRef = STORAGE!!.reference.child(
            key!!
        )
        val file = Uri.fromFile(File(filePath))
        val uploadTask = storageRef.putFile(file)

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
        }.addOnSuccessListener {
            // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
            // ...
        }
            .addOnProgressListener { }
    }

    //https://firebase.google.com/docs/storage/android/download-files
    fun getDownloadUrl(storageRef: StorageReference, uploadTask: UploadTask) {
        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception!!
            }

            // Continue with the task to get the download URL
            storageRef.downloadUrl
        }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                } else {
                    // Handle failures
                    // ...
                }
            }
    }

    companion object {
        private const val TAG = "QtFirebaseStorage"
        private var STORAGE: FirebaseStorage? = null
        private const val CHANNEL_NAME = "com.qt.plugin.firebase.Storage"
        private const val METHOD_INIT = "init"

    }
}