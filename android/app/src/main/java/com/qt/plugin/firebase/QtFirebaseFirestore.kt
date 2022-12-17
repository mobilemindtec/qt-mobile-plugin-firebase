package com.qt.plugin.firebase

import android.util.Log
import com.qt.plugin.firebase.QtFirebaseFirestore
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.*
import com.qt.plugin.core.*
import kotlin.Throws
import java.lang.Exception
import java.util.*

class QtFirebaseFirestore : QtMobilePluginChannel() {

    private var currentMessage: QtMobilePluginChannelMessage? = null
    private var initOk = false
    private val listeners: MutableMap<String, ListenerRegistration> = HashMap()
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
        val collectionName = message.firstArg<String>()
        when (message.methodName) {
            METHOD_INIT -> init()
            METHOD_GET_DOCUMENTS -> getDocuments(
                collectionName,
                message.getArg(1, 0),  // limit
                message.getArg(2, 0),  // offset
                message.getArg<String?>(3, null),  // sort
                message.getArg<String?>(4, null)
            ) // order
            METHOD_ADD_DOCUMENT -> {
                val dataAdd = message.secondArg<Map<*, *>>()
                addDocument(collectionName, dataAdd)
            }
            METHOD_REMOVE_DOCUMENT -> {
                val docId = message.secondArg<String>()
                removeDocument(collectionName, docId)
            }
            METHOD_UPDATE_DOCUMENT -> {
                val dataUpdate = message.secondArg<Map<*, *>>()
                updateDocument(collectionName, dataUpdate)
            }
            METHOD_ADD_OR_UPDATE_DOCUMENT_MULTI -> {
                val dataAddOrUpdate = message.secondArg<QtVariantList>()
                addOrUpdateMulti(collectionName, dataAddOrUpdate)
            }
            METHOD_ADD_COLLECTION_LISTENER -> addCollectionListener(collectionName, message)
            METHOD_REMOVE_COLLECTION_LISTENER -> if (listeners.containsKey(message.id)) {
                listeners[message.id]!!.remove()
            }
            else -> throw QtMobilePluginChannelException(
                String.format(
                    "method %s not available",
                    message.methodName
                )
            )
        }
    }

    fun init() {
        if (FIRE_STORE == null) {
            try {
                Log.i(TAG, "Firebase init..")
                FIRE_STORE = FirebaseFirestore.getInstance()
                Log.i(TAG, "Firebase OK!")
                initOk = true
                sendResult(QtVariant.from(true))
            } catch (e: Exception) {
                Log.w(TAG, "Firebase init error:", e)
                throw QtMobilePluginChannelException(
                    String.format(
                        "Firebase init error: \$s",
                        e.message
                    ), e
                )
            }
        }
    }

    fun getDocuments(collection: String, limit: Int, offset: Int, sort: String?, order: String?) {
        Log.i(TAG, "getCollection $collection")
        if (!initOk) {
            Log.i(TAG, "firestore not init!")
            throw QtMobilePluginChannelException("Firebase not initialized")
        }
        val query: Query = FIRE_STORE!!.collection(collection)
        if (limit > 0) {
            query.limit(limit.toLong())
        }
        query.startAt(offset)
        if (sort != null) {
            query.orderBy(
                sort,
                if (order !== "desc") Query.Direction.ASCENDING else Query.Direction.DESCENDING
            )
        }
        query.get().addOnCompleteListener { task ->
            Log.i(TAG, "onComplete")
            if (task.isSuccessful) {
                Log.i(TAG, "isSuccessful")
                val results = QtVariantList()
                for (document in task.result!!) {
                    results.add(documentToQtVariant(document))
                }
                Log.i(TAG, "call c++ from java")
                sendResult(QtVariant.from(collection), results)
            } else {
                Log.w(TAG, "Error getting documents.", task.exception)
                sendError(task.exception, "firebase task error: " + task.exception!!.message)
            }
        }
        Log.i(TAG, "firestireRead java called from c++.. done")
    }

    fun removeDocument(collection: String, docId: String) {
        Log.i(TAG, "removeDocument $collection")
        if (!initOk) {
            Log.i(TAG, "firestore not init!")
            throw QtMobilePluginChannelException("Firebase not initialized")
        }

        FIRE_STORE!!.collection(collection)
            .document(docId)
            .delete()
            .addOnSuccessListener { 
                Log.d(TAG, "DocumentSnapshot successfully deleted!") 
                sendResult(QtVariant.from(collection), QtVariant.from(true))
            }
            .addOnFailureListener { 
                e -> 
                Log.w(TAG, "Error deleting document.", e)
                sendError(e, "firebase task error: " + e.message)
            }            
        
    }    

    fun addOrUpdate(collection: String, data: Map<*, *>) {
        val id = data["id"]
        if (id == null) {
            addDocument(collection, data)
        } else {
            updateDocument(collection, data)
        }
    }

    fun addDocument(collection: String, data: Map<*, *>?) {
        Log.i(TAG, "addDocument $collection")
        if (!initOk) {
            Log.i(TAG, "firestore not init!")
            throw QtMobilePluginChannelException("Firebase not initialized")
        }
        val doc = FIRE_STORE!!.collection(collection)
            .document()
        doc.set(data!!)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    sendResult(QtVariant.from(collection), QtVariant.from(doc.id))
                } else {
                    Log.w(TAG, "Error add document.", task.exception)
                    sendError(task.exception, "firebase task error: " + task.exception!!.message)
                }
            }
    }

    fun updateDocument(collection: String, data: Map<*, *>) {
        Log.i(TAG, "updateDocument $collection")
        if (!initOk) {
            Log.i(TAG, "firestore not init!")
            throw QtMobilePluginChannelException("Firebase not initialized")
        }
        val id = data["id"]
        if (id == null) {
            sendError(null, "Document id not set")
            return
        }
        val doc = FIRE_STORE!!.collection(collection)
            .document(id.toString())
        doc.set(data)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    sendResult(QtVariant.from(collection), QtVariant.from(doc.id))
                } else {
                    Log.w(TAG, "Error update document.", task.exception)
                    sendError(task.exception, "firebase task error: " + task.exception!!.message)
                }
            }
    }

    fun addOrUpdateMulti(collection: String, data: QtVariantList) {
        Log.i(TAG, "addCollection $collection")
        if (!initOk) {
            Log.i(TAG, "firestore not init!")
            throw QtMobilePluginChannelException("Firebase not initialized")
        }
        val col = FIRE_STORE!!.collection(collection)
        FIRE_STORE!!.runTransaction<Any> { transaction ->
            for (it in data) {
                val item = it as QtVariantMap
                val id = (item["id"] as QtVariant).getValue<String>()
                var docRef: DocumentReference?
                if (id != null) {
                    docRef = col.document(id.toString())
                    transaction.update(docRef, item.toMap())
                } else {
                    docRef = col.document()
                    transaction[docRef] = it
                }
                (item["id"] as QtVariant).setValue(docRef.id)
            }
            null
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                sendResult(data)
            } else {
                Log.w(TAG, "Error addOrUpdate document.", task.exception)
                sendError(task.exception, "firebase task error: " + task.exception!!.message)
            }
        }
    }

    fun addCollectionListener(collection: String?, message: QtMobilePluginChannelMessage) {
        Log.i(TAG, "addCollectionListener")
        if (listeners.containsKey(message.id)) {
            Log.i(TAG, "listener " + message.id + " already exists")
            return
        }
        val registration = FIRE_STORE!!.collection(
            collection!!
        ).addSnapshotListener { value, error ->
            if (error != null) {
                Log.w(TAG, "Error listen.", error.fillInStackTrace())
                sendError(error.fillInStackTrace(), "firebase listen error: " + error.message)
            } else {
                val data = QtVariantList()
                for (doc in value!!.documents) {
                    data.add(documentToQtVariant(doc));
                }
                sendResult(data)
            }
        }
        listeners[message.id] = registration
    }

    fun documentToQtVariant(document: DocumentSnapshot) : QtVariantMap {

        var result = QtVariantMap()

        if(document.data != null) {
            for (item in document.data!!) {
                when (item.value) {
                    is Map<*, *> -> {
                        result.putValue(
                            item.key,
                            QtVariantConverter.fromMap(item.value as Map<*, *>)
                        )
                    }
                    is List<*> -> {
                        result.putValue(
                            item.key,
                            QtVariantConverter.fromList(item.value as List<*>)
                        )
                    }
                    else -> {
                        result.putValue(item.key, item.value)
                    }
                }
            }
        }

        return result;
    }

    companion object {
        private const val TAG = "QtFirebaseFirestore"
        private var FIRE_STORE: FirebaseFirestore? = null
        private const val METHOD_INIT = "init"
        private const val CHANNEL_NAME = "com.qt.plugin.firebase.Firestore"
        private const val METHOD_GET_DOCUMENTS = "getDocuments"
        private const val METHOD_UPDATE_DOCUMENT = "updateDocument"
        private const val METHOD_ADD_DOCUMENT = "addDocument"
        private const val METHOD_REMOVE_DOCUMENT = "removeDocument"
        private const val METHOD_ADD_OR_UPDATE_DOCUMENT_MULTI = "addOrUpdateDocumentMulti"
        private const val METHOD_ADD_COLLECTION_LISTENER = "addCollectionListener"
        private const val METHOD_REMOVE_COLLECTION_LISTENER = "removeCollectionListener"
    }
}