package com.example.mountainbiketrainer

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

// Data class to represent a file item in the UI (can be kept here or in a separate models file)
// If it's already defined elsewhere and imported in SessionScreen.kt, you don't need to redefine.
// For clarity, I'm including it here. If SessionScreen.kt already imports it from a different
// location (like your original MainViewModel), ensure you're using that same definition.
data class SessionFile(
    val name: String,
    val path: String,
    val size: Long, // in bytes
    val lastModified: Long
)

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val _sessionFiles = MutableStateFlow<List<SessionFile>>(emptyList())
    val sessionFiles: StateFlow<List<SessionFile>> = _sessionFiles.asStateFlow()

    // You might want to move API client logic to a repository or a dedicated ApiService class
    // For simplicity here, it's included, but consider abstracting it for better testability.
    // private val apiClient = YourApiClient() // Example if you had a separate API client

    init {
        loadSessionFiles() // Load files when ViewModel is created
    }

    fun loadSessionFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val directory = File(context.filesDir, "sensor_recordings")
                if (directory.exists() && directory.isDirectory) {
                    val files = directory.listFiles { file ->
                        file.isFile && file.name.startsWith("session_accel_") && file.name.endsWith(".json")
                    }?.map { file ->
                        SessionFile(
                            name = file.name,
                            path = file.absolutePath,
                            size = file.length(),
                            lastModified = file.lastModified()
                        )
                    }?.sortedByDescending { it.lastModified } ?: emptyList()
                    _sessionFiles.value = files
                } else {
                    _sessionFiles.value = emptyList()
                    Log.i("SessionViewModel", "Sensor recordings directory does not exist.")
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Error loading session files", e)
                _sessionFiles.value = emptyList()
            }
        }
    }

    fun deleteSessionFile(fileToDelete: SessionFile) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(fileToDelete.path)
                if (file.exists() && file.delete()) {
                    Log.i("SessionViewModel", "Deleted file: ${fileToDelete.name}")
                    loadSessionFiles() // Refresh the list
                } else {
                    Log.e("SessionViewModel", "Error deleting file: ${fileToDelete.name}")
                    // Optionally, communicate this error to the UI via another StateFlow
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Exception while deleting file: ${fileToDelete.name}", e)
            }
        }
    }

    fun sendSessionFileToApi(sessionFile: SessionFile) {
        viewModelScope.launch { // Keep on main thread for now, delegate to IO in actual API call
            try {
                val file = File(sessionFile.path)
                if (!file.exists()) {
                    Log.e("SessionViewModel", "File not found for API send: ${sessionFile.name}")
                    // TODO: Notify UI of this error (e.g., via a StateFlow<Event<String>>)
                    return@launch
                }

                Log.i("SessionViewModel", "Preparing to send file: ${sessionFile.name}")
                // TODO: Show loading indicator in UI

                val success = actualApiCall(sessionFile.name, file) // Extracted actual call

                if (success) {
                    Log.i("SessionViewModel", "Successfully sent ${sessionFile.name} to API")
                    // TODO: Notify UI of success (e.g., Toast, Snackbar)
                    // Optional: Delete file after successful upload or move it, then refresh list
                    // loadSessionFiles()
                } else {
                    Log.e("SessionViewModel", "Failed to send ${sessionFile.name} to API")
                    // TODO: Notify UI of failure
                }
            } catch (e: Exception) { // Catch more general exceptions from the process
                Log.e("SessionViewModel", "Error in sendSessionFileToApi for ${sessionFile.name}", e)
                // TODO: Notify UI of failure
            } finally {
                // TODO: Hide loading indicator in UI
            }
        }
    }

    // Example of how you might structure the actual network call
    private suspend fun actualApiCall(fileName: String, file: File): Boolean {
        return withContext(Dispatchers.IO) { // Perform network operation on IO dispatcher
            try {
                val fileContent = file.readText()
                // --- Replace with your actual API client logic (OkHttp, Retrofit, Ktor) ---
                Log.d("SessionViewModel", "Simulating API call for $fileName with content length: ${fileContent.length}")
                kotlinx.coroutines.delay(2000) // Simulate network delay
                // val apiClient = YourApiClient()
                // return@withContext apiClient.uploadSessionData(fileName, fileContent)
                return@withContext true // Placeholder for success
                // --- End API Sending Logic ---
            } catch (e: IOException) {
                Log.e("SessionViewModel", "IOException during file read/API call for $fileName", e)
                return@withContext false
            } catch (e: Exception) { // Catch specific network exceptions if your client throws them
                Log.e("SessionViewModel", "Network or other error during API call for $fileName", e)
                return@withContext false
            }
        }
    }
}