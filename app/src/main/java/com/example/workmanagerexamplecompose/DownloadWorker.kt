package com.example.workmanagerexamplecompose

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random

@Suppress("BlockingMethodInNonBlockingContext")
class DownloadWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("WORKMANAGER_STARTED", "WOW")
        startForegroundService()
        Log.d("WORKMANAGER_FOREGROUND", "WOW")
        //var imageFile = ""

        //val imageFileTest = workerParams.inputData.getString("file_uri")
        //Log.d("FILE_URI_WORKMANAGER", imageFileTest.toString())

        var imageUri = Uri.parse(workerParams.inputData.getString("file_uri"))
        var imageFile = File(imageUri.path)

        delay(5000L)

        val imageBase64 = workerParams.inputData.getString("base64_image")
        val imageByteArray: ByteArray = Base64.decode(imageBase64, Base64.DEFAULT)

        Log.d("WORKMANAGER_CONTINUED", "WOW")
        Log.d("WORKMANAGER_BYTES_SIZE", imageByteArray.size.toString())


        if (imageFile.exists()){
            Log.d("WORKMANAGER_FILE_EXISTS", "WOW")
        } else{
            Log.d("WORKMANAGER_FILE_DOESNOTEXIST", "WOW")
        }
        //THIS LINE ENDS THE WORKER PROCESS
        val outputStream = FileOutputStream(imageFile)


        return withContext(Dispatchers.IO) {
            Log.d("WORKMANAGER_BEFORE_DYING", "WOW")
            outputStream.use { stream ->
                try {
                    stream.write(imageByteArray)
                } catch (e: IOException) {
                    Log.d("WORKMANAGER_FAILURE", "WOW")

                    return@withContext Result.failure(
                        workDataOf(
                            "error" to "something went wrong"
                        )
                    )
                }
                Log.d("WORKMANAGER_SUCCESS", "WOW")
                Result.success(
                    workDataOf(
                        "success" to "image downloaded successfully"
                    )
                )

            }
        }
    }


    private suspend fun startForegroundService() {
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                NotificationCompat.Builder(context, "download_channel")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentText("Downloading...")
                    .setContentTitle("Download in progress")
                    .build()
            )
        )
    }
}