package com.example.eventure.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.eventure.data.models.Event
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AddEventViewModel @Inject constructor() : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val client = OkHttpClient()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> get() = _saveResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    fun saveEvent(event: Event, imageUris: List<Uri>, context: Context) {
        _isLoading.value = true

        if (imageUris.isEmpty()) {
            saveEventToFirestore(event.copy(imageUrls = emptyList()))
            return
        }

        val uploadedUrls = mutableListOf<String>()
        var completedUploads = 0

        imageUris.forEach { uri ->
            uploadToCloudinary(uri, context,
                onSuccess = { url ->
                    uploadedUrls.add(url)
                    completedUploads++
                    if (completedUploads == imageUris.size) {
                        saveEventToFirestore(event.copy(imageUrls = uploadedUrls))
                    }
                },
                onError = { error ->
                    _errorMessage.postValue("Upload failed: $error")
                    _isLoading.postValue(false)
                }
            )
        }
    }

    private fun uploadToCloudinary(
        uri: Uri,
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cloudName = "dixqzk9qp" // e.g. "myapp123"
        val uploadPreset = "eventure_preset" // e.g. "event_preset"

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: throw Exception("Unable to read image")
            val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())

            val request = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "image.jpg", requestBody)
                .addFormDataPart("upload_preset", uploadPreset)
                .build()

            val requestObj = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .post(request)
                .build()

            client.newCall(requestObj).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onError(e.message ?: "Unknown error")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(response.body!!.string())
                        val imageUrl = jsonResponse.getString("secure_url")
                        onSuccess(imageUrl)
                    } else {
                        onError("HTTP ${response.code}: ${response.message}")
                    }
                }
            })
        } catch (e: Exception) {
            onError(e.message ?: "Error preparing upload")
        }
    }

    private fun saveEventToFirestore(event: Event) {
        firestore.collection("events")
            .add(event)
            .addOnSuccessListener {
                _isLoading.value = false
                _saveResult.value = true
            }
            .addOnFailureListener {
                _isLoading.value = false
                _errorMessage.value = it.message
            }
    }
}