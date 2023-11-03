package com.example.workmanagerexamplecompose

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.text.SimpleDateFormat
import java.util.Date
import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.workDataOf
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt


@Composable
@Destination(start = true)
fun WorkManagerScreen(
    navigator: DestinationsNavigator,
) {

    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var request = OneTimeWorkRequestBuilder<DownloadWorker>().build()
    val coroutineScope = rememberCoroutineScope()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        coroutineScope.launch {
            if (isGranted) {
                try {

                    val fileUri = createImageFile(context)

                    val imageUri =  Uri.parse("android.resource://${context.packageName}/drawable/landscape")

                    val imageBytes = context.contentResolver.openInputStream(imageUri)?.use {
                        it.readBytes()
                    }
                    Log.d("BYTES_SIZE", imageBytes?.size.toString())

                    var reducedImageBytes = ByteArray(0)
                    imageBytes?.let {
                        reducedImageBytes = reduceImageByteSize(imageBytes = imageBytes, 5120)
                    }
                    val imageBase64 = Base64.encodeToString(reducedImageBytes, Base64.DEFAULT)

                    Log.d("REDUCED_BYTES_SIZE", imageBase64.toByteArray().size.toString())

                    request = OneTimeWorkRequestBuilder<DownloadWorker>()
                        .setInputData(
                            workDataOf(
                                "file_uri" to fileUri.toString(),
                                "base64_image" to imageBase64
                            )
                        ).build()

                    workManager
                        .enqueueUniqueWork(
                            "download",
                            ExistingWorkPolicy.KEEP,
                            request
                        )


                } catch (e : Exception){

                }
            }

        }
    }

    val workInfoById = workManager
        .getWorkInfoByIdLiveData(request.id)
        .observeAsState()
        .value

    LaunchedEffect(key1 = workInfoById?.outputData) {
        if (workInfoById?.outputData != null) {
            val outputResult = workInfoById.outputData.getString("success")
            outputResult?.let {
                Log.d("WORKMANAGER_OUTPUT_RESULT", it)
            }
        }
    }


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.landscape),
            contentDescription = ""
        )

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            modifier = Modifier
                .height(50.dp)
                .padding(start = 40.dp, end = 40.dp)
                .clip(RoundedCornerShape(15.dp)),
            onClick = {
                requestPermissionLauncher.launch(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Blue
            )
        ) {

            Text(
                text = "Download image",
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = Color.White
            )
        }
        when (workInfoById?.state) {
            WorkInfo.State.RUNNING -> Text("Downloading...")
            WorkInfo.State.SUCCEEDED -> Text("Download succeeded")
            WorkInfo.State.FAILED -> Text("Download failed")
            WorkInfo.State.CANCELLED -> Text("Download cancelled")
            WorkInfo.State.ENQUEUED -> Text("Download enqueued")
            WorkInfo.State.BLOCKED -> Text("Download blocked")
            else -> Text("No state of download")
        }
    }

}

private fun createImageFile(context: Context): Uri? {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val resolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, timeStamp)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/workmanager_pictures")
        } else {
            Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_DCIM}/workmanager_test")
        }
    }
    return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
}

private fun reduceImageByteSize(imageBytes: ByteArray, thresholdBytesSize: Int): ByteArray {

    var quality = 100
    var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    var outputBytes: ByteArray
    do {
        val outputStream = ByteArrayOutputStream()
        outputStream.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputBytes = outputStream.toByteArray()
            quality -= 10
        }
    } while (outputBytes.size > thresholdBytesSize)

    return outputBytes

}